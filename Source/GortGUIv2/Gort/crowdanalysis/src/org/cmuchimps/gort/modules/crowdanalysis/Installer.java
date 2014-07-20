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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.cmuchimps.gort.api.gort.GortCacheDirectoryService;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        copyTaskFilesThreaded();
    }
    
    private void copyTaskFilesThreaded() {
        Thread t = new Thread() {
            @Override
            public void run() {
                copyTaskFiles();
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
    
    private void copyTaskFiles() {
        System.out.println("Copying crowd task files");
        
        GortCacheDirectoryService gcds = GortCacheDirectoryService.getDefault();
        FileObject gortCacheDirectory = gcds.gortCacheDirectory();
        
        if (gortCacheDirectory == null || !gortCacheDirectory.canRead()) {
            return;
        }
        
        FileObject externalCrowdTasks = gortCacheDirectory.getFileObject("CrowdTasks");
        
        if (externalCrowdTasks != null) {
            try {
                externalCrowdTasks.delete();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        
        FileObject internalCrowdTasks = FileUtil.getConfigFile(String.format("Gort/CrowdTasks"));
        
        if (internalCrowdTasks == null) {
            return;
        }
        
        try {
            internalCrowdTasks.copy(gortCacheDirectory, internalCrowdTasks.getNameExt(), null);
        } catch (IOException ex) {
            System.out.println("Could not copy over internal crowd tasks files");
        }
    }
    
    public static FileObject getCrowdTaskDirectory() {
        GortCacheDirectoryService gcds = GortCacheDirectoryService.getDefault();
        FileObject gortCacheDirectory = gcds.gortCacheDirectory();
        
        if (gortCacheDirectory == null || !gortCacheDirectory.canRead()) {
            return null;
        }
        
        FileObject crowdTasksDirectory = gortCacheDirectory.getFileObject("CrowdTasks");
        
        return crowdTasksDirectory;
    }
    
    public static FileObject getCrowdTaskFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        
        FileObject crowdTasksDir = getCrowdTaskDirectory();
        
        if (crowdTasksDir == null) {
            return null;
        }
        
        return crowdTasksDir.getFileObject(relativePath);
    }
    
    private void testMovieSurvey() {
        System.out.println("Testing mturk java APIs");
        
        // print out the working directory. mturk.properties should be placed here
        // directory returned: gort/Source/GortGUIv2/Gort/mturk
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        System.out.println("Current relative path is: " + s);
        
        MovieSurvey app = new MovieSurvey();
        // Create the new HIT.
        app.createMovieSurvey();
    }

}
