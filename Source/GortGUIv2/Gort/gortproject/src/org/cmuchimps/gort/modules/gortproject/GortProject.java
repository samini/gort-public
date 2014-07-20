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
package org.cmuchimps.gort.modules.gortproject;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.cmuchimps.gort.api.gort.GortDatabaseService;
import org.cmuchimps.gort.api.gort.GortDatabaseServiceFactory;
import org.cmuchimps.gort.api.gort.ProgressStatusServiceFactory;
import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.cmuchimps.gort.api.gort.ProjectDirectoryServiceFactory;
import org.cmuchimps.gort.api.gort.TraversalProcessorServiceFactory;
import org.cmuchimps.gort.api.gort.TraversalProviderServiceFactory;
import org.cmuchimps.gort.api.gort.analysis.CrowdAnalysisService;
import org.cmuchimps.gort.api.gort.analysis.CrowdAnalysisServiceFactory;
import org.cmuchimps.gort.api.gort.analysis.DynamicAnalysisServiceFactory;
import org.cmuchimps.gort.api.gort.analysis.StaticAnalysisServiceFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author shahriyar
 */
public final class GortProject implements Project {
    
    public static final String KEY_UUID = "uuid";
    
    public static final String[] LOGICAL_VIEW_DIRECTORIES = 
        {ProjectDirectoryService.APPS_DIR, ProjectDirectoryService.CROWD_TASKS_DIR};
    
    private final FileObject projectDir;
    LogicalViewProvider logicalView = new GortLogicalView(this);
    private final ProjectState state;
    
    private Lookup lookup;

    // uuid for this project
    private final String uuid;
    
    // properties for this project
    private Properties properties;
    
    // Directory service for this project
    //private final ProjectDirectoryService directoryService;
    
    // Database for this project
    //private final GortDatabaseService databaseService;
    
    // Progress Status Service for this project;
    //private final ProgressStatusService progressStatusService;
    
    public GortProject(FileObject projectDir, ProjectState projectState) {
        this.projectDir = projectDir;
        this.state = projectState;
        
        // load up the properties for this project
        loadProperties();
        
        // This gave an error Rules4JBIProject may not mark the project modified while it is being created.
        // So we do not update the properties right away, we just make sure
        // to save the uuid when the project is being saved
        uuid = produceUUID();
        
        // initialize the lookup and related services
        getLookup();
        
        // create a database for the project
        getLookup().lookup(GortDatabaseService.class).createGortDatabase();
        
        // create a database for this project
        //databaseService = GortDatabaseServiceFactory.getDefault().getInstance(this);
        
        // set the progress service for this project
        //progressStatusService = ProgressStatusService.getDefault();
        
        // initialize the services with the associated project
        //directoryService.init(this);
        //databaseService.init(this);
        //progressStatusService.init(this);
        
        // create the directory structure for the project
        createDirectoryStructureThreaded();
        
        // initialize a gem in background for future use
        initGortEntityManagerThreaded();
        
        // initialize the monitor to check crowd task results
        initCrowdAnalysisMonitorThreaded();
    }
    
    @Override
    public FileObject getProjectDirectory() {
        return projectDir;
    }

    @Override
    public Lookup getLookup() {
        if (lookup == null) {
            lookup = Lookups.fixed(new Object[]{
                        this, //handy to expose a project in its own lookup
                        state, //allow outside code to mark the project as needing saving
                        new ActionProviderImpl(), //Provides standard actions like Build and Clean
                        loadProperties(), //The project properties
                        new Info(), //Project information implementation
                        logicalView, //Logical view of project implementation
                        ProjectDirectoryServiceFactory.getDefault().getInstance(this), // DirectoryService to get directories for this project
                        GortDatabaseServiceFactory.getDefault().getInstance(this), //GortDatabaseService and database access for this project
                        ProgressStatusServiceFactory.getDefault().getInstance(this), //A service to monitor the progress for each APK
                        StaticAnalysisServiceFactory.getDefault().getInstance(this), //Enables static analysis on the project
                        DynamicAnalysisServiceFactory.getDefault().getInstance(this), //Enables static analysis on the project
                        CrowdAnalysisServiceFactory.getDefault().getInstance(this), //Enable crowd analysis on project
                        TraversalProviderServiceFactory.getDefault().getInstance(this), // Allow to get the main traversal folder for an APK
                        TraversalProcessorServiceFactory.getDefault().getInstance(this), // Enable post processing of traversals
                    });
        }
        
        return lookup;
    }
    
    private synchronized Properties loadProperties() {

        if (properties == null) {
        
            System.out.println("Loading project properties.");
            
            FileObject fob = projectDir.getFileObject(GortProjectFactory.PROJECT_DIR
                    + "/" + GortProjectFactory.PROJECT_PROPFILE);

            properties = new NotifyProperties(state);

            if (fob != null) {
                try {
                    properties.load(fob.getInputStream());
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                }
            }
        }
        
        return properties;
    }
    
    public void saveProperties() throws IOException {
        FileObject projectRoot = getProjectDirectory();
        
        //Find the properties file pvproject/project.properties,
        //creating it if necessary
        String propsPath = GortProjectFactory.PROJECT_DIR
                    + "/" + GortProjectFactory.PROJECT_PROPFILE;
        FileObject propertiesFile = projectRoot.getFileObject(propsPath);
        if (propertiesFile == null) {
            //Recreate the properties file if needed
            propertiesFile = projectRoot.createData(propsPath);
        }

        File f = FileUtil.toFile(propertiesFile);
        properties.store(new FileOutputStream(f), "Gort Project Properties");
    }

    private static class NotifyProperties extends Properties {

        private final ProjectState state;

        NotifyProperties(ProjectState state) {
            this.state = state;
        }

        @Override
        public Object put(Object key, Object val) {

            // the uuid gets set during project initialization do not
            // mark the project as changed
            if (key != null && key instanceof String && 
                    ((String) key).equals(KEY_UUID)) {
                return super.put(key, val);
            }
            
            Object result = super.put(key, val);

            if (((result == null) != (val == null)) || (result != null
                    && val != null && !val.equals(result))) {
                state.markModified();
            }

            return result;
        }
    }
    
    FileObject getDir(String dir, boolean create) {
        FileObject result =
            projectDir.getFileObject(dir);

        if (result == null && create) {
            try {
                result = projectDir.createFolder(dir);
            } catch (IOException ioe) {
                Exceptions.printStackTrace(ioe);
            }
        }
        
        return result;
    }
    
    /*
    public FileObject getAppsDir(boolean create) {
        return getDir(APPS_DIR, create);
    }
    
    public FileObject getHeuristicsDir(boolean create) {
        return getDir(HEURISTICS_DIR, create);
    }
    
    public FileObject getHITsDir(boolean create) {
        return getDir(HITS_DIR, create);
    }
    
    public FileObject getProgressDir(boolean create) {
        if (progressStatusService != null) {
            return progressStatusService.getProjectProgressDirectory();
        }
        
        return null;
    }
    
    public FileObject getTraversalsDir(boolean create) {
        return getDir(TRAVERSALS_DIR, create);
    }*/
    
    public synchronized final String produceUUID() {
        
        String retVal = null;
        
        if (properties == null) {
                return null;
        }

        retVal = (String) properties.getProperty(KEY_UUID);

        if (retVal == null || retVal.isEmpty()) {
            try {
                retVal = UUID.randomUUID().toString();
                properties.put(KEY_UUID, retVal);
                // make sure to write the uuid to the properties file right away
                saveProperties();
                System.out.println("Created project id.");
            } catch (IOException ex) {
                retVal = null;
                Exceptions.printStackTrace(ex);
            }
        }

        System.out.println("Project uuid: " + retVal);
        
        return retVal;
    }
    
    public String getUUID() {
        return uuid;
    }
    
    private void initGortEntityManagerThreaded() {
        Thread t = new Thread() {
            @Override
            public void run() {
                getLookup().lookup(GortDatabaseService.class).getGortEntityManager();
            }
        };
        
        t.start();
    }
    
    private void initCrowdAnalysisMonitorThreaded() {
        Thread t = new Thread() {
            @Override
            public void run() {
                getLookup().lookup(CrowdAnalysisService.class).monitor();
            }
        };
        
        t.setPriority(Thread.MIN_PRIORITY);
        
        t.start();
    }
    
    private void createDirectoryStructureThreaded() {
        Thread t = new Thread() {
            @Override
            public void run() {
                // create the directory structure for this project
                Util.createDirectoryStructure(GortProject.this);
            }
        };
        
        t.start();
    }
    
    private final class ActionProviderImpl implements ActionProvider {

        @Override
        public String[] getSupportedActions() {
            return new String[0];
        }

        @Override
        public void invokeAction(String string, Lookup lookup) throws IllegalArgumentException {
            //do nothing
        }

        @Override
        public boolean isActionEnabled(String string, Lookup lookup) throws IllegalArgumentException {
            return false;
        }

    }

    /**
     * Implementation of project system's ProjectInformation class
     */
    public final class Info implements ProjectInformation {

        private Icon icon;
        
        @Override
        public Icon getIcon() {
            if (icon == null) {
                icon = new ImageIcon(ImageUtilities.loadImage(
                    "org/cmuchimps/gort/modules/gortproject/resources/gort_16.png"));
            }
            
            return icon;
        }

        @Override
        public String getName() {
            return getProjectDirectory().getName();
        }

        @Override
        public String getDisplayName() {
            return getName();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener pcl) {
            //do nothing, won't change
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener pcl) {
            //do nothing, won't change
        }

        @Override
        public Project getProject() {
            return GortProject.this;
        }

    }
}
