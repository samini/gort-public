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

package org.cmuchimps.gort.commandprocessor;

import java.util.Timer;
import java.util.TimerTask;

import org.cmuchimps.gort.commandprocessor.service.CommandProcessorService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import org.cmuchimps.gort.commandprocessor.R;
import org.cmuchimps.gort.commandprocessor.service.IService;

public class MainActivity extends Activity {

	private static final String TAG = MainActivity.class.getName();
	
	private ServiceConnection serviceConnection;
	private IService service;
	private boolean serviceBound = false;
	
	private TextView statusTextView = null;
	private RadioButton statusRadioButton = null;
	
	private Handler handler;
	
	// this timer is used for programs
	private static Timer timer; 
	private static TimerTask task;
	private static final long POLL_INTERVAL = 100; // every 30 seconds
	private static final long DELAY_EXECUTION = 100; // delay 100 ms before first executation
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        handler = new Handler();
        
        statusTextView = (TextView) findViewById(R.id.statusTextView);
        statusRadioButton = (RadioButton) findViewById(R.id.statusRadioButton);
        
        Button b;
        
        b = (Button) findViewById(R.id.restartServiceButton);
        b.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				restartService();
			}
        	
        });
        
        b = (Button) findViewById(R.id.stopServiceButton);
        b.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				stopService();
			}
        	
        });
        
        startService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	createTimer();
    	
    	timer.schedule(task, DELAY_EXECUTION, POLL_INTERVAL);
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	
    	cancelTimer();
    }
    
    @Override
    public void onDestroy() {
    	unbindService();
    	super.onDestroy();
    }
    
    public void restartService() {
    	stopService();
    	startService();
    }
    
    public void startService() {
    	Intent serviceStartIntent = new Intent(this, CommandProcessorService.class);
    	startService(serviceStartIntent);
    	bindService();
    }
    
    public void stopService() {
    	
    	if (serviceBound && serviceConnection != null && service != null) {
    		try {
    			service.stopService();
			} catch (RemoteException e) {
				Log.d(TAG, "Remote Exception caught. Trying a different stop service approach");
				stopServiceByIntent();
			}
    	} else {
    		stopServiceByIntent();
    	}
    	
    	unbindService();
    	
    	Log.d(TAG, "Stopped Services.");
    }
    
    public void stopServiceByIntent() {
    	Intent serviceStopIntent = new Intent(this, CommandProcessorService.class);
    	stopService(serviceStopIntent);
    }
    
    public void bindService() {
    	serviceConnection = new ServiceConnection() {

			public void onServiceConnected(ComponentName name, IBinder service) {
				MainActivity.this.service = IService.Stub.asInterface(service);
			}

			public void onServiceDisconnected(ComponentName name) {
				service = null;
				serviceBound = false;
			}
    		
    	};
    	
    	Intent serviceBindIntent = new Intent(this, CommandProcessorService.class);
    	serviceBound = bindService(serviceBindIntent, serviceConnection, Context.BIND_AUTO_CREATE);

    }
    
    public void unbindService() {
    	try {
    		unbindService(serviceConnection);
    	} catch (IllegalArgumentException e) {
    		Log.d(TAG, "Service already unbound?");
    	}
    	serviceBound = false;
    	serviceConnection = null;
    	service = null;
		Log.d(TAG, "Successfully unbound services");
    }
    
    private void createTimer() {
		timer = new Timer();
		task = new TimerTask() {
			@Override
			public void run() {
				int tempPId = -1;
				
				if (serviceBound && serviceConnection != null && service != null) {
					// check if the service is running
					try {
						tempPId = service.getPId();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				
				final int pId = tempPId;
				
				handler.post(new Runnable() {

					public void run() {
						statusRadioButton.setChecked(pId > 0);
						
						if (pId > 0) {
							statusTextView.setText(R.string.service_running);
						} else {
							statusTextView.setText(R.string.service_stopped);
						}
					}
					
				});
			}
		};
    }
    
	private void cancelTimer() {
    	
    	// cancel the task
    	if (task != null) {
    		task.cancel();
    	}
    	
    	// cancel the timer
    	if (timer != null) {
    		timer.purge();
    		timer.cancel();
    	}
    	
    	task = null;
    	timer = null;
    }
}
