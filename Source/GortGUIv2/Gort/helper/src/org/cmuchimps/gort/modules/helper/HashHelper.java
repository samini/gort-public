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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author shahriyar
 */
public class HashHelper {
    public static String md5(byte[] b) {
        if (b == null || b.length <= 0) {
                return null;
        }

        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(b);

            // Create Hex String
            BigInteger bi = new BigInteger(1, digest.digest());
            return String.format("%1$032X", bi);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String md5(String path) {
        if (path == null || path.length() <= 0) {
                return null;
        }
        
        return md5(new File(path));
    }
    
    public static String md5(FileObject fo) {
        return md5(FileUtil.toFile(fo));
    }
    
    public static String md5(File f) {

        if (!f.exists() || !f.canRead()) {
                return null;
        }

        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(f);

            byte[] buffer = new byte[1024];

            int len = 0;

            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");

            while ((len = fileInputStream.read(buffer)) > 0) {
                    digest.update(buffer, 0, len);
            }

            // Create Hex String
            BigInteger bi = new BigInteger(1, digest.digest());
            return String.format("%1$032X", bi);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                    fileInputStream.close();
            } catch (IOException e) {
                    // ignore
            }
        }

        return null;
    }
}
