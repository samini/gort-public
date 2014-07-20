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

import org.cmuchimps.gort.modules.dataobject.Screenshot;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public abstract class ProjectDirectoryService {
    
    public static final String APPS_DIR = "apps"; //NOI18N
    public static final String HEURISTICS_DIR = "heuristics"; //NOI18N
    public static final String CROWD_TASKS_DIR = "crowdtasks"; //NOI18N
    public static final String PROGRESS_DIR = "progress"; //NOI18N
    public static final String SCREENSHOTS_DIR = "screenshots"; //NOI18N
    public static final String TRAVERSALS_DIR = "traversals"; //NOI18N
    
    public static final String MTURK_PROPERTIES_FILENAME = "mturk.properties"; //NOI18N
    
    protected final Project project;
    
    public ProjectDirectoryService(Project project) {
        this.project = project;
    }
    
    public abstract FileObject[] getAPKs();
    public abstract FileObject getDir(String dir, boolean create);
    public abstract FileObject getAppsDir(boolean create);
    public abstract FileObject getHeuristicsDir(boolean create);
    public abstract FileObject getCrowdTasksDir(boolean create);
    public abstract FileObject getMTurkProperties(boolean create);
    public abstract FileObject getProgressDir(boolean create);
    public abstract FileObject getScreenshotsDir(boolean create);
    public abstract FileObject getTraversalDir(boolean create);
    public abstract FileObject getAppsDir();
    public abstract FileObject getHeuristicsDir();
    public abstract FileObject getCrowdTasksDir();
    public abstract FileObject getMTurkProperties();
    public abstract FileObject getProgressDir();
    public abstract FileObject getScreenshotsDir();
    public abstract FileObject getTraversalDir();
    
    // function calls to get the file for a specific screenshot
    public abstract FileObject getScreenshot(Screenshot s);
    public abstract FileObject getScreenshotDefaultHeight(Screenshot s);
    public abstract FileObject getScreenshotThumbnail(Screenshot s);
    
}
