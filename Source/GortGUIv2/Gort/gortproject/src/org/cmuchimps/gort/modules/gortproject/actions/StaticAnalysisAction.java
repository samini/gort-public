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
import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.cmuchimps.gort.api.gort.analysis.StaticAnalysisService;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Project",
        id = "org.cmuchimps.gort.modules.gortproject.actions.StaticAnalysisAction")
@ActionRegistration(
        iconBase = "org/cmuchimps/gort/modules/gortproject/actions/static.png",
        displayName = "#CTL_StaticAnalysisAction")
@ActionReferences({
    @ActionReference(path = "Menu/BuildProject", position = -190),
    @ActionReference(path = "Toolbars/Build", position = -120)
})
@Messages("CTL_StaticAnalysisAction=Project Static Analysis")
public final class StaticAnalysisAction implements ActionListener {

    private final Project context;

    public StaticAnalysisAction(Project context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev == null) {
            return;
        }
        
        // find all the apks for this project
        ProjectDirectoryService pds = context.getLookup().lookup(ProjectDirectoryService.class);
        
        if (pds == null) {
            return;
        }
        
        FileObject[] apks = pds.getAPKs();
        
        if (apks == null || apks.length <= 0) {
            System.out.println("No APKs to analyze.");
        }
        
        StaticAnalysisService sas = context.getLookup().lookup(StaticAnalysisService.class);
        
        if (sas == null) {
            System.out.println("Could not reach Static Analysis service.");
            return;
        }
        
        for (FileObject fo : apks) {
            if (fo == null) {
                continue;
            }
            
            sas.analyze(fo);
        }
    }
}
