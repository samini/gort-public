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

import java.util.Arrays;

/**
 *
 * @author shahriyar
 */
public class MathHelper {
    public static Double median(Integer[] values) {
        if (values == null || values.length <= 0) {
            return null;
        }
        
        Arrays.sort(values);
        
        int middle = values.length / 2;
        
        if (values.length % 2 == 0) {
            return ((double)(values[middle] + values[middle - 1])) / 2.0;
        } else {
            return (double) values[middle];
        }
    }
    
    public static Double mean(Integer[] values) {
        if (values == null || values.length <= 0) {
            return null;
        }
        
        double sum = 0.0;
        double numIntegers = 0;
        
        for (Integer i : values) {
            if (i == null) {
                continue;
            }
            
            sum += i.intValue();
            numIntegers++;
        }
        
        return (sum / numIntegers);
    }
    
    public static Double variance(Integer[] values) {
        Double mean = mean(values);
        
        if (mean == null) {
            return null;
        }
        
        double tmp = 0;
        double numIntegers = 0;
        
        for (Integer i : values) {
            if (i == null) {
                continue;
            }
            
            tmp += (i.intValue() - mean.doubleValue()) * (i.intValue() - mean.doubleValue());
            numIntegers++;
        }
        
        return tmp / numIntegers;
    }
    
    public static Double stdDev(Integer[] values) {
        Double variance = variance(values);
        
        if (variance == null) {
            return null;
        }
        
        return Math.sqrt(variance.doubleValue());
    }
}
