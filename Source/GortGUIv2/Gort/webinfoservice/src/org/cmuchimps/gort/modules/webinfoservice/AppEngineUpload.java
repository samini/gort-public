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
package org.cmuchimps.gort.modules.webinfoservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cmuchimps.gort.modules.helper.HTTPRequestHelper;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public class AppEngineUpload {
    private static final String MTURKSERVER_MAIN_URL = "URL_TO_MTURK_CONTENT_SERVER";
    private static final String MTURKSERVER_BLOB_UPLOAD_URL = MTURKSERVER_MAIN_URL + "/upload";
    private static final String MTURKSERVER_BLOB_DOWNLOAD_URL = MTURKSERVER_MAIN_URL + "/download?blob-key=";
    private static final int NUM_TRIES = 3;
    
    public static String uploadFile(FileObject fo) {
        if (fo == null) {
            System.out.println("File is null.");
            return null;
        }
        
        if (!fo.canRead()) {
            System.out.println("Cannot read file.");
            return null;
        }
        
        String filename = fo.getNameExt();
        
        if (filename == null) {
            return null;
        }
        
        final byte[] data;
        
        try {
             data = fo.asBytes();
        } catch (IOException ex) {
            System.out.println("Could not read the file into byte array.");
            ex.printStackTrace();
            return null;
        }
        
        if (data.length <= 0) {
            System.out.println("File has no content.");
            return null;
        }
        
        System.out.println("Obtaining file upload url...");
        
        // Get the upload url for the file
        String url = blobUploadUrl();
        
        if (url == null || url.isEmpty()) {
            System.out.println("Could not get upload url from appengine");
            return null;
        }
        
        System.out.println(url);
        
        return uploadBlobstoreData(url, filename, fo.getMIMEType(), data);
    }
    
    private static String blobUploadUrl() {
        for (int i = 0; i < NUM_TRIES; i++) {
            String url = HTTPRequestHelper.HTTPRequest(MTURKSERVER_BLOB_UPLOAD_URL);
            if (url != null && !url.isEmpty()) {
                return url;
            }
        }
        
        return null;
    }
    
    // with 3 tries
    private static String uploadBlobstoreData(String url, String filename, String mime, byte[] data) {
        if (url == null || url.length() <= 0) {
            return null;
        }

        if (data == null || data.length <= 0) {
            return null;
        }
        
        for (int i = 0; i < NUM_TRIES; i++) {
            String result = uploadBlobstoreDataNoRetry(url, filename, mime, data);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        }
        
        return null;
    }
    
    private static String uploadBlobstoreDataNoRetry(String url, String filename, String mime, byte[] data) {
        if (url == null || url.length() <= 0) {
            return null;
        }

        if (data == null || data.length <= 0) {
            return null;
        }

        HttpClient httpClient = new DefaultHttpClient();

        HttpPost httpPost = new HttpPost(url);
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        entity.addPart("data", new ByteArrayBody(data, mime, filename));

        httpPost.setEntity(entity);

        try {
            HttpResponse response = httpClient.execute(httpPost);

            System.out.println("Blob upload status code: " + response.getStatusLine().getStatusCode());

            /*
            //http://grinder.sourceforge.net/g3/script-javadoc/HTTPClient/HTTPResponse.html
            // 2xx - success
            if (response.getStatusLine().getStatusCode() / 100 != 2) {
                    return null;
            }
            */

            InputStreamReader isr =
                            new InputStreamReader(response.getEntity().getContent());
            BufferedReader br = new BufferedReader(isr);

            String blobKey = br.readLine();
            
            blobKey = (blobKey != null) ? blobKey.trim() : null;

            br.close();
            isr.close();

            if (blobKey != null && blobKey.length() > 0) {
                return String.format("%s%s", MTURKSERVER_BLOB_DOWNLOAD_URL, blobKey);
            } else {
                return null;
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
