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
package org.cmuchimps.gort.modules.dataobject;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.openide.filesystems.FileAlreadyLockedException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author shahriyar
 */
public class Progress {
    // use google gson to get the status from a file or file object
    
    private boolean staticAnalysis;
    private boolean dynamicAnalysis;
    private boolean crowdAnalysis;

    public Progress() {
        staticAnalysis = false;
        dynamicAnalysis = false;
        crowdAnalysis = false;
    }
    
    public Progress(boolean staticAnalysis, boolean dynamicAnalysis, boolean crowdAnalysis) {
        this();
        this.staticAnalysis = staticAnalysis;
        this.dynamicAnalysis = dynamicAnalysis;
        this.crowdAnalysis = crowdAnalysis;
    }
    
    public boolean isStaticAnalysis() {
        return staticAnalysis;
    }

    public void setStaticAnalysis(boolean staticAnalysis) {
        this.staticAnalysis = staticAnalysis;
    }

    public boolean isDynamicAnalysis() {
        return dynamicAnalysis;
    }

    public void setDynamicAnalysis(boolean dynamicAnalysis) {
        this.dynamicAnalysis = dynamicAnalysis;
    }

    public boolean isCrowdAnalysis() {
        return crowdAnalysis;
    }

    public void setCrowdAnalysis(boolean crowdAnalysis) {
        this.crowdAnalysis = crowdAnalysis;
    }
    
    public static Progress fromJson(FileObject fo) {
        if (fo == null || !fo.canRead()) {
            return null;
        }
        
        return fromJson(FileUtil.toFile(fo));
    };
    
    public static Progress fromJson(File f) {
        if (f == null || !f.exists() || !f.canRead()) {
            return null;
        }
        
        FileReader fr = null;
        BufferedReader br = null;
        Progress retVal = null;
        
        try {
            fr = new FileReader(f);
            br = new BufferedReader(fr);
            Gson gson = new Gson();
            retVal = gson.fromJson(br, Progress.class);
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
            
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
        
        return retVal;
    };
    
    public static Progress fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Progress.class);
    };
    
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
    
    public void toJson(FileObject fo) {
        if (fo == null) {
            return;
        }
        
        String json = this.toJson();
        
        OutputStream out = null;
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;
        
        try {
            out = fo.getOutputStream();
            osw = new OutputStreamWriter(out);
            bw = new BufferedWriter(osw);
            bw.write(json);
            bw.flush();
        } catch (FileAlreadyLockedException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
    }
    
    public void toJson(File f) {
        if (f == null) {
            return;
        }
        
        String json = this.toJson();
        
        FileWriter fw = null;
        
        try {
            fw = new FileWriter(f);
            fw.write(json);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }
}
