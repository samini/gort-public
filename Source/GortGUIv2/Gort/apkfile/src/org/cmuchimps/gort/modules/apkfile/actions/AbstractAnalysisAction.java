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

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.cmuchimps.gort.api.gort.analysis.IAnalyzable;
import org.cmuchimps.gort.modules.apkfile.APKDataNode;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

/**
 *
 * @author shahriyar
 */
public abstract class AbstractAnalysisAction extends AbstractAction implements LookupListener {
    // based on http://wiki.netbeans.org/DevFaqActionContextSensitive
    
    protected Lookup context;
    protected Lookup.Result<IAnalyzable> lkpInfo;
    
    private void init() {
        if (lkpInfo == null) {
            lkpInfo = context.lookupResult(IAnalyzable.class);
            lkpInfo.addLookupListener(this);
            resultChanged(null);
        }
    }
    
    @Override
    public boolean isEnabled() {
        init();
        return super.isEnabled();
    }
    
    @Override
    public void resultChanged(LookupEvent le) {
        setEnabled(!lkpInfo.allInstances().isEmpty());
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Get the first item from lkpInfo and check for node
        if (lkpInfo == null || lkpInfo.allItems() == null) {
            return;
        }
        
        // Perform action on first node found in lookup
        for (Lookup.Item<IAnalyzable> i : lkpInfo.allItems()) {
            IAnalyzable analyzable = i.getInstance();
            
            if (analyzable == null || !(analyzable instanceof APKDataNode)) {
                continue;
            }
            
            actionPerformed((APKDataNode) analyzable);
            return;
        }
    }
    
    protected abstract void actionPerformed(APKDataNode dn);
}
