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

package org.cmuchimps.gort.commandprocessor.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.cmuchimps.gort.commandprocessor.MainActivity;
import org.cmuchimps.gort.commandprocessor.server.GortCommandProcessor;
import org.cmuchimps.gort.commandprocessor.server.ServerThread;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import org.cmuchimps.gort.commandprocessor.R;
import org.cmuchimps.gort.commandprocessor.service.IService;

public class CommandProcessorService extends Service {

	private static final String TAG = CommandProcessorService.class.getSimpleName();
	
	private static CommandProcessorService instance = null;
	
	private static PowerManager sPowerManager;
	private static PowerManager.WakeLock sWakeLock;
	
	// make this a foreground service such that it is not killed
	@SuppressWarnings("rawtypes")
	private static final Class[] mStartForegroundSignature = new Class[] {
	    int.class, Notification.class};
	@SuppressWarnings("rawtypes")
	private static final Class[] mStopForegroundSignature = new Class[] {
	    boolean.class};
	
	private static NotificationManager sNM;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	
	private Notification startForegroundNotification;
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	private final IService.Stub mBinder = new IService.Stub(){

		public int getPId() throws RemoteException {
			return android.os.Process.myPid();
		}

		public void stopService() throws RemoteException {
			stopSelf();
		}
		
	};
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.d(TAG, "Service onCreate called.");
		
		instance = this;
		
		sPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
		
		// put in foreground so service is not killed
		sNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	    try {
	        mStartForeground = getClass().getMethod("startForeground",
	                mStartForegroundSignature);
	        mStopForeground = getClass().getMethod("stopForeground",
	                mStopForegroundSignature);
	    } catch (NoSuchMethodException e) {
	        // Running on an older platform.
	        mStartForeground = mStopForeground = null;
	    }
	    
	    //int icon = R.drawable.gnome_system_monitor;
	    int icon = R.drawable.ic_launcher;
	    CharSequence tickerText = "Gort Commander Processor is running.";
	    long when = System.currentTimeMillis();
	    
	    startForegroundNotification = new Notification(icon, tickerText, when);
	    startForegroundNotification.defaults |= Notification.FLAG_AUTO_CANCEL;
	    ComponentName comp = new ComponentName(getApplicationContext().getPackageName(), 
	    		MainActivity.class.getName());
	    Intent intent = new Intent().setComponent(comp);
	    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 
	    		PendingIntent.FLAG_UPDATE_CURRENT);
	    startForegroundNotification.setLatestEventInfo(getApplicationContext(), "Gort Command Processor", "Running...", pendingIntent);
	    
	    startForegroundCompat(R.string.foreground_service_started, startForegroundNotification);
		
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy called.");
		
		Log.d(TAG, "Halting server...");
		
		// stop the command processing thread from running
		ServerThread.getInstance().halt();
		
		try {
			Log.d(TAG, "Joining server thread...");
			ServerThread.getInstance().join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Log.d(TAG, "Server thread is done excuting.");
		
		// Make sure our notification is gone.
	    stopForegroundCompat(R.string.foreground_service_started);
		
		releaseWakeLock();
		
		super.onDestroy();
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		// keep this service on so that we can process commands as they come
		// do not keep a wake lock here. only keep a wake lock if there is a connection to our service
		// acquireWakeLock();
		ServerThread serverThread = ServerThread.getInstance();
		//serverThread.setCommandProcessor(new SimpleCommandProcessor());
		if (!serverThread.isAlive()) {
			serverThread.setCommandProcessor(new GortCommandProcessor());
			serverThread.start();
		}
		
		return Service.START_STICKY;
	}
	
	public synchronized static void acquireWakeLock() {
		
		if (sWakeLock == null || !sWakeLock.isHeld()) {
			// used to prevent cpu from sleeping when uploading values
			sWakeLock = sPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
			sWakeLock.acquire();
			
			Log.d(TAG, "Wake lock acquired.");
		} else {
			Log.d(TAG, "Wake lock already held.");
		}
	}
	
	public synchronized static void releaseWakeLock() {
		
		Log.d(TAG, "Releasing wake lock.");
		
		if (sWakeLock != null && sWakeLock.isHeld()) {
			sWakeLock.release();
		}
		
		sWakeLock = null;
	}
	
	// from http://developer.android.com/reference/android/app/Service.html
	void invokeMethod(Method method, Object[] args) {
	    try {
	        method.invoke(this, args);
	    } catch (InvocationTargetException e) {
	        // Should not happen.
	        Log.w(TAG, "Unable to invoke method", e);
	    } catch (IllegalAccessException e) {
	        // Should not happen.
	        Log.w(TAG, "Unable to invoke method", e);
	    }
	}
	
	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	void startForegroundCompat(int id, Notification notification) {
	    // If we have the new startForeground API, then use it.
	    if (mStartForeground != null) {
	        mStartForegroundArgs[0] = Integer.valueOf(id);
	        mStartForegroundArgs[1] = notification;
	        try {
	            mStartForeground.invoke(this, mStartForegroundArgs);
	        } catch (InvocationTargetException e) {
	            // Should not happen.
	            Log.w("ApiDemos", "Unable to invoke startForeground", e);
	        } catch (IllegalAccessException e) {
	            // Should not happen.
	            Log.w("ApiDemos", "Unable to invoke startForeground", e);
	        }
	        return;
	    }

	    // Fall back on the old API.
	    setForeground(true);
	    sNM.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	void stopForegroundCompat(int id) {
	    // If we have the new stopForeground API, then use it.
	    if (mStopForeground != null) {
	        mStopForegroundArgs[0] = Boolean.TRUE;
	        try {
	            mStopForeground.invoke(this, mStopForegroundArgs);
	        } catch (InvocationTargetException e) {
	            // Should not happen.
	            Log.w("ApiDemos", "Unable to invoke stopForeground", e);
	        } catch (IllegalAccessException e) {
	            // Should not happen.
	            Log.w("ApiDemos", "Unable to invoke stopForeground", e);
	        }
	        return;
	    }

	    // Fall back on the old API.  Note to cancel BEFORE changing the
	    // foreground state, since we could be killed at that point.
	    sNM.cancel(id);
	    setForeground(false);
	}

	public static Service getInstance() {
		return instance;
	}

}
