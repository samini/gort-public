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
package org.cmuchimps.gort.modules.tablewidgets;

import java.io.Serializable;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author shahriyar
 */
public final class TableScrollPane extends JScrollPane implements Serializable {
    
    private AbstractTableModel model;
    private JTable table;
    
    public TableScrollPane() {
    }
    
    public TableScrollPane(AbstractTableModel model) {
        this.model = model;
    }
    
    public TableScrollPane(AbstractTableModel model, JTable table) {
        this(model);
        setTable(table);
    }

    public AbstractTableModel getModel() {
        return model;
    }

    public void setModel(AbstractTableModel model) {
        this.model = model;
    }

    public JTable getTable() {
        return table;
    }

    public void setTable(JTable table) {
        this.table = table;
        
        if (this.table != null) {
            //System.out.println("Setting ScrollPane Viewport");
            table.setModel(model);
            
            //attach a simple sort to the table
            table.setAutoCreateRowSorter(true);
            
            this.setViewportView(this.table);
            this.setVisible(true);
        }
    }
    
}
