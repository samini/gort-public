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
package org.cmuchimps.gort.api.gort.heuristic;

import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public abstract class AbstractStaticHeuristic extends AbstractHeuristic {

    public AbstractStaticHeuristic() {
    }

    public AbstractStaticHeuristic(String name, String summary) {
        super(name, summary);
    }
    
    public AbstractStaticHeuristic(String name, String summary, String description) {
        super(name, summary, description);
    }

    public AbstractStaticHeuristic(String name, String summary, String description, int concernLevel) {
        super(name, summary, description, concernLevel);
    }
    
    @Override
    public String getType() {
        return AbstractHeuristic.TYPE_STATIC;
    }
    
    public abstract Boolean output(Project project, FileObject apk);
}
