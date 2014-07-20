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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tika.io.IOUtils;
import org.im4java.core.CommandException;
import org.im4java.core.CompareCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.process.OutputConsumer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.Places;

/**
 *
 * @author shahriyar
 */
public class ImageHelper {
    public static final int DEFAULT_HEIGHT = 512; // pixels
    public static final int DEFAULT_HEIGHT_THUMBNAIL = 64;
    
    //Use Normalized cross-correlation
    private static final String METRIC = "NCC";
    private static final double NCC_SIMILARITY_THRESHOLD = 0.8;
    
    private static final int BUFFER_SIZE = 512;
    
    private static final CompareCmd COMPARE_COMMAND;
    
    static {
        COMPARE_COMMAND = new CompareCmd();
        COMPARE_COMMAND.setAsyncMode(false);
    }
    
    public static FileObject scaleToDefaultDimension(FileObject fo) {
        return scaleToDimension(fo, DEFAULT_HEIGHT);
    }
    
    public static FileObject scaleToThumbnailDimension(FileObject fo) {
        return scaleToDimension(fo, DEFAULT_HEIGHT_THUMBNAIL);
    }
    
    public static FileObject scaleToDimension(FileObject fo, int height) {
        if (fo == null) {
            return null;
        }
        
        return FileUtil.toFileObject(scaleToDimension(FileUtil.toFile(fo), height));
    }
    
    public static File scaleToDefaultDimension(File input) {
        return scaleToDimension(input, DEFAULT_HEIGHT);
    }
    
    public static File scaleToThumbnailDimension(File input) {
        return scaleToDimension(input, DEFAULT_HEIGHT_THUMBNAIL);
    }
    
    public static File scaleToDimension(File input, int height) {
        return scaleToDimension(input, null, height);
    }
    
    public static FileObject scaleToDefaultDimension(FileObject input, FileObject output) {
        return scaleToDimension(input, output, DEFAULT_HEIGHT);
    }
    
    public static FileObject scaleToThumbnailDimension(FileObject input, FileObject output) {
        return scaleToDimension(input, output, DEFAULT_HEIGHT_THUMBNAIL);
    }
    
    public static FileObject scaleToDimension(FileObject input, FileObject output, int height) {
        if (input == null) {
            return null;
        }
        
        return FileUtil.toFileObject(scaleToDimension(FileUtil.toFile(input),
                (output != null) ? FileUtil.toFile(output) : null, height));
    }
    
    public static File scaleToDefaultDimension(File input, File output) {
        return scaleToDimension(input, output, DEFAULT_HEIGHT);
    }
    
    public static File scaleToThumbnailDimension(File input, File output) {
        return scaleToDimension(input, output, DEFAULT_HEIGHT_THUMBNAIL);
    }
    
    public static File scaleToDimension(File input, File output, int height) {
        if (height <= 0) {
            return null;
        }
        
        if (input == null || !input.exists() || !input.canRead()) {
            return null;
        }
        
        Dimension d = getDimension(input);
        
        if (d == null || d.getHeight() == 0 || d.getWidth() == 0) {
            return null;
        }
        
        Dimension scaleDimension = scaleToHeight(d, height);
        
        // create a new file for output if it is null
        if (output == null) {
            String parent = input.getParent();
            String filename = input.getName();
            String outputFilename;
            
            int dotIndex = filename.lastIndexOf('.');
            
            if (dotIndex > 0) {
                outputFilename = String.format("%s_h%d.%s",
                        filename.substring(0, dotIndex), height,
                        (dotIndex < filename.length() - 1) ? filename.substring(dotIndex + 1) : "");
            } else {
                outputFilename = String.format("%s_h%d", filename, height);
            }
            
            output = new File(parent, outputFilename);
            
        } else if (output.exists()) {
            output.delete();
        }
        
        try {
            Thumbnails.of(input).size(scaleDimension.width, scaleDimension.height).toFile(output);
        } catch (IOException ex) {
            System.out.println("Could not scale image");
            ex.printStackTrace();
        }
        
        return output;
    }
    
    /*
    public static Dimension scaleToDefaultHeight(Dimension d) {
        return scaleToHeight(d, DEFAULT_HEIGHT);
    }*/
    
    public static Dimension scaleToHeight(Dimension d, int height) {
        if (height <= 0) {
            return null;
        }
        
        if (d == null) {
            return null;
        } else if (d.getHeight() == 0) {
            return null;
        } else if (d.getHeight() == height) {
            return d;
        }
        
        double width = (double) height * d.getWidth() / d.getHeight();
        
        return new Dimension((int) width, height);
    }
    
    public static Dimension getDimension(File f) {
        if (f == null || !f.exists() || !f.canRead()) {
            return null;
        }
        
        BufferedImage bi;
        try {
            bi = ImageIO.read(f);
            return new Dimension(bi.getWidth(), bi.getHeight());
        } catch (IOException ex) {
            System.out.println("Could not read image");
            ex.printStackTrace();
        }
        
        return null;
    }
    
    public static Boolean similar(FileObject image0, FileObject image1) {
        String comparison = compare(image0, image1);
        
        if (comparison == null || comparison.isEmpty()) {
            return null;
        }
        
        try {
            double value = Double.parseDouble(comparison);
            System.out.println("Image comparison ncc value: " + value);
            System.out.println(image0.getPath());
            System.out.println(image1.getPath());
            if (value > NCC_SIMILARITY_THRESHOLD) {
                
                return true;
            } else {
                return false;
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        
    }
    
    // Gort only uses this to compare pngs from traversals
    // both images are taken using DDMS in same conditions
    private static String compare(FileObject image0, FileObject image1) {
        if (image0 == null || image1 == null) {
            System.out.println("Null images are invalid.");
            return null;
        }
        
        if (image0 == image1 || image0.getPath().equals(image1.getPath())) {
            System.out.println("The two images are the same");
            return "" + 1.0;
        }
        
        List<String> args = new LinkedList<String>();
        args.add("-metric");
        args.add(METRIC);
        
        IMOperation op = new IMOperation();
        op.addRawArgs(args);
        
        op.addImage();
        op.addImage();
        op.addImage();

        final CountDownLatch latch = new CountDownLatch(1);
        final CompareOutputConsumer coc = new CompareOutputConsumer(latch);
        
        COMPARE_COMMAND.setOutputConsumer(coc);
        
        try {
            COMPARE_COMMAND.run(op, image0.getPath(), image1.getPath(), getDummyFilePath());
            latch.await();
        } catch (CommandException ex) {
            // output of the image ncc is through std err
            String message = ex.getMessage();
            
            if (message == null || message.isEmpty()) {
                return null;
            }
            
            String[] messageSplit = message.split(" ");
            return messageSplit[messageSplit.length - 1];
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return null;
        } catch (IM4JavaException ex) {
            ex.printStackTrace();
            return null;
        }
        
        return coc.getOutput();
    }
    
    private static class CompareOutputConsumer implements OutputConsumer {

        private String output = null;
        
        private final CountDownLatch latch;

        public CompareOutputConsumer(CountDownLatch latch) {
            this.latch = latch;
        }
        
        @Override
        public void consumeOutput(InputStream in) throws IOException {
            if (in == null) {
                return;
            }
            
            output = IOUtils.toString(in);
            
            if (latch != null) {
                latch.countDown();
            }
        }

        public String getOutput() {
            return output;
        }
    }
    
    private static String getDummyFilePath() {
        File cache = Places.getCacheDirectory();
        
        String path = "dummy.png";
        
        if (cache != null && cache.exists()) {
            File f = new File(cache, path);
            path = f.getAbsolutePath();
        }
        
        return path;
    }
}
