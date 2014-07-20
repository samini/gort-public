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

import org.openide.util.Lookup;

/**
 *
 * @author shahriyar
 */
public abstract class GortCommanderService {
    public static final String COMMAND_PACKAGE = "package";
    public static final String COMMAND_PROCESS = "process";
    public static final String COMMAND_SOURCE_DIR = "sourcedir";
    public static final String COMMAND_MD5 = "md5";
    public static final String COMMAND_FILE_SIZE = "filesize";
    
    public static GortCommanderService getDefault() {
        return Lookup.getDefault().lookup(GortCommanderService.class);
    }
    
    public abstract String packageName(String appName);
    
    public abstract String processName(String packageName);
    
    public abstract String sourceDir(String packageName);
    
    public abstract String md5(String packageName);
    
    public abstract int fileSize(String packageName);
}
