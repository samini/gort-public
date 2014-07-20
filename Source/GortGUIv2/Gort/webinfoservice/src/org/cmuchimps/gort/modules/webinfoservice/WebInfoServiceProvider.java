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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.tika.mime.MediaType;
import org.cmuchimps.gort.api.gort.WebInfoService;
import org.cmuchimps.gort.modules.dataobject.WhoisRecord;
import org.cmuchimps.gort.modules.helper.FileHelper;
import org.openide.filesystems.FileAlreadyLockedException;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author shahriyar
 */
@ServiceProvider(service=WebInfoService.class)
public class WebInfoServiceProvider extends WebInfoService {
    
    private static final String MIME_TYPE_IMAGE = "image";
    private static final int DOWNLOAD_SCREENSHOT_CONNECTION_TIMEOUT = 1000;
    private static final int DOWNLOAD_SCREENSHOT_READ_TIMEOUT = 1000;
    
    @Override
    public String appDescription(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        
        GooglePlayInfo gpi = GooglePlayInfo.GooglePlayInfoByPackage(packageName);
        
        if (gpi == null) {
            return null;
        }
        
        String desc = gpi.getAppDesciption();
        
        if (desc == null || desc.isEmpty()) {
            return null;
        }
        
        return desc;
    }
    
    @Override
    public String appName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        
        GooglePlayInfo gpi = GooglePlayInfo.GooglePlayInfoByPackage(packageName);
        
        if (gpi == null) {
            return null;
        }
        
        String name = gpi.getAppName();
        
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        return name;
    }
    
    @Override
    public String appCategory(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        
        GooglePlayInfo gpi = GooglePlayInfo.GooglePlayInfoByPackage(packageName);
        
        if (gpi == null) {
            return null;
        }
        
        String category = gpi.getAppCategory();
        
        if (category == null || category.isEmpty()) {
            return null;
        }
        
        return category;
    }
    
    @Override
    public String appDeveloper(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        
        GooglePlayInfo gpi = GooglePlayInfo.GooglePlayInfoByPackage(packageName);
        
        if (gpi == null) {
            return null;
        }
        
        String developer = gpi.getAppDeveloper();
        
        if (developer == null || developer.isEmpty()) {
            return null;
        }
        
        return developer;
    }
    
    @Override
    public List<FileObject> downloadAppScreenshots(FileObject targetDir, String packageName) {
        if (targetDir == null || !targetDir.isFolder() || !targetDir.canRead()) {
            return null;
        }
        
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        
        GooglePlayInfo gpi = GooglePlayInfo.GooglePlayInfoByPackage(packageName);
        
        if (gpi == null) {
            return null;
        }
        
        List<String> urls = gpi.getScreenshotURLs();
        
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        
        List<FileObject> retVal = new LinkedList<FileObject>();
        
        for (String url : urls) {
            FileObject fo = downloadAppScreenshot(targetDir, url);
            if (fo != null) {
                retVal.add(fo);
            }
        }
        
        return (retVal.size() > 0) ? retVal : null;
    }
    
    private FileObject downloadAppScreenshot(FileObject targetDir, String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // create a new dummy file to store content
        String filename = UUID.randomUUID().toString();
        // Note: FileUtil is part of the netbeans File System API
        File file = new File(FileUtil.toFile(targetDir), filename);
        
        try {
            // Download the file using Commons IO
            // Note: FileUtils is part of Apache Commons IO
            FileUtils.copyURLToFile(new URL(url), file,
                    DOWNLOAD_SCREENSHOT_CONNECTION_TIMEOUT,
                    DOWNLOAD_SCREENSHOT_READ_TIMEOUT);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        
        // check if the file is downloaded
        if (!file.exists() || !file.canRead() || file.length() == 0) {
            System.out.println("Downloaded an invalid screenshot file.");
            return null;
        }
        
        // check the files mime type
        MediaType mt = FileHelper.getMediaType(file);
        
        // has to be of image type
        if (mt == null || !mt.getType().equals(MIME_TYPE_IMAGE)) {
            System.out.println("File downloaded is not of image type.");
            return null;
        }
        
        String extension = FileHelper.getExtension(mt);
        
        if (extension == null || extension.isEmpty()) {
            return null;
        }
        
        // change the filename extension to appropriate extension
        FileObject fo = FileUtil.toFileObject(file);
        
        FileLock lock = null;
        
        try {
            lock = fo.lock();
            fo.rename(lock, fo.getName(), extension.replaceFirst(".", ""));
        } catch (FileAlreadyLockedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
        
        return fo;
    }

    @Override
    public String uploadFile(FileObject fo) {
        return AppEngineUpload.uploadFile(fo);
    }

    @Override
    public WhoisRecord serverInfo(String ip) {
        return WhoisService.whois(ip);
    }
    
}
