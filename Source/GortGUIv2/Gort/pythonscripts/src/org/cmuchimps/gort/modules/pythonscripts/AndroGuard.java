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
package org.cmuchimps.gort.modules.pythonscripts;

import org.cmuchimps.gort.api.gort.ExternalProcessService;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public class AndroGuard {
    
    private static ExternalProcessService eps = ExternalProcessService.getDefault();
    
    //"org/cmuchimps/gort/modules/gortproject/resources/gort_16.png");
    private static String BASIC_INFO_FILENAME = "basicinfo.py";
    private static String DALVIK_INFO_FILENAME = "dalvikinfo.py";
    private static String ACTIVITIES_FLAG = "-a";
    private static String FILES_FLAG = "-f";
    private static String PERMISSIONS_FLAG = "-p";
    private static String PROVIDERS_FLAG = "-q";
    private static String RECEIVERS_FLAG = "-r";
    private static String SERVICES_FLAG = "-s";
    private static String ALL_OPTIONS_FLAG = "--all-options";
    
    private static String TAB_LABEL = "Static Analysis";
    
    public static boolean appActivities(String dbURL, Integer id, FileObject apk) {
        return execBasicInfo(dbURL, id, apk, ACTIVITIES_FLAG);
    }

    public static boolean appFiles(String dbURL, Integer id, FileObject apk) {
        return execBasicInfo(dbURL, id, apk, FILES_FLAG);
    }

    public static boolean appPermissions(String dbURL, Integer id, FileObject apk) {
        return execBasicInfo(dbURL, id, apk, PERMISSIONS_FLAG);
    }
    
    public static boolean appProviders(String dbURL, Integer id, FileObject apk) {
        return execBasicInfo(dbURL, id, apk, PROVIDERS_FLAG);
    }

    public static boolean appReceivers(String dbURL, Integer id, FileObject apk) {
        return execBasicInfo(dbURL, id, apk, RECEIVERS_FLAG);
    }

    public static boolean appServices(String dbURL, Integer id, FileObject apk) {
        return execBasicInfo(dbURL, id, apk, SERVICES_FLAG);
    }
    
    public static boolean appAllOptions(String dbURL, Integer id, FileObject apk) {
        return execBasicInfo(dbURL, id, apk, ALL_OPTIONS_FLAG);
    }
    
    public static boolean dalvikAllOptions(String dbURL, Integer id, FileObject apk) {
        return execDalvikInfo(dbURL, id, apk, ALL_OPTIONS_FLAG);
    }
    
    private static boolean execBasicInfo(String dbURL, Integer id, FileObject apk, String flag) {
        return execScript(BASIC_INFO_FILENAME, dbURL, id, apk, flag);
    }
    
    private static boolean execDalvikInfo(String dbURL, Integer id, FileObject apk, String flag) {
        return execScript(DALVIK_INFO_FILENAME, dbURL, id, apk, flag);
    }
    
    //private static String execBasicInfo(FileObject apk, String flag) {
    private static boolean execScript(String scriptFilename, String dbURL, Integer id, FileObject apk, String flag) {
        if (dbURL == null || dbURL.isEmpty()) {
            return false;
        }
        
        if (id == null || id.intValue() < 0) {
            return false;
        }
        
        if (apk == null || !apk.canRead()) {
            return false;
        }
        
        FileObject pythonScriptFolder = Installer.pythonScriptsFolder();
        
        FileObject script = pythonScriptFolder.getFileObject(scriptFilename);
        String scriptPath = script.getPath();
        String path = apk.getPath();
        
        String command = String.format("python %s -d %s -j %s -i %s %s", 
                scriptPath, dbURL, id.toString(), path, flag);
        
        System.out.println(command);
        
        String output = eps.output(command, AndroGuard.TAB_LABEL);
        //int exitCode = eps.exitCode(command);
        return (output != null);
    }
    
    private static String[] processBasicInfoOutput(String s) {
        if ((s == null) || s.isEmpty()) {
            return null;
        }
        
        // remove all instances of rectangle brackets, single quotes, and spaces
        s = s.replaceAll("\\]|\\[|'| ", "");
        
        if (s.isEmpty()) {
            return null;
        }
        
        return s.split(",");
    }
    
}
