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
package org.cmuchimps.gort.modules.helper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author shahriyar
 */
public class CompressionHelper {
    
    public static void gunzip(FileObject fo) {
        gunzip(FileUtil.toFile(fo));
    }
    
    // currently under the assumption that the file has a '.gz' extension
    public static void gunzip(File f) {
        if (f == null || !f.exists() || !f.canRead()) {
            return;
        }
        
        File parent = f.getParentFile();
        
        if (parent == null || !parent.exists() || !parent.canWrite()) {
            return;
        }
        
        String outputFilename = f.getName().replace(".gz", "");
        
        File output = new File(parent, outputFilename);
        
        FileInputStream fin = null;
        BufferedInputStream in = null;
        FileOutputStream out = null;
        GzipCompressorInputStream gzIn = null;
        
        try {
            fin = new FileInputStream(f);
            in = new BufferedInputStream(fin);
            out = new FileOutputStream(output);
            gzIn = new GzipCompressorInputStream(in);
            final byte[] buffer = new byte[4096];
            int n = 0;
            while (-1 != (n = gzIn.read(buffer))) {
                out.write(buffer, 0, n);
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
            if (gzIn != null) {
                try {
                    gzIn.close();
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
    
    public static void untar(FileObject fo) {
        untar(FileUtil.toFile(fo));
    }
    
    public static void untar(File f) {
        if (f == null || !f.exists() || !f.canRead()) {
            return;
        } else {
            System.out.println("Untarring file: " + f.getAbsolutePath());
        }
        
        File parent = f.getParentFile();
        
        if (parent == null || !parent.exists() || !parent.canWrite()) {
            return;
        }
        
        FileInputStream fin = null;
        BufferedInputStream in = null;
        TarArchiveInputStream tarIn = null;
        TarArchiveEntry entry = null; 
        
        try {
            fin = new FileInputStream(f);
            in = new BufferedInputStream(fin);
            tarIn = new TarArchiveInputStream(in);
            
            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                final File outputFile = new File(parent, entry.getName());
                if (entry.isDirectory()) {
                    if (!outputFile.exists()) {
                        outputFile.mkdirs();
                    }
                } else {
                    final FileOutputStream outputFileStream = new FileOutputStream(outputFile); 
                    IOUtils.copy(tarIn, outputFileStream);
                    outputFileStream.close();
                }
                
                //System.out.println("Processed: " + outputFile.getAbsolutePath());
            }
            
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
            if (tarIn != null) {
                try {
                    tarIn.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
