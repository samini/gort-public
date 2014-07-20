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

import java.io.IOException;
import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author shahriyar
 */
@ServiceProvider(service=ProjectFactory.class)
public class GortProjectFactory implements ProjectFactory {
    
    public static final String PROJECT_DIR = "gtproject";
    public static final String PROJECT_PROPFILE = "project.properties";

    @Override
    public boolean isProject(FileObject projectDirectory) {
        return projectDirectory.getFileObject(PROJECT_DIR) != null;
    }

    @Override
    public Project loadProject(FileObject projectDirectory, ProjectState projectState) throws IOException {
        return isProject(projectDirectory) ? new GortProject(projectDirectory, projectState) : null;
    }

    @Override
    public void saveProject(Project project) throws IOException, ClassCastException {

        FileObject projectRoot = project.getProjectDirectory();
        if (projectRoot.getFileObject(PROJECT_DIR) == null) {
            throw new IOException ("Project dir " + projectRoot.getPath() + " deleted," +
                    " cannot save project");
        }

        GortProject gortProject = project.getLookup().lookup(GortProject.class);
        ProjectDirectoryService pds = gortProject.getLookup().lookup(ProjectDirectoryService.class);
        
        //Force creation of directory structure if anything was deleted
        Util.createDirectoryStructure(project);

        gortProject.saveProperties();
        
        /* // Original code from povray tutorial
        //Find the properties file pvproject/project.properties,
        //creating it if necessary
        String propsPath = PROJECT_DIR + "/" + PROJECT_PROPFILE;
        FileObject propertiesFile = projectRoot.getFileObject(propsPath);
        if (propertiesFile == null) {
            //Recreate the properties file if needed
            propertiesFile = projectRoot.createData(propsPath);
        }

        Properties properties = (Properties) project.getLookup().lookup(Properties.class);
        File f = FileUtil.toFile(propertiesFile);
        properties.store(new FileOutputStream(f), "Gort Project Properties");
        */
    }
    
}
