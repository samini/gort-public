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

package org.cmuchimps.gort.commandprocessor.server;

import java.io.File;
import java.util.List;

import org.cmuchimps.gort.commandprocessor.helper.HashHelper;
import org.cmuchimps.gort.commandprocessor.service.CommandProcessorService;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;


public class Commands {
	
	private static final String TAG = Commands.class.getSimpleName();
	
	public static List<ApplicationInfo> getInstalledApps() {
		Service s = CommandProcessorService.getInstance();
		
		if (s == null) {
			return null;
		}
		
		PackageManager pm = s.getPackageManager();
		
		return (pm != null) ? pm.getInstalledApplications(0) : null;
	}
	
	public static ApplicationInfo getApplicationInfoByName(String appName) {
		if (appName == null || appName.length() <= 0) {
			return null;
		}
		
		List<ApplicationInfo> installedApps = Commands.getInstalledApps();
		
		if (installedApps == null) {
			return null;
		}
		
		Service s = CommandProcessorService.getInstance();
		
		if (s == null) {
			return null;
		}
		
		PackageManager pm = s.getPackageManager();
		
		if (pm == null) {
			return null;
		}
		
		for (ApplicationInfo ai : installedApps) {
			String aiLabel = ai.loadLabel(pm).toString().toLowerCase();
			if (appName.toLowerCase().equals(aiLabel)) {
				return ai;
			}
		}
		
		return null;
	}
	
	public static ApplicationInfo getApplicationInfoByPackage(String packageName) {
		Service s = CommandProcessorService.getInstance();
		
		if (s == null) {
			return null;
		}
		
		PackageManager pm = s.getPackageManager();
		
		if (pm == null) {
			return null;
		}
		
		try {
			return pm.getApplicationInfo(packageName, 0);
		} catch (NameNotFoundException e) {
			return null;
		}
	}
	
	public static String getProcessName(String packageName) {
		ApplicationInfo ai = Commands.getApplicationInfoByPackage(packageName);
		
		if (ai == null) {
			return null;
		}
		
		return ai.processName;
	}
	
	public static String getPackageName(String appName) {
		ApplicationInfo ai = Commands.getApplicationInfoByName(appName);
		
		if (ai == null) {
			return null;
		}
		
		return ai.packageName;
	}
	
	public static String getLaunchActivity(String packageName) {
		Service s = CommandProcessorService.getInstance();
		
		if (s == null) {
			return null;
		}
		
		PackageManager pm = s.getPackageManager();
		
		if (pm == null) {
			return null;
		}
		
		Intent intent = pm.getLaunchIntentForPackage(packageName);
		
		if (intent == null) {
			return null;
		}
		
		ComponentName comp = intent.getComponent();
		return (comp != null) ? comp.flattenToShortString() : null;
	}
	
	public static String getSourceDir(String packageName) {
		ApplicationInfo ai = Commands.getApplicationInfoByPackage(packageName);
		
		if (ai == null) {
			return null;
		}
		
		return ai.sourceDir;
	}
	
	public static String getMD5(String packageName) {
		String sourceDir = Commands.getSourceDir(packageName);
		return HashHelper.md5(sourceDir);
	}
	
	public static long getFileSize(String packageName) {
		String sourceDir = Commands.getSourceDir(packageName);
		
		if (sourceDir == null || sourceDir.length() <= 0) {
			return -1;
		}
		
		File f = new File(sourceDir);
		
		if (!f.exists() || !f.canRead()) {
			Log.d(TAG, "Could not read file.");
			return -1;
		}
		
		return f.length();
	}
	
}
