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

import javax.swing.JTable;

/**
 *
 * @author shahriyar
 */
public class StringTable extends JTable {
    private StringTableModel model;

    public StringTable() {
        super();
        
        //attach a simple sort to the table
        setAutoCreateRowSorter(true);
    }

    public StringTable(StringTableModel model) {
        this();
        this.model = model;
    }

    public StringTableModel getModel() {
        return model;
    }

    public void setModel(StringTableModel model) {
        this.model = model;
    }
    
}
