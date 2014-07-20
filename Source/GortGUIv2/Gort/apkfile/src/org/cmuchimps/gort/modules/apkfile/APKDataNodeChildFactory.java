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
package org.cmuchimps.gort.modules.apkfile;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.cmuchimps.gort.api.gort.TraversalProcessorService;
import org.cmuchimps.gort.api.gort.TraversalProviderService;
import org.cmuchimps.gort.api.gort.TraversalProviderService.TraversalProviderChangeEvent;
import org.cmuchimps.gort.modules.helper.FileHelper;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Result;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author shahriyar
 */
public class APKDataNodeChildFactory extends ChildFactory<Node> implements FileChangeListener, LookupListener {

    private static final DateFormat TRAVERSAL_DATE_ORIGINAL_FORMAT =
            new SimpleDateFormat("yyyyMMdd'-'HHmmss");
        
    private static final DateFormat TRAVERSAL_DATE_DISPLAY_FORMAT = 
            new SimpleDateFormat("yyyy'/'MM'/'dd' 'HH:mm:ss");

    private static final Image TRAVERSAL_ICON = ImageUtilities.loadImage(
            "org/cmuchimps/gort/modules/apkfile/resources/graph_16.png");
    
    // request processor to deal with nodes
    private static final RequestProcessor RP = new RequestProcessor(
            APKDataNodeChildFactory.class.getSimpleName(), 20);
    
    private final APKDataNode parent;
    private final DataObject parentDO;
    private final FileObject parentFO;
    
    private TraversalProviderService tps;
    private FileObject traversalDirectory;
    
    // keeps a reference to the children as they are created
    private final Map<String, Reference> children = new HashMap<String, Reference>();
    
    // also listen to other ways of a traversal being set
    private final Result<TraversalProviderService.TraversalProviderChangeEvent> lookupResult;
    
    // main traversal object associated with this particular APK
    FileObject mainTraversal;
    
    APKDataNodeChildFactory(APKDataNode parent) {
        this.parent = parent;
        
        if (this.parent != null) {
            this.parentDO = this.parent.getDataObject();
            
            if (parentDO != null) {
                this.parentFO = parentDO.getPrimaryFile();
            } else {
                parentFO = null;
            }
        } else {
            parentDO = null;
            parentFO = null;
        }
        
        TraversalProviderService t = getTPS();
        
        if (t != null) {
            lookupResult = tps.getLookup().lookupResult(TraversalProviderChangeEvent.class);
            lookupResult.addLookupListener(this);
        } else {
            lookupResult = null;
        }
        
        FileObject traversalDir = getTraversalDirectory();
        
        if (traversalDir != null) {
            //traversalDir.addFileChangeListener(this);
            traversalDir.addFileChangeListener(
                    FileUtil.weakFileChangeListener(this, traversalDir));
        }
        
    }

    @Override
    protected boolean createKeys(List<Node> list) {
        System.out.println("Creating traversal node keys.");
        
        if (parentFO == null) {
            System.out.println("Parent APK is null.");
            return true;
        }
        
        String parentFilename = parentFO.getNameExt();
        
        if (parentFilename == null || parentFilename.isEmpty()) {
            System.out.println("Parent APK filename is invalid.");
            return true;
        }
        
        FileObject traversalDir = getTraversalDirectory();
        
        if (traversalDir == null) {
            System.out.println("Traversal directory is null.");
            return true;
        }
        
        boolean createdTraversalNode = false;
        
        for (FileObject fo : FileHelper.getOrderedChildren(traversalDir)) {
            try {
                
                if (fo == null || !fo.isFolder()) {
                    continue;
                }
                
                String folderName = fo.getNameExt();
                
                if (folderName == null || folderName.isEmpty()) {
                    continue;
                } 
                
                if (folderName.startsWith(parentFilename)) {
                    System.out.println("Creating child node for: " + folderName);
                    createdTraversalNode = true;
                    Node delegate = DataObject.find(fo).getNodeDelegate();
                    //list.add(new TraversalNode(delegate));
                    list.add(new TraversalNode(delegate, fo));
                    
                    // add a listener to the folder object
                    fo.addFileChangeListener(FileUtil.weakFileChangeListener(this, fo));
                }
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        
        if (createdTraversalNode) {
            System.out.println("Created at least one traversal node.");
            // once all traversal nodes have been created do a check
            // for the main traversal so we can distinguish which one it is
            RP.post(new Runnable() {

                @Override
                public void run() {
                    TraversalProviderService t = getTPS();

                    if (t == null) {
                        return;
                    }

                    t.getMainTraversal(parentFO);
                }

            });
        }
        
        return true;
    }
    
    private Project getParentProject() {
        if (parentFO == null) {
            return null;
        }
        
        return FileOwnerQuery.getOwner(parentFO);
    }
    
    private FileObject getTraversalDirectory() {
        if (traversalDirectory == null) {
            Project project = getParentProject();

            if (project == null) {
                return null;
            }

            ProjectDirectoryService pds = project.getLookup().lookup(ProjectDirectoryService.class);

            if (pds == null) {
                return null;
            }

            traversalDirectory= pds.getTraversalDir();
        }
        
        return traversalDirectory;
    }
    
    private TraversalProviderService getTPS() {
        if (tps == null && parent != null) {
            tps = (TraversalProviderService) 
                    parent.getFromProject(TraversalProviderService.class);
        }
        
        return tps;
    }

    @Override
    protected Node createNodeForKey(Node key) {
        return key;
    }

    @Override
    public void fileFolderCreated(FileEvent fe) {
        if (fe == null) {
            return;
        }
        
        FileObject fo = fe.getFile();
        
        // we are only interested in traversal folders
        if (fo == null) {
            return;
        } else {
            System.out.println("Folder created: " + fo.getNameExt());
        }
        
        FileObject parent = fo.getParent();
        
        if (parent != null || parent.equals(getTraversalDirectory())) {
            this.refresh(true);
        }
    }

    @Override
    public void fileDataCreated(FileEvent fe) {
        // ignore
    }

    // When a file is changed the event is called for the file itself and 
    // it's parent directory. We only need to refresh if directories are added  
    // deleted, or renamed
    @Override
    public void fileChanged(FileEvent fe) {
        // ignore
    }

    @Override
    public void fileDeleted(FileEvent fe) {
        this.refresh(true);
    }

    @Override
    public void fileRenamed(FileRenameEvent fre) {
        this.refresh(true);
    }

    @Override
    public void fileAttributeChanged(FileAttributeEvent fae) {
        // ignore
    }

    // The lookup for this lookup listener is in TraversalProviderService
    @Override
    public void resultChanged(LookupEvent le) {
        //System.out.println("APKDataNodeChildFactory result changed called.");
        
        if (le == null) {
            return;
        }
        
        Lookup.Result r = (Lookup.Result) le.getSource();
        Collection<TraversalProviderChangeEvent> c = r.allInstances();
        
        if (!c.isEmpty()) {
            
            TraversalProviderChangeEvent ce = (TraversalProviderChangeEvent) c.iterator().next();
            FileObject apk = ce.getApk();
            FileObject traversal = ce.getTraversal();
            Integer type = ce.getType();

            // parentFO is the associated apk, if the lookup change is not related
            // to this particular APK
            if (apk == null || !apk.equals(parentFO)) {
                return;
            }
            
            if (traversal == null || !traversal.canRead()) {
                return;
            } else {
                mainTraversal = traversal;
            }
            
            if (traversal != null) {
                if (type == TraversalProviderChangeEvent.TYPE_SET_NEW_MAIN_TRAVERSAL) {
                    Reference ref = children.get(traversal.getPath());

                    if (ref != null) {
                        TraversalNode t = (TraversalNode) ref.get();
                        t.fireTraversalNodeDisplayNameChange(t.getDisplayName(), t.getHtmlDisplayName());
                    } else {
                        System.out.println("Reference is null.");
                    }
                } else if (type == TraversalProviderChangeEvent.TYPE_GET_MAIN_TRAVERSAL) {
                    // call display name change on all children
                    for (Reference ref : children.values()) {
                        if (ref == null) {
                            continue;
                        }
                        
                        TraversalNode t = (TraversalNode) ref.get();
                        
                        if (t == null) {
                            continue;
                        }
                        
                        if (mainTraversal.equals(t.getOriginalFO())) {
                            t.fireTraversalNodeDisplayNameChange(t.getDisplayName(), t.getHtmlDisplayName());
                        } else {
                            t.fireTraversalNodeDisplayNameChange(null, t.getDisplayName());
                        }
                        
                    }
                }
            } else {
                System.out.println("New Traversal is null.");
            }
        }
    }
    
    private final class TraversalNode extends FilterNode {
        
        private FileObject originalFO;
        
        public TraversalNode(Node original) {
            super(original);
        }
        
        public TraversalNode(Node original, FileObject originalFO) {
            this(original);
            this.originalFO = originalFO;
            if (this.originalFO != null) {
                Reference r = new WeakReference(this);
                children.put(originalFO.getPath(), r);
            }           
        }

        public FileObject getOriginalFO() {
            return originalFO;
        }

        @Override
        public Image getIcon(int type) {
            return TRAVERSAL_ICON;
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }
        
        // Display Name sometimes gets errors parsing the information?
        private static final int GET_DISPLAY_NAME_TRIES = 3;
        
        @Override
        public String getDisplayName() {
            String displayName = super.getDisplayName();
            
            if (displayName == null) {
                return null;
            }
            
            if (!displayName.isEmpty()) {
                int index = displayName.lastIndexOf('_');
                
                if (index > 0 && index < displayName.length() - 1) {
                    
                    displayName = displayName.substring(index + 1);
                    
                    int tries = 0;
                    
                    while (tries < GET_DISPLAY_NAME_TRIES) {
                        try {
                            Date time = TRAVERSAL_DATE_ORIGINAL_FORMAT.parse(displayName);
                            displayName = TRAVERSAL_DATE_DISPLAY_FORMAT.format(time);
                            break;
                        } catch (ParseException ex) {
                            //ex.printStackTrace();
                        } catch (NumberFormatException nfe) {
                            //nfe.printStackTrace();
                        } catch (Exception e) {
                            // ignore
                        } finally {
                            tries++;
                        }
                    }
                }
            }
            
            return displayName;
        }
        
        private int exponentialMultiplier = 1;
        
        @Override
        public String getHtmlDisplayName() {
            String retVal = getDisplayName();
            
            if (mainTraversal != null && mainTraversal.equals(originalFO)) {
                retVal = String.format("<b>%s</b>", retVal);
            }
            
            return retVal;
        }

        private void fireTraversalNodeDisplayNameChange(String o, String n) {
            this.fireDisplayNameChange(o, n);
        }
        
        @Override
        public Action[] getActions(boolean context) {
            Action[] actions = super.getActions(context);
            
            if (actions == null) {
                return null;
            }
            
            Action[] retVal = new Action[actions.length + 3];
            
            int size = actions.length;
            
            for (int i = 0; i < size; i++) {
                retVal[i] = actions[i];
            }
            
            retVal[size] = null;
            retVal[size + 1] = new SetMainTraversalAction();
            retVal[size + 2] = new ProcessTraversalAction();
            
            return retVal;
        }
        
        @NbBundle.Messages("CTL_SetMainTraversal=Set As Main Traversal")
        private final class SetMainTraversalAction extends AbstractAction {

            public SetMainTraversalAction() {
                putValue(NAME, Bundle.CTL_SetMainTraversal());
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Set Main Traversal actionPerformed called.");
                
                TraversalProviderService t = getTPS();
                
                if (t == null) {
                    return;
                }
                
                if (parentDO == null) {
                    return;
                }
                
                FileObject oldMainTraversal = t.getMainTraversal(parentFO);
                
                System.out.println("Previous Main traversal: " + oldMainTraversal);
                
                if (originalFO == null) {
                    return;
                }
                
                t.setMainTraversal(parentFO, originalFO);
                
                // also call getMainTraversal to update display names
                System.out.println("Action calling getMainTraversal to update nodes.");
                t.getMainTraversal(parentFO);
                
            }

            @Override
            public boolean isEnabled() {
                TraversalProviderService t = getTPS();
                
                return (t != null && !t.isMainTraversal(parentFO, originalFO));
            }
            
        }
        
        @NbBundle.Messages("CTL_ProcessTraversal=Process Traversal")
        private final class ProcessTraversalAction extends AbstractAction {

            public ProcessTraversalAction() {
                putValue(NAME, Bundle.CTL_ProcessTraversal());
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (parent == null) {
                    return;
                }
                
                if (originalFO == null) {
                    return;
                }
                
                TraversalProcessorService processor = (TraversalProcessorService)
                        parent.getFromProject(TraversalProcessorService.class);
                
                if (processor == null) {
                    return;
                }
                
                processor.processTraversal(originalFO, true);
            }
        }
    }
    
}