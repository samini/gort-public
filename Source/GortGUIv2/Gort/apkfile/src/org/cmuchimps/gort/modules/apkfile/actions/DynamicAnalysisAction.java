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
import org.cmuchimps.gort.api.gort.analysis.DynamicAnalysisService;
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
        id = "org.cmuchimps.gort.modules.apkfile.actions.DynamicAnalysisAction")
@ActionRegistration(
        iconBase = "org/cmuchimps/gort/modules/apkfile/actions/dynamic.png",
        displayName = "#CTL_DynamicAnalysisAction")
@ActionReferences({
    @ActionReference(path = "Menu/BuildProject", position = 1300),
    @ActionReference(path = "Loaders/application/vnd.android.package-archive/Actions", position = 1600)
})
@Messages("CTL_DynamicAnalysisAction=Dynamic Analysis")
public final class DynamicAnalysisAction extends AbstractAnalysisAction implements ContextAwareAction {
    
    private static final Image ICON;
    
    static {
        ICON = ImageUtilities.loadImage("org/cmuchimps/gort/modules/apkfile/actions/dynamic.png");
    }
    
    public DynamicAnalysisAction() {
        this(Utilities.actionsGlobalContext());
    }
    
    private DynamicAnalysisAction(Lookup context) {
        putValue(Action.NAME, NbBundle.getMessage(StaticAnalysisAction.class, "CTL_DynamicAnalysisAction"));
        putValue(Action.SMALL_ICON, ICON);
        this.context = context;
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        return new DynamicAnalysisAction(lkp);
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
        
        DynamicAnalysisService dasp = (DynamicAnalysisService)
                dn.getFromProject(DynamicAnalysisService.class);
        
        if (dasp == null) {
            return;
        }
        
        dasp.analyze(fo);
    }
    
}
