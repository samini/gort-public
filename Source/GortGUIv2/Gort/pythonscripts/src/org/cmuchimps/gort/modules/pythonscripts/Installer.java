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

import java.io.IOException;
import org.cmuchimps.gort.api.gort.GortCacheDirectoryService;
import org.cmuchimps.gort.modules.helper.CompressionHelper;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;

public class Installer extends ModuleInstall {
    
    private static Boolean DEBUG = false;
    
    public static String SCRIPTS_FOLDER_NAME = "Scripts";
    public static String PYTHON_SCRIPTS_FOLDER_NAME = "Scripts/Python";
    public static String ANDROGUARD_FILENAME_GZ = "androguard-1.9.tar.gz";
    public static String ANDROGUARD_FILENAME_TAR = "androguard-1.9.tar";
    public static String ANDROGUARD_FOLDER = "androguard-1.9";
    public static String ANDROGUARD_LIBRARY = "androguard";
    
    @Override
    public void restored() {
        initThreaded();
        //test();
    }
    
    public void init() {
        copyFilesToCache();
        //decompressLibs();
    }
    
    public void initThreaded() {
        Thread t = new Thread() {
            @Override
            public void run() {
                init();
            }
        };
        
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
    
    private void copyFilesToCache() {
        GortCacheDirectoryService gcds = GortCacheDirectoryService.getDefault();
        FileObject gortCacheDirectory = gcds.gortCacheDirectory();
        
        if (gortCacheDirectory == null) {
            return;
        }
        
        FileObject internalScriptsFolder = FileUtil.getConfigFile(String.format("Gort/%s", SCRIPTS_FOLDER_NAME));
        
        if (internalScriptsFolder == null) {
            return;
        }
        
        FileObject externalScriptsFolder = gortCacheDirectory.getFileObject(SCRIPTS_FOLDER_NAME);
        
        // Delete the old scripts folder
        if (DEBUG) {
            if (externalScriptsFolder != null) {
                try {
                    externalScriptsFolder.delete();
                    externalScriptsFolder = null;
                } catch (IOException ex) {
                    // Do nothing
                }
            }
        }
        
        if (externalScriptsFolder == null) {
            try {
                internalScriptsFolder.copy(gortCacheDirectory, SCRIPTS_FOLDER_NAME, null);
                decompressLibs();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    
    public static FileObject pythonScriptsFolder() {
        GortCacheDirectoryService gcds = GortCacheDirectoryService.getDefault();
        FileObject gortCacheDirectory = gcds.gortCacheDirectory();
        return gortCacheDirectory.getFileObject(PYTHON_SCRIPTS_FOLDER_NAME);
    }
    
    private static void decompressLibs() {
        decompressAndroGuard();
    }
    
    private static void decompressAndroGuard() {
        FileObject pyScripts = pythonScriptsFolder();
        
        if (pyScripts == null) {
            return;
        }
        
        FileObject tgz = pyScripts.getFileObject(ANDROGUARD_FILENAME_GZ);
        
        if (tgz == null || !tgz.canRead()) {
            System.out.println("Androguard file not found or not readable.");
        }
        
        // Decompress the file
        CompressionHelper.gunzip(tgz);
        
        FileObject tar = pyScripts.getFileObject(ANDROGUARD_FILENAME_TAR);
        
        if (tar == null || !tar.canRead()) {
            System.out.println("Androguard tar not found or not readable.");
        }
        
        CompressionHelper.untar(tar);
        
        // move the folder where the script files are
        
        FileObject folder = pyScripts.getFileObject(ANDROGUARD_FOLDER);
        
        if (folder == null || !folder.canRead()) {
            System.out.println("Androguard folder not found or not readable.");
        }
        
        FileObject androguard = folder.getFileObject(ANDROGUARD_LIBRARY);
        
        if (androguard == null || !androguard.canRead()) {
            System.out.println("Androguard library not found or not readable.");
        }
        
        // copy the library folder into the scripts folder
        try {
            androguard.copy(pyScripts, ANDROGUARD_LIBRARY, null);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    /*
    private void test() {
        System.out.println("Python Scripts module test running.");
        
        PythonScriptService pss = PythonScriptService.getDefault();
        SamplesService ss = SamplesService.getDefault();
        
        System.out.println("Samples service initialized: " + ss);
        
        FileObject fo = ss.sampleAPKFileObject();
        
        if (fo == null) {
            System.out.println("Sample APK file object is null");
        } else {
            System.out.println("Sample: " + fo);
        }
        
        System.out.println(fo.getPath());
        
        printArray(pss.appActivities(fo));
        printArray(pss.appFiles(fo));
        printArray(pss.appPermissions(fo));
        printArray(pss.appProviders(fo));
        printArray(pss.appReceivers(fo));
        printArray(pss.appServices(fo));
    }
    
    private void printArray(String[] s) {
        if (s == null || s.length <= 0) {
            return;
        }
        
        for (String t : s) {
            System.out.println(t);
        }
    }*/
}
