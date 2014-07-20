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
package org.cmuchimps.gort.api.gort.heuristic;

/**
 *
 * @author shahriyar
 */
public class IHeuristic {
    public static final int CONCERN_LEVEL_HIGH = 3;
    public static final int CONCERN_LEVEL_MIDIUM = 2;
    public static final int CONCERN_LEVEL_LOW = 1;
    public static final int CONCERN_LEVEL_UNKNOWN = 0;
    
    public static final String STATUS_SAFE = "Safe";
    public static final String STATUS_UNSAFE = "Unsafe";
    public static final String STATUS_UNKNOWN = "Unknown";
}
