/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
public class TransmitsRecordedAudio extends AbstractDynamicHeuristic {
    public static final String NAME = "Transmits User's Recorded Audio";
    public static final String SUMMARY = "App transmits user's audio as obtained through microphone.";
    
    private boolean output = false;
    
    public TransmitsRecordedAudio() {
        super(NAME, SUMMARY);
    }

    public TransmitsRecordedAudio(Project project, FileObject apk, Traversal traversal) {
        this();
        this.project = project;
        this.apk = apk;
        this.traversal = traversal;
    }
    
    @Override
    public AbstractDynamicHeuristic getInstance(Project project, FileObject apk, Traversal traversal) {
        return new TransmitsRecordedAudio(project, apk, traversal);
    }

    @Override
    public void init() {
        if (!this.hasAllPermissions(new String[] {AndroidPermissions.INTERNET, AndroidPermissions.RECORD_AUDIO})) {
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
        
        if (TaintHelper.checkTag(taintTag, TaintHelper.TAINT_MIC)) {
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
