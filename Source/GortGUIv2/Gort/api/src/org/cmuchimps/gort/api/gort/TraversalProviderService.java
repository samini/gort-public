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
package org.cmuchimps.gort.api.gort;

import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author shahriyar
 */
public abstract class TraversalProviderService implements Lookup.Provider {
    protected final Project project;
    
    // behaves a list that stores any instances of any object
    protected final InstanceContent content;
    protected final Lookup lookup;
    
    public final String STATE_GRAPH_FILENAME = "graph_state_layout.dot";
    
    public TraversalProviderService(Project project) {
        this.project = project;
        content = new InstanceContent();
        lookup = new AbstractLookup(content);
    }
    
    public abstract int getNumTraversals(FileObject apk);
    public abstract FileObject getMainTraversal(FileObject apk, boolean userChoose);
    public abstract FileObject getStateGraph(FileObject traversal);
    public abstract FileObject[] getTraversals(FileObject apk);
    public abstract void setMainTraversal(FileObject apk, FileObject traversal);
    
    @Override
    public Lookup getLookup() {
        return lookup;
    }
    
    public FileObject getMainTraversal(FileObject apk) {
        return getMainTraversal(apk, false);
    }
    
    public boolean isMainTraversal(FileObject apk, FileObject traversal) {
        if (apk == null || traversal == null) {
            return false;
        }
        
        return traversal.equals(getMainTraversal(apk));
    }
    
    public static final class TraversalProviderChangeEvent {
        public static final Integer TYPE_SET_NEW_MAIN_TRAVERSAL = 0x01;
        public static final Integer TYPE_GET_MAIN_TRAVERSAL = 0x02;
        
        private final FileObject apk;
        private final FileObject traversal;
        private final Integer type;

        public TraversalProviderChangeEvent(FileObject apk, FileObject traversal, Integer type) {
            this.apk = apk;
            this.traversal = traversal;
            this.type = type;
        }

        public FileObject getApk() {
            return apk;
        }

        public FileObject getTraversal() {
            return traversal;
        }

        public Integer getType() {
            return type;
        }
    }
    
}
