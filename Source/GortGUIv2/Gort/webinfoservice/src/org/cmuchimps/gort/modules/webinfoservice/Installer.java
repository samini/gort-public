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
package org.cmuchimps.gort.modules.webinfoservice;

import java.util.List;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    private static String SAMPLES_FOLDER_PATH = "Gort/Samples";
    private static String SAMPLE_IMAGE_FILENAME = "mobilitymonitor.png";
    
    @Override
    public void restored() {
        //test();
    }
    
    private void test() {
        testGooglePlayInfo();
        testAppEngineUpload();
        testWhoisService();
    }
    
    private void testGooglePlayInfo() {
        GooglePlayInfo gpi = GooglePlayInfo.GooglePlayInfoByPackage("com.yelp.android");
        if (gpi == null) {
            System.out.println("Google Play Info is null.");
        } else {
            System.out.print(gpi);
            System.out.println(gpi.getAppName());
            System.out.println(gpi.getAppDesciption());
            
            List<String> urls = gpi.getScreenshotURLs();
            for (String url : urls) {
                System.out.println(url);
            }
        }
    }
    
    private void testAppEngineUpload() {
        FileObject internalSamplesFolder = FileUtil.getConfigFile(SAMPLES_FOLDER_PATH);
        FileObject image = internalSamplesFolder.getFileObject(SAMPLE_IMAGE_FILENAME);
        System.out.println("Uploaded file: " + AppEngineUpload.uploadFile(image));
    }
    
    private void testWhoisService() {
        System.out.println(WhoisService.whois("8.8.8.8"));
    }
}
