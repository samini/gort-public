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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author shahriyar
 */
public class DateHelper {
    private static final String SIMPLE = "yyyyMMdd_HHmmss";
    
    public static String getDate(String format, long time, String timezoneId) {
        if (format == null || format.isEmpty()) {
            return null;
        }
        
        if (time < 0) {
            return null;
        }

        if (timezoneId == null || timezoneId.length() <= 0) {
            timezoneId = "GMT";
        }

        TimeZone tz = TimeZone.getTimeZone(timezoneId);

        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(tz);

        return sdf.format(new Date(time));
    }
    
    public static String getDate(long time, String timezoneId) {
        return getDate(SIMPLE, time, timezoneId);
    }
    
    private static final String TIMEZONE_ID_UTC = "UTC";
    private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone(TIMEZONE_ID_UTC);
    
    // This should be the same as calling new Date() as Date() only holds seconds
    // since epoch
    public static Date getUTC() {
        return Calendar.getInstance(TIMEZONE_UTC).getTime();
    }
}
