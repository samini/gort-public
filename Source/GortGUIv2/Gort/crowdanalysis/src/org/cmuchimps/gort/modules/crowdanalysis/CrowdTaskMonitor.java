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

import com.amazonaws.mturk.dataschema.QuestionFormAnswers;
import com.amazonaws.mturk.dataschema.QuestionFormAnswersType;
import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.service.axis.RequesterService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.persistence.EntityManager;
import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.cmuchimps.gort.api.gort.ProjectUtility;
import org.cmuchimps.gort.api.gort.TraversalProviderService;
import org.cmuchimps.gort.modules.crowdanalysis.tasks.AppResourceJustification;
import org.cmuchimps.gort.modules.crowdanalysis.tasks.AppTaskVerification;
import org.cmuchimps.gort.modules.crowdanalysis.tasks.AppTaskVerificationVoter;
import org.cmuchimps.gort.modules.dataobject.App;
import org.cmuchimps.gort.modules.dataobject.CrowdTask;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.dataobject.HIT;
import org.cmuchimps.gort.modules.dataobject.Traversal;
import org.cmuchimps.gort.modules.helper.DateHelper;
import org.cmuchimps.gort.modules.helper.FileHelper;
import org.cmuchimps.gort.modules.helper.MathHelper;
import org.cmuchimps.gort.modules.helper.TaintHelper;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 *
 * @author shahriyar
 */
public class CrowdTaskMonitor implements Runnable {
    // there should only be 1 monitor running for an entire project
    //private static final RequestProcessor RP = new RequestProcessor(CrowdTaskMonitor.class);
    private static final RequestProcessor RP = new RequestProcessor(CrowdTaskMonitor.class.getName(), 1, true);
    private static final int DELAY_MILLISECONDS = 2 * 60 * 1000;
    
    // introduce a jitter so that if there are multiple monitors created
    // they check at various times
    private static final double DELAY_JITTER_MILLISECONDS = 30 * 1000;
    
    // number of max assignments for when a hit is complete
    private static final int MAX_ASSIGNMENTS = 10;
    
    // in case the maximums for tasks are different, they should be taken into account
    private static final int MAX_ASSIGNMENTS_EXTRACTION = 1;
    private static final int MAX_ASSIGNMENTS_VERIFICATION = 1;
    private static final int MAX_ASSIGNMENTS_JUSTIFICATION = 1;
    
    // answer identifiers
    public static final String QUESTION_IDENTIFIER_ANDROID_VERSION = "AndroidVersion";
    public static final String QUESTION_IDENTIFIER_USED_APP = "UsedApp";
    public static final String QUESTION_IDENTIFIER_APP_TYPE = "AppType";
    public static final String QUESTION_IDENTIFIER_INTENDED_TASK = "IntendedTask";
    public static final String QUESTION_IDENTIFIER_COMMENTS = "Comments";
    public static final String QUESTION_IDENTIFIER_SINGLE_SCREENSHOT = "SingleScreenshot";
    public static final String QUESTION_IDENTIFIER_EXPECTED = "Expected";
    public static final String QUESTION_IDENTIFIER_COMFORT = "Comfort";
    
    public static final String QUESTION_IDENTIFIER_VALIDATION_ID = "RateTaskDesc11";
    private static final String VALIDATION_QUESTION_ANSWER = "6";
    
    public static final String QUESTION_IDENTIFIER_TASK_ID_STARTER = "RateTaskDesc";
    public static final String[] QUESTION_IDENTIFIERS_TASK_IDS = {"RateTaskDesc01",
        "RateTaskDesc02",
        "RateTaskDesc03",
        "RateTaskDesc04",
        "RateTaskDesc05",
        "RateTaskDesc06",
        "RateTaskDesc07",
        "RateTaskDesc08",
        "RateTaskDesc09",
        "RateTaskDesc10",
    };
    
    private static final String EXPECTED_ANSWER_YES = "Yes";
    private static final String EXPECTED_ANSWER_NO = "No";
    private static final String EXPECTED_ANSWER_DONT_KNOW = "DontKnow";
    
    private final RequestProcessor.Task task;
    private final int delay;
    
    private final CrowdAnalysisServiceProvider parent;
    private final Project project;
    
    // one instance per project
    private static Map<Project, CrowdTaskMonitor> instances = new HashMap<Project, CrowdTaskMonitor>();
    
    private CrowdTaskMonitor(CrowdAnalysisServiceProvider parent, Project project) {
        this.parent = parent;
        this.project = project;
        task = RP.create(this);
        delay = DELAY_MILLISECONDS + (int) (new Random().nextDouble() * DELAY_JITTER_MILLISECONDS);
    }
    
    public static synchronized CrowdTaskMonitor getInstance(CrowdAnalysisServiceProvider parent, Project project) {
        if (parent == null || project == null) {
            return null;
        }
        
        if (!instances.containsKey(project)) {
            instances.put(project, new CrowdTaskMonitor(parent, project));
        }
        
        return instances.get(project);
    }
    
    @Override
    public void run() {
        if (project == null) {
            return;
        }
        
        // if the project is not currently open in the UI no need to check on its crowdtasks
        if (OpenProjects.getDefault() != null && !OpenProjects.getDefault().isProjectOpen(project)) {
            return;
        }
        
        execute();
        task.schedule(delay);
    }
    
    private void execute() {
        System.out.println("Executing monitor for project: " + project.getProjectDirectory().getNameExt());
        
        TraversalProviderService tps = project.getLookup().lookup(TraversalProviderService.class);
                
        if (tps == null) {
            System.out.println("Could not find traversal provider service");
            return;
        }
        
        ProjectDirectoryService pds = ProjectUtility.getProjectDirectoryService(project);
        
        if (pds == null) {
            return;
        }
        
        // get the apps directory
        FileObject[] apks = pds.getAPKs();
        
        if (apks == null || apks.length <= 0) {
            System.out.println("No apps to monitor");
        }
        
        GortEntityManager gem = ProjectUtility.getGortEntityManager(project);
        
        if (gem == null) {
            return;
        }
        
        EntityManager em = null;
        
        try {
            em = gem.getEntityManager();
            
            // if the project is closed, sometimes the above returns an em with
            // a closed connection.
            if (em == null || !em.isOpen()) {
                return;
            }
            
            for (FileObject apk : apks) {
                if (apk == null) {
                    continue;
                }
                
                App app = gem.selectApp(em, apk.getNameExt());
                
                if (app == null) {
                    continue;
                }
                
                FileObject traversalFO = tps.getMainTraversal(apk);
                
                if (traversalFO == null) {
                    System.out.println("Apk does not have any traversal folders. Skipping in monitor.");
                    continue;
                }
                
                Traversal traversal = gem.selectTraversal(em, traversalFO.getNameExt());
                
                if (traversal == null) {
                    System.out.println("Apk does not a main traversal. Skipping in monitor.");
                    continue;
                }
                
                processApp(gem, em, app, traversal);
            }
        } catch (NullPointerException e) {
            // apps may not have been initialized
        } finally {
            GortEntityManager.closeEntityManager(em);
        }
    }
    
    // under the assumption that app and traversal are attached to em
    private void processApp(GortEntityManager gem, EntityManager em, App app, Traversal traversal) {
        if (gem == null || em == null || app == null || traversal == null) {
            return;
        }
        
        List<CrowdTask> crowdTasks = traversal.getCrowdTasks();
        
        if (crowdTasks == null || crowdTasks.isEmpty()) {
            return;
        }
        
        Iterator<CrowdTask> iterator = crowdTasks.iterator();
        
        while (iterator.hasNext()) {
            CrowdTask ct = iterator.next();
            
            if (ct == null) {
                continue;
            }
            
            processCrowdTask(gem, em, app, traversal, ct);
        }
    }
    
    private void processCrowdTask(GortEntityManager gem, EntityManager em, App app, Traversal traversal, CrowdTask crowdTask) {
        List<HIT> hits = crowdTask.getHits();
        
        if (hits == null || hits.isEmpty()) {
            System.out.println(String.format("CrowdTask does not have any hits. (id=%d)", crowdTask.getId()));
            return;
        }
        
        Iterator<HIT> iterator = hits.iterator();
        
        while (iterator.hasNext()) {
            HIT h = iterator.next();
            String hitId = h.getHitId();
            
            if (hitId == null || hitId.isEmpty()) {
                continue;
            }
            
            // check if the hit is not in flight produce the next hit
            // if it has not already been produced
            if (Boolean.FALSE.equals(h.getInFlight())) {
                // if another hit has already been produced or this is the final
                // state of crowd sourcing don't do anything
                if (!iterator.hasNext() && !HIT.TYPE_RESOURCE_JUSTIFICATION.equals(h.getType())) {
                    // produce the hit for the next stage
                    processCompletedHIT(gem, em, app, traversal, crowdTask, h);
                }
                
                // continue. no need to get assignments the hit is no longer in flight
                // so all assignments have been received
                continue;
            }
            
            System.out.println("Getting assignments for hit: " + hitId);
            
            Assignment[] assignments = Utility.getSubmittedAssignments(parent.getService(false), hitId);
            
            if (assignments == null || assignments.length <= 0) {
                System.out.println("Skipping hit. Hit does not have any assignments.");
                continue;
            }
            
            for (Assignment a : assignments) {
                String assignmentId = a.getAssignmentId();
                
                String xmlAnswer = a.getAnswer();
                
                if (xmlAnswer == null || xmlAnswer.isEmpty()) {
                    continue;
                }
                
                org.cmuchimps.gort.modules.dataobject.Assignment gortAssignment = 
                        gem.selectAssignment(em, hitId, assignmentId);
                
                if (gortAssignment == null) {
                    gortAssignment = Utility.createDBAssignment(gem, em, a);
                    gortAssignment.setHit(h);
                    gem.updateEntity(em, gortAssignment);
                }
                
                System.out.println(xmlAnswer);
            }
            
            // refresh the hit from db with potentially new assignments
            em.refresh(h);
            
            if (h.getAssignments().size() >= CrowdTaskMonitor.MAX_ASSIGNMENTS) {
                processCompletedHIT(gem, em, app, traversal, crowdTask, h);
            }
        }
    }
    
    private void processCompletedHIT(GortEntityManager gem, EntityManager em, App app, Traversal traversal, CrowdTask crowdTask, HIT hit) {
        if (hit == null) {
            return;
        }
        
        // set the completeion time for the hit
        setHITCompletion(gem, em, hit);
        
        String type = hit.getType();
        
        if (HIT.TYPE_TASK_EXTRACTION.equals(type)) {
            processCompletedExtractionHIT(gem, em, app, traversal, crowdTask, hit);
        } else if (HIT.TYPE_TASK_VERIFICATION.equals(type)) {
            processCompletedVerifictionHIT(gem, em, app, traversal, crowdTask, hit);
        } else if (HIT.TYPE_RESOURCE_JUSTIFICATION.equals(type)) {
            processCompletedJustificationHIT(gem, em, app, traversal, crowdTask, hit);
        }    
    }
    
    private void processCompletedExtractionHIT(GortEntityManager gem, EntityManager em, App app, Traversal traversal, CrowdTask crowdTask, HIT hit) {
        List<org.cmuchimps.gort.modules.dataobject.Assignment> assignments = hit.getAssignments();
        Iterator<org.cmuchimps.gort.modules.dataobject.Assignment> iterator = assignments.iterator();
        
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        
        FileObject resultsFolder = parent.getResultsFolder();
        
        if (resultsFolder == null) {
            return;
        }
        
        String time = DateHelper.getDate(System.currentTimeMillis(), Calendar.getInstance().getTimeZone().getID());
        FileObject mTurkInputFile = null;
        FileOutputStream fileOutputStream = null;
        Writer writer = null;
        
        try {
            mTurkInputFile = resultsFolder.createData(String.format("%s_taskverification_%s.input", app.getApk(), time));
            File outputFile = FileUtil.toFile(mTurkInputFile);
            fileOutputStream = new FileOutputStream(outputFile);
            writer = new PrintWriter(fileOutputStream);
        } catch (IOException ex) {
            // ignore
        }
        
        if (mTurkInputFile == null || fileOutputStream == null || writer == null) {
            return;
        }
        
        try {
            AppTaskVerification.writeHeader(writer);
            
            List<String> descriptions = new LinkedList<String>();
            String single = null;
            
            while (iterator.hasNext()) {
                org.cmuchimps.gort.modules.dataobject.Assignment assignment = iterator.next();

                if (assignment == null) {
                    continue;
                }

                String answerXML = assignment.getAnswer();

                if (answerXML == null || answerXML.isEmpty()) {
                    continue;
                }
                
                // parse the answers
                QuestionFormAnswers qfa = RequesterService.parseAnswers(answerXML);
                List<QuestionFormAnswersType.AnswerType> answers =
                    (List<QuestionFormAnswersType.AnswerType>) qfa.getAnswer();
                
                String version = null;
                String usedApp = null;
                String appType = null;
                String intendedTask = null;
                
                for (QuestionFormAnswersType.AnswerType answer : answers) {
                    if (QUESTION_IDENTIFIER_ANDROID_VERSION.equals(answer.getQuestionIdentifier())) {
                        version = answer.getFreeText();
                    } else if (QUESTION_IDENTIFIER_USED_APP.equals(answer.getQuestionIdentifier())) {
                        usedApp = answer.getFreeText();
                    } else if (QUESTION_IDENTIFIER_APP_TYPE.equals(answer.getQuestionIdentifier())) {
                        appType = answer.getFreeText();
                    } else if (QUESTION_IDENTIFIER_INTENDED_TASK.equals(answer.getQuestionIdentifier())) {
                        intendedTask = answer.getFreeText();
                    } else if (QUESTION_IDENTIFIER_SINGLE_SCREENSHOT.equals(answer.getQuestionIdentifier())) {
                        single = answer.getFreeText();
                    }
                }
                
                if (!checkAndroidVersion(version)) {
                    // junk answer, skip it
                    continue;
                }
                
                if (usedApp == null || usedApp.isEmpty()) {
                    // junk answer, skip it
                    continue;
                }
                
                // TODO: do more with app type
                if (appType == null || appType.isEmpty()) {
                    // junk answer, skip it
                    continue;
                }
                
                // make sure the description has less than or equal to 1 word
                if (intendedTask == null || intendedTask.isEmpty() || intendedTask.trim().split(" ").length <= 1) {
                    // junk answer, skip it
                    continue;
                }
                
                //TODO: strings have to be sanitized here &amp;
                //descriptions.add(intendedTask.trim());
                descriptions.add(intendedTask.trim().replace("&", "&amp;"));
            }
            
            while (descriptions.size() < 10) {
                descriptions.add("DESCRIPTION_NONE");
            }
            
            // should be already uploaded at this point
            List<String> screenshotURLs = parent.getAppScreenshotURLs(gem, em, app);
            
            AppTaskVerification.writeInput(writer, 
                    Utility.sanitizeMTurkInput(app.getName()),
                    Utility.sanitizeMTurkInput(app.getDescription()),
                    screenshotURLs.get(0),
                    screenshotURLs.get(1),
                    crowdTask.getStartScreenshot().getGortURL(),
                    crowdTask.getEndScreenshot().getGortURL(),
                    descriptions,
                    single);
            
            AppTaskVerification atv = new AppTaskVerification(parent.getResultsFolder(),
                    parent.getProperties(), mTurkInputFile, Collections.singletonList(crowdTask),
                    gem, em);
            
            GortHITCreation ghc = atv.createHITs();
            
            if (ghc != null) {
                
                // only 1 crowdTask
                // the one currently attached
                em.refresh(crowdTask);
                
                crowdTask.setInputFile(FileHelper.pathRelativeToDirectory(
                        project.getProjectDirectory().getPath(),
                        mTurkInputFile.getPath()));
                
                String successFilePath = ghc.getSuccessFile();
                String failureFilePath = ghc.getFailureFile();

                if (new File(successFilePath).exists()) {
                    crowdTask.setSuccessFile(FileHelper.pathRelativeToDirectory(project.getProjectDirectory().getPath(), successFilePath));
                }

                if (new File(failureFilePath).exists()) {
                    crowdTask.setFailureFile(FileHelper.pathRelativeToDirectory(project.getProjectDirectory().getPath(), failureFilePath));
                }
                
                // update the crowdtask
                gem.updateEntity(em, crowdTask);
                
                String input = delimitedInput(descriptions);
                
                for (HIT h : crowdTask.getHits()) {
                    if (h == null) {
                        continue;
                    }
                    
                    // mark the initial task extraction as no longer in flight
                    if (HIT.TYPE_TASK_EXTRACTION.equals(h.getType())) {
                        h.setInFlight(Boolean.FALSE);
                        gem.updateEntity(em, h);
                    } else if (HIT.TYPE_TASK_VERIFICATION.equals(h.getType())) {
                        h.setInput(input);
                        gem.updateEntity(em, h);
                    }
                }
                
            }
            
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
            
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
        
    }
    
    private void processCompletedVerifictionHIT(GortEntityManager gem, EntityManager em, App app, Traversal traversal, CrowdTask crowdTask, HIT hit) {
        List<org.cmuchimps.gort.modules.dataobject.Assignment> assignments = hit.getAssignments();
        Iterator<org.cmuchimps.gort.modules.dataobject.Assignment> iterator = assignments.iterator();
        
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        
        FileObject resultsFolder = parent.getResultsFolder();
        
        if (resultsFolder == null) {
            return;
        }
        
        String time = DateHelper.getDate(System.currentTimeMillis(), Calendar.getInstance().getTimeZone().getID());
        FileObject mTurkInputFile = null;
        FileOutputStream fileOutputStream = null;
        Writer writer = null;
        
        try {
            mTurkInputFile = resultsFolder.createData(String.format("%s_taskjustification_%s.input", app.getApk(), time));
            File outputFile = FileUtil.toFile(mTurkInputFile);
            fileOutputStream = new FileOutputStream(outputFile);
            writer = new PrintWriter(fileOutputStream);
        } catch (IOException ex) {
            // ignore
        }
        
        if (mTurkInputFile == null || fileOutputStream == null || writer == null) {
            return;
        }
        
        try {
            AppResourceJustification.writeHeader(writer);
            
            // Create a new voter for this HIT
            AppTaskVerificationVoter atvv = AppTaskVerificationVoter.getInstance(QUESTION_IDENTIFIERS_TASK_IDS);
            
            while (iterator.hasNext()) {
                org.cmuchimps.gort.modules.dataobject.Assignment assignment = iterator.next();

                if (assignment == null) {
                    continue;
                }

                String answerXML = assignment.getAnswer();

                if (answerXML == null || answerXML.isEmpty()) {
                    continue;
                }
                
                // parse the answers
                QuestionFormAnswers qfa = RequesterService.parseAnswers(answerXML);
                List<QuestionFormAnswersType.AnswerType> answers =
                    (List<QuestionFormAnswersType.AnswerType>) qfa.getAnswer();
                
                String version = null;
                String usedApp = null;
                String appType = null;
                String validationVote = null;
                String vote = null;
                
                for (QuestionFormAnswersType.AnswerType answer : answers) {
                    if (QUESTION_IDENTIFIER_ANDROID_VERSION.equals(answer.getQuestionIdentifier())) {
                        version = answer.getFreeText();
                    } else if (QUESTION_IDENTIFIER_USED_APP.equals(answer.getQuestionIdentifier())) {
                        usedApp = answer.getFreeText();
                    } else if (QUESTION_IDENTIFIER_APP_TYPE.equals(answer.getQuestionIdentifier())) {
                        appType = answer.getFreeText();
                    } else if (QUESTION_IDENTIFIER_VALIDATION_ID.equals(answer.getQuestionIdentifier())) {
                        validationVote = answer.getFreeText();
                    }
                }
                
                if (!checkAndroidVersion(version)) {
                    // junk answer, skip it
                    continue;
                }
                
                if (usedApp == null || usedApp.isEmpty()) {
                    // junk answer, skip it
                    continue;
                }
                
                // TODO: do more with app type
                if (appType == null || appType.isEmpty()) {
                    // junk answer, skip it
                    continue;
                }
                
                // check that the validation vote is set and it is the correct vote
                // there seems to be an issue with validation input
                // it does not show up for some tasks?
                //if (validationVote == null || !validationVote.trim().equals(VALIDATION_QUESTION_ANSWER)) {
                if (validationVote != null && !validationVote.trim().equals(VALIDATION_QUESTION_ANSWER)) {
                    // user did not select the correct response for the validation vote
                    continue;
                }
                
                // once validation checks have run add the votes to the voter
                for (QuestionFormAnswersType.AnswerType answer : answers) {
                    if (answer.getQuestionIdentifier() == null) {
                        continue;
                    } else if (QUESTION_IDENTIFIER_VALIDATION_ID.equals(answer.getQuestionIdentifier())) {
                        continue;
                    } else if (answer.getQuestionIdentifier().startsWith(QUESTION_IDENTIFIER_TASK_ID_STARTER)) {
                        vote = answer.getFreeText();
                        System.out.println("Received vote: " + vote + " for candidate " + answer.getQuestionIdentifier());
                        atvv.addVote(answer.getQuestionIdentifier(), vote);
                    }
                }
            }
            
            // run the voter and get the voted description
            String winner = atvv.getWinningCandidate();
            
            if (winner == null || winner.isEmpty()) {
                System.out.println("Cannot have non-labeled winner.");
                return;
            } else if (!winner.startsWith(QUESTION_IDENTIFIER_TASK_ID_STARTER)) {
                return;
            }
            
            // get the index for the chosen task description
            winner = winner.replace(QUESTION_IDENTIFIER_TASK_ID_STARTER, "");
            
            int winnerIndex = Integer.parseInt(winner) - 1;
            
            if (winnerIndex < 0) {
                System.out.println("Cannot have negative winner index.");
                return;
            } else {
                System.out.println("Winning index is: " + winnerIndex);
            }
            
            // get the input task descriptions for the hit
            String taskDescriptions = hit.getInput();
            
            if (taskDescriptions == null || taskDescriptions.isEmpty()) {
                return;
            } else {
                System.out.println("Task descriptions: " + taskDescriptions);
            }
            
            String[] taskDescriptionsSplit = splitInput(taskDescriptions);
            
            if (taskDescriptionsSplit.length <= 0) {
                return;
            }
            
            String winningDescription = null;
            
            try {
                winningDescription = taskDescriptionsSplit[winnerIndex];
                System.out.println("Winning description: " + winningDescription);
            } catch (IndexOutOfBoundsException e) {
                // could not get a description to use for crowdsourcing
                return;
            }
            
            // get the list of resources
            Integer combinedTag = crowdTask.getCombinedTaintTag();
            
            if (combinedTag == null) {
                return;
            }
            
            String resources = TaintHelper.crowdDefinitionsFromTag(crowdTask.getCombinedTaintTag());
            
            if (resources == null || resources.isEmpty()) {
                System.out.println("No resources to use for crowd task!");
                return;
            } else {
                System.out.println("Resources used: " + resources);
            }
            
            // should be already uploaded at this point
            List<String> screenshotURLs = parent.getAppScreenshotURLs(gem, em, app);
            
            AppResourceJustification.writeInput(writer, 
                    Utility.sanitizeMTurkInput(app.getName()),
                    Utility.sanitizeMTurkInput(app.getDescription()),
                    screenshotURLs.get(0),
                    screenshotURLs.get(1),
                    winningDescription,
                    resources);
            
            AppResourceJustification arj = new AppResourceJustification(parent.getResultsFolder(),
                    parent.getProperties(), mTurkInputFile, Collections.singletonList(crowdTask),
                    gem, em);
            
            GortHITCreation ghc = arj.createHITs();
            
            if (ghc != null) {
                
                // only 1 crowdTask
                // the one currently attached
                em.refresh(crowdTask);
                
                crowdTask.setInputFile(FileHelper.pathRelativeToDirectory(
                        project.getProjectDirectory().getPath(),
                        mTurkInputFile.getPath()));
                
                String successFilePath = ghc.getSuccessFile();
                String failureFilePath = ghc.getFailureFile();

                if (new File(successFilePath).exists()) {
                    crowdTask.setSuccessFile(FileHelper.pathRelativeToDirectory(project.getProjectDirectory().getPath(), successFilePath));
                }

                if (new File(failureFilePath).exists()) {
                    crowdTask.setFailureFile(FileHelper.pathRelativeToDirectory(project.getProjectDirectory().getPath(), failureFilePath));
                }
                
                // update the crowdtask
                gem.updateEntity(em, crowdTask);
                
                String input = winningDescription;
                
                for (HIT h : crowdTask.getHits()) {
                    if (h == null) {
                        continue;
                    }
                    
                    // mark the initial task extraction as no longer in flight
                    if (HIT.TYPE_TASK_EXTRACTION.equals(h.getType())) {
                        h.setInFlight(Boolean.FALSE);
                        gem.updateEntity(em, h);
                    } else if (HIT.TYPE_TASK_VERIFICATION.equals(h.getType())) {
                        h.setInFlight(Boolean.FALSE);
                        gem.updateEntity(em, h);
                    } else if (HIT.TYPE_RESOURCE_JUSTIFICATION.equals(h.getType())) {
                        h.setInput(input);
                        gem.updateEntity(em, h);
                    }
                }
                
            }
            
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
            
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }
    
    private void processCompletedJustificationHIT(GortEntityManager gem, EntityManager em, App app, Traversal traversal, CrowdTask crowdTask, HIT hit) {
        List<org.cmuchimps.gort.modules.dataobject.Assignment> assignments = hit.getAssignments();
        Iterator<org.cmuchimps.gort.modules.dataobject.Assignment> iterator = assignments.iterator();
        
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        
        int numExpectedYes = 0;
        int numExpectedNo = 0;
        List<Integer> comfortLevels = new LinkedList<Integer>();
        
        while (iterator.hasNext()) {
            org.cmuchimps.gort.modules.dataobject.Assignment assignment = iterator.next();

            if (assignment == null) {
                continue;
            }

            String answerXML = assignment.getAnswer();

            if (answerXML == null || answerXML.isEmpty()) {
                continue;
            }

            // parse the answers
            QuestionFormAnswers qfa = RequesterService.parseAnswers(answerXML);
            List<QuestionFormAnswersType.AnswerType> answers =
                (List<QuestionFormAnswersType.AnswerType>) qfa.getAnswer();

            String version = null;
            String usedApp = null;
            String appType = null;
            String expected = null;
            
            String comfort = null;
            int comfortLevel = Integer.MIN_VALUE;

            for (QuestionFormAnswersType.AnswerType answer : answers) {
                if (QUESTION_IDENTIFIER_ANDROID_VERSION.equals(answer.getQuestionIdentifier())) {
                    version = answer.getFreeText();
                } else if (QUESTION_IDENTIFIER_USED_APP.equals(answer.getQuestionIdentifier())) {
                    usedApp = answer.getFreeText();
                } else if (QUESTION_IDENTIFIER_APP_TYPE.equals(answer.getQuestionIdentifier())) {
                    appType = answer.getFreeText();
                } else if (QUESTION_IDENTIFIER_EXPECTED.equals(answer.getQuestionIdentifier())) {
                    expected = answer.getFreeText();
                } else if (QUESTION_IDENTIFIER_COMFORT.equals(answer.getQuestionIdentifier())) {
                    comfort = answer.getFreeText();
                }
            }

            if (!checkAndroidVersion(version)) {
                // junk answer, skip it
                continue;
            }

            if (usedApp == null || usedApp.isEmpty()) {
                // junk answer, skip it
                continue;
            }

            // TODO: do more with app type
            if (appType == null || appType.isEmpty()) {
                // junk answer, skip it
                continue;
            }
            
            if (expected == null || expected.isEmpty()) {
                // junk answer, skip it
                continue;
            }

            if (comfort == null || comfort.isEmpty()) {
                // junk answer, skip it
                continue;
            } else {
                try {
                    comfortLevel = Integer.parseInt(comfort.trim());
                } catch (NumberFormatException e) {
                    // junk answer, skip it
                    continue;
                }
            }
            
            // check that comfort level falls in the comfort level range
            if (!checkComfortLevel(comfortLevel)) {
                // junk answer, skip it
                continue;
            }
            
            // process the expected and comfort values
            if (EXPECTED_ANSWER_YES.equals(expected)) {
                numExpectedYes++;
            } else if (EXPECTED_ANSWER_NO.equals(expected)) {
                numExpectedNo++;
            } else if (EXPECTED_ANSWER_DONT_KNOW.equals(expected)) {
                // junk answer, skip it
                continue;
            }
            
            // add the comfort level to the list
            comfortLevels.add(comfortLevel);
        }
        
        // set the final values for the crowdtask
        em.refresh(crowdTask);
        
        if (numExpectedYes == numExpectedNo) {
            //crowdTask.setExpectedMedian(null);
        } else if (numExpectedYes > numExpectedNo) {
            crowdTask.setExpectedMedian(Boolean.TRUE);
        } else {
            crowdTask.setExpectedMedian(Boolean.FALSE);
        }
        
        if (comfortLevels != null && comfortLevels.size() > 0) {
            Integer[] comfortLevelsArray = comfortLevels.toArray(new Integer[comfortLevels.size()]);
            crowdTask.setValid(comfortLevels.size());
            crowdTask.setComfortAverage(MathHelper.mean(comfortLevelsArray));
            crowdTask.setComfortMedian(MathHelper.median(comfortLevelsArray));
            crowdTask.setComfortStdDev(MathHelper.stdDev(comfortLevelsArray));
            gem.updateEntity(em, crowdTask);
        }

        Date completionDate = null;
        
        for (HIT h : crowdTask.getHits()) {
            if (h == null) {
                continue;
            }
            
            // mark the initial task extraction as no longer in flight
            if (HIT.TYPE_TASK_EXTRACTION.equals(h.getType())) {
                h.setInFlight(Boolean.FALSE);
                gem.updateEntity(em, h);
            } else if (HIT.TYPE_TASK_VERIFICATION.equals(h.getType())) {
                h.setInFlight(Boolean.FALSE);
                gem.updateEntity(em, h);
            } else if (HIT.TYPE_RESOURCE_JUSTIFICATION.equals(h.getType())) {
                //TODO: uncomment for production
                completionDate = h.getCompletion();
                h.setInFlight(Boolean.FALSE);
                gem.updateEntity(em, h);
            }
        }
        
        // set the crowdtask to no longer in flight
        //TODO: uncomment for production
        crowdTask.setCompletion(completionDate);
        crowdTask.setInFlight(Boolean.FALSE);
        gem.updateEntity(em, crowdTask);
    }
    
    // based on http://en.wikipedia.org/wiki/Android_version_history#Android_1.0_.28API_level_1.29
    private static final String[] ACCEPTED_VERSIONS = {"1.0", "1.1", "1.5", "1.6", "2.0", "2.1", "2.2", "2.3", "3.0",
        "3.1", "3.2", "4.0", "4.1", "4.2", "4.3", "4.4"};
    
    private boolean checkAndroidVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        
        for (String s : ACCEPTED_VERSIONS) {
            if (version.trim().startsWith(s)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static final int[] ACCEPTED_COMFORT_LEVELS = {-2, -1, 1, 2};
    
    private boolean checkComfortLevel(int comfortLevel) {
        for (int i : ACCEPTED_COMFORT_LEVELS) {
            if (comfortLevel == i) {
                return true;
            }
        }
        
        return false;
    }
    
    private static final String DELIMITER = "||";
    private static final String DELIMITER_REGEX = "\\|\\|";
    
    private static String delimitedInput(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (String s : items) {
            if (s == null) {
                continue;
            }
            
            sb.append(s);
            sb.append(DELIMITER);
        }
        
        sb.replace(sb.length() - DELIMITER.length(), sb.length(), "");
        
        return sb.toString();
    }
    
    private static String[] splitInput(String input) {
        if (input == null) {
            return null;
        }
        
        return input.trim().split(DELIMITER_REGEX);
    }
    
    private void setHITCompletion(GortEntityManager gem, EntityManager em, HIT hit) {
        // get the submission from assignments. if there is none, set the current time
        Date assignmentsMaxDate = getHITAssignmentsMaxDate(gem, em, hit);
        
        if (assignmentsMaxDate != null) {
            hit.setCompletion(assignmentsMaxDate);
        } else {
            hit.setCompletion(DateHelper.getUTC());
        }
        
        gem.updateEntity(em, hit);
    }
    
    private Date getHITAssignmentsMaxDate(GortEntityManager gem, EntityManager em, HIT hit) {
        // refresh the hit to get all its assignments
        em.refresh(hit);
        
        List<org.cmuchimps.gort.modules.dataobject.Assignment> assignments = hit.getAssignments();
        
        if (assignments == null) {
            return null;
        }
        
        Iterator<org.cmuchimps.gort.modules.dataobject.Assignment> iterator = assignments.iterator();
        
        Date max = null;
        
        while (iterator.hasNext()) {
            org.cmuchimps.gort.modules.dataobject.Assignment assignment = iterator.next();
            
            Date submission = assignment.getSubmission();
            
            if (submission == null) {
                continue;
            }
            
            if (max == null) {
                max = submission;
                continue;
            } else if (max.before(submission)) {
                max = submission;
            }
        }
        
        return max;
    }
}
