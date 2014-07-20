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
package org.cmuchimps.gort.api.gort;

import org.cmuchimps.gort.modules.dataobject.App;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.dataobject.Traversal;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public class ProjectUtility {
    public static FileObject getProjectDirectory(Project project) {
        if (project == null) {
            return null;
        }
        
        return project.getProjectDirectory();
    }
    
    public static ProjectDirectoryService getProjectDirectoryService(Project project) {
        if (project == null) {
            return null;
        }
        
        return project.getLookup().lookup(ProjectDirectoryService.class);
    }
    
    public static GortDatabaseService getDatabaseService(Project project) {
        if (project == null) {
            return null;
        }
        
        return project.getLookup().lookup(GortDatabaseService.class);
    }
    
    public static GortEntityManager getGortEntityManager(Project project) {
        GortDatabaseService gds = getDatabaseService(project);
        return (gds != null) ? gds.getGortEntityManager() : null;
    }
    
    public static TraversalProviderService getTraversalProviderService(Project project) {
        if (project == null) {
            return null;
        }
        
        return project.getLookup().lookup(TraversalProviderService.class);
    }
    
    public static App getApp(Project project, FileObject apk)
    {
        if (project == null || apk == null) {
            return null;
        }
        
        return getApp(project, apk.getNameExt());
    }
    
    public static App getApp(Project project, String filename)
    {
        GortEntityManager gem = getGortEntityManager(project);
        
        if (gem == null) {
            return null;
        }
        
        return gem.selectApp(filename);
    }
    
    public static FileObject getAPK(Project project, String filename) {
        ProjectDirectoryService pds = getProjectDirectoryService(project);
        
        if (pds == null) {
            return null;
        }
        
        FileObject appDir = pds.getAppsDir();
        
        if (appDir == null) {
            return null;
        }
        
        return appDir.getFileObject(filename);
    }
    
    public static FileObject getAPK(Project project, Traversal traversal) {
        if (traversal == null) {
            return null;
        }
        
        App app = traversal.getApp();
        
        if (app == null) {
            return null;
        }
        
        return getAPK(project, app.getApk());
    }
    
}
