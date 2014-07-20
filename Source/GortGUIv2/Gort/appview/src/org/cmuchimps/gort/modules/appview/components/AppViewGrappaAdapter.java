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
package org.cmuchimps.gort.modules.appview.components;

import att.grappa.Attribute;
import att.grappa.Element;
import att.grappa.GrappaAdapter;
import att.grappa.GrappaPanel;
import att.grappa.GrappaPoint;
import att.grappa.Subgraph;
import java.awt.event.InputEvent;
import java.util.Collections;
import org.cmuchimps.gort.api.gort.GestureCollection;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author shahriyar
 */
public class AppViewGrappaAdapter extends GrappaAdapter implements Lookup.Provider {
    
    // lookup allows other components to find out what is selected in the CFG
    private final InstanceContent content;
    private final Lookup lookup;
    
    public AppViewGrappaAdapter() {
        // initialize the lookup components
        content = new InstanceContent();
        lookup = new AbstractLookup(content);
    }
    
    @Override
    public Lookup getLookup() {
        return lookup;
    }
    
    private void singeltonLooup(Object o) {
        content.set(Collections.singleton(o), null);
    }
    
    private void clearLookup() {
        // send out an empty 
        content.set(Collections.singleton(
                new GrappaStateSelectedChangeEvent(
                GrappaStateSelectedChangeEvent.INVALID_ID, null)),
                null);
    }
    
    @Override
    public void grappaClicked(Subgraph subg, Element elem, GrappaPoint pt, int modifiers, int clickCount, GrappaPanel panel) {
        // perform the super actions as previous
        super.grappaClicked(subg, elem, pt, modifiers, clickCount, panel);
        
        // if we have received a click
        if (modifiers == InputEvent.BUTTON1_MASK)
        {
            if (elem == null || !elem.isNode() || subg == null) {
                clearLookup();
                return;
            }
            //TODO: updates components with state's activity
            // this returns the name of the node which is currently
            // set to be the activity name
            if (subg.currentSelection == elem) {
                
                int id = GrappaStateSelectedChangeEvent.INVALID_ID;
                String name = elem.getName();
                
                Attribute idAttribute = elem.getAttribute("id");
                
                String idString = null;
                
                if (idAttribute != null) {
                    idString = idAttribute.getStringValue();
                } else if (name != null) {
                    String[] nameSplit = name.split("-");
                    idString = nameSplit[nameSplit.length - 1];
                }
                
                if (idString != null) {
                    try {
                        id = Integer.parseInt(idString);
                    } catch (Exception e) {
                        // ignore
                    }
                }

                singeltonLooup(new GrappaStateSelectedChangeEvent(id, name));
                GestureCollection.getInstance().stateClick(name);
            } else {
                clearLookup();
                return;
            }
            
        } else {
            clearLookup();
        }
    }
    
    public static final class GrappaStateSelectedChangeEvent {
        
        public static final Integer INVALID_ID = -1;
        
        private final int id;
        private final String name;

        public GrappaStateSelectedChangeEvent(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
        
        public boolean isValid() {
            // id index starts from 1
            return (id > 0);
        }
    }
}
