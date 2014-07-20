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
package org.cmuchimps.gort.modules.gortproject;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

/**
 *
 * @author shahriyar
 */
// This is another option to create the nodes underneath the project
@NodeFactory.Registration(projectType = "org-cmuchimps-gort-modules-gortproject", position = 10)
public class GortNodeFactory implements NodeFactory {

    @Override
    public NodeList<?> createNodes(Project project) {
        GortProject gortProject = project.getLookup().lookup(GortProject.class);
        return new GortNodeList(gortProject);
    }
    
    private class GortNodeList implements NodeList<Node> {

        GortProject project;

        public GortNodeList(GortProject project) {
            this.project = project;
        }
        
        @Override
        public List<Node> keys() {
            FileObject projectDir = project.getProjectDirectory();
            
            List<Node> retVal = new ArrayList<Node>();
            
            if (projectDir != null) {
                for (FileObject fo : projectDir.getChildren()) {
                    try {
                        String name = fo.getName();

                        for (String s : GortProject.LOGICAL_VIEW_DIRECTORIES) {
                            if (s.equals(name)) {
                                retVal.add(DataObject.find(fo).getNodeDelegate());
                                break;
                            }
                        }
                        
                    } catch (DataObjectNotFoundException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
            
            return retVal;
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
            
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
            
        }

        @Override
        public Node node(Node node) {
            return node;
        }

        @Override
        public void addNotify() {
            
        }

        @Override
        public void removeNotify() {
            
        }
        
    }
    
}
