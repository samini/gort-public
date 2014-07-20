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
package org.cmuchimps.gort.modules.appview.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.cmuchimps.gort.api.gort.GestureCollection;
import org.cmuchimps.gort.api.gort.heuristic.IHeuristic;
import org.cmuchimps.gort.modules.dataobject.Heuristic;
import org.cmuchimps.gort.modules.tablewidgets.StringTableModel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 *
 * @author shahriyar
 */
public class HeuristicsTable {
    //private static final String[] HEADERS = {"Heuristic", "Type", "Status"};
    private static final String[] HEADERS = {"Heuristic", "Status"};
    
    public static final int COLUMN_INDEX_HEURISTIC = 0;
    //public static final int COLUMN_INDEX_TYPE = 1;
    //public static final int COLUMN_INDEX_STATUS = 2;
    public static final int COLUMN_INDEX_STATUS = 1;
    
    private StringTableModel model;
    private JTable table;
    
    // TODO: need a cleaner implementation
    private Map<String, String> heuristicSummaryMap = new HashMap<String, String>();
    private List<Heuristic> heuristicsList = new ArrayList<Heuristic>();

    public HeuristicsTable() {
        model = new StringTableModel(HEADERS);
        
        table = new JTable() {

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                
                // set the color for heuristic outputs
                // result column
                if (column == COLUMN_INDEX_STATUS) {
                    Object o = getValueAt(row, column);
                    
                    if (o != null && o instanceof String) {
                        String value = (String) o;
                        if (value.equals(IHeuristic.STATUS_SAFE)) {
                            c.setForeground(Colors.CUSTOM_GREEN);
                        } else if (value.equals(IHeuristic.STATUS_UNSAFE)) {
                            c.setForeground(Color.RED);
                        } else if (value.equals(IHeuristic.STATUS_UNKNOWN)) {
                            c.setForeground(Colors.CUSTOM_ORANGE);
                        } else {
                            c.setForeground(Color.BLACK);
                        }
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                    
                } else {
                    c.setForeground(Color.BLACK);
                }
                
                int adjustedIndex = table.convertRowIndexToModel(row);
                
                if (adjustedIndex >= 0 && adjustedIndex < heuristicsList.size()) {
                    
                    Heuristic h = heuristicsList.get(adjustedIndex);
                    if (h != null && c instanceof JComponent) {
                        JComponent jc = (JComponent) c;
                        jc.setToolTipText(h.getSummary());
                        GestureCollection.getInstance().heuristicTooltipView(h.getSummary());
                    }
                }
                
                /*
                // set a tooltip for rows to show heuristic summary
                Object o = getValueAt(row, 0);
                
                if (heuristicSummaryMap != null && o != null && o instanceof String) {
                    String value = (String) o;
                    
                    if (c instanceof JComponent) {
                        JComponent jc = (JComponent) c;
                        jc.setToolTipText(heuristicSummaryMap.get(value));
                    }
                }*/
                
                return c;
            }
            
        };
        
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent lse) {
                if (lse == null) {
                    return;
                }
                
                if (lse.getValueIsAdjusting()) {
                    return;
                }
                
                final int row = table.getSelectedRow();
                
                if (row < 0) {
                    return;
                }
                
                GestureCollection.getInstance().heuristicSelectionChange("" + row);
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
                    
                    int adjustedIndex = table.convertRowIndexToModel(row);
                    
                    if (adjustedIndex >= 0 && adjustedIndex < heuristicsList.size()) {
                    
                    Heuristic h = heuristicsList.get(adjustedIndex);
                        if (h != null) {
                            if (h.getDescription() != null) {
                                displayInformationMessage(h.getDescription());
                            } else if (h.getSummary() != null) {
                                displayInformationMessage(h.getSummary());
                            }
                        }
                    }
                    
                }
            }
        });
        
        table.setAutoCreateRowSorter(true);
    }
    
    public StringTableModel getModel() {
        return model;
    }

    public JTable getTable() {
        return table;
    }
    
    public void clear() {
        if (model != null) {
            model.setNoData();
            model.fireTableDataChanged();
        }
        
        /*
        if (heuristicSummaryMap != null) {
            heuristicSummaryMap.clear();
        }*/
        
        if (heuristicsList != null) {
            heuristicsList.clear();
        }
    }
    
    public void setData(List<Heuristic> heuristics) {
        if (heuristics == null || heuristics.isEmpty()) {
            return;
        }
        
        List<Heuristic> checkedHeuristics = checkHeuristics(heuristics);
        
        String[][] data = new String[checkedHeuristics.size()][HEADERS.length];
        
        int index = 0;
        
        // Add sorting option to the columns!
        for (Heuristic h : checkedHeuristics) {
            if (h == null) {
                continue;
            }
            
            String name = h.getName();
            
            if (name == null || name.isEmpty()) {
                continue;
            }
            
            String summary = h.getSummary();
            String desc = h.getDescription();
            
            //heuristicSummaryMap.put(name, summary);
            heuristicsList.add(h);
            
            Boolean result = h.getResult();
            
            data[index][0] = name;
            
            //data[index][1] = h.getType();
            
            String status = null;
            
            if (Boolean.TRUE.equals(result)) {
                status = IHeuristic.STATUS_UNSAFE;
            } else if (Boolean.FALSE.equals(result)) {
                status = IHeuristic.STATUS_SAFE;
            } else {
                status = IHeuristic.STATUS_UNKNOWN;
            }
            
            data[index][COLUMN_INDEX_STATUS] = status;
            
            index++;
        }
        
        RowSorter sorter = table.getRowSorter();
        
        if (sorter != null) {
            try {
                sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(COLUMN_INDEX_STATUS, SortOrder.DESCENDING)));
            } catch (Exception e) {
                // ignore
            }
        }
        
        model.setData(data);
        model.fireTableDataChanged();
    }
    
    public void removeColumn(int index) {
        if (table == null) {
            return;
        }
        
        TableColumnModel tcm = table.getColumnModel();
        
        if (tcm == null) {
            return;
        }
        
        TableColumn column = null;
        
        try {
            column = tcm.getColumn(index);
        } catch (ArrayIndexOutOfBoundsException e) {
            column = null;
        }
        
        if (column == null) {
            return;
        }
        
        try {
            table.removeColumn(column);
        } catch (Exception e) {
            // ignore
        }
    }
    
    private void displayInformationMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        
        GestureCollection.getInstance().heuristicDoubleClick(message);
        
        final NotifyDescriptor nd = new NotifyDescriptor.Message(
                message, NotifyDescriptor.INFORMATION_MESSAGE);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                DialogDisplayer.getDefault().notify(nd);
            }
        });
    }
    
    // some heuristics may be double computed. 
    // TODO: needs to be fixed during computation for not also check for double computations
    private List<Heuristic> checkHeuristics(List<Heuristic> heuristics) {
        if (heuristics == null || heuristics.size() <= 1) {
            return heuristics;
        }
        
        LinkedHashMap<String, Heuristic> tmp = new LinkedHashMap<String, Heuristic>();
        
        for (Heuristic h : heuristics) {
            if (h == null || h.getClazz() == null) {
                continue;
            }
            
            if (!tmp.containsKey(h.getClazz())) {
                tmp.put(h.getClazz(), h);
            } else {
                Heuristic storedHeuristic = tmp.get(h.getClazz());
                if (storedHeuristic == h || Boolean.TRUE.equals(storedHeuristic.getResult())) {
                    continue;
                } else if (storedHeuristic.getResult() == null) {
                    tmp.put(h.getClazz(), h);
                    // give preference to showing an unsafe result
                } else if (Boolean.TRUE.equals(h.getResult())) {
                    tmp.put(h.getClazz(), h);
                }
            }
        }
        
        List<Heuristic> retVal = new LinkedList<Heuristic>();
        
        retVal.addAll(tmp.values());
        
        return retVal;
    }
    
}
