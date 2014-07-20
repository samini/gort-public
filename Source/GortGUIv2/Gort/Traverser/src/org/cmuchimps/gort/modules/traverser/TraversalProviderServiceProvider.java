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
package org.cmuchimps.gort.modules.traverser;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import org.cmuchimps.gort.api.gort.GortDatabaseService;
import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.cmuchimps.gort.api.gort.TraversalProviderService;
import org.cmuchimps.gort.modules.dataobject.App;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.dataobject.Traversal;
import org.netbeans.api.project.Project;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/**
 *
 * @author shahriyar
 */
public class TraversalProviderServiceProvider extends TraversalProviderService {

    public TraversalProviderServiceProvider(Project project) {
        super(project);
    }

    @Override
    public int getNumTraversals(FileObject apk) {
        FileObject[] fos = getTraversals(apk);
        return (fos != null) ? fos.length : 0;
    }

    // use a dialog to select a main traversal for the apk
    @Override
    public FileObject getMainTraversal(FileObject apk, boolean userChoose) {
        FileObject[] fos = getTraversals(apk);
        FileObject retVal = null;
        
        if (fos == null || fos.length <= 0) {
            retVal = null;
        } else if (fos.length == 1) {
            retVal = fos[0];
        } else {
            // if there is more than 1 traversal option available
            GortDatabaseService gds = project.getLookup().lookup(GortDatabaseService.class);
            ProjectDirectoryService pds = project.getLookup().lookup(ProjectDirectoryService.class);

            if (gds != null && pds != null) {
                GortEntityManager gem = gds.getGortEntityManager();

                if (gem != null) {
                    EntityManager em = gem.getEntityManager();
                    App app = gem.selectApp(em, apk.getNameExt());

                    if (app != null) {
                        for (Traversal t: app.getTraversals()) {
                            if (Boolean.TRUE.equals(t.getMain())) {
                                retVal = pds.getTraversalDir().getFileObject(t.getDirectory());
                            }
                        }
                    }

                    GortEntityManager.closeEntityManager(em);
                }
            }

            // user can choose only if there is more than 1 option available
            if (retVal == null && userChoose) {
                retVal = showChooseMainTraversalDlg(apk);

                // set the main traversal to the one choosen by the user
                setMainTraversal(apk, retVal);

                /*
                // note this is the only method where the associated node is not
                // directory involved in setting the traversal. as such we
                // inform the nodes lookup listener
                content.set(Arrays.asList(
                        new TraversalProviderChangeEvent(apk, retVal,
                        TraversalProviderChangeEvent.TYPE_SET_NEW_MAIN_TRAVERSAL)),
                        null);
                        * */
            }
        }
        
        // also broadcast what is the main traversal for the particular apk
        if (retVal != null) {
            content.set(
                    Arrays.asList(
                    new TraversalProviderChangeEvent(apk, retVal,
                    TraversalProviderChangeEvent.TYPE_GET_MAIN_TRAVERSAL)),
                    null);
        }
        
        return retVal;
    }

    @Override
    public FileObject[] getTraversals(FileObject apk) {
        if (apk == null) {
            return null;
        }
        
        if (project == null) {
            return null;
        }
        
        ProjectDirectoryService pds = project.getLookup().lookup(ProjectDirectoryService.class);
        
        if (pds == null) {
            return null;
        }
        
        FileObject traversalDir = pds.getTraversalDir();
        
        if (traversalDir == null) {
            return null;
        }
        
        // Get all the related traversals
        List<FileObject> apkTraversals = new ArrayList<FileObject>();
        
        for (FileObject child : traversalDir.getChildren()) {
            if (child == null || !child.isFolder()) {
                continue;
            }
            
            if (child.getName().startsWith(apk.getName())) {
                apkTraversals.add(child);
            }
        }
        
        return (apkTraversals.size() > 0) ? 
                apkTraversals.toArray(new FileObject[apkTraversals.size()]) : null;
    }
    
    @Override
    public void setMainTraversal(FileObject apk, FileObject traversal) {
        if (apk == null) {
            return;
        }
        
        if (traversal == null) {
            return;
        }
        
        System.out.println(String.format("Setting %s as the main traversal for %s",
                traversal.getNameExt(), apk.getNameExt()));
        
        // Get the traversals for the app and set the main traversal as the file specified
        GortDatabaseService gds = project.getLookup().lookup(GortDatabaseService.class);
        
        if (gds == null) {
            return;
        }
        
        GortEntityManager gem = gds.getGortEntityManager();
        
        if (gem == null) {
            return;
        }
        
        EntityManager em = gem.getEntityManager();
        
        App app = gem.selectApp(em, apk.getNameExt());
        
        em.getTransaction().begin();
        
        for (Traversal t : app.getTraversals()) {
            System.out.println(t.getDirectory());
            if (t.getDirectory().equals(traversal.getNameExt())) {
                t.setMain(true);
            } else {
                t.setMain(false);
            }
            
            em.merge(t);
        }
        
        //em.flush();
        em.getTransaction().commit();
                
        GortEntityManager.closeEntityManager(em);
        
        System.out.println("Done setting new traversal.");
    }
    
    @NbBundle.Messages("TTL_ChooseMainTraversal=Choose Main Traversal")
    private FileObject showChooseMainTraversalDlg(final FileObject apk) {
        final MainTraversalChooser chooser = new MainTraversalChooser(project, apk);
        
        String title = Bundle.TTL_ChooseMainTraversal();
        
        final DialogDescriptor desc = new DialogDescriptor(chooser, title);
        
        // OK button disabled initially
        desc.setValid(false);
        
        //Create a property change listener.  It will listen on the selection
        //in our MainTraversalChooser, and enable the OK button if an appropriate
        //node is selected:
        PropertyChangeListener pcl = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                String propName = pce.getPropertyName();
                //System.out.println("Property name: " + propName);
                
                Node[] n = chooser.getExplorerManager().getSelectedNodes();
                    
                boolean valid = false;
                    
                if (n != null && n.length == 1) {
                    String nodeName = n[0].getName();
                    if (nodeName.startsWith(apk.getNameExt())) {
                        valid = true;
                    }
                }
                    
                desc.setValid(valid);
            }
        };
        
        chooser.getExplorerManager().addPropertyChangeListener(pcl);
        
        Object dialogResult = DialogDisplayer.getDefault().notify(desc);
        
        //If the user clicked OK, try to set the main file
        //from the selection
        if (DialogDescriptor.OK_OPTION.equals(dialogResult)) {
            //Get the selected Node
            Node[] n = chooser.getExplorerManager().getSelectedNodes();

            //If it's > 1, explorer is broken—we set
            //single selection mode
            assert n.length <= 1;
            DataObject ob = (DataObject) n[0].getLookup().lookup(
                    DataObject.class);

            //Get the file from the data object
            return ob.getPrimaryFile();
        }
        
        return null;
    }

    @Override
    public FileObject getStateGraph(FileObject traversal) {
        if (traversal == null || !traversal.isFolder()) {
            return null;
        }
        
        return traversal.getFileObject(STATE_GRAPH_FILENAME);
    }
}
