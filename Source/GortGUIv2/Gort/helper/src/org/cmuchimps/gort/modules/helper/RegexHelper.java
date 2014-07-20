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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author shahriyar
 */
public class RegexHelper {
    // note that the actual code needs double '\' for '\'
    // regex patterns
    private static final String PATTERN_STRING_IP = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
    private static final String PATTERN_STRING_TAG = "tag 0x\\d+";
    // GET followd by any number of non-spaces
    private static final String PATTERN_STRING_GET = "GET \\S*";
    
    public static Pattern PATTERN_IP = Pattern.compile(PATTERN_STRING_IP);
    public static Pattern PATTERN_TAG = Pattern.compile(PATTERN_STRING_TAG);
    public static Pattern PATTERN_GET_REQUEST = Pattern.compile(PATTERN_STRING_GET);
    
    public static Matcher ipMatcher(String s) {
        return PATTERN_IP.matcher(s);
    }
    
    public static Matcher tagMatcher(String s) {
        return PATTERN_TAG.matcher(s);
    }
    
    public static Matcher getRequestMatcher(String s) {
        return PATTERN_GET_REQUEST.matcher(s);
    }
}
