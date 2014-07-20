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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.cmuchimps.gort.api.gort.heuristic.AbstractDynamicHeuristic;
import org.cmuchimps.gort.modules.helper.AndroidPermissions;
import org.cmuchimps.gort.modules.dataobject.History;
import org.cmuchimps.gort.modules.dataobject.Server;
import org.cmuchimps.gort.modules.dataobject.State;
import org.cmuchimps.gort.modules.dataobject.TaintLog;
import org.cmuchimps.gort.modules.dataobject.Traversal;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public class FourPlusServers extends AbstractDynamicHeuristic {
    
    public static final String NAME = "Contacts 4+ Servers";
    public static final String SUMMARY = "App contacts four or more servers.";
    
    private static final int THRESHOLD = 3;
    
    private Set<String> ips = new HashSet<String>();
    private boolean output = false;

    public FourPlusServers() {
        super(NAME, SUMMARY);
    }

    public FourPlusServers(Project project, FileObject apk, Traversal traversal) {
        this();
        this.project = project;
        this.apk = apk;
        this.traversal = traversal;
    }
    
    @Override
    public AbstractDynamicHeuristic getInstance(Project project, FileObject apk, Traversal traversal) {
        return new FourPlusServers(project, apk, traversal);
    }
    
    @Override
    public void init() {
        if (!hasPermission(AndroidPermissions.INTERNET)) {
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
            System.out.println("Already computed.");
            return;
        }
        
        if (t == null) {
            System.out.println("Taint is null.");
            return;
        }
        
        Server s = t.getServer();
        
        if (s == null) {
            System.out.println("Server is null.");
            return;
        }
        
        String ip = s.getIp();
        
        if (ip == null || ip.isEmpty()) {
            System.out.println("Ip is invalid.");
            return;
        }
        
        ips.add(ip);
        
        if (ips.size() > THRESHOLD) {
            output = true;
        }
        
        State state = t.getState();
        
        if (state != null) {
            getStateIds().add(state.getId());
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
