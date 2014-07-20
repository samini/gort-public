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
package org.cmuchimps.gort.modules.dynamicanalysis;

import java.util.prefs.Preferences;
import org.cmuchimps.gort.api.gort.AdbService;
import org.cmuchimps.gort.api.gort.GortDatabaseService;
import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.cmuchimps.gort.api.gort.TraverserService;
import org.cmuchimps.gort.api.gort.analysis.DynamicAnalysisService;
import org.cmuchimps.gort.modules.dataobject.App;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.netbeans.api.project.Project;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 *
 * @author shahriyar
 */
public class DynamicAnalysisServiceProvider extends DynamicAnalysisService {

    private static final String KEY_TARGET_DEVICE = "targetdevice";
    
    // The following is to turn off uninstalling and installing the apk
    // turned off for testing purposes
    private static final boolean INSTALL_APK = false;
    private static final boolean UNINSTALL_APK = false;
    
    // String representing the target devices serial
    private static String target;
    
    public DynamicAnalysisServiceProvider(Project project) {
        super(project);
    }
    
    @Override
    public void analyze(FileObject apk) {
        if (project == null) {
            return;
        }
        
        if (apk == null) {
            return;
        }
        
        analyzeInThread(apk);
    }
    
    @NbBundle.Messages("MSG_NoReachableTarget=Gort cannot reach target traversal device. Stopping dynamic analysis.")
    private void analyzeInThread(final FileObject apk) {
        Thread t = new Thread() {
            @Override
            public void run() {
                
                System.out.println("Performing dynamic analysis on " + apk.getNameExt());
        
                // Step 0: Obtain the application index and path and database connection url
                GortDatabaseService gds = project.getLookup().lookup(GortDatabaseService.class);
                
                if (gds == null) {
                    return;
                }
                
                GortEntityManager gem = gds.getGortEntityManager();
                
                if (gem == null) {
                    return;
                }
                
                App app = gem.selectApp(apk.getNameExt());
                
                if (app == null) {
                    return;
                }
                
                Integer id = app.getId();
                
                if (id == null || id < 0) {
                    return;
                }
                
                String dbURL = gds.dbConnectionURL();
                
                if (dbURL == null || dbURL.isEmpty()) {
                    return;
                }
                
                String packageName = app.getPackage();
                
                // get the directory service so we can find out where to put the results
                ProjectDirectoryService pds = project.getLookup().lookup(ProjectDirectoryService.class);
                
                if (pds == null) {
                    return;
                }
                
                FileObject traversalDirectory = pds.getTraversalDir();

                // step 1: get the traversal target device
                if (getTarget() == null) {
                    // present a dialog that cannot find an attach device and return
                    NotifyDescriptor nd = new NotifyDescriptor.Message(
                            NbBundle.getMessage(DynamicAnalysisServiceProvider.class,
                            "MSG_NoReachableTarget"), NotifyDescriptor.INFORMATION_MESSAGE);
                    DialogDisplayer.getDefault().notify(nd);
                    return;
                }
                
                // Step 2: uninstall any previusly installed version of the app
                uninstallPackage(packageName);
                
                // Step 3: (re)-install the application
                installPackage(apk);

                // Step 4: traverse the application
                System.out.println("Traversing application " + apk.getNameExt());
                TraverserService ts = TraverserService.getDefault();
                
                if (ts != null) {
                    ts.traverse(dbURL, id, traversalDirectory, target);
                }
            }
        };
        
        t.start();
    }
    
    private boolean uninstallPackage(String packageName) {
        if (!UNINSTALL_APK) {
            return false;
        }
        
        System.out.println("Uninstalling previous install version of the app");
        
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        
        AdbService asp = AdbService.getDefault();
        
        if (asp == null) {
            return false;
        }
        
        return asp.uninstallPackage(packageName);
    }
    
    private boolean installPackage(FileObject apk) {
        if (!INSTALL_APK) {
            return false;
        }
        
        System.out.println("Installing the app on the phone...");
        
        AdbService asp = AdbService.getDefault();
        
        if (asp == null) {
            return false;
        }
        
        return asp.installPackage(apk);
    }
    
    @NbBundle.Messages({"LBL_InputTargetSerial=Provide Traversal Device Serial Number", "LBL_Serial=Attached Device Serial"})
    private static String getTarget() {
        // It is important that we have a adb service accessible
        AdbService as = AdbService.getDefault();
        
        if (as == null) {
            return null;
        }
        
        if (target == null || target.isEmpty() || !as.deviceConnected(target)) {
            // Attempt to get the target from preferences
            Preferences prefs = getPreferences();
            String potentialTarget = prefs.get(KEY_TARGET_DEVICE, null);
            
            if (as.deviceConnected(potentialTarget)) {
                target = potentialTarget;
            } else {
                Object result = null;
                String userInput = null;
                
                // ask the user to input the Serial into a dialog box
                NotifyDescriptor.InputLine input = new NotifyDescriptor.InputLine(
                        NbBundle.getMessage(DynamicAnalysisServiceProvider.class, "LBL_Serial"),
                        NbBundle.getMessage(DynamicAnalysisServiceProvider.class, "LBL_InputTargetSerial"));

                result = DialogDisplayer.getDefault().notify(input);
                
                if (result == NotifyDescriptor.OK_OPTION && input.getInputText() != null) {
                    userInput = input.getInputText().trim();
                }
                
                if (userInput != null) {
                    target = userInput;
                }
            }
            
            // if target is still not connect return null
            if (as.deviceConnected(target)) {
                prefs.put(KEY_TARGET_DEVICE, target);
            } else {
                target = null;
            }
        }
        
        return target;
    }
    
    private static Preferences getPreferences() {
        return Preferences.userNodeForPackage(DynamicAnalysisService.class);
    }
}
