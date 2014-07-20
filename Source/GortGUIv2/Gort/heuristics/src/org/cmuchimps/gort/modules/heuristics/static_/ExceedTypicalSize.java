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
import org.cmuchimps.gort.modules.dataobject.App;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.cmuchimps.gort.api.gort.ProjectUtility;


/**
 *
 * @author Song
 * @author shahriyar
 */
public class ExceedTypicalSize extends AbstractStaticHeuristic {

    public static final String NAME = "Exceeds Typical Apk Size";
    public static final String SUMMARY = "Exceeds more than 80% of other apps by installation filesize (>7.2 MB, Data from Dec. 2013)";
    public static final long SizeLimit = 7553450;
    
    public ExceedTypicalSize() {
        super(NAME, SUMMARY);
    }
    
    @Override
    public Boolean output(Project project, FileObject apk) {
        App app = ProjectUtility.getApp(project, apk);
        if (app == null)
            return null;
        Boolean result = app.getSize() > SizeLimit;
        return result;
    }

}
