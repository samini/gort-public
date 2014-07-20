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
package org.cmuchimps.gort.modules.heuristics.dynamic;

import java.util.LinkedHashSet;
import org.cmuchimps.gort.api.gort.heuristic.AbstractDynamicHeuristic;
import org.cmuchimps.gort.modules.helper.AndroidPermissions;
import org.cmuchimps.gort.modules.dataobject.History;
import org.cmuchimps.gort.modules.dataobject.State;
import org.cmuchimps.gort.modules.dataobject.TaintLog;
import org.cmuchimps.gort.modules.dataobject.Traversal;
import org.cmuchimps.gort.modules.helper.TaintHelper;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public class TransmitsLocation extends AbstractDynamicHeuristic {
    public static final String NAME = "Transmits User's Location";
    public static final String SUMMARY = "App transmits user's location (of any type).";
    
    private boolean output = false;
    
    public TransmitsLocation() {
        super(NAME, SUMMARY);
    }

    public TransmitsLocation(Project project, FileObject apk, Traversal traversal) {
        this();
        this.project = project;
        this.apk = apk;
        this.traversal = traversal;
    }
    
    @Override
    public AbstractDynamicHeuristic getInstance(Project project, FileObject apk, Traversal traversal) {
        return new TransmitsLocation(project, apk, traversal);
    }

    @Override
    public void init() {
        if (!hasPermission(AndroidPermissions.INTERNET) ||
                !hasAnyPermission(new String[]{AndroidPermissions.ACCESS_COARSE_LOCATION, AndroidPermissions.ACCESS_FINE_LOCATION})) {
            setComputed(true);
        }
    }

    @Override
    public void onActivityChange(History h) {
        // do nothing
    }

    @Override
    public void onBack(History h) {
        // do nothing
    }

    @Override
    public void onClick(History h) {
        // do nothing
    }

    @Override
    public void onDialog(History h) {
        // do nothing
    }

    @Override
    public void onKeyboard(History h) {
        // do nothing
    }

    @Override
    public void onStateChange(History h) {
        // do nothing
    }

    @Override
    public void onTransmissionTaintLog(TaintLog t) {
        if (isComputed()) {
            return;
        }
        
        if (t == null) {
            return;
        }
        
        Integer taintTag = t.getTainttag();
        
        if (taintTag == null) {
            return;
        }
        
        if (TaintHelper.checkTag(taintTag, TaintHelper.TAINT_LOCATION)) {
            output = true;
            
            State state = t.getState();
            
            if (state != null) {
                getStateIds().add(state.getId());
            }
        }
    }

    @Override
    public void onNonTransmissionTaintLog(TaintLog t) {
        // do nothing
    }

    @Override
    public void onTraversalStart(History h) {
        // do nothing
    }
    
    @Override
    public Boolean output() {
        super.output();
        return hasPermission(AndroidPermissions.INTERNET) && output;
    }
    
    @Override
    public LinkedHashSet<Integer> getAssociatedStateIds() {
        if (output()) {
            return getStateIds();
        }
        
        return null;
    }
}
