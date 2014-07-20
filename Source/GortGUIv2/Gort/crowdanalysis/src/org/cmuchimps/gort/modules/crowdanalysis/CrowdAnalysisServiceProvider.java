/*
   Copyright 2014 Shahriyar Amini

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.cmuchimps.gort.modules.crowdanalysis;

import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.swing.SwingUtilities;
import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.cmuchimps.gort.api.gort.ProjectUtility;
import org.cmuchimps.gort.api.gort.TraversalProviderService;
import org.cmuchimps.gort.api.gort.WebInfoService;
import org.cmuchimps.gort.api.gort.analysis.CrowdAnalysisService;
import org.cmuchimps.gort.api.gort.heuristic.AbstractHeuristic;
import org.cmuchimps.gort.modules.crowdanalysis.tasks.AppTaskExtraction;
import org.cmuchimps.gort.modules.dataobject.Activity;
import org.cmuchimps.gort.modules.dataobject.App;
import org.cmuchimps.gort.modules.dataobject.CrowdTask;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.dataobject.HIT;
import org.cmuchimps.gort.modules.dataobject.History;
import org.cmuchimps.gort.modules.dataobject.Screenshot;
import org.cmuchimps.gort.modules.dataobject.State;
import org.cmuchimps.gort.modules.dataobject.Traversal;
import org.cmuchimps.gort.modules.helper.AndroidPermissions;
import org.cmuchimps.gort.modules.helper.DateHelper;
import org.cmuchimps.gort.modules.helper.FileHelper;
import org.cmuchimps.gort.modules.helper.ImageHelper;
import org.cmuchimps.gort.modules.helper.TaintHelper;
import org.netbeans.api.project.Project;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileAlreadyLockedException;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author shahriyar
 */
public class CrowdAnalysisServiceProvider extends CrowdAnalysisService implements FileChangeListener {
    // TODO: this class needs to be organized
    
    // Max out the number of crowd tasks per app. We do not want to spend more than $100
    // each crowdtask can take up to $6 dollars
    private static final int MAX_CROWD_TASKS_PER_APP = 16;
    //private static final int MAX_CROWD_TASKS_PER_APP = 25;
    
    private static final String KEY_ACCESS_KEY = "access_key";
    private static final String KEY_SECRET_KEY = "secret_key";
    
    private static final int DELAY_BEFORE_MONITORING_MILLISECONDS = 1 * 60 * 1000;
    
    // upload a maximum of this many screenshots
    private static final int APP_SCREENSHOTS_UPLOAD_THRESHOLD = 2;
    
    // variables to indicate similar prior tasks created
    private static final String SIMILAR_TASK_NONE = "no_prior_task";
    private static final String SIMILAR_TASK_IN_BATCH = "prior_task_in_batch";
    private static final String SIMILAR_TASK_OUT_OF_BATCH = "prior_task_in_flight";
    
    private RequesterService service;
    
    // support about 10 open projects. In reality this would be limited by number of db connections
    // TODO: find a realistic number based on possible db connections.
    private RequestProcessor RP = new RequestProcessor(CrowdAnalysisServiceProvider.class.getName(), 10, true);
    
    public CrowdAnalysisServiceProvider(Project project) {
        super(project);
    }
    
    @NbBundle.Messages({"MSG_InvalidMTurkProperties=Please make sure crowdtasks/mturk.properties exists and has valid access keys.",
    "MSG_ExisitingCrowdTask=There is an existing crowd task with the associated application state in flight. Would you like to create an additional crowd task?",
    "MSG_NotEnoughFunds=There is not enough funds to run crowd tasks.",
    "MSG_NoAppTraversal=You have to perform Dynamic Analysis prior to doing Crowd Analysis.",
    "MSG_NoAppHistory=The app traversal is empty. Try doing Dynamic Analysis again or switching the main traversal.",
    "MSG_NoAppName=The app you have selected for crowd analysis does not have a name associated with it. Try running static analysis again.",
    "MSG_NoAppDescription=The app you have selected for crowd analysis does not have a description associated with it Try running static analysis again.",
    "MSG_NoAppScreenshot=The app you have selected for crowd analysis does not have any screenshots. Try running static analysis again.",
    "MSG_ReachedAppBudget=Crowd Analysis has reached the budget for the selected app. Additional HITs up to the budget may have been created."})
    @Override
    public void analyze(FileObject apk) {
        if (apk == null || !apk.canRead()) {
            return;
        }
        
        analyzeThreaded(apk);
    }
    
    private void analyzeThreaded(final FileObject apk) {
        Thread t = new Thread() {
            @Override
            public void run() {
                // check the mturk.properties file
                if (!checkProperties()) {
                    System.out.println("Mechanical Turk properties check failed.");
                    displayInformationMessage("MSG_InvalidMTurkProperties");
                    return;
                }

                // make sure the account has funds to crowd source
                if (!hasEnoughFund()) {
                    System.out.println("Mechanical Turk account does not have enough funds.");
                    displayInformationMessage("MSG_NotEnoughFunds");
                    return;
                }
                
                // get the main traversal folder for the project
                TraversalProviderService tps = project.getLookup().lookup(TraversalProviderService.class);
                
                if (tps == null) {
                    System.out.println("Could not find traversal provider service");
                    return;
                }
                
                FileObject fo = tps.getMainTraversal(apk, true);
                
                if (fo == null) {
                    displayInformationMessage("MSG_NoAppTraversal");
                    return;
                } else {
                    System.out.println("Main traversal:" + fo.getPath());
                }
                
                GortEntityManager gem = ProjectUtility.getGortEntityManager(project);
                
                if (gem == null) {
                    return;
                }
                
                EntityManager em = null;
                FileObject mTurkInputFile = null;
                FileOutputStream fileOutputStream = null;
                PrintWriter writer = null;
                
                try {
                    em = gem.getEntityManager();
                    
                    App app = gem.selectApp(em, apk.getNameExt());
                    
                    if (app == null) {
                        return;
                    } else {
                        System.out.println("Performing crowd analysis on: " + app.getName());
                    }
                    
                    if (app.getName() == null) {
                        System.out.println("Cannot perform crowd analysis without app name");
                        displayInformationMessage("MSG_NoAppName");
                        return;
                    }
                    
                    if (app.getDescription() == null) {
                        System.out.println("Cannot perform crowd analysis without description");
                        displayInformationMessage("MSG_NoAppDescription");
                        return;
                    }
                    
                    List<String> appScreenshotURLs = CrowdAnalysisServiceProvider.this.getAppScreenshotURLs(gem, em, app);
                    
                    if (appScreenshotURLs == null || appScreenshotURLs.isEmpty()) {
                        displayInformationMessage("MSG_NoAppScreenshot");
                        return;
                    }
                    
                    Traversal traversal = gem.selectTraversal(em, fo.getNameExt());
                    
                    if (traversal == null) {
                        System.out.println("Traversal is null.");
                        return;
                    }
                    
                    List<History> history = traversal.getHistory();
                    
                    if (history == null || history.isEmpty()) {
                        System.out.println("History element is invalid");
                        displayInformationMessage("MSG_NoAppHistory");
                        return;
                    }
                    
                    String time = DateHelper.getDate(System.currentTimeMillis(), Calendar.getInstance().getTimeZone().getID());
                    FileObject resultsFolder = getResultsFolder();
                    
                    if (resultsFolder == null) {
                        System.out.println("No results folder for crowd results.");
                        return;
                    }
                    
                    mTurkInputFile = null;
                    
                    try {
                        mTurkInputFile = resultsFolder.createData(String.format("%s_taskextraction_%s.input", app.getApk(), time));
                        File outputFile = FileUtil.toFile(mTurkInputFile);
                        fileOutputStream = new FileOutputStream(outputFile);
                    } catch (FileAlreadyLockedException ex) {
                            // ignore
                    } catch (IOException ex) {
                        //ignore
                    }
                    
                    writer = new PrintWriter(fileOutputStream);
                    AppTaskExtraction.writeHeader(writer);
                    
                    // only ask user for confirmation once
                    // once this is set to true. do not keep asking
                    boolean userConfirmedRecreate = false;
                    
                    // whether to recreate tasks that are already in flight.
                    // note this does not kill old tasks
                    boolean recreate = false;
                    
                    List<CrowdTask> tasksCreated = new ArrayList<CrowdTask>();
                    
                    // batch id for this set of tasks
                    String batch = UUID.randomUUID().toString();
                    
                    for (History h : history) {
                        
                        // check if we have already reached the budget for this app
                        // do not create more any more crowd tasks
                        if (reachedBudgetForApp(gem, em, traversal)) {
                            displayInformationMessage("MSG_ReachedAppBudget");
                            break;
                        }
                        
                        if (h == null) {
                            System.out.println("History element is null.");
                            continue;
                        }
                        
                        org.cmuchimps.gort.modules.dataobject.State startState, endState;
                        startState = h.getStartState();
                        endState = h.getEndState();
                        
                        // if there is no transition
                        if (endState == null) {
                            System.out.println("End state is null.");
                            continue;
                        }
                        
                        Activity endActivity = endState.getActivity();
                        
                        // if there is no activity for end state
                        if (endActivity == null) {
                            System.out.println("End activity is null.");
                            continue;
                        }
                        
                        // if the end state is not in the app continue
                        //if (!Boolean.TRUE.equals(endActivity.getInApp())) {
                        if (!Boolean.TRUE.equals(endActivity.getInAppLenient(app.getPackage()))) {
                            System.out.println("End activity is not in app: " + endActivity.getName());
                            continue;
                        }
                        
                        Screenshot endScreenshot = h.getEndScreenshot();
                        
                        // if there is no end screenshot continue
                        if (endScreenshot == null) {
                            System.out.println("End screenshot is null.");
                            continue;
                        }
                        
                        // if the start state is null use the end state
                        if (startState == null) {
                            System.out.println("Start state is null. Setting start state to end state.");
                            startState = endState;
                        }
                        
                        Activity startActivity = startState.getActivity();
                        
                        Screenshot startScreenshot = h.getStartScreenshot();
                        
                        // if there is no start screenshot use the same as the end
                        if (startScreenshot == null) {
                            startScreenshot = endScreenshot;
                        }
                        
                        // if neither the start or end state has a transmission taint
                        // skip it. no point analysing taints that do not have transmissions
                        // TODO: make this more efficient by combining with combinedTaintTag
                        if (!(startState.hasTransmissionTaintlogs() || endState.hasTransmissionTaintlogs())) {
                            System.out.println("Neither state has transmission taintlogs: " + startState.getId() + ' ' + endState.getId());
                            continue;
                        }
                        
                        Integer tmp;
                        int combinedTaintTag = 0;
                        
                        tmp = org.cmuchimps.gort.modules.dataobject.State.mergeCombinedTaintTags(startState, endState);
                        
                        if (tmp != null) {
                            combinedTaintTag = tmp.intValue();
                        }
                        
                        if (TaintHelper.checkTag(combinedTaintTag, TaintHelper.TAINT_CONTACTS)) {
                            if (!AbstractHeuristic.hasPermission(project, apk, AndroidPermissions.READ_CONTACTS)) {
                                combinedTaintTag -= TaintHelper.TAINT_CONTACTS;
                            }
                        }
                        
                        System.out.println("Combined taint tags: " + tmp);
                        
                        if (combinedTaintTag == 0) {
                            continue;
                        }
                        
                        /*
                        // see if we already have a crowdtask for the state 
                        CrowdTask prior = gem.selectCrowdTask(em, 
                                traversal.getId(), startState.getId(), 
                                endState.getId(), startScreenshot.getId(),
                                endScreenshot.getId());
                                * */
                        
                        // see if we already have a crowdtask for the states
                        // just use states as screenshots should be similar
                        // using states rather than screenshots reduces # of 
                        // crowdtasks which helps with cost effectiveness
                        String priorTask = similarPriorTask(gem, em, traversal, startState, endState, batch);
                        
                        if (priorTask.equals(CrowdAnalysisServiceProvider.SIMILAR_TASK_IN_BATCH)) {
                            // if we have an existing task for the states in this batch
                            // do not re-create it
                            continue;
                        } else if (priorTask.equals(CrowdAnalysisServiceProvider.SIMILAR_TASK_OUT_OF_BATCH)) {
                            
                            // if we have a similar task in flight in another batch
                            // ask the user if we should recreate it
                            if (!userConfirmedRecreate) {
                                recreate = recreateTaskConfirmationMessage();
                                userConfirmedRecreate = true;
                            }
                            
                            if (!recreate) {
                                continue;
                            }
                        }
                        
                        // Check similarity of the two screenshots
                        FileObject f0 = project.getProjectDirectory().getFileObject(startScreenshot.getPath());
                        FileObject f1 = project.getProjectDirectory().getFileObject(endScreenshot.getPath());
                        
                        // if the states are in different activities then crowd source both
                        // if the states are in the same activity try an image similarity algorithm first
                        
                        if (startState.getActivity() == endState.getActivity()) {
                            System.out.println("Start and end state have the same activity:");
                            System.out.println("Images are similar: " + ImageHelper.similar(f0, f1));
                        }
                        
                        //System.out.println("Images are similar: " + ImageHelper.similar(f0, f1));
                        
                        // get the screenshot URLs
                        String startScreenshotURL = uploadScreenshot(gem, em, startScreenshot);
                        String endScreenshotURL = uploadScreenshot(gem, em, endScreenshot);
                        
                        if (startScreenshotURL == null || endScreenshotURL == null) {
                            continue;
                        }
                        
                        // and santizie
                        String name = Utility.sanitizeMTurkInput(app.getName());
                        String description = Utility.sanitizeMTurkInput(app.getDescription());
                        
                        boolean single = false;
                        
                        if (startState.getActivity() == endState.getActivity()) {
                            if (ImageHelper.similar(f0, f1)) {
                                single = true;
                            }
                        }
                        
                        AppTaskExtraction.writeInput(writer, name, description, 
                                appScreenshotURLs.get(0), appScreenshotURLs.get(1),
                                startScreenshotURL, endScreenshotURL,
                                traversal.getId(), h.getId(), startState.getId(),
                                endState.getId(), startScreenshot.getId(),
                                endScreenshot.getId(), single);
                        
                        // uuid is the batch number
                        CrowdTask ct = new CrowdTask(batch, combinedTaintTag, traversal,
                                startState, endState, startScreenshot, endScreenshot);
                        gem.insertEntity(em, ct);
                        tasksCreated.add(ct);
                    }
                    
                    // close the stream and the writer and create hits
                    // since all hits are creaed at once at the hits to all the created
                    // crowd tasks and later which which one corresponds to it based
                    // on the start and end state put in
                    writer.close();
                    fileOutputStream.close();
                    
                    if (mTurkInputFile != null && tasksCreated.size() > 0 ) {
                        AppTaskExtraction ate = new AppTaskExtraction(getResultsFolder(),
                                getProperties(), mTurkInputFile, tasksCreated,
                                gem, em);
                        GortHITCreation ghc = ate.createHITs();
                        
                        if (ghc != null) {
                            // update all the crowdtasks created with gort hits
                            // based on hit results, CrowdTask can later decide which hits belong to it
                            for (CrowdTask ct : tasksCreated) {
                                em.refresh(ct);
                                
                                // if the crowdtask has no hits. it should be removed
                                // since it should not be monitored or viewable by the user
                                if (ct.getHits() == null) {
                                    System.out.println(String.format("Removing crowdtask without hits. (id=%d)", ct.getId()));
                                    gem.removeEntity(em, ct);
                                    continue;
                                } else {
                                    // iterate through the hits to initialize
                                    Iterator<HIT> iterator = ct.getHits().iterator();
                                    
                                    while (iterator.hasNext()) {
                                        iterator.next();
                                    }
                                    
                                    if (ct.getHits().isEmpty()) {
                                        System.out.println(String.format("Removing crowdtask without hits. (id=%d)", ct.getId()));
                                        gem.removeEntity(em, ct);
                                        continue;
                                    }
                                }
                                
                                ct.setInputFile(
                                        FileHelper.pathRelativeToDirectory(
                                        project.getProjectDirectory().getPath(),
                                        mTurkInputFile.getPath()));
                                
                                String successFilePath = ghc.getSuccessFile();
                                String failureFilePath = ghc.getFailureFile();
                                
                                if (new File(successFilePath).exists()) {
                                    ct.setSuccessFile(FileHelper.pathRelativeToDirectory(project.getProjectDirectory().getPath(), successFilePath));
                                }
                                
                                if (new File(failureFilePath).exists()) {
                                    ct.setFailureFile(FileHelper.pathRelativeToDirectory(project.getProjectDirectory().getPath(), failureFilePath));
                                }
                                
                                ct.setSubmission(ghc.getStart());
                                
                                gem.updateEntity(em, ct);

                                /*
                                 * // type now placed in AbstractCrowdTask
                                // check the hits for the ct
                                for (HIT h: ct.getHits()) {
                                    System.out.println("crowdTask: " + ct.getId() + " hitId: " + h.getId());
                                    // make sure the hits have task extraction type
                                    h.setType(HIT.TYPE_TASK_EXTRACTION);
                                    gem.updateEntity(em, h);
                                }*/
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                } finally {
                    GortEntityManager.closeEntityManager(em);
                    
                    if (writer != null) {
                        writer.flush();
                        writer.close();
                    }
                    
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
                
            }
        };
        
        t.start();
    }

    @Override
    public void monitor() {
        if (project == null) {
            return;
        }
        
        CrowdTaskMonitor m = CrowdTaskMonitor.getInstance(this, project);
        
        if (m == null) {
            return;
        }
        
        RP.post(m, DELAY_BEFORE_MONITORING_MILLISECONDS);
    }
    
    // TODO: perhaps the limit should be based on all traversals
    // rather than the selected one
    private boolean reachedBudgetForApp(GortEntityManager gem, EntityManager em, 
                                Traversal traversal) {
        // refresh the traversal in case it has had updates to its crowd tasks
        em.refresh(traversal);
        
        List<CrowdTask> crowdTasks = traversal.getCrowdTasks();
        
        if (crowdTasks == null) {
            return false;
        }
        
        int numCount = 0;
        
        // use an iterator to load all associated crowdtasks
        Iterator<CrowdTask> iterator = crowdTasks.iterator();
        
        while (iterator.hasNext()) {
            iterator.next();
            numCount++;
        }
        
        return numCount >= MAX_CROWD_TASKS_PER_APP;
    }
    
    private String similarPriorTask(GortEntityManager gem, EntityManager em, 
                                Traversal traversal, State startState, State endState,
                                String batch) {
        
        List<CrowdTask> priors = gem.selectCrowdTask(em, 
                                traversal.getId(), startState.getId(), 
                                endState.getId());
        
        if (priors == null || priors.isEmpty()) {
            return SIMILAR_TASK_NONE;
        }
        
        Iterator<CrowdTask> iterator = priors.iterator();
        
        while (iterator.hasNext()) {
            CrowdTask c = iterator.next();
            
            // if task has the same batch, ignore. we are after similar
            // tasks from a different batch
            if (batch != null && batch.equals(c.getBatch())) {
                return SIMILAR_TASK_IN_BATCH;
            }
            
            if (Boolean.TRUE.equals(c.getInFlight())) {
                return SIMILAR_TASK_OUT_OF_BATCH;
            }
        }
        
        return SIMILAR_TASK_NONE;
    }
    
    private void displayInformationMessage(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return;
        }
        
        final NotifyDescriptor nd = new NotifyDescriptor.Message(
                NbBundle.getMessage(CrowdAnalysisServiceProvider.class,
                messageId), NotifyDescriptor.INFORMATION_MESSAGE);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                DialogDisplayer.getDefault().notify(nd);
            }
        });
    }
    
    private boolean recreateTaskConfirmationMessage() {
        final NotifyDescriptor nd = new NotifyDescriptor.Confirmation(
                NbBundle.getMessage(CrowdAnalysisServiceProvider.class,
                "MSG_ExisitingCrowdTask"), NotifyDescriptor.YES_NO_OPTION);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    DialogDisplayer.getDefault().notify(nd);
                }
            });
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        }
        
        // see if the return value is set to YES
        System.out.println("User selected: " + nd.getValue());
        
        return nd.getValue().equals(NotifyDescriptor.YES_OPTION);
    }
    
    // assums that app is connected to the EntityManager input
    public List<String> getAppScreenshotURLs(GortEntityManager gem, EntityManager em, App app) {
        if (gem == null || em == null || app == null) {
            return null;
        }
        
        List<Screenshot> appScreenshots = app.getScreenshots();
                    
        if (appScreenshots == null || appScreenshots.isEmpty()) {
            return null;
        }
        
        int numUploaded = 0;
        Iterator<Screenshot> iterator = appScreenshots.iterator();
        
        List<String> retVal = new LinkedList<String>();
        
        while (numUploaded < APP_SCREENSHOTS_UPLOAD_THRESHOLD && iterator.hasNext()) {
            Screenshot s = iterator.next();
            
            if (s == null) {
                continue;
            }
            
            // if the screenshot has already been uploaded
            if (s.getGortURL() != null && !s.getGortURL().isEmpty()) {
                retVal.add(s.getGortURL());
                numUploaded++;
                continue;
            }
            
            // upload the screenshot first
            String url = this.uploadScreenshot(gem, em, s);            
            if (url != null && !url.isEmpty()) {
                retVal.add(url);
                numUploaded++;
            }
        }

        return (retVal.size() > 0) ? retVal : null;
    }
    
    // assums that s is attached to em. if a gorturl already exists for the screenshot
    // returns the same URL. Note this assumes one is not deleting screenshots from server!
    private String uploadScreenshot(GortEntityManager gem, EntityManager em, Screenshot s) {
        if (gem == null || em == null || s == null) {
            return null;
        }
        
        String gortURL = s.getGortURL();
        
        if (gortURL != null && !gortURL.isEmpty()) {
            return gortURL;
        }
        
        String path = s.getPath();
        String defaultHeightPath = s.getDefaultHeightPath();
        
        // if there is no path to work with, return null
        if ((path == null || path.isEmpty()) && 
                (defaultHeightPath == null || defaultHeightPath.isEmpty())) {
            return null;
        }
        
        FileObject defaultHeightFO = null;
        
        boolean createdScaledImage = false;
        
        if (defaultHeightPath == null || defaultHeightPath.isEmpty()) {
            FileObject fo = project.getProjectDirectory().getFileObject(path);
            if (fo == null) {
                return null;
            }
            // scale to proper dimension for crowdtasks
            defaultHeightFO = ImageHelper.scaleToDefaultDimension(fo);
            createdScaledImage = true;
        } else {
            defaultHeightFO = project.getProjectDirectory().getFileObject(defaultHeightPath);
        }
        
        if (defaultHeightFO == null) {
            return null;
        }
                
        try {
            if (createdScaledImage) {
                String relativePath = FileHelper.pathRelativeToDirectory(
                        project.getProjectDirectory().getPath(), 
                        defaultHeightFO.getPath());
                System.out.println("Default Height Screenshot relative path: " + relativePath);
                s.setDefaultHeightPath(relativePath);
            }
            
            // upload the file and update the GortURL
            WebInfoService wis = WebInfoService.getDefault();
            if (wis != null) {
                gortURL = wis.uploadFile(defaultHeightFO);
                if (gortURL == null || gortURL.isEmpty()) {
                    return null;
                }
                
                s.setGortURL(gortURL);
                return s.getGortURL();
            }
            
        } finally {
            gem.updateEntity(em, s);
        }
        
        return null;
    }
    
    public FileObject getResultsFolder() {
        if (this.project == null) {
            return null;
        }
        
        ProjectDirectoryService pds = 
                project.getLookup().lookup(ProjectDirectoryService.class);
        
        if (pds == null) {
            return null;
        }
        
        return pds.getCrowdTasksDir();
    }
    
    public FileObject getProperties() {
        if (this.project == null) {
            return null;
        }
        
        ProjectDirectoryService pds = 
                project.getLookup().lookup(ProjectDirectoryService.class);
        
        if (pds == null) {
            return null;
        }
        
        return pds.getMTurkProperties();
    }
    
    private boolean checkProperties() {
        
        FileObject fo = getProperties();
        
        if (fo == null || !fo.canRead()) {
            return false;
        }
        
        Properties properties = new Properties();
        InputStream is = null;
        
        try {
            is = fo.getInputStream();
            properties.load(is);
        } catch (FileNotFoundException ex) {
            System.out.println("Could not find mturk.properties file");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Could not read mturk.properties file");
            ex.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        String accessKey = properties.getProperty(KEY_ACCESS_KEY);
        String secretKey = properties.getProperty(KEY_SECRET_KEY);
        
        boolean retVal = (accessKey != null && !accessKey.isEmpty()) &&
                (secretKey != null && !secretKey.isEmpty());
        
        if (!retVal) {
            System.out.println("Access keys are not set");
        }
        
        return retVal;
    }
    
    public synchronized RequesterService getService(boolean rebuild) {
        if (service == null || rebuild) {
            
            FileObject fo = getProperties();
            
            // listen for changes in the properties file. Service should be
            // rebuilt if there are changes to the properties file
            fo.addFileChangeListener(FileUtil.weakFileChangeListener(this, fo));
            
            if (fo == null || !fo.canRead()) {
                service = null;
                return null;
            }
            
            String path = fo.getPath();
            
            if (path == null || path.isEmpty()) {
                service = null;
                return null;
            } else {
                System.out.println("MTurk properties file path: " + path);
            }
            
            service = new RequesterService(new PropertiesClientConfig(path));
        }
        
        return service;
    }
    
    private boolean hasEnoughFund() {
        double balance = getService(false).getAccountBalance();
        System.out.println("Got account balance: " + RequesterService.formatCurrency(balance));
        return balance > 0;
    }

    // no calls should occur
    @Override
    public void fileFolderCreated(FileEvent fe) {
        return;
    }

    // no calls should occur to this
    @Override
    public void fileDataCreated(FileEvent fe) {
        return;
    }

    @Override
    public void fileChanged(FileEvent fe) {
        // rebuild the service
        getService(true);
    }

    @Override
    public void fileDeleted(FileEvent fe) {
        // rebuild the service
        getService(true);
    }

    @Override
    public void fileRenamed(FileRenameEvent fre) {
        // rebuild the service
        getService(true);
    }

    @Override
    public void fileAttributeChanged(FileAttributeEvent fae) {
        // rebuild the service
        getService(true);
    }
}
