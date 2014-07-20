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

import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.cmuchimps.gort.api.gort.GortDatabaseService;
import org.cmuchimps.gort.api.gort.analysis.IAnalyzable;
import org.cmuchimps.gort.api.gort.ProgressStatusService;
import org.cmuchimps.gort.api.gort.WebInfoService;
import org.cmuchimps.gort.modules.dataobject.App;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.dataobject.Progress;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataNode;
import org.openide.nodes.Children;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author shahriyar
 */
public class APKDataNode extends DataNode implements IAnalyzable, ChangeListener {
    // icons to indicate at which level of analysis for the apk
    private static final String IMAGE_PATH_PREFIX = "org/cmuchimps/gort/modules/apkfile/resources/";
    private static final String IMAGE_STATIC_ANALYSIS = "analysis_brown.png";
    private static final String IMAGE_DYNAMIC_ANALYSIS = "analysis_darkgreen.png";
    private static final String IMAGE_CROWD_ANALYSIS = "analysis_blue.png";
    
    private static final Image BADGE_STATIC_ANALYSIS, 
            BADGE_DYNAMIC_ANALYSIS, BADGE_CROWD_ANALYSIS;
    
    static {
        //BADGE_NO_ANALYSIS = ImageUtilities.loadImage(IMAGE_PATH_PREFIX + IMAGE_NO_ANALYSIS);
        BADGE_STATIC_ANALYSIS = ImageUtilities.loadImage(IMAGE_PATH_PREFIX + IMAGE_STATIC_ANALYSIS);
        BADGE_DYNAMIC_ANALYSIS = ImageUtilities.loadImage(IMAGE_PATH_PREFIX + IMAGE_DYNAMIC_ANALYSIS);
        BADGE_CROWD_ANALYSIS = ImageUtilities.loadImage(IMAGE_PATH_PREFIX + IMAGE_CROWD_ANALYSIS);
    }
    
    public APKDataNode(APKDataObject obj) {
        // we could change this to maybe expose the manifest file
        // or other portions of the APK
        super(obj, Children.LEAF);
        this.setChildren(Children.create(new APKDataNodeChildFactory(this), true));
        
        // speed up node initialization by listening to progress
        // and adding to db on another thread
        monitorProgressThreaded();
        setupAPKThreaded();
    }
    
    public FileObject getFile() {
        return getDataObject().getPrimaryFile();
    }
    
    // getFromProject tries to find the project that owns the file, and if it 
    // finds one, queries its Lookup, asking it for an instance of the Class 
    // that was passed into this method (i.e., one of the classes in the API we
    // just defined)
    public Object getFromProject (Class c) {
        Object result = null;
        Project p = FileOwnerQuery.getOwner(getFile());
        if (p != null) {
            result = p.getLookup().lookup(c);
        } else {
            result = null;
        }
        return result;
    }
    
    @Override
    public Image getIcon(int type) {
        Image result = super.getIcon(type);
        
        ProgressStatusService pss = (ProgressStatusService) getFromProject(ProgressStatusService.class);
        
        if (pss == null) {
            return result;
        }
        
        Progress progress = pss.progressForAPK(getFile());
        
        if (progress == null) {
            System.out.println("Progress is null for " + getFile().getPath());
            return result;
        }
        
        Image badge = null;
        
        if (progress.isCrowdAnalysis()) {
            badge = BADGE_CROWD_ANALYSIS;
        } else if (progress.isDynamicAnalysis()) {
            badge = BADGE_DYNAMIC_ANALYSIS;
        } else if (progress.isStaticAnalysis()) {
            badge = BADGE_STATIC_ANALYSIS;
        }
        
        if (badge != null) {
            result = ImageUtilities.mergeImages(result, badge, 8, 8);
        }
        
        return result;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return this.getIcon(type);
    }
    
    @Override
    public void stateChanged(ChangeEvent e) {
        // change the icon to let analyst know that more progress has been made
        fireIconChange();
    }
    
    @NbBundle.Messages("LBL_Traverse=Traverse")
    private class TraverseAction extends AbstractAction {

        public TraverseAction() {
            putValue(Action.NAME, Bundle.LBL_Traverse());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO(samini): provide an action for traversal
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
    @NbBundle.Messages("LBL_View=View")
    private class ViewAction extends AbstractAction {

        public ViewAction() {
            putValue(Action.NAME, Bundle.LBL_View());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO(samini): provide an action for viewing
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
    private void monitorProgressThreaded() {
        Thread t = new Thread() {
            @Override
            public void run() {
                monitorProgress();
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
    
    private void monitorProgress() {
        ProgressStatusService pss = (ProgressStatusService) 
                getFromProject(ProgressStatusService.class);
        if (pss != null) {
            //Could be an isolated file outside of a project, in which
            //case there is no ProgressStatusService
            pss.addChangeListener(getFile(), this);
        }
    }
    
    private void setupAPKThreaded() {
        Thread t = new Thread() {
            @Override
            public void run() {
                setupAPK();
            }
        };
        
        // App insertion into database should not slow down loading time
        t.setPriority(Thread.MIN_PRIORITY);
        
        t.start();
    }
    
    private void setupAPK() {
        GortDatabaseService gds = (GortDatabaseService)
                getFromProject(GortDatabaseService.class);
        
        if (gds == null) {
            return;
        }
        
        GortEntityManager gem = gds.getGortEntityManager();
        
        String apk = getFile().getNameExt();
        
        App app = gem.selectApp(apk);
        
        if (app == null) {
            app = new App();
            app.setApk(apk);
            gem.insertApp(app);
        }
        
        // if we don't have a package name for the app
        // cannot look up any app info
        if (app.getPackage() == null || app.getPackage().isEmpty()) {
            return;
        }
        
        // if we don't have a package name for the app
        // cannot look up desc
        
        // get the default one...
        WebInfoService wis = WebInfoService.getDefault();
        
        if (wis == null) {
            return;
        }
        
        // Also update the app apk description
        String desc = app.getDescription();
        
        if (desc == null || desc.isEmpty()) {
            // Either reads the app description or downloads it
            // from Google Play
            desc = wis.appDescription(app.getPackage());

            if (desc != null) {
                app.setDescription(desc);
            }
        }
        
        String name = app.getName();
        
        if (name == null || name.isEmpty()) {
            name = wis.appName(app.getPackage());
            
            if (name != null) {
                app.setName(name);
            }
        }
        
        String developer = app.getDeveloper();
        
        if (developer == null || developer.isEmpty()) {
            developer = wis.appDeveloper(app.getPackage());
            
            if (developer != null) {
                app.setDeveloper(developer);
            }
        }
        
        String category = app.getCategory();
        
        if (category == null || category.isEmpty()) {
            category = wis.appCategory(app.getPackage());
            
            if (category != null) {
                app.setCategory(category);
            }
        }
        
        gem.updateApp(app);
    }
}
