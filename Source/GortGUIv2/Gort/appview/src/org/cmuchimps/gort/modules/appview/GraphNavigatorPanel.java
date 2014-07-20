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

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.spi.navigator.NavigatorPanel;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Result;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.TopComponent;

/**
 *
 * @author shahriyar
 */
@NavigatorPanel.Registration(mimeType = "application/vnd.android.package-archive", displayName = "Control Flow Navigator")
public class GraphNavigatorPanel extends JPanel implements NavigatorPanel, LookupListener, PropertyChangeListener {

    public static final String DISPLAY_NAME = "Control Flow Navigator";
    
    JScrollPane pane;
    
    private final InstanceContent content;
    private final Lookup lookup;
    
    private Result<Scene> lookupResult;
    
    public GraphNavigatorPanel() {
        super();
        setLayout(new BorderLayout());
        pane = new JScrollPane();
        //add(pane, BorderLayout.CENTER);
        
         // initialize the lookup components
        content = new InstanceContent();
        lookup = new AbstractLookup(content);
        
        // Add a listener to this so that we can update
        TopComponent.Registry reg = TopComponent.getRegistry();
        reg.addPropertyChangeListener(WeakListeners.propertyChange(this, reg));
    }
    
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getDisplayHint() {
        return DISPLAY_NAME;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    
    @Override
    public void panelActivated(Lookup lkp) {
        System.out.println(getDisplayName() + " panelActivated called.");
        update();
    }

    @Override
    public void panelDeactivated() {
        if (lookupResult != null) {
            lookupResult.removeLookupListener(this);
        }
    }

    @Override
    public void resultChanged(LookupEvent le) {
        if (le == null) {
            System.out.println("GraphNavigator LookupEvent is null.");
            return;
        } else {
            System.out.println("Lookup Event for Scene");
        }
        
        Result result = (Result) le.getSource();
        Collection<Scene> c = result.allInstances();
        
        boolean setScene = false;
        
        //if (!lookupResult.allInstances().isEmpty()) {
        //    Scene s = lookupResult.allInstances().iterator().next();
        if (!c.isEmpty()) {
            Scene s = c.iterator().next();
            if (s != null) {
                System.out.println("Updating the navigator pane...");
                pane.setViewportView(s.createSatelliteView());
                add(pane, BorderLayout.CENTER);
                setScene = true;
            } else {
                System.out.println("Scene object is null.");
            }
        } else {
            System.out.println("Scene collection is empty.");
            pane.setViewportView(new JLabel("<No Control Flow Information Available>"));
            add(pane, BorderLayout.CENTER);
        }
        
        this.revalidate();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt == null) {
            return;
        }
        
        if (TopComponent.Registry.PROP_ACTIVATED.equals(evt.getPropertyName()) ||
                TopComponent.Registry.PROP_TC_CLOSED.equals(evt.getPropertyName())) {
            // remove the listener to the old lookup
            if (lookupResult != null) {
                lookupResult.removeLookupListener(this);
            }
            
            update();
        }
        
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }
    
    private void clear() {
        if (pane != null) {
            this.remove(pane);
        }
        this.revalidate();
    }
    
    private void update() {
        //System.out.println("GraphNavigator update called.");
        
        TopComponent activated = TopComponent.getRegistry().getActivated();
        
        if (activated == null || !(activated instanceof AppViewCloneableTopComponent)) {
            return;
        }
        
        // clear out results from a previously activated AppView
        clear();
        
        AppViewCloneableTopComponent appView = (AppViewCloneableTopComponent) activated;
        
        lookupResult = appView.getLookup().lookupResult(Scene.class);
        lookupResult.addLookupListener(this);
        resultChanged(new LookupEvent(lookupResult));
    }
    
}
