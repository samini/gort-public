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

import org.cmuchimps.gort.api.gort.GortCacheDirectoryService;
import org.cmuchimps.gort.api.gort.SamplesService;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author shahriyar
 */
@ServiceProvider(service=SamplesService.class)
public class SamplesServiceProvider extends SamplesService {
    
    @Override
    public FileObject samplesFolderFileObject() {
        GortCacheDirectoryService gcds = GortCacheDirectoryService.getDefault();
        FileObject gortCacheDirectory = gcds.gortCacheDirectory();
        return gortCacheDirectory.getFileObject(Constants.SAMPLES_FOLDER_NAME);
    }

    @Override
    public FileObject sampleAPKFileObject() {
        FileObject tmp = samplesFolderFileObject();
        return (tmp != null) ? tmp.getFileObject(Constants.SAMPLE_APK_FILENAME) : null;
    }
    
}
