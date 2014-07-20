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
package org.cmuchimps.gort.modules.samples;

import java.io.IOException;
import org.cmuchimps.gort.api.gort.GortCacheDirectoryService;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        copyFilesToCache();
    }
    
    private void copyFilesToCache() {
        
        GortCacheDirectoryService gcds = GortCacheDirectoryService.getDefault();
        FileObject gortCacheDirectory = gcds.gortCacheDirectory();
        
        if (gortCacheDirectory == null) {
            return;
        }
        
        FileObject externalSamplesFolder = gortCacheDirectory.getFileObject(Constants.SAMPLES_FOLDER_NAME);
        FileObject internalSamplesFolder = FileUtil.getConfigFile(Constants.SAMPLES_FOLDER_PATH);
        
        if (externalSamplesFolder == null) {
            
            System.out.println("External samples folder does not exist.");
            
            // Samples folder did not exist copy the entire directory
            try {
                System.out.println("Copying samples directory to gort cache: " + gortCacheDirectory);
                internalSamplesFolder.copy(gortCacheDirectory, Constants.SAMPLES_FOLDER_NAME, null);
            } catch (IOException ex) {
                //Exceptions.printStackTrace(ex);
            }
        }
    }
}
