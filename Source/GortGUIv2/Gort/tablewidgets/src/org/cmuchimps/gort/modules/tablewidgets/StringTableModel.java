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

import javax.swing.table.AbstractTableModel;

/**
 *
 * @author shahriyar
 */
public final class StringTableModel extends AbstractTableModel {
    private String[] header;
    private String[][] data;
    
    public StringTableModel() {
        super();
    }
    
    public StringTableModel(String[] header) {
        this();
        this.header = header;
    }
    
    public StringTableModel(String[] header, String[] data) {
        this(header);
        setData(data);
    }
    
    public StringTableModel(String[] header, String[][] data) {
        this(header);
        this.data = data;
    }

    @Override
    public int getRowCount() {
        return (data != null) ? data.length : 0;
    }

    // each table has its own data so only 1 column
    @Override
    public int getColumnCount() {
        if (header != null) {
            return header.length;
        }
        
        return (data != null) ? data[0].length : 0;
    }

    @Override
    public Object getValueAt(int i, int i1) {
        try {
            return data[i][i1];
        }
        catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String getColumnName(int column) {
        return (header != null && header.length > column) ? header[column] : null;
    }

    public String[][] getData() {
        return data;
    }

    public void setData(String[][] data) {
        this.data = data;
    }
    
    public void setData(String[] data) {
        if (data == null) {
            this.data = null;
            return;
        }
        
        String[][] tmp = new String[data.length][1];
        
        for (int i = 0; i < data.length; i++) {
            tmp[i][0] = data[i];
        }
        
        setData(tmp);
    }
    
    public void setNoData() {
        int numCols = this.getColumnCount();
        
        if (numCols <= 0) {
            return;
        }
        
        String[][] data = new String[1][this.getColumnCount()];
        data[0][0] = "<No Data>";
        this.setData(data);
    }
}
