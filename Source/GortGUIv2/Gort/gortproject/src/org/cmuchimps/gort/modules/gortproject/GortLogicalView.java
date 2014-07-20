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

import java.awt.Image;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author shahriyar
 */
public class GortLogicalView implements LogicalViewProvider {

    private final GortProject project;
    
    public GortLogicalView(GortProject project) {
        this.project = project;
    }
    
    @Override
    public Node createLogicalView() {
        try {
            return GortProjectNode.gortProjectNodeFromProject(project);
        } catch (DataObjectNotFoundException donfe) {
            Exceptions.printStackTrace(donfe);

            //Fallback—the directory couldn't be created -
            //read-only filesystem or something evil happened:
            return new AbstractNode (Children.LEAF);
        }
    }

    @Override
    public Node findPath(Node node, Object o) {
        // leave unimplemented for now
        return null;
    }
    
    private static final class GortProjectNode extends FilterNode {
        final GortProject project;
        
        Image icon;
        
        public GortProjectNode(Node node, Children children, GortProject project) 
                throws DataObjectNotFoundException {
            
            
            // uses the old implementation of the nodes
            super(node,
                    /*
                    // another option to create the children for the project
                    NodeFactorySupport.createCompositeChildren(
                        project, 
                        "Projects/org-cmuchimps-gort-modules-gortproject/Nodes"),
                        */
                    children, // old implementation of the child nodes
                    //The projects system wants the project in the Node's lookup.
                    //NewAction and friends want the original Node's lookup.
                    //Make a merge of both:
                    new ProxyLookup(
                        Lookups.singleton(project),
                        node.getLookup())
                    );
            
            this.project = project;
        }
        
        public static GortProjectNode gortProjectNodeFromProject(GortProject project) 
            throws DataObjectNotFoundException{
            
            FileObject projectDirectory = project.getProjectDirectory();
            
            // Get the data object that represents the project directory
            DataFolder projectDataObject = DataFolder.findFolder(projectDirectory);
            
            // get the default node that represents the project data object
            Node projectNode = projectDataObject.getNodeDelegate();
            
            // get the children of the node and only present the project
            // directory important files
            //Children children = new FilterNode.Children(projectNode);
            
            Children children = new GortProjectNode.Children(projectNode);
            
            // create a GortProjectNode for it
            return new GortProjectNode(projectNode, children, project);
        }

        @Override
        public String getDisplayName() {
            return project.getProjectDirectory().getName();
        }

        @Override
        public Image getIcon(int type) {
            if (icon == null) {
                icon = ImageUtilities.loadImage(
                        "org/cmuchimps/gort/modules/gortproject/resources/gort_16.png");
            }
            
            return icon;
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }
        
        public static final class Children extends FilterNode.Children {
            public Children(Node owner) {
                super(owner);
            }

            @Override
            protected Node[] createNodes(Node key) {
                List<Node> result = new LinkedList<Node>();
                for (Node node : super.createNodes(key)) {
                    if (accept(node)) {
                        result.add(node);
                    }
                }

                return result.toArray(new Node[result.size()]);
            }

            private boolean accept(Node node) {
                if (node == null || node.getName() == null) {
                    return false;
                }

                String name = node.getName();

                for (String s : GortProject.LOGICAL_VIEW_DIRECTORIES) {
                    if (name.equals(s)) {
                        return true;
                    }
                }

                return false;
            }

        }
    }
    
    
}
