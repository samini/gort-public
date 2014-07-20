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

/**
 *
 * @author shahriyar
 */
public class ISO8601Helper {
    private static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    public static String toISO8601(long time, String timezoneId) {
        return DateHelper.getDate(ISO8601_FORMAT, time, timezoneId);
    }
    
    public static String toISO8601Date(long time, String timezoneId) {
        String retVal = toISO8601(time, timezoneId);
        return (retVal != null) ? retVal.split("T")[0] : null;
    }
}