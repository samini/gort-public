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

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import org.cmuchimps.gort.api.gort.GestureCollection;
import org.cmuchimps.gort.api.gort.GortDatabaseService;
import org.cmuchimps.gort.api.gort.ProjectUtility;
import org.cmuchimps.gort.api.gort.TraversalProcessorService;
import org.cmuchimps.gort.api.gort.TraversalProviderService;
import org.cmuchimps.gort.modules.appview.components.Colors;
import org.cmuchimps.gort.modules.dataobject.App;
import org.cmuchimps.gort.modules.dataobject.CrowdTask;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.dataobject.HIT;
import org.cmuchimps.gort.modules.dataobject.Traversal;
import org.cmuchimps.gort.modules.helper.DataHelper;
import org.cmuchimps.gort.modules.helper.TaintHelper;
import org.cmuchimps.gort.modules.tablewidgets.StringTableModel;
import org.netbeans.api.project.Project;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.cmuchimps.gort.modules.appview//Crowd//EN",
        autostore = false)
@TopComponent.Description(
        preferredID = "CrowdTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
//@TopComponent.Registration(mode = "commonpalette", openAtStartup = false)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "org.cmuchimps.gort.modules.appview.CrowdTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_CrowdAction",
        preferredID = "CrowdTopComponent")
@Messages({
    "CTL_CrowdAction=Crowd",
    "CTL_CrowdTopComponent=Crowd",
    "HINT_CrowdTopComponent=This is a Crowd window"
})
public final class CrowdTopComponent extends TopComponent implements LookupListener, Lookup.Provider, PropertyChangeListener {

    private static final String[] HEADERS = {"Task", "Resources", "Expected", "Comfort (µ,σ)", "N", "Submission", "Completion"};
    
    private static final int COLUMN_INDEX_EXPECTED = 2;
    private static final int COLUMN_INDEX_COMFORT = 3;
    
    private static final String TEXT_YES = "Yes";
    private static final String TEXT_NO = "No";
    private static final String TEXT_UNKNOWN = "Unknown";
    
    private static final String REGEX_COMFORT = "\\(.+, .+\\)";
    
    private Lookup.Result<TraversalProcessorService.TraversalProcessedChangeEvent> lookupResult;
    
    private StringTableModel model;
    private JTable table;
    
    private final List<CrowdTask> showingCrowdTasks = new ArrayList();
    
    private final InstanceContent content;
    private final Lookup lookup;
    
    private Project project;
    
    public CrowdTopComponent() {
        initComponents();
        setName(Bundle.CTL_CrowdTopComponent());
        setToolTipText(Bundle.HINT_CrowdTopComponent());
        
        content = new InstanceContent();
        lookup = new AbstractLookup(content);
        
        model = new StringTableModel(HEADERS);
        crowdTableScrollPane.setModel(model);
        
        table = new JTable() {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                if (row < 0) {
                    return c;
                }
                
                Object o = getValueAt(row, column);
                String value = null;
                
                if (o != null && o instanceof String) {
                    value = (String) o;
                }
                
                // set the color for SSL
                if (column == COLUMN_INDEX_EXPECTED) {
                    if (TEXT_YES.equals(value)) {
                        c.setForeground(Colors.CUSTOM_GREEN);
                    } else if (TEXT_NO.equals(value)) {
                        c.setForeground(Color.RED);
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                } else if (column == COLUMN_INDEX_COMFORT) {
                    if (value == null || !value.contains("(") || !value.contains(")")) {
                        c.setForeground(Color.BLACK);
                    } else {
                        value = value.replace("(", "");
                        value = value.replace(")", "");
                        String comfort = value.split(",")[0];
                        try {
                            double comfortLevel = Double.parseDouble(comfort);
                            if (comfortLevel > 0) {
                                c.setForeground(Colors.CUSTOM_GREEN);
                            } else if (comfortLevel < 0) {
                                c.setForeground(Color.RED);
                            } else {
                                c.setForeground(Color.BLACK);
                            }
                        } catch (NumberFormatException e) {
                            c.setForeground(Color.BLACK);
                        }
                    }
                } else {
                    c.setForeground(Color.BLACK);
                }
                
                return c;
            }
        };
        
        // add listeners to the jtable for clicks and selection
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent event) {
                // do some actions here, for example
                // print first column value from selected row
                //System.out.println(table.getValueAt(table.getSelectedRow(), 0).toString());
                int row = table.getSelectedRow();
                
                if (row < 0) {
                    return;
                }
                
                row = table.convertRowIndexToModel(row);
                
                // put the associated crowdtask in the lookup
                updateLookup(row);
            }
        });
        
        // double clicks should open top component if not open
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table =(JTable) me.getSource();
                Point p = me.getPoint();
                
                if (p == null) {
                    return;
                }
                
                int row = table.rowAtPoint(p);
                
                if (row < 0) {
                    return;
                }
                
                if (me.getClickCount() == 2) {
                    GestureCollection.getInstance().crowdTaskDoubleClick("" + row);
                    
                    CrowdDetailsTopComponent cd = CrowdDetailsTopComponent.getInstance();
                    if (!cd.isOpened()) {
                        cd.open();
                    }
                    
                    cd.requestActive();
                    
                    // put the associated crowdtask in the lookup
                    updateLookup(row);
                }
            }
        });
        
        crowdTableScrollPane.setTable(table);
        
        // specify a new sorter for the table
        TableRowSorter sorter = (TableRowSorter) table.getRowSorter();
        
        if (sorter == null) {
            sorter = new TableRowSorter<StringTableModel>();
            table.setRowSorter(sorter);
        }
        
        sorter.setComparator(COLUMN_INDEX_COMFORT, new Comparator<String> () {

            @Override
            public int compare(String s1, String s2) {
                // if both null or both are the same thing
                if (s1 == s2) {
                    return 0;
                }
                
                if (s1 == null) {
                    return -1;
                } else if (s2 == null) {
                    return 1;
                }
                
                // if they conform to the comfort regex use only the comfort value
                if (s1.matches(REGEX_COMFORT) && s2.matches(REGEX_COMFORT)) {
                    try {
                        Double comfort1 = Double.parseDouble(s1.replaceAll(", .+\\)", "").replace("(", ""));
                        Double comfort2 = Double.parseDouble(s2.replaceAll(", .+\\)", "").replace("(", ""));
                        
                        int meanCompare = comfort1.compareTo(comfort2);
                        
                        if (meanCompare != 0) {
                            return meanCompare;
                        }
                        
                        // check the stddevs. the one with higher standard dev should be ranked lower
                        Double stddev1 = Double.parseDouble(s1.replaceAll("\\(.+, ", "").replace(")", ""));
                        Double stddev2 = Double.parseDouble(s2.replaceAll("\\(.+, ", "").replace(")", ""));
                        
                        // a higher std dev shows less comfort!
                        return -stddev1.compareTo(stddev2);
                        
                    } catch (Exception e) {
                        // ignore
                    }
                }
                
                return s1.compareTo(s2);
            }
            
        });

        // Add a listener to this so that we can update
        TopComponent.Registry reg = TopComponent.getRegistry();
        reg.addPropertyChangeListener(WeakListeners.propertyChange(this, reg));
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }
    
    private void updateLookup(int row) {
        if (row < -1) {
            return;
        }
        
        //System.out.println("Updating Crowd Lookup with row: " + row);
        
        if (showingCrowdTasks == null) {
            return;
        }
        
        CrowdTask ct = null;
        
        try {
            ct = showingCrowdTasks.get(row);
            GestureCollection.getInstance().crowdTaskSelectionChanged("" +  ct.getId());
            System.out.println(ct.getResourceJustificationHIT().getInput());
        } catch (NullPointerException e) {
            // ignore
        } catch (IndexOutOfBoundsException e) {
            // ignore
        }
        
        if (ct != null) {
            content.set(Collections.singleton(ct), null);
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        crowdTableScrollPane = new org.cmuchimps.gort.modules.tablewidgets.TableScrollPane();

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(crowdTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(crowdTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.cmuchimps.gort.modules.tablewidgets.TableScrollPane crowdTableScrollPane;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        GestureCollection.getInstance().topComponentOpened(this.getClass());
        super.componentOpened();
    }

    @Override
    public void componentClosed() {
        GestureCollection.getInstance().topComponentClosed(this.getClass());
        super.componentClosed();
    }

    @Override
    protected void componentShowing() {
        GestureCollection.getInstance().topComponentShowing(this.getClass());
        super.componentShowing();
    }

    @Override
    protected void componentHidden() {
        GestureCollection.getInstance().topComponentHidden(this.getClass());
        super.componentHidden();
    }

    @Override
    protected void componentActivated() {
        GestureCollection.getInstance().topComponentActivated(this.getClass());
        super.componentActivated(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void componentDeactivated() {
        GestureCollection.getInstance().topComponentDeactivated(this.getClass());
        super.componentDeactivated(); //To change body of generated methods, choose Tools | Templates.
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

    @Override
    public void resultChanged(LookupEvent le) {
        if (le == null) {
            return;
        }
        
        Lookup.Result r = (Lookup.Result) le.getSource();
        Collection c = r.allInstances();
        
        if (c == null || c.isEmpty()) {
            return;
        }
        
        Object o = c.iterator().next();
        
        if (o == null) {
            return;
        }

        if (o instanceof TraversalProcessorService.TraversalProcessedChangeEvent) {
            TraversalProcessorService.TraversalProcessedChangeEvent e = (TraversalProcessorService.TraversalProcessedChangeEvent) o;
            resultChanged(e);
        }
    }
    
    private void resultChanged(TraversalProcessorService.TraversalProcessedChangeEvent e) {
        if (e == null) {
            return;
        }
        
        FileObject fo = e.getTraversal();
        
        if (fo == null) {
            return;
        }
        
        // only update the heuristics panel for the current viewable file
        TopComponent activated = TopComponent.getRegistry().getActivated();
        
        if (activated == null || !(activated instanceof AppViewCloneableTopComponent)) {
            return;
        }
        
        AppViewCloneableTopComponent appView = (AppViewCloneableTopComponent) activated;
        
        FileObject viewableFO = appView.getFileObject();
        
        if (viewableFO == null) {
            return;
        }
        
        if (fo.getNameExt().startsWith(viewableFO.getNameExt())) {
            update();
        } else {
            System.out.println("Not updating Crowd. Different apk is processed.");
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt == null) {
            return;
        }
        
        if (!TopComponent.Registry.PROP_ACTIVATED.equals(evt.getPropertyName())) {
            return;
        }
        
        update();
    }
    
    private void update() {
        //System.out.println("Crowd TopComponent Update called.");
        
        TopComponent activated = TopComponent.getRegistry().getActivated();
        
        if (activated == null) {
            return;
        }
        
        if (activated == this) {
            // find if there are any associated AppViews
            activated = AppViewCloneableTopComponent.getShowingAppView();
            if (activated == null) {
                return;
            }
        } else if (!(activated instanceof AppViewCloneableTopComponent)) {
            return;
        }
        
        System.out.println("Updating appview Crowd component");
        
        AppViewCloneableTopComponent appView = (AppViewCloneableTopComponent) activated;
        
        Project project = appView.getProject();
        FileObject fo = appView.getFileObject();
        
        if (project == null || fo == null) {
            return;
        }
        
        // TODO: check if this may add multiple instances of listeners or just adds one
        TraversalProcessorService processor = project.getLookup().lookup(TraversalProcessorService.class);
        lookupResult = processor.getLookup().lookupResult(TraversalProcessorService.TraversalProcessedChangeEvent.class);
        lookupResult.addLookupListener(this);
        
        GortDatabaseService gds = project.getLookup().lookup(GortDatabaseService.class);
        
        if (gds == null) {
            return;
        }
        
        GortEntityManager gem = gds.getGortEntityManager();
        
        if (gem == null) {
            return;
        }
        
        EntityManager em = gem.getEntityManager();
        
        try {
            App app = gem.selectApp(em, fo.getNameExt());
            
            if (app == null) {
                return;
            }
            
            clear();
            
            TraversalProviderService provider = ProjectUtility.getTraversalProviderService(project);
            
            if (provider == null) {
                return;
            }
            
            FileObject traversalFO = provider.getMainTraversal(fo);
            
            if (traversalFO == null) {
                return;
            }
            
            Traversal traversal = gem.selectTraversal(em, traversalFO.getNameExt());
            
            if (traversal == null) {
                return;
            }
            
            setData(project, traversal.getCrowdTasks());
        } finally {
            GortEntityManager.closeEntityManager(em);
        }
    }
    
    private synchronized void clear() {
        model.setNoData();
        model.fireTableDataChanged();
        showingCrowdTasks.clear();
        project = null;
    }
    
    private synchronized void setData(Project project, List<CrowdTask> crowdTasks) {
        if (crowdTasks == null || crowdTasks.isEmpty()) {
            return;
        }
        
        Iterator<CrowdTask> iterator = crowdTasks.iterator();
        
        List<String[]> data = new LinkedList<String[]>();
        
        while (iterator.hasNext()) {
            
            CrowdTask ct = iterator.next();
            
            List<HIT> hits = ct.getHits();
            
            String task = null;
            String resources = null;
            String expected = null;
            String comfort = null;
            String submission = null;
            String completion = null;
            
            task = ct.getSelectedLabel();
            
            if (task == null) {
                HIT h = ct.getResourceJustificationHIT();
                if (h != null) {
                    task = h.getInput();
                }
            }
            
            if (task == null) {
                HIT h = ct.getTaskVerificationHIT();
                if (h != null) {
                    String input = h.getInput();
                    task = (input != null) ? input.split("\\|\\|")[0] : null;
                }
            }
            
            resources = TaintHelper.crowdDefinitionsFromTag(ct.getCombinedTaintTag());
            
            if (ct.getExpectedMedian() == null) {
                expected = TEXT_UNKNOWN;
            } else if (Boolean.TRUE.equals(ct.getExpectedMedian())) {
                expected = TEXT_YES;
            } else {
                expected = TEXT_NO;
            }
            
            Double mean = ct.getComfortAverage();
            Double stddev = ct.getComfortStdDev();
            
            if (mean == null || stddev == null) {
                comfort = TEXT_UNKNOWN;
            } else {
                comfort = String.format("(%.1f, %.1f)", mean.doubleValue(), stddev.doubleValue());
            }
            
            // {"Task", "Resources", "Expected", "Comfort (µ,σ)", "Submission", "Completion"};
            String[] row = new String[HEADERS.length];
            
            row[0] = task;
            row[1] = resources;
            row[2] = expected;
            row[3] = comfort;
            row[4] = "" + ct.getValid();
            row[5] = (ct.getSubmission() != null) ? ct.getSubmission().toLocaleString() : null;
            row[6] = (ct.getCompletion()!= null) ? ct.getCompletion().toLocaleString() : null;
            
            data.add(row);
            showingCrowdTasks.add(ct);
        }
        
        RowSorter sorter = table.getRowSorter();
        
        if (sorter != null) {
            try {
                sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(COLUMN_INDEX_COMFORT, SortOrder.ASCENDING)));
            } catch (Exception e) {
                // ignore
            }
        }
        
        // updating the data table. also set the associated project
        this.project = project;
        model.setData(DataHelper.to2DArray(data));
        model.fireTableDataChanged();
    }

    public Project getProject() {
        return project;
    }
    
}
