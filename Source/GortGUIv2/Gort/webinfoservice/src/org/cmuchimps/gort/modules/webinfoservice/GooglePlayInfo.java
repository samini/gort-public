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
package org.cmuchimps.gort.modules.webinfoservice;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.cmuchimps.gort.api.gort.WebInfoService;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author shahriyar
 * Note: these functions in this class break if Google Play decides 
 * to change its html code, which has already happened.
 */
public class GooglePlayInfo {
    
    // cache some responses, so multiple calls are not made for webpages
    private static HashMap<String, Document> cache = new HashMap<String, Document>();
    private static final int TRIES = 3;
    
    private Document doc;
    private String url;
    private String appCategory;
    private String appDescription;
    private String appName;
    private String appPermissions;
    private String appDeveloper;
    private List<String> screenshotURLs;
    
    private static final String[] DESCRIPTION_CLASSES = {
        "div.id-app-orig-desc",
        "div.app-orig-desc",
    };
    
    private static final String CLASS_NAME = "div.document-title";
    private static final String CLASS_DEVELOPER = "a.document-subtitle.primary";
    private static final String CLASS_CATEGORY = "a.document-subtitle.category";
    
    public GooglePlayInfo(Document doc) {
        this.doc = doc;
    }
    
    public Document getDoc() {
        return doc;
    }
    
    public String getAppName() {
        if (appName == null) {
            if (doc != null) {
                Elements nameElements = doc.select(CLASS_NAME);
                
                if (nameElements != null) {
                    Element nameElement = nameElements.first();
                    
                    if (nameElement != null) {
                        appName = nameElement.text();
                    }
                }
            }
        }
        
        return appName;
    }
    
    public String getAppDesciption() {
        if (appDescription == null) {
            if (doc != null) {
                //return doc.select("doc-original-text").val();
                for (String descClass : DESCRIPTION_CLASSES) {
                    Elements descElements = doc.select(descClass);
                    
                    if (descElements == null) {
                        continue;
                    }
                    
                    Element descElement = descElements.first();
                    
                    if (descElement == null) {
                        continue;
                    }
                    
                    appDescription = descElement.html();
                    break;
                }
            }
        }
        
        return appDescription;
    }
    
    public String getAppDeveloper() {
        if (appDeveloper == null) {
            if (doc != null) {
                Elements nameElements = doc.select(CLASS_DEVELOPER);
                
                if (nameElements != null) {
                    Element nameElement = nameElements.first();
                    
                    if (nameElement != null) {
                        appDeveloper = nameElement.text();
                    }
                }
            }
        }
        
        return appDeveloper;
    }
    
    public String getAppCategory() {
        if (appCategory == null) {
            if (doc != null) {
                Elements nameElements = doc.select(CLASS_CATEGORY);
                
                if (nameElements != null) {
                    Element nameElement = nameElements.first();
                    
                    if (nameElement != null) {
                        appCategory = nameElement.text();
                    }
                }
            }
        }
        
        return appCategory;
    }
    
    private final String ATTRIBUTE_KEY_SRC = "src";
    
    public List<String> getScreenshotURLs() {
        if (screenshotURLs == null) {
            if (doc != null) {
                Elements elements = doc.getElementsByClass("full-screenshot");
                
                if (elements != null && elements.size() > 0) {
                    screenshotURLs = new LinkedList<String>();
                    
                    for (int i = 0; i < elements.size(); i++) {
                        Element e = elements.get(i);
                        
                        if (e == null || !e.hasAttr(ATTRIBUTE_KEY_SRC)) {
                            continue;
                        }
                        
                        String url = e.attr(ATTRIBUTE_KEY_SRC);
                        
                        if (url == null || url.isEmpty()) {
                            continue;
                        }
                        
                        screenshotURLs.add(url);
                    }
                    
                    if (screenshotURLs.size() <= 0) {
                        screenshotURLs = null;
                    }
                }
            }
        }
        
        return screenshotURLs;
    }
    
    @Override
    public String toString() {
        if (doc != null) {
            return doc.html();
        }
        
        return super.toString();
    }
    
    public static GooglePlayInfo GooglePlayInfoByURL(String url) {
        Document tmp = requestGooglePlayJSON(url);
        
        if (tmp == null) {
            return null;
        }
        
        return new GooglePlayInfo(tmp);
    }
    
    public static GooglePlayInfo GooglePlayInfoByPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        
        String url = String.format("%s%s", WebInfoService.ANDROID_MARKET_URL, packageName);
        
        return GooglePlayInfoByURL(url);
    }
    
    private static Document requestGooglePlayJSON(String url) {
        synchronized (cache) {
            if (cache.containsKey(url) && cache.get(url) != null) {
                return cache.get(url);
            }
            
            int tries = 0;
            
            Document tmp;
            
            while (tries < TRIES) {
                tries++;
                try {
                    tmp = Jsoup.connect(url).get();
                    
                    if (tmp != null) {
                        cache.put(url, tmp);
                    }
                    
                    return tmp;
                } catch (HttpStatusException hse) {
                    System.out.println("Received incorrect HTTP status. Try: " + tries);
                    hse.printStackTrace();
                } catch (IOException ex) {
                    System.out.println("Could not get Google Play information. Try: " + tries);
                    ex.printStackTrace();;
                }
            }
            
            return null;
        }
    }
}
