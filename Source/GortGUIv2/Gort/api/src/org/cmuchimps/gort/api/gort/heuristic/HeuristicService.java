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
package org.cmuchimps.gort.api.gort.heuristic;

import java.util.Collection;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author shahriyar
 */
public abstract class HeuristicService implements Lookup.Provider {

    private final InstanceContent content;
    private final Lookup lookup;
    
    public HeuristicService() {
        content = new InstanceContent();
        lookup = new AbstractLookup(content);
    }
    
    public static HeuristicService getDefault() {
        return Lookup.getDefault().lookup(HeuristicService.class);
    }
    
    @Override
    public Lookup getLookup() {
        return lookup;
    }
    
    public Collection<? extends AbstractStaticHeuristic> getStaticHeuristics() {
        return lookup.lookupAll(AbstractStaticHeuristic.class);
    }
    
    public Collection<? extends AbstractDynamicHeuristic> getDynamicHeuristics() {
        return lookup.lookupAll(AbstractDynamicHeuristic.class);
    }
    
    public void addHeuristic(AbstractHeuristic heuristic) {
        if (heuristic != null) {
            content.add(heuristic);
        }
    }
}
