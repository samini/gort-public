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
package org.cmuchimps.gort.modules.appview;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import org.cmuchimps.gort.api.gort.GestureCollection;
import org.cmuchimps.gort.api.gort.ProjectUtility;
import org.cmuchimps.gort.api.gort.WebInfoService;
import org.cmuchimps.gort.modules.dataobject.App;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.netbeans.api.project.Project;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.WeakListeners;
import org.openide.windows.Mode;
import org.openide.windows.WindowManager;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.cmuchimps.gort.modules.appview//AppStatistics//EN",
        autostore = false)
@TopComponent.Description(
        preferredID = "AppStatisticsTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "org.cmuchimps.gort.modules.appview.AppStatisticsTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_AppStatisticsAction",
        preferredID = "AppStatisticsTopComponent")
@Messages({
    "CTL_AppStatisticsAction=App Information",
    "CTL_AppStatisticsTopComponent=App Information",
    "HINT_AppStatisticsTopComponent=This is a AppInformation window"
})
public final class AppInformationTopComponent extends TopComponent implements
        PropertyChangeListener {
    
    private static final String DEFAULT_MODE = "output";

    public AppInformationTopComponent() {
        initComponents();
        setName(Bundle.CTL_AppStatisticsTopComponent());
        setToolTipText(Bundle.HINT_AppStatisticsTopComponent());
        
        // Add a listener to this so that we can update
        TopComponent.Registry reg = TopComponent.getRegistry();
        reg.addPropertyChangeListener(WeakListeners.propertyChange(this, reg));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        name = new javax.swing.JLabel();
        developer = new javax.swing.JLabel();
        category = new javax.swing.JLabel();
        size = new javax.swing.JLabel();
        md5 = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(AppInformationTopComponent.class, "AppInformationTopComponent.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(AppInformationTopComponent.class, "AppInformationTopComponent.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(AppInformationTopComponent.class, "AppInformationTopComponent.jLabel3.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(AppInformationTopComponent.class, "AppInformationTopComponent.jLabel4.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(AppInformationTopComponent.class, "AppInformationTopComponent.jLabel5.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(name, org.openide.util.NbBundle.getMessage(AppInformationTopComponent.class, "AppInformationTopComponent.name.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(developer, org.openide.util.NbBundle.getMessage(AppInformationTopComponent.class, "AppInformationTopComponent.developer.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(category, org.openide.util.NbBundle.getMessage(AppInformationTopComponent.class, "AppInformationTopComponent.category.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(size, org.openide.util.NbBundle.getMessage(AppInformationTopComponent.class, "AppInformationTopComponent.size.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(md5, org.openide.util.NbBundle.getMessage(AppInformationTopComponent.class, "AppInformationTopComponent.md5.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(name))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(developer))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(category))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(size))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(md5)))
                .addContainerGap(315, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(name))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(developer))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(category))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(size))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(md5))
                .addContainerGap(190, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel category;
    private javax.swing.JLabel developer;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel md5;
    private javax.swing.JLabel name;
    private javax.swing.JLabel size;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        GestureCollection.getInstance().topComponentOpened(this.getClass());
        super.componentOpened();
    }

    @Override
    public void componentClosed() {
        GestureCollection.getInstance().topComponentClosed(this.getClass());
        super.componentClosed();
    }

    @Override
    protected void componentShowing() {
        GestureCollection.getInstance().topComponentShowing(this.getClass());
        super.componentShowing();
    }

    @Override
    protected void componentHidden() {
        GestureCollection.getInstance().topComponentHidden(this.getClass());
        super.componentHidden();
    }

    @Override
    protected void componentActivated() {
        GestureCollection.getInstance().topComponentActivated(this.getClass());
        super.componentActivated(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void componentDeactivated() {
        GestureCollection.getInstance().topComponentDeactivated(this.getClass());
        super.componentDeactivated(); //To change body of generated methods, choose Tools | Templates.
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
    
    @Override
    public void open() {
        Mode mode = WindowManager.getDefault().findMode(DEFAULT_MODE);
        if (mode != null) {
            mode.dockInto(this);
        }
        super.open();
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt == null) {
            return;
        }
        
        if (!TopComponent.Registry.PROP_ACTIVATED.equals(evt.getPropertyName())) {
            return;
        }
        
        update();
    }
    
    private void clear() {
        if (name != null) {
            name.setText("");
        }
        if (developer != null) {
            developer.setText("");
        }
        if (category != null) {
            category.setText("");
        }
        if (size != null) {
            size.setText("");
        }
        if (md5 != null) {
            md5.setText("");
        }
    }
    
    private static final long KILO = 1024;
    
    private void update() {
        TopComponent activated = TopComponent.getRegistry().getActivated();
        
        // follow checks to see whether the text for this component should
        // be updated at all
        if (activated == null) {
            return;
        } else if (activated instanceof AppViewCloneableTopComponent) {
            // continue
        } else if (activated == this) {
            // do something else...
            activated = AppViewCloneableTopComponent.getShowingAppView();
            if (activated == null) {
                return;
            }
        } else {
            return;
        }
        
        AppViewCloneableTopComponent appView = (AppViewCloneableTopComponent) activated;
        
        Project project = appView.getProject();
        FileObject fo = appView.getFileObject();
        
        clear();
        
        if (project == null || fo == null) {
            return;
        }
        
        final GortEntityManager gem = ProjectUtility.getGortEntityManager(project);
        
        if (gem == null) {
            return;
        }
        
        final App app = gem.selectApp(fo.getNameExt());
        
        if (app == null) {
            return;
        }
        
        name.setText(app.getName());
        developer.setText(app.getDeveloper());
        category.setText(app.getCategory());
        md5.setText(app.getMd5());
        
        if (app.getSize() != null) {
            
            long tmp = app.getSize() / KILO;
            size.setText("" + tmp);
        }
        
        if (name.getText() != null && developer.getText() != null && category.getText() != null) {
            return;
        }
        
        // try to get the values for name, developer, and category from the Web Info Service
        SwingWorker sw = new SwingWorker<String[], Void>() {

            @Override
            protected String[] doInBackground() throws Exception {
                String packageName = app.getPackage();
                
                if (packageName == null || packageName.isEmpty()) {
                    return null;
                }
                
                WebInfoService wis = WebInfoService.getDefault();
                
                if (wis == null) {
                    return null;
                }
                
                return new String[]{wis.appName(packageName),
                    wis.appDeveloper(packageName),
                    wis.appCategory(packageName)};
            }

            @Override
            protected void done() {
                String[] result = null;
                
                try {
                    result = this.get();
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (ExecutionException ex) {
                    Exceptions.printStackTrace(ex);
                }
                
                if (result == null || result.length < 3) {
                    return;
                }
                
                boolean change = false;
                
                if ((app.getName() == null || app.getName().isEmpty()) && result[0] != null) {
                    name.setText(app.getName());
                    app.setName(result[0]);
                    change = true;
                }
                
                if ((app.getDeveloper() == null || app.getDeveloper().isEmpty()) && result[1] != null) {
                    developer.setText(app.getDeveloper());
                    app.setDeveloper(result[1]);
                    change = true;
                }
                
                if ((app.getCategory() == null || app.getCategory().isEmpty()) && result[2] != null) {
                    category.setText(app.getCategory());
                    app.setCategory(result[2]);
                    change = true;
                }
                
                if (change) {
                    gem.updateApp(app);
                }
            }
        };
    }
}
