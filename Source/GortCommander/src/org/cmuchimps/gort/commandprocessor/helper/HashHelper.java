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

package org.cmuchimps.gort.commandprocessor.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashHelper {
	
	public static String md5(byte[] b) {
		if (b == null || b.length <= 0) {
			return null;
		}
		
		try {
	        // Create MD5 Hash
	        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
	        digest.update(b);
	        byte messageDigest[] = digest.digest();
	        
	        // Create Hex String
	        StringBuffer hexString = new StringBuffer();
	        for (int i=0; i< messageDigest.length; i++)
	            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
	        return hexString.toString();
	        
	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    }
	    
	    return null;
	}
	
	public static String md5(String path) {
		if (path == null || path.length() <= 0) {
			return null;
		}
		
		File f = new File(path);
		
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
			
			byte messageDigest[] = digest.digest();
			
			// Create Hex String
	        StringBuffer hexString = new StringBuffer();
	        for (int i=0; i< messageDigest.length; i++)
	            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
	        return hexString.toString();
			
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
