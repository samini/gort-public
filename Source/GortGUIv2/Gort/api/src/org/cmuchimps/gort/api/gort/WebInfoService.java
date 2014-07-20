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

import java.util.List;
import org.cmuchimps.gort.modules.dataobject.WhoisRecord;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author shahriyar
 */
public abstract class WebInfoService {
    
    public static final String ANDROID_MARKET_URL = "https://market.android.com/details?id=";
    
    public static WebInfoService getDefault() {
        return Lookup.getDefault().lookup(WebInfoService.class);
    }
    
    public abstract String appDescription(String packageName);
    
    public abstract String appName(String packageName);
    
    public abstract String appCategory(String packageName);
    
    public abstract String appDeveloper(String packageName);
    
    public abstract List<FileObject> downloadAppScreenshots(FileObject targetDir, String packageName);
    
    public abstract String uploadFile(FileObject fo);
    
    public abstract WhoisRecord serverInfo(String ip);
}
