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

public final class Constants {
	public static final int SERVER_PORT = 38300;
	public static final int SECOND_TO_MILLISECOND = 1000;
	public static final int SERVER_TIMEOUT = 60 * SECOND_TO_MILLISECOND;
	
	public static final String COMMAND_LAUNCH_ACTIVITY = "launchactivity";
	public static final String COMMAND_APP_PACKAGE = "package";
	public static final String COMMAND_APP_PROCESS = "process";
	public static final String COMMAND_APP_SOURCE_DIR = "sourcedir";
	public static final String COMMAND_APP_MD5 = "md5";
	public static final String COMMAND_APP_File_SIZE = "filesize";
	
	public static final String INTENT_COMPONENT_EXTRA = "cmp";
}
