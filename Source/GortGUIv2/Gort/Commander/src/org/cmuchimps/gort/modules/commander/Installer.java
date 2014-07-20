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
package org.cmuchimps.gort.modules.commander;

import org.cmuchimps.gort.api.gort.GortCommanderService;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    /*
    @Override
    public void restored() {
        //test();
    }*/

    @Override
    public void close() {
        super.close();
        
        // close the socket
        GortCommanderServiceProvider.close();
    }
    
    private void test() {
        GortCommanderService gcs = GortCommanderService.getDefault();
        String packageName = gcs.packageName("yelp");
        String processName = gcs.processName(packageName);
        int fileSize = gcs.fileSize(packageName);
        String md5 = gcs.md5(packageName);
        String sourceDir = gcs.sourceDir(packageName);
        
        System.out.println(packageName);
        System.out.println(processName);
        System.out.println(sourceDir);
        System.out.println(fileSize);
        System.out.println(md5);
    }
    
}
