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
package org.cmuchimps.gort.modules.appview;

import att.grappa.Graph;
import att.grappa.GrappaConstants;
import att.grappa.GrappaPanel;
import att.grappa.Node;
import att.grappa.Parser;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.cmuchimps.gort.api.gort.GestureCollection;
import org.cmuchimps.gort.api.gort.GortDatabaseService;
import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.cmuchimps.gort.api.gort.TraversalProcessorService;
import org.cmuchimps.gort.api.gort.TraversalProcessorService.TraversalProcessedChangeEvent;
import org.cmuchimps.gort.api.gort.TraversalProviderService;
import org.cmuchimps.gort.modules.appview.components.AppViewGrappaAdapter;
import org.cmuchimps.gort.modules.appview.components.AppViewGrappaAdapter.GrappaStateSelectedChangeEvent;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.dataobject.Heuristic;
import org.cmuchimps.gort.modules.dataobject.Screenshot;
import org.cmuchimps.gort.modules.dataobject.Server;
import org.cmuchimps.gort.modules.dataobject.State;
import org.cmuchimps.gort.modules.dataobject.TaintLog;
import org.cmuchimps.gort.modules.helper.DataHelper;
import org.netbeans.api.project.Project;
import org.netbeans.api.settings.ConvertAsProperties;
import org.netbeans.api.visual.widget.ComponentWidget;
import org.netbeans.api.visual.widget.Scene;
import org.openide.awt.ActionID;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.MultiDataObject;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Result;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.CloneableTopComponent;
import org.openide.windows.TopComponentGroup;
import org.openide.windows.WindowManager;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.cmuchimps.gort.modules.appview//AppViewCloneable//EN",
        autostore = false)
@TopComponent.Description(
        preferredID = "AppViewCloneableTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_NEVER)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.cmuchimps.gort.modules.appview.AppViewCloneableTopComponent")
//@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_AppViewCloneableAction",
        preferredID = "AppViewCloneableTopComponent")
@Messages({
    "CTL_AppViewCloneableAction=AppViewCloneable",
    "CTL_AppViewCloneableTopComponent=AppViewCloneable",
    "HINT_AppViewCloneableTopComponent=This is a AppViewCloneable window"
})
public final class AppViewCloneableTopComponent extends CloneableTopComponent implements LookupListener, Lookup.Provider, PropertyChangeListener {

    // whether or not to show heuristics for the user study
    private static final boolean SHOW_HEURISTICS = true;
    
    private Project project;
    private MultiDataObject dataObject;
    private FileObject fileObject;
    
    private Result<TraversalProcessedChangeEvent> traversalProcessedResult;
    private Result<GrappaStateSelectedChangeEvent> stateSelectedResult;
    
    private GortDatabaseService databaseService;
    private ProjectDirectoryService directoryService;
    private TraversalProviderService traversalProvider;
    private TraversalProcessorService traversalProcessor;
    
    // graph to show the state graph from the traversal
    private Scene graphScene;
    //private JScrollPane graphScrollPane;
    private Graph graph;
    
    // the lookup allows us to inform the navigator of scene objects
    private final InstanceContent content;
    private final Lookup lookup;
    
    // panels/tables to present information
    private NetworkRequestsPanel networkRequestsPanel;
    private ServersPanel serversPanel;
    private PermissionsPanel permissionsPanel;
    private HeuristicsPanel heuristicsPanel;
    
    public AppViewCloneableTopComponent() {
        initComponents();
        setName(Bundle.CTL_AppViewCloneableTopComponent());
        setToolTipText(Bundle.HINT_AppViewCloneableTopComponent());
        
        this.coverFlowPanel.setVisible(false);
        
        // initialize the lookup components
        content = new InstanceContent();
        lookup = new AbstractLookup(content);
        
        // information panels
        networkRequestsPanel = new NetworkRequestsPanel();
        serversPanel = new ServersPanel();
        permissionsPanel = new PermissionsPanel(this);
        heuristicsPanel = new HeuristicsPanel();
        
        // set the alignment for screenshotLabel
        screenshotLabel.setHorizontalAlignment(SwingConstants.CENTER);
        screenshotLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        // Add a listener to this so that we can update
        TopComponent.Registry reg = TopComponent.getRegistry();
        reg.addPropertyChangeListener(WeakListeners.propertyChange(this, reg));
    }
    
    public AppViewCloneableTopComponent(Project project, MultiDataObject mdo) {
        this();
        this.project = project;
        this.dataObject = mdo;
        
        if (project != null) {
            TraversalProcessorService processor = project.getLookup().lookup(TraversalProcessorService.class);
            traversalProcessedResult = processor.getLookup().lookupResult(TraversalProcessedChangeEvent.class);
            traversalProcessedResult.addLookupListener(this);
        }
        
        if (this.dataObject != null) {
            fileObject = mdo.getPrimaryFile();
        }

        if (fileObject != null) {
            setDisplayName(fileObject.getName());
        }
        
        // process and show the traversal results
        //loadAnalysisThreaded();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        coverFlowPanel = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        cfgSplitPane = new javax.swing.JSplitPane();
        graphSceneScrollPane = new javax.swing.JScrollPane();
        screenshotLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        infoTabbedPane = new javax.swing.JTabbedPane();
        jLabel1 = new javax.swing.JLabel();

        setBackground(new java.awt.Color(226, 226, 226));
        setLayout(new java.awt.BorderLayout());

        coverFlowPanel.setBackground(new java.awt.Color(226, 226, 226));
        coverFlowPanel.setPreferredSize(new java.awt.Dimension(800, 100));

        javax.swing.GroupLayout coverFlowPanelLayout = new javax.swing.GroupLayout(coverFlowPanel);
        coverFlowPanel.setLayout(coverFlowPanelLayout);
        coverFlowPanelLayout.setHorizontalGroup(
            coverFlowPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1139, Short.MAX_VALUE)
        );
        coverFlowPanelLayout.setVerticalGroup(
            coverFlowPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        add(coverFlowPanel, java.awt.BorderLayout.PAGE_START);

        jSplitPane1.setBackground(new java.awt.Color(226, 226, 226));
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        cfgSplitPane.setBackground(new java.awt.Color(226, 226, 226));
        cfgSplitPane.setPreferredSize(new java.awt.Dimension(33080, 800));

        graphSceneScrollPane.setBackground(new java.awt.Color(238, 238, 238));
        graphSceneScrollPane.setMinimumSize(new java.awt.Dimension(560, 350));
        graphSceneScrollPane.setPreferredSize(new java.awt.Dimension(800, 800));
        cfgSplitPane.setLeftComponent(graphSceneScrollPane);

        org.openide.awt.Mnemonics.setLocalizedText(screenshotLabel, org.openide.util.NbBundle.getMessage(AppViewCloneableTopComponent.class, "AppViewCloneableTopComponent.screenshotLabel.text")); // NOI18N
        screenshotLabel.setMaximumSize(new java.awt.Dimension(240, 16));
        screenshotLabel.setMinimumSize(new java.awt.Dimension(45, 350));
        screenshotLabel.setPreferredSize(new java.awt.Dimension(240, 800));
        screenshotLabel.setSize(new java.awt.Dimension(240, 400));
        cfgSplitPane.setRightComponent(screenshotLabel);

        jSplitPane1.setLeftComponent(cfgSplitPane);

        jPanel1.setBackground(new java.awt.Color(226, 226, 226));
        jPanel1.setLayout(new java.awt.BorderLayout());

        infoTabbedPane.setMaximumSize(new java.awt.Dimension(32767, 100));
        infoTabbedPane.setMinimumSize(new java.awt.Dimension(800, 100));
        infoTabbedPane.setPreferredSize(new java.awt.Dimension(800, 150));
        jPanel1.add(infoTabbedPane, java.awt.BorderLayout.CENTER);

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(AppViewCloneableTopComponent.class, "AppViewCloneableTopComponent.jLabel1.text")); // NOI18N
        jPanel1.add(jLabel1, java.awt.BorderLayout.PAGE_START);

        jSplitPane1.setRightComponent(jPanel1);

        add(jSplitPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSplitPane cfgSplitPane;
    private javax.swing.JPanel coverFlowPanel;
    private javax.swing.JScrollPane graphSceneScrollPane;
    private javax.swing.JTabbedPane infoTabbedPane;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel screenshotLabel;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        GestureCollection.getInstance().topComponentOpened(this.getClass());
        //infoTabbedPane.add("Summary", new SummaryPanel());
        infoTabbedPane.add("Network Requests", networkRequestsPanel);
        infoTabbedPane.add("Servers", serversPanel);
        infoTabbedPane.add("Permissions", permissionsPanel);
        
        // for user study
        if (SHOW_HEURISTICS) {
            infoTabbedPane.add("Heuristics", heuristicsPanel);
        }
        
        infoTabbedPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                GestureCollection.getInstance().stateDetailTabClick("" + infoTabbedPane.getSelectedIndex());
            }
            
        });
        
        clearStateInformation();
        
        // make a call to update the information for this topcomponent
        update();
    }

    @Override
    public void componentClosed() {
        GestureCollection.getInstance().topComponentClosed(this.getClass());
        super.componentClosed();
    }

    @Override
    protected void componentShowing() {
        GestureCollection.getInstance().topComponentShowing(this.getClass());
        GestureCollection.getInstance().appViewShowing(fileObject.getNameExt());
        super.componentShowing();
        //System.out.println("componentShowing called.");
        openAppViewGroup();
    }

    @Override
    protected void componentHidden() {
        GestureCollection.getInstance().topComponentHidden(this.getClass());
        super.componentHidden();
        
        //System.out.println("componentHidden called.");
        
        Registry registry = TopComponent.getRegistry();
        
        Set<TopComponent> opened = registry.getOpened();
        
        boolean hideGroup = true;
        
        boolean atLeastOneInstanceOpen = false;
        boolean crowdDetailsOpen = false;
        
        for (TopComponent t : opened) {
            if (t instanceof AppViewCloneableTopComponent) {
                if (t.isShowing()) {
                    hideGroup = false;
                    break;
                } else {
                    atLeastOneInstanceOpen = true;
                }
            } else if (t instanceof CrowdDetailsTopComponent) {
                crowdDetailsOpen = true;
            }
        }
        
        if (crowdDetailsOpen && atLeastOneInstanceOpen) {
            hideGroup = false;
        }
        
        if (hideGroup) {
            closeAppViewGroup();
        }
    }

    @Override
    protected void componentActivated() {
        GestureCollection.getInstance().topComponentActivated(this.getClass());
        super.componentActivated();
    }

    @Override
    protected void componentDeactivated() {
        GestureCollection.getInstance().topComponentDeactivated(this.getClass());
        super.componentDeactivated();
    }
    
    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
    
    private TopComponentGroup getAppViewGroup() {
        return WindowManager.getDefault().findTopComponentGroup("AppViewGroup");
    }
    
    private void openAppViewGroup() {
        TopComponentGroup group = getAppViewGroup();
        
        if (group == null) {
            //System.out.println("AppViewGroup not found");
            return;
        }
        
        //System.out.println("Opening AppView group");
        group.open();
    }
    
    private void closeAppViewGroup() {
        TopComponentGroup group = getAppViewGroup();
        
        if (group == null) {
            //System.out.println("AppViewGroup not found");
            return;
        }
        
        group.close();
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }
    
    public Project getProject() {
        return project;
    }

    public MultiDataObject getDataObject() {
        return dataObject;
    }

    public FileObject getFileObject() {
        return fileObject;
    }
    
    public GortDatabaseService getDatabaseService() {
        if (databaseService == null) {
            if (project != null) {
                databaseService = (GortDatabaseService)
                        project.getLookup().lookup(GortDatabaseService.class);
            }
        }
        
        return databaseService;
    }
    
    public ProjectDirectoryService getDirectoryService() {
        if (directoryService == null) {
            if (project != null) {
                directoryService = (ProjectDirectoryService)
                        project.getLookup().lookup(ProjectDirectoryService.class);
            }
        }
        
        return directoryService;
    }
    
    public TraversalProviderService getTraversalProviderService() {
        if (traversalProvider == null) {
            if (project != null) {
                traversalProvider = (TraversalProviderService)
                        project.getLookup().lookup(TraversalProviderService.class);
            }
        }
        
        return traversalProvider;
    }
    
    public TraversalProcessorService getTraversalProcessorService() {
        if (traversalProcessor == null) {
            if (project != null) {
                traversalProcessor = (TraversalProcessorService)
                        project.getLookup().lookup(TraversalProcessorService.class);
            }
        }
        
        
        return traversalProcessor;
    }

    @Override
    public void resultChanged(LookupEvent le) {
        System.out.println("Result changed listening to Traversal Processing Service");
        
        if (le == null) {
            System.out.println("Lookup event is null.");
            return;
        }
        
        Lookup.Result result = (Lookup.Result) le.getSource();
        
        if (result == null) {
            return;
        }
        
        Collection c = result.allInstances();
        
        if (c == null || c.isEmpty()) {
            System.out.println("AppView lookup result is empty.");
            return;
        }
        
        Object o = c.iterator().next();
        
        if (o instanceof TraversalProcessedChangeEvent) {
            resultChanged((TraversalProcessedChangeEvent) o);
        } else if (o instanceof GrappaStateSelectedChangeEvent) {
            resultChanged((GrappaStateSelectedChangeEvent) o);
        }
    }
    
    private void resultChanged(TraversalProcessedChangeEvent e) {
        if (e == null) {
            return;
        }
        
        FileObject fo = e.getTraversal();
        if (fo != null && fo.getNameExt().startsWith(getFileObject().getNameExt())) {
            update();
        } else {
            System.out.println("Not updating. Different apk is processed.");
        }
    }
    
    private void resultChanged(GrappaStateSelectedChangeEvent e) {
        if (e == null) {
            return;
        }
        
        int stateId = e.getId();
        String stateName = e.getName();
        
        System.out.println(
                String.format("Received grappa state selected event %s %d", stateName, stateId));
        
        clearStateInformation();
        
        //TODO: update all screens related to the selected change event
        if (e.isValid()) {
            updateStateInformation(stateId, stateName);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // ignore
    }
    
    private void update() {
        System.out.println("Updating AppView UI...");
        
        if (fileObject == null) {
            return;
        }
        
        TraversalProviderService provider = getTraversalProviderService();
        TraversalProcessorService processor = getTraversalProcessorService();
        
        if (provider == null || processor == null) {
            return;
        }
        
        // get/set the main traversal for this apk
        FileObject traversal = provider.getMainTraversal(fileObject, true);
        
        if (traversal == null) {
            return;
        }
        
        // update will be called again once processing is done
        if (!processor.isProcessed(traversal)) {
            processor.processTraversal(traversal);
            return;
        }
        
        // show the results
        updateGraph(provider.getStateGraph(traversal));
    }
    
    private void updateGraph(FileObject dot) {
        System.out.println("Updating state graph...");
        
        // remove any listeners from the prior graph
        if (stateSelectedResult != null) {
            stateSelectedResult.removeLookupListener(this);
        }
        
        readGraph(dot);
        //processGraph();
        renderGraph();
    }
    
    private void readGraph(FileObject dot) {
        System.out.println("Reading graph dot file...");
        
        // get the dot file for the associated file
        if (dot == null || !dot.canRead()) {
            System.out.println("Invalid dot file for the state graph");
            return;
        } else {
            System.out.println("Graph file: " + dot.getPath());
        }
        
        InputStream input = null;
        
        try {
            input = dot.getInputStream();
            Parser parser = new Parser(input, System.err);
            parser.parse();
            graph = parser.getGraph();
            
            if (graph == null) {
                return;
            }
            
            graph.setEditable(false);
            graph.setErrorWriter(new PrintWriter(System.err,true));
            
        } catch (FileNotFoundException ex) {
            System.err.println("Could not find the graph dot file.");
            ex.printStackTrace();
        } catch (Exception e) {
            System.err.println("Parsing error while reading graph dot file.");
            e.printStackTrace();
        }finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }
    
    private void processGraph() {
        if (graph == null) {
            return;
        }
        
        Node[] nodes = graph.nodeElementsAsArray();
        
        if (nodes == null || nodes.length <= 0) {
            return;
        }
        
        graph.setEditable(true);
        
        for (int i = 0; i < nodes.length; i++) {
            Node n = nodes[i];
            
            if (n == null) {
                continue;
            }
            
            // clean the node name
            String name = n.getName();
            
            if (name != null) {
                String[] split = name.split(" ");
                n.setName(split[split.length - 1]);
            }
            
            // set the nodes shape to be box
            n.setAttribute(GrappaConstants.SHAPE_ATTR, GrappaConstants.BOX_SHAPE);
            n.setAttribute(GrappaConstants.WIDTH_ATTR, "2.25");
            n.setAttribute(GrappaConstants.HEIGHT_ATTR, "0.75");
            
        }
        
        graph.setEditable(false);
    }
    
    private void renderGraph() {
        System.out.println("Rendering state graph...");
        
        if (graph == null) {
            System.out.println("Cannot render graph. Graph is null.");
            return;
        }
        
        AppViewGrappaAdapter adapter = new AppViewGrappaAdapter();
        stateSelectedResult = adapter.getLookup().lookupResult(GrappaStateSelectedChangeEvent.class);
        stateSelectedResult.addLookupListener(this);
        
        GrappaPanel gp = new GrappaPanel(graph);
        gp.addGrappaListener(adapter);
        gp.setScaleToFit(false);
        
        if (graphScene != null) {
            graphScene.revalidate();
            graphScene.removeChildren();
            graphScene.revalidate();
            graphScene = null;
        }
        
        graphScene = new Scene();
        graphSceneScrollPane.setViewportView(graphScene.createView());
        graphScene.revalidate();
        
        ComponentWidget cw = new ComponentWidget(graphScene, gp);
        graphScene.addChild(cw);
        graphScene.revalidate();
        
        //the original scrollpane version
        //graphScrollPane.setViewportView(gp);
        
        System.out.println("Rendering graph file done.");
        
        // Add the scene to the lookup
        content.set(Arrays.asList(graphScene), null);
    }
    
    private synchronized void clearStateInformation() {
        clearScreenshot();
        //also network requests, servers, permissions, heuristics
        clearNetworkRequests();
        clearServers();
        clearPermissionsPanel();
    }
    
    private synchronized void updateStateInformation(int id, String name) {
        if (id < 0) {
            String[] nameSplit = name.replace("\"", "").split("-");
            try {
                id = Integer.parseInt(nameSplit[nameSplit.length - 1]);
            } catch (Exception e) {
                System.out.println("Issue getting id for the state");
                e.printStackTrace();
                return;
            }
        }
        
        GortDatabaseService gds = getDatabaseService();
        
        if (gds == null) {
            return;
        }
        
        GortEntityManager gem = gds.getGortEntityManager();
        
        if (gem == null) {
            return;
        }
        
        EntityManager em = gem.getEntityManager();
        
        try {
            State s = gem.selectState(em, id);
            
            if (s == null) {
                return;
            }
            
            List<Screenshot> screenshots = s.getScreenshots();
            
            // get the first screenshot
            if (screenshots != null && !screenshots.isEmpty()) {
                Screenshot screenshot = screenshots.iterator().next();
                updateScreenshot(screenshot.getPath());
            }
            
            updateServers(s.getServers());
            
            // Get the taintlogs and also initialize the server for each one
            List<TaintLog> taintLogs = s.getTaintLogs();
            
            if (taintLogs != null) {
                for (TaintLog t : taintLogs) {
                    t.getServer();
                }
            }
            
            updateNetworkRequests(taintLogs);
            
            updatePermissionsPanel(taintLogs);
            
            updateHeuristicsPanel(s.getHeuristics());
            
        } finally {
            GortEntityManager.closeEntityManager(em);
        }
    }
    
    private void clearScreenshot() {
        screenshotLabel.setIcon(null);
        
        // inform that no state is selected
        screenshotLabel.setText("<Select State for Information>");
    }
    
    private void updateScreenshot(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        
        if (project == null) {
            return;
        }
        
        FileObject dir = project.getProjectDirectory();
        
        if (dir == null) {
            return;
        }
        
        FileObject imageFO = dir.getFileObject(path);
        
        if (imageFO == null || !imageFO.canRead()) {
            return;
        }

        try {
            Image img = ImageIO.read(FileUtil.toFile(imageFO));
            //Image resizedImage = img.getScaledInstance(Constants.SCREENSHOT_WIDTH, Constants.SCREENSHOT_HEIGHT, Image.SCALE_DEFAULT);
            ImageIcon icon = null;
            int width = screenshotLabel.getWidth();
            int height = screenshotLabel.getHeight();
            
            if (width > 0 && height > 0) {
                Image resizedImage = img.getScaledInstance(width, height, Image.SCALE_DEFAULT);
                icon = new ImageIcon(resizedImage);
            } else {
                icon = new ImageIcon(img);
            }
            screenshotLabel.setText(null);
            screenshotLabel.setIcon(icon);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private void clearServers() {
        if (serversPanel == null) {
            return;
        }
        
        serversPanel.getModel().setNoData();
        serversPanel.getModel().fireTableDataChanged();
    }
    
    private void updateServers(List<Server> servers) {
        System.out.println("Updating servers panel.");
        
        if (servers == null || servers.isEmpty()) {
            System.out.println("Servers list is null or invalid.");
            return;
        }
        
        if (serversPanel == null) {
            return;
        }
        
        List<String[]> data = new LinkedList<String[]>();
        
        Set<Server> dataAdded = new HashSet<Server>();
        
        for (Server s : servers) {
            if (s == null) {
                continue;
            }
            
            String ip = s.getIp();
            
            if (ip == null || ip.isEmpty()) {
                continue;
            }
            
            // Do not repeat servers
            if (dataAdded.contains(s)) {
                continue;
            } else {
                dataAdded.add(s);
            }
            
            String[] row = new String[serversPanel.numCols()];
            
            row[0] = ip;
            row[1] = s.getHostname();
            row[2] = s.getName();
            row[3] = s.getAddress();
            row[4] = s.getCity();
            row[5] = s.getStateprov();
            row[6] = s.getCountry();
            row[7] = s.getPostalCode();
            row[8] = s.getPhone();
            row[9] = s.getEmail();
            
            data.add(row);
        }
        
        if (data.size() <= 0) {
            return;
        }
        
        serversPanel.getModel().setData(DataHelper.to2DArray(data));
        serversPanel.getModel().fireTableDataChanged();
    }
    
    private void clearNetworkRequests() {
        if (networkRequestsPanel == null) {
            return;
        }
        
        networkRequestsPanel.clear();
    }
    
    private void updateNetworkRequests(List<TaintLog> taintLogs) {
        System.out.println("Updating network requests panel.");
        
        if (networkRequestsPanel == null) {
            return;
        }
        
        networkRequestsPanel.setData(taintLogs);
    }
    
    private void clearPermissionsPanel() {
        if (permissionsPanel == null) {
            return;
        }

        permissionsPanel.getModel().setNoData();
        permissionsPanel.getModel().fireTableDataChanged();
    }
    
    private void updatePermissionsPanel(List<TaintLog> taintLogs) {
        System.out.println("Updating permissions panel.");
        
        if (permissionsPanel == null) {
            return;
        }
        
        permissionsPanel.setData(taintLogs);
    }
    
    public static AppViewCloneableTopComponent getShowingAppView() {
        Set<TopComponent> components = TopComponent.getRegistry().getOpened();
            
        if (components == null) {
            return null;
        }

        boolean found = false;

        for (TopComponent c : components) {
            if (c instanceof AppViewCloneableTopComponent && c.isShowing()) {
                return (AppViewCloneableTopComponent) c;
            }
        }

        return null;
    }
    
    private void clearHeuristicsPanel() {
        if (heuristicsPanel == null || !SHOW_HEURISTICS) {
            return;
        }
        
        heuristicsPanel.clear();
    }
    
    private void updateHeuristicsPanel(List<Heuristic> heuristics) {
        if (!SHOW_HEURISTICS) {
            return;
        }
        
        heuristicsPanel.setData(heuristics);
    }
}
