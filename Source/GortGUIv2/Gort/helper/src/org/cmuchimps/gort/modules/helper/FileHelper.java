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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public final class FileHelper {
    private static final MimeTypes MIME_TYPES = MimeTypes.getDefaultMimeTypes();
    private static final Detector DETECTOR = new DefaultDetector(MIME_TYPES);
    
    public static final MediaType getMediaType(File f) {
        if (f == null || !f.exists() || !f.canRead()) {
            return null;
        }
        
        MediaType retVal = null;
        
        TikaInputStream is = null;
        
        try {
            is = TikaInputStream.get(f);
            
            Metadata md = new Metadata();
            md.set(Metadata.RESOURCE_NAME_KEY, f.getName());
            retVal = DETECTOR.detect(is, md);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        return retVal;
    }
    
    public static final String getExtension(File f) {
        return getExtension(getMediaType(f));
    }
    
    public static final String getExtension(MediaType mt) {
        if (mt == null) {
            return null;
        }
        
        String retVal = null;
        
        try {
            retVal = MIME_TYPES.forName(mt.toString()).getExtension();
        } catch (MimeTypeException ex) {
            ex.printStackTrace();
        }
        
        return retVal;
    }
    
    public static final FileObject[] getOrderedChildren(FileObject fo) {
        if (fo == null) {
            return null;
        }
        
        FileObject[] unorderedChildren = fo.getChildren();
        
        if (unorderedChildren == null || unorderedChildren.length <= 0) {
            return unorderedChildren;
        }
        
        // get the filenames
        List<String> filenames = new ArrayList<String>();
        
        for (FileObject item : unorderedChildren) {
            if (item == null || item.getNameExt().isEmpty()) {
                continue;
            }
            
            filenames.add(item.getNameExt());
        }
        
        Collections.sort(filenames);
        
        FileObject[] retVal = new FileObject[filenames.size()];
        
        int index = 0;
        
        for (String filename : filenames) {
            retVal[index++] = fo.getFileObject(filename);
        }
        
        return retVal;
    }
    
    public static String pathRelativeToDirectory(String directory, String path) {
        if (path == null) {
            return null;
        }
        
        if (directory == null || directory.isEmpty()) {
            return path;
        }
        
        if (path.length() > directory.length() && path.startsWith(directory)) {
            path = path.substring(directory.length());
        }

        // if starts with file seperator remove the file separator
        if (path.startsWith(File.separator)) {
            path = path.substring(1);
        }
        
        return path;
    }
}
