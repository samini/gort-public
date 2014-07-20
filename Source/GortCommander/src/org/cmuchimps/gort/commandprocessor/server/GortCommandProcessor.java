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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.cmuchimps.gort.commandprocessor.Constants;

import android.util.Log;


public class GortCommandProcessor implements ICommandProcessor {

	private static final String TAG = GortCommandProcessor.class.getSimpleName();
	
	public void processCommands(InputStream in, OutputStream out) {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		PrintWriter pw = new PrintWriter(out);
		
		String input = null;
		
		try {
			while ((input = br.readLine()) != null) {
				input = input.toLowerCase().replace("\r\n", "");
				
				Log.d(TAG, "rcvd: " + input);
				
				String output = null;
				
				try {
					
					int firstSpaceIndex = input.indexOf(' ');
					String args = input.substring(firstSpaceIndex + 1);
					String[] argsSplit = args.split(" ");
					
					if (input.startsWith(Constants.COMMAND_APP_PACKAGE)) {
						output = Commands.getPackageName(args);
					} else if (input.startsWith(Constants.COMMAND_APP_PROCESS)) {
						output = Commands.getProcessName(args);
					} else if (input.startsWith(Constants.COMMAND_LAUNCH_ACTIVITY)) {
						output = Commands.getLaunchActivity(args);
					} else if (input.startsWith(Constants.COMMAND_APP_SOURCE_DIR)) {
						output = Commands.getSourceDir(args);
					} else if (input.startsWith(Constants.COMMAND_APP_MD5)) {
						output = Commands.getMD5(args);
					} else if (input.startsWith(Constants.COMMAND_APP_File_SIZE)) {
						output = "" + Commands.getFileSize(args);
					} else {
						output = "unrecognized command";
					}
					
					if (output == null) {
						Log.d(TAG, "Command output is null.");
						output = "invalid";
					}
					
					pw.println(output);
					pw.flush();
					
				} catch (IndexOutOfBoundsException e) {
					continue;
				}
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pw.close();
		}
		
	}

}
