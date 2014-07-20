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
package org.cmuchimps.gort.modules.apkfile.actions;

import java.awt.Image;
import javax.swing.Action;
import org.cmuchimps.gort.api.gort.analysis.StaticAnalysisService;
import org.cmuchimps.gort.modules.apkfile.APKDataNode;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

@ActionID(
        category = "File",
        id = "org.cmuchimps.gort.modules.apkfile.actions.StaticAnalysisAction")
@ActionRegistration(
        iconBase = "org/cmuchimps/gort/modules/apkfile/actions/static.png",
        displayName = "#CTL_StaticAnalysisAction")
@ActionReferences({
    @ActionReference(path = "Menu/BuildProject", position = 1200, separatorBefore = 1150),
    @ActionReference(path = "Loaders/application/vnd.android.package-archive/Actions", position = 1500, separatorBefore = 1450)
})
@Messages("CTL_StaticAnalysisAction=Static Analysis")
public final class StaticAnalysisAction extends AbstractAnalysisAction implements ContextAwareAction {
    
    private static final Image ICON;
    
    static {
        ICON = ImageUtilities.loadImage("org/cmuchimps/gort/modules/apkfile/actions/static.png");
    }
    
    public StaticAnalysisAction() {
        this(Utilities.actionsGlobalContext());
    }
    
    private StaticAnalysisAction(Lookup context) {
        putValue(Action.NAME, NbBundle.getMessage(StaticAnalysisAction.class, "CTL_StaticAnalysisAction"));
        putValue(Action.SMALL_ICON, ICON);
        this.context = context;
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        return new StaticAnalysisAction(lkp);
    }

    @Override
    protected void actionPerformed(APKDataNode dn) {
        if (dn == null) {
            return;
        }
        
        FileObject fo = dn.getFile();
        
        if (fo == null) {
            return;
        }
        
        StaticAnalysisService sasp = (StaticAnalysisService)
                dn.getFromProject(StaticAnalysisService.class);
        
        if (sasp == null) {
            return;
        }
        
        sasp.analyze(fo);
    }
    
}
