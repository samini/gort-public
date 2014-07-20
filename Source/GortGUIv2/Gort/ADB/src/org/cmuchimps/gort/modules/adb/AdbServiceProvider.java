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
package org.cmuchimps.gort.modules.adb;

import java.io.File;
import java.util.prefs.Preferences;
import org.cmuchimps.gort.api.gort.AdbService;
import org.cmuchimps.gort.api.gort.ExternalProcessService;
import org.cmuchimps.gort.api.gort.FileChooserService;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author shahriyar
 */
@ServiceProvider(service=AdbService.class)
public class AdbServiceProvider extends AdbService{
    
    private static final String ADB_FORWARD = "forward";
    
    private static final String ADB_DEVICES = "devices";
    
    private static final String ADB_INSTALL = "install";
    
    private static final String ADB_UNINSTALL = "uninstall";
    
    private static final String KEY_ADB_EXEC = "adbtool";
    
    private static String adbPath;

    ExternalProcessService eps = ExternalProcessService.getDefault();
    
    private static String getAdbPath() {
        if (adbPath == null || adbPath.isEmpty()) {
            
            // Attempt to get the path from Preferences
            Preferences prefs = getPreferences();
            String potentialPath = prefs.get(KEY_ADB_EXEC, null);
            
            File adbFile = null;
            
            if (potentialPath != null) {
                adbFile = new File(potentialPath);
            }
            
            if (adbFile == null || !adbFile.exists()) {
                // if not successful ask the user for the adb location
                FileChooserService fcs = FileChooserService.getDefault();
                adbFile = fcs.locateFile("Please select the adb tool file location.");
            }
            
            if (adbFile != null && adbFile.exists() && adbFile.getAbsolutePath().toLowerCase().contains("adb")) {
                adbPath = adbFile.getAbsolutePath();
                prefs.put(KEY_ADB_EXEC, adbPath);
            }
            
        }
        
        return adbPath;
    }
    
    private static Preferences getPreferences() {
        return Preferences.userNodeForPackage(AdbServiceProvider.class);
    }
    
    @Override
    public boolean forward(String host, String device) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        
        if (device == null || device.isEmpty()) {
            return false;
        }
        
        if (getAdbPath() == null) {
            return false;
        }
        
        String command = String.format("%s %s %s %s", getAdbPath(), ADB_FORWARD, host, device);
        
        int exitCode = eps.exitCode(command);
        
        if (exitCode == 0) {
            System.out.println("Successfully forwarded host port " + host +
                    " to device port " + device + '.');
        } else {
            System.out.println("Was not able to forward port with adb.");
        }
        
        return (exitCode == 0);
    }
    
    @Override
    public boolean deviceConnected(String serial) {
        if (serial == null || serial.isEmpty()) {
            return false;
        }
        
        String result = eps.output(String.format("%s %s", getAdbPath(), ADB_DEVICES));
        
        if (result == null || result.isEmpty()) {
            return false;
        }
        
        return result.toLowerCase().contains(serial.trim().toLowerCase());
    }

    @Override
    public boolean installPackage(FileObject apk) {
        if (apk == null || !apk.canRead()) {
            return false;
        }
        
        if (getAdbPath() == null) {
            return false;
        }
        
        String path = apk.getPath();
        
        if (path == null) {
            return false;
        }
        
        String command = String.format("%s %s %s", getAdbPath(), ADB_INSTALL, path); 
        
        String result = eps.output(command);
        return (result != null && result.length() > 0);
    }
    
    @Override
    public boolean uninstallPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        
        if (getAdbPath() == null) {
            return false;
        }
        
        String command = command = String.format("%s %s %s", getAdbPath(), ADB_UNINSTALL, packageName);
        
        String result = eps.output(command);
        return (result != null && result.length() > 0);
    }
    
    @Override
    public String adbPath() {
        return AdbServiceProvider.getAdbPath();
    }
}
