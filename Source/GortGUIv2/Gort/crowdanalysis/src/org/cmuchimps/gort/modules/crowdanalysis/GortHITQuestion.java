/*
 * Copyright 2007-2012 Amazon Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */ 


package org.cmuchimps.gort.modules.crowdanalysis;

import com.amazonaws.mturk.addon.HITQuestion;
import com.amazonaws.mturk.addon.HITQuestionHelper;
import java.io.File;
import java.io.StringWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.tools.VelocityFormatter;

/**
 * The HITQuestion class provides a structured way to read a HIT Question XML
 * from a file. 
 */
public class GortHITQuestion extends HITQuestion  {

  protected static Logger log = Logger.getLogger(GortHITQuestion.class);

  public final static String ENCODED_KEY_SUFFIX = "_urlencoded";
  public final static String RAW_KEY_SUFFIX = "_raw";	
  private Template velocityTemplate = null;
  private String question;

  public GortHITQuestion() {
    // Don't use a velocity template but instead 
    //   set questionXML explicitly using setQuestionXML()
  }

  public GortHITQuestion(String fileName) throws Exception {

    // Setup a velocity template to create templated questions
    //   using getQuestion(Map<String, String> input)

    VelocityEngine engine = new VelocityEngine();
    File f = new File(fileName);

    Properties p = new Properties();

    p.setProperty( "resource.loader", "file" );
    p.setProperty( "file.resource.loader.class", 
        "org.apache.velocity.runtime.resource.loader.FileResourceLoader" );
    p.setProperty( "file.resource.loader.path", f.getParent() == null ? "." : f.getParent());
    p.setProperty( "file.resource.loader.cache", "true");
    p.setProperty( "file.resource.loader.modificationCheckInterval", "2");
    p.setProperty( "input.encoding", "UTF-8");
    
    engine.setProperty( VelocityEngine.RUNTIME_LOG_LOGSYSTEM, log);    
    engine.init(p);

    velocityTemplate = engine.getTemplate( f.getName() );

    this.question = null;
  }

  @Override
  public void setQuestion(String questionXML) {
    this.question = questionXML; 
  }

  @Override
  public String getQuestion() {
    if (this.question != null)
      return this.question;

    return getQuestion(null);
  }

  @Override
  public String getQuestion(Map<String, String> input) {

    // If there is not velocity template associated with this question
    if (this.velocityTemplate == null) {
      // Return the explicitly set qeustion XML
      return this.question;
    }

    try {
      VelocityContext context = new VelocityContext();

      // Add some generic helpers just in case
      context.put("formatter", new VelocityFormatter(context));
      context.put("helper", new HITQuestionHelper());
      context.put("today", new Date());     

      if (input != null && input.values() != null) {        
        Iterator iter = input.keySet().iterator();
        while (iter.hasNext()) {
          String key = (String)iter.next();

          // Make a RAW version that's untouched
          context.put(key + RAW_KEY_SUFFIX, input.get(key));

          // Make the default version a QAP-cleaned version
          if (input.get(key) != null && input.get(key) instanceof String)
              // SMA: changed cleanString
            context.put(key, cleanString((String)input.get(key)));
          else
            context.put(key, input.get(key));
        }
      }

      StringWriter writer = new StringWriter();

      this.velocityTemplate.merge(context, writer);
      this.question = writer.toString();
      return question;
    }
    catch (Exception e) {
      log.error("Could not read Question", e);
      return null;
    }
  }

  // SMA: previous cleanString would remove html markup,
  // our version allows html markup, just makes sure that 
  // html reference characters with numerical references end 
  // with a semi-colon
  private String cleanString(String s) {
    if (s == null) {
        return null;
    }

    Pattern pattern = Pattern
        .compile("&(#x([0-9a-fA-F]+)|#([0-9]+))(?!;)");
    Matcher matcher = pattern.matcher(s);

    StringBuffer sb = new StringBuffer(s.length());

    while(matcher.find()) {

        String text = matcher.group();

        if (text != null && !text.isEmpty()) {
            text = text + ';';
        } else {
            text = "";
        }

        matcher.appendReplacement(sb, Matcher.quoteReplacement(text));
    }

    matcher.appendTail(sb);

    return sb.toString();
  }
}
