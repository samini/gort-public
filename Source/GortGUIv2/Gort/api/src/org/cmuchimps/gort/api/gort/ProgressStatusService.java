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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.cmuchimps.gort.modules.dataobject.Progress;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public abstract class ProgressStatusService {
    
    // Similar in concept to the RenderService in Pov-ray project which
    // checks the progress on Pov-ray rendering
    // https://platform.netbeans.org/tutorials/nbm-povray-8.html
    
    // Location of progress directory under the project
    //ublic static final String PROGRESS_DIR = "progress"; //NOI18N
    
    public static final String PROGRESS_FILE_TYPE_APK = "apk"; //NOI18N
    
    // Gort Progress file type
    public static final String PROGRESS_FILE_TYPE_GTP = "gtp"; //NOI18N
    
    public static final String[] PROGRESS_FILE_TYPES = {
        PROGRESS_FILE_TYPE_APK,
        PROGRESS_FILE_TYPE_GTP,
    };
    
    // keep a link to the associated project
    protected Project project;
    
    // a map to contain the listeners for ProgressStatusService
    private Map statusListeners = new HashMap();
    
    public ProgressStatusService(Project project) {
        this.project = project;
    }
    
    // Only listens to files that are of the types in PROGRESS_FILE_TYPES
    public final void addChangeListener(FileObject fo, ChangeListener l) {
        
        if (fo == null) {
            return;
        }
        
        if (l == null) {
            return;
        }
        
        // Check that the file is in the list of accepted files
        if (!inAcceptedFileTypes(fo)) {
            return;
        }
        
        // Get the string name of the file. We do not need to actually
        // hold on to the FileObject in memory, holding the string path
        // is less expensive
        String path = fo.getPath();
        
        if (path == null) {
            return;
        }
        
        System.out.println("ProgressStatusService.addChangeListener called on " + path);
        
        // Use a weak reference to listeners, rather than have a remove
        // listener method. This will allow our nodes to be garbage collected
        // if they are hidden
        Reference listenerRef = new WeakReference(l);
        
        synchronized (statusListeners) {
            
            List listeners = (List) statusListeners.get(path);
            
            if (listeners == null) {
                listeners = new LinkedList();
                statusListeners.put(path, listeners);
            }
            
            // Add the weak reference to the list of listeners interested in 
            // the particular file
            listeners.add(listenerRef);
        }
        
        // Call the callback method
        listenerAdded(fo, l);
    }
    
    public final void removeChangeListener(FileObject fo) {
        if (fo == null) {
            return;
        }
        
        String path = fo.getPath();
        
        if (path == null) {
            return;
        }
        
        System.out.println("ProgressStatusService.removeChangeListener called on " + path);
        
        synchronized (statusListeners) {
            
            if (!statusListeners.containsKey(path)) {
                return;
            }
            
            List listeners = (List) statusListeners.get(path);
            
            if (listeners != null && !listeners.isEmpty()) {
                listeners.clear();
            }
            
            statusListeners.remove(path);
        }
        
        noLongerListeningTo(fo);
    }
    
    protected abstract void listenerAdded(FileObject fo, ChangeListener l);
    
    protected abstract void noLongerListeningTo(FileObject fo);
    
    protected final void fireChange(FileObject fo) {
        System.out.println("fireChange called");
        
        if (fo == null) {
            return;
        }
        
        List fireTo = listenersForFile(fo);
        
        if (fireTo != null && !fireTo.isEmpty()) {
            for (Object o : fireTo) {
                if (o == null) {
                    continue;
                }
                
                ChangeListener l = (ChangeListener) o;
                l.stateChanged(new ChangeEvent(this));
            }
        } else {
            System.out.println("Change listener is not valid.");
        }
    }
    
    public List<ChangeListener> listenersForFile(FileObject fo) {
        if (fo == null) {
            return null;
        }
        
        String path = fo.getPath();
        
        if (path == null) {
            return null;
        }
        
        List<ChangeListener> retVal = null;
        
        Boolean stillListening = null;
        
        synchronized (statusListeners) {
            List listeners = (List) statusListeners.get(path);
            
            if (listeners == null) {
                return null;
            }
            
            if (!listeners.isEmpty()) {
                retVal = new ArrayList(3);
                
                for (Iterator i = listeners.iterator(); i.hasNext(); ) {
                    Reference ref = (Reference) i.next();
                    ChangeListener l = (ChangeListener) ref.get();
                    if (l != null) {
                        retVal.add(l);
                    } else {
                        i.remove();
                    }
                }
            }
            
            if (listeners.isEmpty()) {
                statusListeners.remove(path);
                stillListening = Boolean.FALSE;
            } else {
                stillListening = Boolean.TRUE;
            }
        }
        
        // call the listener removal method outside the synch block
        // stillListening will be null if we were never listening at all
        if (stillListening != null && Boolean.FALSE.equals(stillListening)) {
            noLongerListeningTo(fo);
        }
        
        return retVal;
    }
    
    public boolean inAcceptedFileTypes(FileObject fo) {
        if (fo == null) {
            return false;
        }
        
        String ext = fo.getExt();
        
        if (ext == null || ext.isEmpty()) {
            return false;
        }
        
        for (String t : PROGRESS_FILE_TYPES) {
            if (t.equals(ext.trim().toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isAPK(FileObject fo) {
        return isOfType(fo, PROGRESS_FILE_TYPE_APK);
    }
    
    public boolean isGTP(FileObject fo) {
        return isOfType(fo, PROGRESS_FILE_TYPE_GTP);
    }
    
    private boolean isOfType(FileObject fo, String ft) {
        if (fo == null) {
            return false;
        }
        
        if (ft == null) {
            return false;
        }
        
        String ext = fo.getExt();
        
        if (ext == null) {
            return false;
        }
        
        return ft.trim().toLowerCase().equals(ext.trim().toLowerCase());
    }
    
    public abstract FileObject gtpForAPK(FileObject apk);
    
    public abstract FileObject apkForGTP(FileObject gtp);
    
    public abstract Progress progressForAPK(FileObject apk);
}
