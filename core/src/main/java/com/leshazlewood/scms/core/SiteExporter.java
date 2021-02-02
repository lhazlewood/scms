/*
 * Copyright 2013 Les Hazlewood, scms contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.leshazlewood.scms.core;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

/** @since 0.1 */
public class SiteExporter implements Runnable {

  public static final String ROOT_CONFIG_SECTION_NAME = "scms";
  public static final String DEFAULT_CONFIG_FILE_NAME =
      "config." + ROOT_CONFIG_SECTION_NAME + ".groovy";
  public static final String DEFAULT_EXCLUDES_ENABLED_NAME = "defaultExcludesEnabled";

  private static final String METADATA_KV_PAIR_DELIMITER = ":";

  private File sourceDir;
  private File destDir;
  private File templatesDir;
  private File configFile;

  private Map<String, Object> scmsConfig;
  private boolean defaultExcludesEnabled = true;

  private PatternMatcher patternMatcher;
  private PegDownProcessor pegDownProcessor;
  private VelocityEngine velocityEngine;

  public SiteExporter() {
    this.patternMatcher = new AntPathMatcher();
  }

  public void setSourceDir(File sourceDir) {
    this.sourceDir = sourceDir;
  }

  public void setDestDir(File destDir) {
    this.destDir = destDir;
  }

  public void setTemplatesDir(File templatesDir) {
    this.templatesDir = templatesDir;
  }

  public void setConfigFile(File configFile) {
    this.configFile = configFile;
  }

  public void setVelocityEngine(VelocityEngine velocityEngine) {
    this.velocityEngine = velocityEngine;
  }

  @SuppressWarnings("unchecked")
  public void init() throws Exception {

    if (sourceDir == null) {
      sourceDir = new File(System.getProperty("user.dir"));
    }
    ensureDirectory(sourceDir);

    if (destDir == null) {
      destDir = new File(sourceDir, "output");
    }
    ensureDirectory(destDir);

    if (sourceDir.getAbsolutePath().equals(destDir.getAbsolutePath())) {
      throw new IllegalArgumentException(
          "Source directory and destination directory cannot be the same.");
    }

    if (templatesDir == null) {
      templatesDir = new File(sourceDir, "templates");
    }
    ensureDirectory(templatesDir);

    if (velocityEngine == null) {
      velocityEngine =
          new DefaultVelocityEngineFactory(sourceDir, templatesDir).createVelocityEngine();
    }

    if (configFile == null) {
      configFile = new File(sourceDir, DEFAULT_CONFIG_FILE_NAME);
    }

    if (configFile.exists()) {
      if (configFile.isDirectory()) {
        throw new IllegalArgumentException(
            "Expected configuration file " + configFile + " is a directory, not a file.");
      }
    } else {
      String msg =
          "Configuration file not found.  Create a default "
              + DEFAULT_CONFIG_FILE_NAME
              + " file in your source directory or set the configFile property.";
      throw new IllegalStateException(msg);
    }

    ConfigObject config = new ConfigSlurper().parse(configFile.toURI().toURL());
    scmsConfig = getValue(config, ROOT_CONFIG_SECTION_NAME, Map.class);

    if (scmsConfig.containsKey(DEFAULT_EXCLUDES_ENABLED_NAME)) {
      defaultExcludesEnabled = getValue(scmsConfig, DEFAULT_EXCLUDES_ENABLED_NAME, Boolean.class);
    }

    pegDownProcessor = new PegDownProcessor(Extensions.ALL);
  }

  private void ensureDirectory(File f) throws IOException {
    if (f.exists()) {
      if (!f.isDirectory()) {
        throw new IllegalArgumentException("Specified file " + f + " is not a directory.");
      }
      return;
    }

    if (!f.mkdirs()) {
      throw new IOException("Unable to create directory " + f);
    }
  }

  public void execute() throws IOException {
    recurse(sourceDir);
  }

  @Override
  public void run() {
    try {
      recurse(sourceDir);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String getRelativePath(File parent, File child) {
    String dirAbsPath = parent.getAbsolutePath();
    String fileAbsPath = child.getAbsolutePath();
    if (!fileAbsPath.startsWith(dirAbsPath)) {
      throw new IllegalArgumentException(
          "The specified file is not a child or grandchild of the 'parent' argument.");
    }
    String relPath = fileAbsPath.substring(dirAbsPath.length());
    if (relPath.startsWith(File.separator)) {
      relPath = relPath.substring(1);
    }
    return relPath;
  }

  private String getRelativeDirectoryPath(String path) {
    if (path == null) {
      throw new IllegalArgumentException("path argument cannot be null.");
    }
    int lastSeparatorIndex = path.lastIndexOf(File.separatorChar);
    if (lastSeparatorIndex <= 0) {
      return ".";
    }
    String[] segments = path.split(File.separator);

    StringBuilder sb = new StringBuilder("");
    for (int i = 0; i < segments.length - 1; i++) {
      if (sb.length() > 0) {
        sb.append(File.separatorChar);
      }
      sb.append("..");
    }
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  private boolean isIncluded(File f) {

    if (defaultExcludesEnabled && f.equals(configFile)) {
      return false;
    }

    String absPath = f.getAbsolutePath();

    /*if (absPath.startsWith(destDir.getAbsolutePath()) ||
            absPath.startsWith(templatesDir.getAbsolutePath()) ||
            f.equals(configFile)) {
        return false;
    }*/

    // only forcefully exclude the destDir (we require this so we avoid infinite recursion).
    // We don't however forcefully exclude the scms config and/or templatesDir in the produced
    // site in case the user wants to allow site viewers to see this information, e.g.
    // an open source community site might want to show their config and templates to help others.

    if (absPath.startsWith(destDir.getAbsolutePath())) {
      return false;
    }

    // now check excluded patterns:
    String relPath = getRelativePath(sourceDir, f);
    List<String> excludes = getValue(scmsConfig, "excludes", List.class);
    if (excludes != null) {
      for (String pattern : excludes) {
        if (patternMatcher.matches(pattern, relPath)) {
          return false;
        }
      }
    }

    return true;
  }

  private String applyExtension(String path, String ext) {
    int i = path.lastIndexOf('.');
    if (i > 0) {
      return path.substring(0, i) + ext;
    }
    return path + ext;
  }

  private boolean isHtmlFile(String relPath) {
    if (relPath == null) {
      return false;
    }

    // TODO - make extensions configurable
    return relPath.endsWith(".html") || relPath.endsWith(".htm");
  }

  @SuppressWarnings("unchecked")
  private void recurse(File dir) throws IOException {

    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }

    for (File f : files) {

      if (f.equals(destDir)) {
        continue; // don't infinitely recurse
      }

      String relPath = getRelativePath(sourceDir, f);

      if (f.isDirectory()) {
        if (isIncluded(f)) {
          File copiedDir = new File(destDir, relPath);
          ensureDirectory(copiedDir);
          recurse(f);
        }
      } else {

        Map<String, Object> patterns = getValue(scmsConfig, "patterns", Map.class);

        boolean rendered = false;

        // TODO clean this up and refactor to helper methods.  yuck.

        for (Map.Entry<String, Object> patternEntry : patterns.entrySet()) {
          String pattern = patternEntry.getKey();

          if (patternMatcher.matches(pattern, relPath)) {

            Map<String, Object> model = new LinkedHashMap<String, Object>();

            String relDirPath = getRelativeDirectoryPath(relPath);
            if ("".equals(relDirPath)) {
              // still need to reference it with a separator char in the file:
              relDirPath = ".";
            }
            model.put("root", relDirPath);

            Map<String, Object> globalModel = getValue(scmsConfig, "model", Map.class);
            append(model, globalModel);

            Map<String, Object> patternCfg = (Map<String, Object>) patternEntry.getValue();

            Map<String, Object> patternCfgModel = getValue(patternCfg, "model", Map.class);
            append(model, patternCfgModel);

            String templatePath = (String) patternCfg.get("template");
            if (templatePath != null) {
              templatePath = templatePath.trim();
            }
            if (templatePath == null) {
              String msg = "Required 'template' value is missing for pattern '" + pattern + "'";
              throw new IllegalStateException(msg);
            }

            String extension = (String) patternCfg.get("destFileExtension");
            if (extension == null) {
              extension =
                  ".html"; // temporary default until this can be configured at the global level.
            }

            String destFileRelPath = applyExtension(relPath, extension);
            File destFile = new File(destDir, destFileRelPath);
            ensureFile(destFile);

            String content = readFile(f);

            // currently only HTML or Markdown files are supported:
            if (!isHtmlFile(relPath)) {
              // assume markdown for now:
              content = stripMetadata(content, model);
              content = pegDownProcessor.markdownToHtml(content);
            }

            model.put("content", content);
            VelocityContext ctx = new VelocityContext(model);

            FileWriter fw = new FileWriter(destFile);
            BufferedWriter writer = new BufferedWriter(fw);

            velocityEngine.mergeTemplate(templatePath, "UTF-8", ctx, writer);
            writer.close();

            rendered = true;
            break;
          }
        }

        // System.out.println("File: " + f);

        boolean included = isIncluded(f);
        // System.out.println("\tincluded: " + included);
        // System.out.println("\trendered: " + rendered);

        boolean copy = !rendered && included;
        // System.out.println("\tcopy: " + copy);

        if (rendered
            && included
            && !defaultExcludesEnabled) { // auto exclude the raw content files that are merged with
          // a template.
          copy = true;
          // System.out.println("\tdefaultExcludesEnabled, copy: " + copy);
        }

        if (copy) {
          // no pattern matched - just copy the file over:
          File destFile = new File(destDir, relPath);
          ensureFile(destFile);
          copy(f, destFile);
        }
      }
    }
  }

  protected String stripMetadata(String markdown, Map<String, Object> model) {
    if (model == null) {
      throw new IllegalArgumentException("model argument cannot be null.");
    }

    Scanner scanner = new Scanner(markdown);
    int lineCount = 0;
    int charCount = 0; // counter for determining where to cut the metadata from non-metadata

    String key = null;
    List<String> value = new ArrayList<String>();

    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      lineCount++;
      charCount +=
          line.length() + 1; // +1 is to account for the newline character that the scanner stripped
      line = line.trim();

      if (lineCount == 1) {
        if (line.equals("") || !line.contains(METADATA_KV_PAIR_DELIMITER)) {
          // does not conform to Markdown Metadata expectations:
          // - cannot be any blank lines above first line of content
          // - first line of content must be a ':' delimited key/value pair
          return markdown;
        }
      } else { // 2nd line or more
        if ("".equals(line)) {
          // we found the end of metadata - add last key/value pair and stop looping:
          applyValue(model, key, value);
          break;
        }
      }

      int index = line.indexOf(METADATA_KV_PAIR_DELIMITER);
      if (index > 0) {
        applyValue(model, key, value);
        key = line.substring(0, index).trim();
        String valueString = line.substring(index + 1).trim();
        value = new ArrayList<String>();
        value.add(valueString);
      } else {
        value.add(line);
      }
    }

    if (charCount < markdown.length()) {
      return markdown.substring(charCount).trim();
    }

    return markdown;
  }

  private void applyValue(Map<String, Object> model, String key, List<String> value) {
    if (key != null && value != null && !value.isEmpty()) {
      if (value.size() == 1) {
        model.put(key, value.get(0));
      } else {
        model.put(key, value);
      }
    }
  }

  private void append(Map dest, Map src) {
    if (dest != null && src != null && !src.isEmpty()) {
      dest.putAll(src);
    }
  }

  private <T> T getValue(Map<String, Object> src, String name, Class<T> type) {

    Object o = src.get(name);
    if (o == null) {
      return null;
    }

    if (!type.isInstance(o)) {
      String msg =
          "Configuration property '"
              + name
              + "' is expected to be a "
              + type.getName()
              + " instance.  Instead a "
              + o.getClass().getName()
              + " was discovered.";
      throw new IllegalStateException(msg);
    }

    return type.cast(o);
  }

  private void copy(File src, File dest) throws IOException {

    FileChannel source = new FileInputStream(src).getChannel();
    try {
      FileChannel destination = new FileOutputStream(dest).getChannel();
      try {
        source.transferTo(0, source.size(), destination);
        // destination.transferFrom(source, 0, source.size());
      } finally {
        if (destination != null) {
          destination.close();
        }
      }
    } finally {
      if (source != null) {
        source.close();
      }
    }
  }

  private String readFile(File file) throws IOException {
    FileInputStream stream = new FileInputStream(file);
    try {
      FileChannel fc = stream.getChannel();
      MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
      /* Instead of using default, pass in a decoder. */
      return Charset.forName("UTF-8").decode(bb).toString();
    } finally {
      stream.close();
    }
  }

  private void ensureFile(File f) throws IOException {
    if (f.exists()) {
      if (f.isDirectory()) {
        throw new IllegalStateException(
            "File " + f + " was expected to be a file, not a directory.");
      }
      return;
    }

    f.getParentFile().mkdirs();
    f.createNewFile();
  }
}
