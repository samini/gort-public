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
public abstract class TraversalProcessorService implements Lookup.Provider {
    protected final Project project;
    
    // behaves a list that stores any instances of any object
    protected final InstanceContent content;
    protected final Lookup lookup;
    
    public TraversalProcessorService(Project project) {
        this.project = project;
        content = new InstanceContent();
        lookup = new AbstractLookup(content);
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }
    
    public abstract Boolean isProcessed(FileObject traversal);
    
    public void processTraversal(FileObject traversal) {
        processTraversal(traversal, false);
    }
    
    public abstract void processTraversal(FileObject traversal, boolean redo);
    
    // an event which signifies that a traversal has finished being post processed
    public static final class TraversalProcessedChangeEvent {
        private final FileObject traversal;

        public TraversalProcessedChangeEvent(FileObject traversal) {
            this.traversal = traversal;
        }

        public FileObject getTraversal() {
            return traversal;
        }
    }
}
