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
package org.cmuchimps.gort.modules.gortproject;

import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.netbeans.api.project.Project;

/**
 *
 * @author shahriyar
 */
public final class Util {
    public static final void createDirectoryStructure(Project project) {
        if (project == null) {
            return;
        }
        
        ProjectDirectoryService pds = project.getLookup().lookup(ProjectDirectoryService.class);
        
        if (pds == null) {
            return;
        }
        
        //Create the apps/ dir
        pds.getAppsDir(true);

        //Create the heuristics/ dir
        pds.getHeuristicsDir(true);

        //Force creation of the hits/ dir
        pds.getCrowdTasksDir(true);

        //Force creation of the mturk.properties file
        pds.getMTurkProperties(true);

        //Force creation of the progress dir
        pds.getProgressDir(true);
        
        //Force creation of a screenshots dir
        pds.getScreenshotsDir(true);

        //Force creation of the traversals dir
        pds.getTraversalDir(true);
    }
}
