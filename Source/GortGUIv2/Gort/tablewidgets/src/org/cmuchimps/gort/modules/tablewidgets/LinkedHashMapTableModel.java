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

import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author shahriyar
 */
public final class LinkedHashMapTableModel extends AbstractTableModel {
    
    private static final int DEFAULT_ROW_COUNT = 16;
    
    private String header;
    private LinkedHashMap<Object, Object> map;
    
    public LinkedHashMapTableModel() {
        map = new LinkedHashMap();
    }
    
    public LinkedHashMapTableModel(String header) {
        this();
        this.header = header;
    }
    
    @Override
    public int getRowCount() {
        /*
        return (map != null && 
                map.size() > DEFAULT_ROW_COUNT) ? map.size() : DEFAULT_ROW_COUNT;
                */
        return (map != null) ? map.size() : 0;
    }

    @Override
    public int getColumnCount() {
        return (map != null) ? 1 : 0;
    }
    
    @Override
    public String getColumnName(int column) {
        return (header != null) ? header : "";
    }

    public Map.Entry getEntryAt(int rowIndex, int columnIndex) {
        if (map == null || map.size() <= 0 || map.size() <= rowIndex) {
            return null;
        }
        
        int i = 0;
        
        for (Map.Entry<Object, Object> kv : map.entrySet()) {
            if (i == rowIndex) {
                return kv;
            }
            
            i++;
        }
        
        return null;
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        //System.out.println("Value requested for: " + rowIndex);
        Map.Entry kv = getEntryAt(rowIndex, columnIndex);
        return (kv != null) ? kv.getValue() : null;
    }
    
    public Object getKeyAt(int rowIndex, int columnIndex) {
        Map.Entry kv = getEntryAt(rowIndex, columnIndex);
        return (kv != null) ? kv.getKey() : null;
    }

    public LinkedHashMap getMap() {
        return map;
    }
    
    public void setMap(LinkedHashMap map) {
        this.map = map;
    }
    
    public void put(Object key, Object value) {
        if (map != null) {
            map.put(key, value);
        }
    }
    
    public void checkAndPut(Object key, Object value) {
        if (map != null && !map.containsKey(key)) {
            map.put(key, value);
        }
    }
    
    public void remove(Object key) {
        if (map != null && key != null) {
            map.remove(key);
        }
    }
    
    public void remove(int row) {
        Object key = getKeyAt(row, 1);
        
        remove(key);
    }
}
