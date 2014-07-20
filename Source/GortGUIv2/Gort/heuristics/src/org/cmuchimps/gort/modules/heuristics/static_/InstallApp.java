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
package org.cmuchimps.gort.modules.heuristics.static_;

import org.cmuchimps.gort.api.gort.heuristic.AbstractStaticHeuristic;
import org.cmuchimps.gort.modules.helper.AndroidPermissions;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Song
 * @author shahriyar
 */
public class InstallApp extends AbstractStaticHeuristic {

    public static final String NAME = "Can Install App";
    public static final String SUMMARY = "Has the ability to install other apps, i.e., has the android.permission.INSTALL_PACKAGES permission.";
    public static final String[] PERMISSIONS = {
        AndroidPermissions.INSTALL_PACKAGES
    };
    
    public InstallApp() {
        super(NAME, SUMMARY);
    }
    
    @Override
    public Boolean output(Project project, FileObject apk) {
        Boolean result = this.hasAllPermissions(project, apk, PERMISSIONS);        
        return result;
    }

}
