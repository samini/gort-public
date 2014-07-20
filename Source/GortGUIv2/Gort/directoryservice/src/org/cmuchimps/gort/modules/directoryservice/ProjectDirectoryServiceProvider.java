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
package org.cmuchimps.gort.modules.directoryservice;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.cmuchimps.gort.api.gort.GortDatabaseService;
import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.dataobject.Screenshot;
import org.cmuchimps.gort.modules.helper.ImageHelper;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author shahriyar
 */
public class ProjectDirectoryServiceProvider extends ProjectDirectoryService {

    public ProjectDirectoryServiceProvider(Project project) {
        super(project);
    }

    @Override
    public FileObject getDir(String dir, boolean create) {
        if (project == null || dir == null || dir.isEmpty()) {
            return null;
        }

        FileObject projectDir = project.getProjectDirectory();

        if (projectDir == null) {
            return null;
        }

        FileObject retVal = projectDir.getFileObject(dir);

        if (retVal == null && create) {
            try {
                retVal = projectDir.createFolder(dir);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        return retVal;
    }

    @Override
    public FileObject[] getAPKs() {
        FileObject appsDir = getAppsDir();
        
        if (appsDir == null) {
            return null;
        }
        
        FileObject[] children = appsDir.getChildren();
        
        if (children == null || children.length <= 0) {
            return null;
        }
        
        List<FileObject> list = new LinkedList<FileObject>();
        
        for (FileObject fo : children) {
            if (fo == null) {
                continue;
            }
            
            String ext = fo.getExt();
            
            if (ext == null || ext.isEmpty()) {
                continue;
            }
            
            if (ext.trim().toLowerCase().equals("apk")) {
                list.add(fo);
            }
        }
        
        return !list.isEmpty() ? list.toArray(new FileObject[list.size()]) : null; 
    }
    
    @Override
    public FileObject getAppsDir(boolean create) {
        return getDir(APPS_DIR, create);
    }

    @Override
    public FileObject getHeuristicsDir(boolean create) {
        return getDir(HEURISTICS_DIR, create);
    }

    @Override
    public FileObject getCrowdTasksDir(boolean create) {
        return getDir(CROWD_TASKS_DIR, create);
    }

    @Override
    public FileObject getProgressDir(boolean create) {
        return getDir(PROGRESS_DIR, create);
    }

    @Override
    public FileObject getScreenshotsDir(boolean create) {
        return getDir(SCREENSHOTS_DIR, create);
    }

    @Override
    public FileObject getTraversalDir(boolean create) {
        return getDir(TRAVERSALS_DIR, create);
    }

    @Override
    public FileObject getAppsDir() {
        return getAppsDir(false);
    }

    @Override
    public FileObject getHeuristicsDir() {
        return getHeuristicsDir(false);
    }

    @Override
    public FileObject getCrowdTasksDir() {
        return getCrowdTasksDir(false);
    }

    @Override
    public FileObject getProgressDir() {
        return getProgressDir(false);
    }

    @Override
    public FileObject getScreenshotsDir() {
        return getScreenshotsDir(false);
    }

    @Override
    public FileObject getTraversalDir() {
        return getTraversalDir(false);
    }

    @Override
    public FileObject getMTurkProperties(boolean create) {
        FileObject crowdTasksDir = getCrowdTasksDir(create);
        
        if (crowdTasksDir == null) {
            return null;
        }
        
        FileObject properties = crowdTasksDir.getFileObject(MTURK_PROPERTIES_FILENAME);
        
        if (properties == null && create) {
            // copy over the properties file
            FileObject internalMTurkProperties = FileUtil.getConfigFile(String.format("Gort/Properties/%s", MTURK_PROPERTIES_FILENAME));
            
            if (internalMTurkProperties != null) {
                try {
                    internalMTurkProperties.copy(crowdTasksDir, internalMTurkProperties.getName(), internalMTurkProperties.getExt());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return properties;
    }

    @Override
    public FileObject getMTurkProperties() {
        return getMTurkProperties(false);
    }
    
    @Override
    public FileObject getScreenshot(Screenshot s) {
        if (s == null) {
            return null;
        }
        
        if (project == null) {
            return null;
        }
        
        FileObject projectDir = project.getProjectDirectory();

        if (projectDir == null) {
            return null;
        }
        
        String path = s.getPath();
        
        if (path == null) {
            return null;
        }

        return projectDir.getFileObject(path);
    }
    
    @Override
    public FileObject getScreenshotDefaultHeight(Screenshot s) {
        return getScreenshotFitted(s, ImageHelper.DEFAULT_HEIGHT);
    }
    
    @Override
    public FileObject getScreenshotThumbnail(Screenshot s) {
        return getScreenshotFitted(s, ImageHelper.DEFAULT_HEIGHT_THUMBNAIL);
    }
    
    private FileObject getScreenshotFitted(Screenshot s, int height) {
        if (s == null) {
            return null;
        }
        
        if (height != ImageHelper.DEFAULT_HEIGHT && height != ImageHelper.DEFAULT_HEIGHT_THUMBNAIL) {
            return null;
        }
        
        if (project == null) {
            return null;
        }
        
        FileObject projectDir = project.getProjectDirectory();

        if (projectDir == null) {
            return null;
        }
        
        String path = null;
        
        if (height == ImageHelper.DEFAULT_HEIGHT) {
            path = s.getDefaultHeightPath();
        } else if (height == ImageHelper.DEFAULT_HEIGHT_THUMBNAIL) {
            path = s.getThumbnailPath();
        }
        
        FileObject fo;
        
        if (path != null && (fo = projectDir.getFileObject(path)) != null) {
            return fo;
        }
        
        FileObject orig = getScreenshot(s);
        
        FileObject scaled = null;
        
        if (height == ImageHelper.DEFAULT_HEIGHT) {
            scaled = ImageHelper.scaleToDefaultDimension(orig);
        } else if (height == ImageHelper.DEFAULT_HEIGHT_THUMBNAIL) {
            scaled = ImageHelper.scaleToThumbnailDimension(orig);
        } 
        
        if (scaled == null) {
            return null;
        }
        
        FileObject parent = scaled.getParent();
        
        // Note the assumption here is that the screenshots reside somewhere
        // inside the project directory
        String relativeParent = parent.getPath().replaceFirst(projectDir.getPath(), "");
        
        if (relativeParent != null && !relativeParent.isEmpty()) {
            // always get the path name with respect to the project directory
            if (relativeParent.startsWith(File.separator)) {
                relativeParent = relativeParent.replaceFirst(File.separator, "");
            }
            
            path = String.format("%s%s%s", relativeParent, 
                    File.separator, scaled.getNameExt());
        } else {
            path = scaled.getNameExt();
        }

        if (height == ImageHelper.DEFAULT_HEIGHT) {
            s.setDefaultHeightPath(path);
        } else if (height == ImageHelper.DEFAULT_HEIGHT_THUMBNAIL) {
            s.setThumbnailPath(path);
        } 
        
        GortDatabaseService gds = project.getLookup().lookup(GortDatabaseService.class);
        
        if (gds != null) {
            GortEntityManager gem = gds.getGortEntityManager();
            gem.updateEntity(s);
        }
        
        return scaled;
    }
}
