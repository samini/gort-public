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
package org.cmuchimps.gort.modules.gortproject.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Project",
        id = "org.cmuchimps.gort.modules.gortproject.actions.CrowdAnalysisAction")
@ActionRegistration(
        iconBase = "org/cmuchimps/gort/modules/gortproject/actions/crowd.png",
        displayName = "#CTL_CrowdAnalysisAction")
@ActionReferences({
    @ActionReference(path = "Menu/BuildProject", position = -40),
    @ActionReference(path = "Toolbars/Build", position = 30)
})
@Messages("CTL_CrowdAnalysisAction=Project Crowd Analysis")
public final class CrowdAnalysisAction implements ActionListener {

    private final Project context;

    public CrowdAnalysisAction(Project context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        // TODO use context
    }
}
