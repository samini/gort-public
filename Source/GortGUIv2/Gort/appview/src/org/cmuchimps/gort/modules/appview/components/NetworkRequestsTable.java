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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import org.cmuchimps.gort.api.gort.DetailMessageService;
import org.cmuchimps.gort.api.gort.GestureCollection;
import org.cmuchimps.gort.modules.dataobject.Server;
import org.cmuchimps.gort.modules.dataobject.TaintLog;
import org.cmuchimps.gort.modules.helper.DataHelper;
import org.cmuchimps.gort.modules.helper.ISO8601Helper;
import org.cmuchimps.gort.modules.helper.TaintHelper;
import org.cmuchimps.gort.modules.tablewidgets.StringTableModel;

/**
 *
 * @author shahriyar
 */
public class NetworkRequestsTable {
    private static final String[] HEADERS = {
        "Timestamp", "IP", "Hostname", "Registrant", "Uses Encryption (SSL)", "Transmits", "Data"
        };
    
    private static final int TOOL_TIP_TEXT_LENGTH = 128;
    private static final int COLUMN_INDEX_SSL = 4;
    private static final int COLUMN_INDEX_DATA = 6;
    
    private static final String TEXT_YES = "Yes";
    private static final String TEXT_NO = "No";
    
    private StringTableModel model;
    private JTable table;
    
    private DetailMessageService dms;

    public NetworkRequestsTable() {
        super();
        model = new StringTableModel(HEADERS);
        
        table = new JTable() {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                if (row < 0) {
                    return c;
                }
                
                // set the color for SSL
                if (column == COLUMN_INDEX_SSL) {
                    Object o = getValueAt(row, column);
                    
                    if (o != null && o instanceof String) {
                        String value = (String) o;
                        if (value.equals(TEXT_YES)) {
                            c.setForeground(Colors.CUSTOM_GREEN);
                        } else if (value.equals(TEXT_NO)) {
                            c.setForeground(Color.RED);
                        } else {
                            c.setForeground(Color.BLACK);
                        }
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                } else {
                    c.setForeground(Color.BLACK);
                }
                
                // set the tool tip
                Object o = null;
                
                try {
                    o = getValueAt(row, COLUMN_INDEX_DATA);
                } catch (Exception e) {
                    o = null;
                }
                
                if (o != null && o instanceof String) {
                    String value = (String) o;
                    
                    if (c instanceof JComponent && !value.isEmpty()) {
                        JComponent jc = (JComponent) c;
                        
                        String text = value;
                        
                        if (value.length() > TOOL_TIP_TEXT_LENGTH) {
                            text = text.substring(0, TOOL_TIP_TEXT_LENGTH) + "...";
                        }
                        
                        GestureCollection.getInstance().taintlogTooltipView(text);
                        jc.setToolTipText(text);
                    }
                }
                
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
                
                Object o = null;
                
                try {
                    o = table.getValueAt(row, COLUMN_INDEX_DATA);
                } catch (Exception e) {
                    o = null;
                }
                
                if (o == null) {
                    return;
                }
                
                if (o instanceof String) {
                    GestureCollection.getInstance().taintlogSelectionChange("" + o);
                    String value = (String) o;
                    getDms().sendMessage(value);
                }
                
            }
        });
        
        table.setModel(model);
    }
    
    public DetailMessageService getDms() {
        if (dms == null) {
            dms = DetailMessageService.getDefault();
        }
        
        return dms;
    }

    public StringTableModel getModel() {
        return model;
    }

    public JTable getTable() {
        return table;
    }
    
    public void clear() {
        model.setNoData();
        model.fireTableDataChanged();
    }
    
    public void setData(List<TaintLog> taintLogs) {
        
        if (taintLogs == null || taintLogs.isEmpty()) {
            System.out.println("Taintlog list is null or invalid.");
            return;
        }
        
        List<String[]> data = new LinkedList<String[]>();
        
        for (TaintLog t : taintLogs) {
            if (t == null || !t.isTransmission()) {
                continue;
            }
            
            String[] row = new String[model.getColumnCount()];
            
            Date date = t.getTimestamp();
            
            String timestamp = null;
            
            if (date != null) {
                timestamp = ISO8601Helper.toISO8601(date.getTime(), null);
                String[] timestampSplit = timestamp.split("T");
                timestamp = timestampSplit[timestampSplit.length - 1];
            }
            
            row[0] = timestamp;
            
            Server s = t.getServer();
            
            if (s != null) {
                row[1] = s.getIp();
                row[2] = s.getHostname();
                row[3] = s.getName();
            }
            
            row[COLUMN_INDEX_SSL] = t.isEncrypedTransmission() ? TEXT_YES : TEXT_NO;
            
            String[] sources = TaintHelper.definitionsFromTag(t.getTainttag());
            
            if (sources != null) {
                StringBuilder sb = new StringBuilder();
                
                for (String source: sources) {
                    sb.append(source);
                    sb.append(", ");
                }
                
                sb.replace(sb.length()-2, sb.length(), "");
                row[5] = sb.toString();
            }
            
            row[COLUMN_INDEX_DATA] = t.getMessage();
            
            data.add(row);
        }
        
        if (data.size() <= 0) {
            return;
        }
        
        model.setData(DataHelper.to2DArray(data));
        model.fireTableDataChanged();
    }
}
