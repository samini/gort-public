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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.cmuchimps.gort.commandprocessor.Constants;
import org.cmuchimps.gort.commandprocessor.service.CommandProcessorService;

import android.util.Log;


public class ServerThread extends Thread {

	private static final String TAG = ServerThread.class.getSimpleName();
	
	private static ServerThread instance = null;
	private boolean running = false;
	private ServerSocket server = null;
	private Socket socket = null;
	private ICommandProcessor commandProcessor = null;
	
	private ServerThread() {
		super();
	}
	
	public synchronized static ServerThread getInstance() {
		if (instance == null) {
			instance = new ServerThread();
		}
		
		return instance;
	}
	
	@Override
	public void run() {
		Log.d(TAG, "ServerThread run method called");
		acceptConnections();
	}
	
	public void halt() {
		Log.d(TAG, "Thread halt called.");
		
		running = false;
		
		Log.d(TAG, "Closing...");
		
		close();
		
		instance = null;
		
		CommandProcessorService.releaseWakeLock();
	}
	
	private void acceptConnections() {
		Log.d(TAG, "acceptConnections called.");
		
		if (running) {
			Log.d(TAG, "Already running.");
			return;
		}
		
		running = true;
		
		while (running) {
			try {
				Log.d(TAG, "Initializing socket on port " + Constants.SERVER_PORT + "...");
				
				server = new ServerSocket(Constants.SERVER_PORT);
				server.setSoTimeout(Constants.SERVER_TIMEOUT);
				
				Log.d(TAG, "Accepting connections...");
				
				socket = server.accept();
				
				if (commandProcessor != null) {
					// once the connection has been accepted acquire the wake lock
					CommandProcessorService.acquireWakeLock();
					
					commandProcessor.processCommands(socket.getInputStream(), socket.getOutputStream());
				}
			} catch (SocketException e) {
				Log.d(TAG, "SocketException raised!");
				e.printStackTrace();
			} catch (IOException e) {
				Log.d("TAG", "IOException raised!");
				e.printStackTrace();
			} finally {
				close();
			}
		}
	}
	
	private void closeSocket() {
		Log.d(TAG, "Closing socket...");
		
		if (socket != null) {
			try {
				socket.close();
				Log.d(TAG, "Socket closed.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		socket = null;
	}
	
	private void closeServer() {
		Log.d(TAG, "Closing server...");
		
		if (server != null) {
			try {
				server.close();
				Log.d(TAG, "Server closed.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		server = null;
	}
	
	private void close() {
		closeSocket();
		closeServer();
	}
	
	public void setCommandProcessor(ICommandProcessor commandProcessor) {
		this.commandProcessor = commandProcessor;
	}

}
