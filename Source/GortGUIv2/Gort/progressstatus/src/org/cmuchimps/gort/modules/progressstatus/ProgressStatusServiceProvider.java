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
package org.cmuchimps.gort.modules.progressstatus;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.event.ChangeListener;
import org.cmuchimps.gort.api.gort.ProgressStatusService;
import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.cmuchimps.gort.modules.dataobject.Progress;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileAlreadyLockedException;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.util.Exceptions;

/**
 *
 * @author shahriyar
 */
//@ServiceProvider(service=ProgressStatusService.class)
public class ProgressStatusServiceProvider extends ProgressStatusService implements FileChangeListener {
    
    // Keep a list of all files listened to
    // Need a hard reference to file objects to continously listen to them
    // http://osdir.com/ml/java.netbeans.modules.openide.devel/2005-04/msg00028.html
    private Set<FileObject> filesListenedTo = new HashSet<FileObject>();
    
    public ProgressStatusServiceProvider(Project project) {
        super(project);
    }
    
    @Override
    public void fileFolderCreated(FileEvent fe) {
        // Should not get called since we are just listening to APKs and GTPs
        System.out.println("ProgressStatusServiceProvider fileFolderCreated called");
    }

    @Override
    public void fileDataCreated(FileEvent fe) {
        // Should not get called since we are just listening to APKs and GTPs
        System.out.println("ProgressStatusServiceProvider fileDataCreated called");
    }

    @Override
    public void fileChanged(FileEvent fe) {
        System.out.println("ProgressStatusServiceProvider fileChanged called");
        
        if (fe == null) {
            return;
        }
        
        FileObject fo = fe.getFile();
        
        if (fo == null) {
            return;
        }
        
        System.out.println("ProgressStatusServiceProvider fileChanged " + fo.getPath());
        
        // if the file is an APK or GTP fire the change listener
        if (isAPK(fo) || isGTP(fo)) {
            fireChange(fo);
        }
    }

    @Override
    public void fileDeleted(FileEvent fe) {
        System.out.println("ProgressStatusServiceProvider fileDeleted called");
        
        if (fe == null) {
            return;
        }
        
        FileObject fo = fe.getFile();
        
        if (fo == null) {
            return;
        }
        
        System.out.println("ProgressStatusServiceProvider fileDeleted " + fo.getPath());
        
        // if the file is an apt, stop listening to it and its GTP
        if (isAPK(fo)) {
            FileObject gtp = gtpForAPK(fo);
            removeChangeListener(gtp);
            removeChangeListener(fo);
        } else if (isGTP(fo)) {
            fireChange(fo);
            FileObject apk = apkForGTP(fo);
            removeChangeListener(fo);
            
            // Create a new GTP for the apk
            if (apk != null) {
                FileObject gtp = gtpForAPK(apk);
                // get the listener object for APK
                List<ChangeListener> listeners = listenersForFile(apk);
                for (ChangeListener cl : listeners) {
                    if (cl == null) {
                        continue;
                    }
                    
                    super.addChangeListener(gtp, cl);
                }
            }
        }
        
    }

    @Override
    public void fileRenamed(FileRenameEvent fre) {
        System.out.println("ProgressStatusServiceProvider fileRenamed called");
        
        if (fre == null) {
            return;
        }
        
        FileObject fo = fre.getFile();
        
        if (fo == null) {
            return;
        }
        
        System.out.println("ProgressStatusServiceProvider fileRenamed " + fo.getPath());
        
        // if the APK file is renamed, the Progress file should also be renamed
        boolean isAPK = isAPK(fo);
        
        if (!isAPK) {
            return;
        }
        
        FileObject gtp = gtpForAPK(fo);
        
        // stop listening to the progress file
        removeChangeListener(gtp);
        
        // also rename the associated progress file
        
        FileLock lock = null;
        
        try {
            lock = gtp.lock();
            
            //String originalPath = gtp.getPath();
            
            gtp.rename(lock, fo.getName(), gtp.getExt());
            
            /*
            try {
                // old file remains after the rename...?
                // delete any remainig file
                File oldFile = new File(originalPath);
                FileUtil.toFileObject(oldFile).delete(lock);
            } catch (Exception e) {
                // ignore
            }*/
            
        } catch (FileAlreadyLockedException e) {
            e.printStackTrace();
            // TODO: Try to rename again later
            // For now just ignore. Analysis will reoccur if there is a lock on file
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Always make sure to release the lock
            lock.releaseLock();
        }
    }

    @Override
    public void fileAttributeChanged(FileAttributeEvent fae) {
        // Should not matter if the attributes change
        System.out.println("ProgressStatusServiceProvider fileAttributeChanged called");
    }

    @Override
    protected void listenerAdded(FileObject fo, ChangeListener l) {
        if (fo == null) {
            return;
        }
        
        String path = fo.getPath();
        
        if (path == null) {
            return;
        }
        
        boolean apkFile = isAPK(fo);
        //boolean gtpFile = isGTP(fo);
        
        synchronized (this) {
            if (filesListenedTo.add(fo)) {
                
                // Listen to the file
                listenTo(fo);
                
                // if the file is an APK file make sure to listen to its
                // associated gtp file
                if (apkFile) {
                    // Create a progress file for the APK if not created already
                    // And also listen to the Progress file
                    FileObject gtp = gtpForAPK(fo);
                    
                    // add a the same change listener for the GTP as for the APK
                    // the change listener should implement logic to handle 
                    // changes in the GTP file although, this is mainly
                    // for the change in icon.
                    // TODO: create a change listener for when a file is open for view
                    // or certain app analysis is updated
                    System.out.println("Calling addChangeListener on " + gtp.getNameExt());
                    
                    super.addChangeListener(gtp, l);
                }
            }
        }
    }

    @Override
    protected void noLongerListeningTo(FileObject fo) {
        if (fo == null) {
            return;
        }
        
        fo.removeFileChangeListener(this);
        
        synchronized (this) {
            filesListenedTo.remove(fo);
        }
    }
    
    // Add ourselves as a weak listener to the file. This way we can
    // be garbage collected if the project is closed.
    private void listenTo(FileObject fo) {
        if (fo == null) {
            return;
        }
        
        System.out.println("Listening to: " + fo.getPath());
        
        //fo.addFileChangeListener(FileUtil.weakFileChangeListener(this, fo));
        fo.addFileChangeListener(this);
    }
    
    // Creates an empty Gort Progress (GTP) file for the APK if one does not
    // exist already. This is under the assumption that no scans have been done for
    // the APK file.
    public FileObject gtpForAPK(FileObject apk) {
        if (apk == null) {
            return null;
        }
        
        String name = apk.getName();
        
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        ProjectDirectoryService pds = project.getLookup().lookup(ProjectDirectoryService.class);
        FileObject progressDir = pds.getProgressDir();
        
        if (progressDir == null) {
            return null;
        }
        
        String gtpFileName = String.format("%s.%s", name, ProgressStatusService.PROGRESS_FILE_TYPE_GTP);
        
        FileObject gtp = progressDir.getFileObject(gtpFileName);
        
        if (gtp != null) {
            return gtp;
        }
        
        System.out.println("No progress file for " + name + ". Creating one...");
        
        // create an empty progress file. assume no progress has been made
        // if this file does not exist
        // TODO: check it is actually the case that no progress has been made
        Progress progress = new Progress();
        
        try {
            gtp = progressDir.createData(gtpFileName);
            progress.toJson(gtp);
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
        }
        
        return gtp;
    }
    
    public FileObject apkForGTP(FileObject gtp) {
        if (gtp == null) {
            return null;
        }
        
        String name = gtp.getName();
        
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        ProjectDirectoryService pds = project.getLookup().lookup(ProjectDirectoryService.class);
        FileObject appDir = pds.getAppsDir();
        
        if (appDir == null) {
            return null;
        }
        
        String[] possibleAPKFileNames = {
            String.format("%s.%s", name, ProgressStatusService.PROGRESS_FILE_TYPE_APK),
            String.format("%s.%s", name, ProgressStatusService.PROGRESS_FILE_TYPE_APK.toUpperCase())
        };
        
        for (String apkFileName : possibleAPKFileNames) {
            FileObject apk = appDir.getFileObject(apkFileName);
            if (apk != null) {
                return apk;
            }
        }
        
        return null;
    }
    
    @Override
    public Progress progressForAPK(FileObject apk) {
        FileObject gtp = gtpForAPK(apk);
        
        if (gtp == null) {
            return null;
        }
        
        return Progress.fromJson(gtp);
    }
    
}
