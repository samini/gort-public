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
package org.cmuchimps.gort.modules.apkfile;

import java.io.IOException;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;

@Messages({
    "LBL_APK_LOADER=Files of APK"
})
@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_APK_LOADER",
        mimeType = "application/vnd.android.package-archive",
        extension = {"apk", "APK"})
@DataObject.Registration(
        mimeType = "application/vnd.android.package-archive",
        iconBase = "org/cmuchimps/gort/modules/apkfile/Android_Robot_16x16.png",
        displayName = "#LBL_APK_LOADER",
        position = 300)
@ActionReferences({
    @ActionReference(
            path = "Loaders/application/vnd.android.package-archive/Actions",
            id =
            @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200),
    @ActionReference(
            path = "Loaders/application/vnd.android.package-archive/Actions",
            id =
            @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
            position = 300),
    @ActionReference(
            path = "Loaders/application/vnd.android.package-archive/Actions",
            id =
            @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
            position = 400,
            separatorAfter = 500),
    @ActionReference(
            path = "Loaders/application/vnd.android.package-archive/Actions",
            id =
            @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 600),
    @ActionReference(
            path = "Loaders/application/vnd.android.package-archive/Actions",
            id =
            @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
            position = 700,
            separatorAfter = 800),
    @ActionReference(
            path = "Loaders/application/vnd.android.package-archive/Actions",
            id =
            @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"),
            position = 900,
            separatorAfter = 1000),
    @ActionReference(
            path = "Loaders/application/vnd.android.package-archive/Actions",
            id =
            @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
            position = 1100,
            separatorAfter = 1200),
    @ActionReference(
            path = "Loaders/application/vnd.android.package-archive/Actions",
            id =
            @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 1300),
    @ActionReference(
            path = "Loaders/application/vnd.android.package-archive/Actions",
            id =
            @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
            position = 1400)
})
public class APKDataObject extends MultiDataObject {

    private Lookup lookup;
    private InstanceContent ic = new InstanceContent();
    
    public APKDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        //registerEditor("application/vnd.android.package-archive", true);
        
        // enable the APK files to be opened with another identifer?
        CookieSet cookies = this.getCookieSet();
        cookies.add(((Node.Cookie) new APKOpenSupport(getProject(), this)));
        
        lookup = new ProxyLookup(getCookieSet().getLookup(), new AbstractLookup(ic));
        ic.add(ic);
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }
    
    @Override
    protected int associateLookup() {
        return 1;
    }

    @MultiViewElement.Registration(
            displayName = "#LBL_APK_EDITOR",
            iconBase = "org/cmuchimps/gort/modules/apkfile/Android_Robot_16x16.png",
            mimeType = "application/vnd.android.package-archive",
            persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
            preferredID = "APK",
            position = 1000)
    @Messages("LBL_APK_EDITOR=Source")
    public static MultiViewEditorElement createEditor(Lookup lkp) {
        return new MultiViewEditorElement(lkp);
    }

    // Use our own custom APKDataNode rather than the generic system node
    @Override
    protected Node createNodeDelegate() {
        return new APKDataNode(this);
        //return APKFilterNode.apkFilterNodeFromDataObject(this);
    }
    
    public Project getProject() {
        return FileOwnerQuery.getOwner(this.getPrimaryFile());
    }
    
}
