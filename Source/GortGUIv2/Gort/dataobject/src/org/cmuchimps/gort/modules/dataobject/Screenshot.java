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
package org.cmuchimps.gort.modules.dataobject;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 *
 * @author shahriyar
 */
@Entity
public class Screenshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String path;
    private String defaultHeightPath;
    private String thumbnailPath;
    private String originalURL;
    private String gortURL;
    
    @OneToOne
    private Annotation annotation;
    
    //TODO(samini): should the screenshot also be linked to interactions?
    
    public Screenshot() {
        
    }

    public Screenshot(String path) {
        this.path = path;
    }

    public Screenshot(String path, String defaultHeightPath) {
        this(path);
        this.defaultHeightPath = defaultHeightPath;
    }

    public Screenshot(String path, String defaultHeightPath, String thumbnailPath) {
        this(path, defaultHeightPath);
        this.thumbnailPath = thumbnailPath;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDefaultHeightPath() {
        return defaultHeightPath;
    }

    public void setDefaultHeightPath(String defaultHeightPath) {
        this.defaultHeightPath = defaultHeightPath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getGortURL() {
        return gortURL;
    }

    public void setGortURL(String gortURL) {
        this.gortURL = gortURL;
    }
    
    public String getOriginalURL() {
        return originalURL;
    }

    public void setOriginalURL(String originalURL) {
        this.originalURL = originalURL;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }
    
}
