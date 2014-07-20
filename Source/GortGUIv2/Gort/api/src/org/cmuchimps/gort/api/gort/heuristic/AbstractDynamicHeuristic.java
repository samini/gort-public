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

import java.util.LinkedHashSet;
import org.cmuchimps.gort.modules.dataobject.History;
import org.cmuchimps.gort.modules.dataobject.TaintLog;
import org.cmuchimps.gort.modules.dataobject.Traversal;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public abstract class AbstractDynamicHeuristic extends AbstractHeuristic {

    protected Project project;
    protected FileObject apk;
    protected Traversal traversal;
    protected LinkedHashSet<Integer> stateIds = new LinkedHashSet<Integer>();
    private boolean computed;
    
    public AbstractDynamicHeuristic() {
        super();
    }

    public AbstractDynamicHeuristic(String name, String summary) {
        super(name, summary);
    }

    public AbstractDynamicHeuristic(String name, String summary, String description) {
        super(name, summary, description);
    }

    public AbstractDynamicHeuristic(String name, String summary, String description, int concernLevel) {
        super(name, summary, description, concernLevel);
    }

    public AbstractDynamicHeuristic(Project project, FileObject apk, Traversal traversal, String name, String summary) {
        super(name, summary);
        this.project = project;
        this.apk = apk;
        this.traversal = traversal;
    }

    public AbstractDynamicHeuristic(Project project, FileObject apk, Traversal traversal, String name, String summary, String description) {
        super(name, summary, description);
        this.project = project;
        this.apk = apk;
        this.traversal = traversal;
    }

    public AbstractDynamicHeuristic(Project project, FileObject apk, Traversal traversal, String name, String summary, String description, int concernLevel) {
        super(name, summary, description, concernLevel);
        this.project = project;
        this.apk = apk;
        this.traversal = traversal;
    }
    
    @Override
    public String getType() {
        return AbstractHeuristic.TYPE_DYNAMIC;
    }
    
    public abstract AbstractDynamicHeuristic getInstance(Project project, FileObject apk, Traversal traversal);
    
    // at the beginning of creating the heuristic
    public abstract void init();
    
    public abstract void onActivityChange(History h);
    public abstract void onBack(History h);
    public abstract void onClick(History h);
    public abstract void onDialog(History h);
    public abstract void onKeyboard(History h);
    public abstract void onStateChange(History h);
    public abstract void onTransmissionTaintLog(TaintLog t);
    public abstract void onNonTransmissionTaintLog(TaintLog t);
    public abstract void onTraversalStart(History h);
    
    public Boolean hasPermission(String permission) {
        Boolean retVal = hasPermission(project, apk, permission);
        System.out.println("hasPermission called. " + retVal);
        return retVal;
    }
    
    public Boolean hasAllPermissions(String[] permissions) {
        return hasAllPermissions(project, apk, permissions);
    }
    
    public Boolean hasAnyPermission(String[] permissions) {
        return hasAnyPermission(project, apk, permissions);
    }
    
    // the following method should be overriden, just does a simple check
    // to let developer know of issues with setting up the heuristic
    // return the output of the heuristic
    public Boolean output() {
        if (project == null || apk == null || traversal == null) {
            throw new IllegalArgumentException("Project, APK, and Traversal " + 
                    "have to be set for dynamic heuristics");
        }
        
        return null;
    }

    // if the heuristics is computed, it does not get called on again on events
    public boolean isComputed() {
        return computed;
    }

    public void setComputed(boolean computed) {
        this.computed = computed;
    }

    protected LinkedHashSet<Integer> getStateIds() {
        return stateIds;
    }
    
    public abstract LinkedHashSet<Integer> getAssociatedStateIds();
}
