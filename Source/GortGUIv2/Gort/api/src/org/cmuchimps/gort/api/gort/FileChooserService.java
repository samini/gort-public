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

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

/**
 *
 * @author shahriyar
 */
public abstract class FileChooserService {
    
    public static FileChooserService getDefault() {
        FileChooserService result = Lookup.getDefault().lookup(FileChooserService.class); 
        if (result == null) { 
           result = new FileChooserServiceProvider(); 
        } 
        return result;
    }
    
    public abstract File locateFile(String dialogTitle);
    
    public abstract File locateFolder(String dialogTitle);
    
    public abstract File locateFileOrFolder(String dialogTitle);
    
    private static class FileChooserServiceProvider extends FileChooserService {
        
        @Override
        public File locateFile(String dialogTitle) {
            return locate(dialogTitle, JFileChooser.FILES_ONLY);
        }
        
        @Override
        public File locateFolder(String dialogTitle) {
            return locate(dialogTitle, JFileChooser.DIRECTORIES_ONLY);
        }
        
        @Override
        public File locateFileOrFolder(String dialogTitle) {
            return locate(dialogTitle, JFileChooser.FILES_AND_DIRECTORIES);
        }
        
        public File locate(String dialogTitle, int selectionMode) {
            final JFileChooser jfc = new JFileChooser();
            jfc.setDialogTitle(dialogTitle);
            jfc.setFileSelectionMode(selectionMode);
            jfc.showOpenDialog(WindowManager.getDefault().getMainWindow());
            if (SwingUtilities.isEventDispatchThread()) {
                jfc.showOpenDialog(WindowManager.getDefault().getMainWindow());
            } else {
                // make sure to show dialog in the UI thread
                try {
                    EventQueue.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            jfc.showOpenDialog(WindowManager.getDefault().getMainWindow());
                        }
                    });
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            }
            File result = jfc.getSelectedFile();
            return result;
        }
    }
}
