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
package org.cmuchimps.gort.api.gort.analysis;

import org.netbeans.api.project.Project;

/**
 *
 * @author shahriyar
 */
public abstract class CrowdAnalysisService extends AbstractAnalysisService {
    public CrowdAnalysisService(Project project) {
        super(project);
    }
    
    // monitor all the open tasks for the project
    public abstract void monitor();
}
