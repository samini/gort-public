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
package org.cmuchimps.gort.api.gort;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author shahriyar
 */
public final class GestureCollection {
    public static final String UI_LOGGER_NAME = "org.netbeans.ui.gort";
    public static final String UI_FILE_LOGGER_NAME = "org.netbeans.ui.gort.filelogger";
    public static final String EVENT_STATE_CLICK = "STATE_CLICK";
    public static final String EVENT_TOPCOMPONENT_OPENED = "TOPCOMPONENT_OPENED";
    public static final String EVENT_TOPCOMPONENT_CLOSED = "TOPCOMPONENT_CLOSED";
    public static final String EVENT_TOPCOMPONENT_ACTIVATED = "TOPCOMPONENT_ACTIVIATED";
    public static final String EVENT_TOPCOMPONENT_DEACTIVATED = "TOPCOMPONENT_DEACTIVATED";
    public static final String EVENT_TOPCOMPONENT_SHOWING = "TOPCOMPONENT_SHOWING";
    public static final String EVENT_TOPCOMPONENT_HIDDEN = "TOPCOMPONENT_HIDDEN";
    public static final String EVENT_STATE_DETAIL_TAB_CLICK = "STATE_DETAIL_TAB_CLICK";
    public static final String EVENT_CROWD_TASK_DOUBLE_CLICK = "CROWD_TASK_DOUBLE_CLICK";
    public static final String EVENT_CROWD_TASK_SELECTION_CHANGE = "CROWD_TASK_SELECTION_CHANGED";
    public static final String EVENT_HEURISTIC_SELECTION_CHANGE = "HEURISTIC_SELECTION_CHANGE";
    public static final String EVENT_HEURISTIC_TOOLTIP_VIEW = "HEURISTIC_TOOLTIP_VIEW";
    public static final String EVENT_HEURISTIC_DOUBLE_CLICK = "HEURISTIC_DOUBLE_CLICK";
    public static final String EVENT_TAINTLOG_TOOLTIP_VIEW = "TAINTLOG_TOOLTIP_VIEW";
    public static final String EVENT_TAINTLOG_SELECTION_CHANGE = "TAINTLOG_SELECTION_CHANGE";
    public static final String EVENT_APPVIEW_SHOWING = "APPVIEW_OPENED";
    
    public static final Logger LOGGER = Logger.getLogger(UI_LOGGER_NAME);
    public static final Logger FILE_LOGGER = Logger.getLogger(UI_FILE_LOGGER_NAME);
    
    public static GestureCollection instance = null;
    
    private static final int LOG_SIZE = 1024 * 1024 * 32; // 32 MB
    private static final int LOG_COUNT = 10;
    
    private GestureCollection() {
        FileObject gortCacheFO = GortCacheDirectoryService.getDefault().gortCacheDirectory();
        File gortCacheF = FileUtil.toFile(gortCacheFO);
        File log = new File(gortCacheF, new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) + ".log");
        
        try {
            FileHandler fh = new FileHandler(log.getPath(), LOG_SIZE, LOG_COUNT);
            FILE_LOGGER.addHandler(fh);
        } catch (IOException ex) {
            //Logger.getLogger(GestureCollection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            //Logger.getLogger(GestureCollection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static synchronized GestureCollection getInstance() {
        if (instance == null) {
            instance = new GestureCollection();
        }
        
        return instance;
    }
    
    public synchronized void log(String event, String parameter) {
        LogRecord record = new LogRecord(Level.INFO, event);
        record.setParameters(new Object[]{parameter});
        record.setLoggerName(UI_LOGGER_NAME);
        LOGGER.log(record);
        FILE_LOGGER.log(record);
    }
    
    public void topComponentOpened(Class c) {
        log(EVENT_TOPCOMPONENT_OPENED, c.getSimpleName());
    }
    
    public void topComponentClosed(Class c) {
        log(EVENT_TOPCOMPONENT_CLOSED, c.getSimpleName());
    }
    
    public void topComponentActivated(Class c) {
        log(EVENT_TOPCOMPONENT_ACTIVATED, c.getSimpleName());
    }
    
    public void topComponentDeactivated(Class c) {
        log(EVENT_TOPCOMPONENT_DEACTIVATED, c.getSimpleName());
    }
    
    public void topComponentShowing(Class c) {
        log(EVENT_TOPCOMPONENT_SHOWING, c.getSimpleName());
    }
    
    public void topComponentHidden(Class c) {
        log(EVENT_TOPCOMPONENT_HIDDEN, c.getSimpleName());
    }
    
    public void stateClick(String s) {
        log(EVENT_STATE_CLICK, s);
    }
    
    public void stateDetailTabClick(String s) {
        log(EVENT_STATE_DETAIL_TAB_CLICK, s);
    }
    
    public void crowdTaskDoubleClick(String s) {
        log(EVENT_CROWD_TASK_DOUBLE_CLICK, s);
    }
    
    public void crowdTaskSelectionChanged(String s) {
        log(EVENT_CROWD_TASK_SELECTION_CHANGE, s);
    }
    
    public void heuristicSelectionChange(String s) {
        log(EVENT_HEURISTIC_SELECTION_CHANGE, s);
    }
    
    public void heuristicTooltipView(String s) {
        log(EVENT_HEURISTIC_TOOLTIP_VIEW, s);
    }
    
    public void heuristicDoubleClick(String s) {
        log(EVENT_HEURISTIC_DOUBLE_CLICK, s);
    }
    
    public void taintlogTooltipView(String s) {
        log(EVENT_TAINTLOG_TOOLTIP_VIEW, s);
    }
    
    public void taintlogSelectionChange(String s) {
        log(EVENT_TAINTLOG_SELECTION_CHANGE, s);
    }
    
    public void appViewShowing(String s) {
        log(EVENT_APPVIEW_SHOWING, s);
    }
    
}
