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
package org.cmuchimps.gort.modules.traverser;

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.prefs.Preferences;
import org.cmuchimps.gort.api.gort.AdbService;
import org.cmuchimps.gort.api.gort.ExternalProcessService;
import org.cmuchimps.gort.api.gort.FileChooserService;
import org.cmuchimps.gort.api.gort.TraverserService;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author shahriyar
 */
@NbBundle.Messages({"LBL_TraversalTab=Traversal", 
    "MSG_NoSquiddyTool=Gort cannot find traversal tool. Stopping traversal."})
@ServiceProvider(service=TraverserService.class)
public class TraverserServiceProvider extends TraverserService {

    // Note this class requires tema.android-adapter to be installed
    // and the command to be accessable in the Gort environment
    private static String squiddyPath = null;
    
    private static File tmp;
    
    private static final String KEY_SQUIDDY_EXEC = "squiddyexec";
    
    private static final String ANDROID_ADAPTER = "android-adapter";
    
    private static final int TRAVERSAL_DURATION_MINUTES = 30;
    
    private static synchronized String getSquiddyPath() {
        if (squiddyPath == null || squiddyPath.isEmpty() ||
                !(new File(squiddyPath).exists())) {
            
            // Attempt to get the path from Preferences
            Preferences prefs = getPreferences();
            String potentialPath = prefs.get(KEY_SQUIDDY_EXEC, null);
            
            File squiddyFile = null;
            
            if (potentialPath != null) {
                squiddyFile = new File(potentialPath);
            }
            
            if (squiddyFile == null || !squiddyFile.exists()) {
                // if not successful ask the user for the adb location

                try {
                    EventQueue.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            FileChooserService fcs = FileChooserService.getDefault();
                            tmp = fcs.locateFile("Select the Squiddy binary");
                        }
                        
                    });
                    
                    squiddyFile = tmp;
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                }

            }
            
            if (squiddyFile != null && squiddyFile.exists() && 
                    squiddyFile.getAbsolutePath().toLowerCase().contains(ANDROID_ADAPTER)) {
                squiddyPath = squiddyFile.getAbsolutePath();
                prefs.put(KEY_SQUIDDY_EXEC, squiddyPath);
            }
            
        }
        
        return squiddyPath;
    }
    
    private static Preferences getPreferences() {
        return Preferences.userNodeForPackage(TraverserServiceProvider.class);
    }
    
    // Make sure all the tools used by the traversal are in the PATH variable
    // accessed by the JAVA runtime exec. To see what this is simply do:
    // System.out.println(System.getenv("PATH"));
    // This is not the path variable set in .bashrc or another location but
    // the global path variable. Under our current setup this value is set under:
    // /private/etc/paths
    
    @Override
    public boolean traverse(String dbURL, Integer id, FileObject traversalDirectory, String target) {
        if (dbURL == null || dbURL.isEmpty()) {
            return false;
        }
        
        if (id == null || id.intValue() < 0) {
            return false;
        }
        
        if (traversalDirectory == null || !traversalDirectory.canRead()) {
            return false;
        }
        
        if (target == null || target.isEmpty()) {
            return false;
        }
        
        ExternalProcessService eps = ExternalProcessService.getDefault();
        
        if (eps == null) {
            return false;
        }
        
        // get the traversal executable similar to the povray project
        if (getSquiddyPath() == null) {
            java.awt.EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    NotifyDescriptor nd = new NotifyDescriptor.Message(
                            NbBundle.getMessage(TraverserServiceProvider.class, "MSG_NoSquiddyTool"),
                            NotifyDescriptor.INFORMATION_MESSAGE);
                    DialogDisplayer.getDefault().notify(nd);
                }
                
            });
            
            return false;
        }
        
        System.out.println(System.getenv("PATH"));
        
        String command = String.format("%s -e -d %s -n %s -f %s -m %d %s", getSquiddyPath(),
                dbURL, id.toString(), traversalDirectory.getPath(),
                TRAVERSAL_DURATION_MINUTES, target);
        
        String output = eps.output(command, 
                NbBundle.getMessage(TraverserServiceProvider.class, "LBL_TraversalTab"));
        
        return (output != null);
    }
    
    private void setPath() {
        AdbService as = AdbService.getDefault();
        
        if (as == null) {
            return;
        }
        
        String adbPath = as.adbPath();
        
        if (adbPath == null || adbPath.isEmpty()) {
            return;
        }
    }

}
