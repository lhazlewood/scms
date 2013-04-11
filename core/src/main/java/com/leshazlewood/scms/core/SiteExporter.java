/*
 * Copyright 2013 Les Hazlewood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.pegdown.PegDownProcessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @since 0.1
 */
public class SiteExporter implements Runnable {

    private File sourceDir;
    private File destDir;
    private File templatesDir;
    private File configFile;

    private ConfigObject config;

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
            throw new IllegalArgumentException("Source directory and destination directory cannot be the same.");
        }

        if (templatesDir == null) {
            templatesDir = new File(sourceDir, "templates");
        }
        ensureDirectory(templatesDir);

        if (velocityEngine == null) {
            velocityEngine = new DefaultVelocityEngineFactory(sourceDir, templatesDir).createVelocityEngine();
        }

        if (configFile == null) {
            configFile = new File(sourceDir, "scms.cfg");
        }

        if (configFile.exists()) {
            if (configFile.isDirectory()) {
                throw new IllegalArgumentException("Expected configuration file " + configFile + " is a directory, not a file.");
            }
        } else {
            String msg = "Configuration file not found.  Create a default scms.cfg file in your source directory " +
                    "or set the configFile property.";
            throw new IllegalStateException(msg);
        }

        config = new ConfigSlurper().parse(configFile.toURI().toURL());

        pegDownProcessor = new PegDownProcessor();
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
            throw new IllegalArgumentException("The specified file is not a child or grandchild of the 'parent' argument.");
        }
        String relPath = fileAbsPath.substring(dirAbsPath.length());
        if (relPath.startsWith(File.separator)) {
            relPath = relPath.substring(1);
        }
        return relPath;
    }

    private boolean isIncluded(File f) {
        String absPath = f.getAbsolutePath();
        return !absPath.startsWith(destDir.getAbsolutePath()) &&
                !absPath.startsWith(templatesDir.getAbsolutePath()) &&
                !f.equals(configFile);
    }

    private String applyExtension(String path, String ext) {
        int i = path.lastIndexOf('.');
        if (i > 0) {
            return path.substring(0, i) + ext;
        }
        return path + ext;
    }

    @SuppressWarnings("unchecked")
    private void recurse(File dir) throws IOException {

        File[] files = dir.listFiles();

        if (files != null) {
            for (File f : files) {
                if (isIncluded(f)) {

                    String relPath = getRelativePath(sourceDir, f);

                    if (f.isDirectory()) {
                        File copiedDir = new File(destDir, relPath);
                        ensureDirectory(copiedDir);
                        recurse(f);
                    } else {

                        Map<String, Object> patterns = getValue(config, "patterns", Map.class);

                        boolean rendered = false;

                        for (Map.Entry<String, Object> patternEntry : patterns.entrySet()) {
                            String pattern = patternEntry.getKey();

                            if (patternMatcher.matches(pattern, relPath)) {

                                Map<String, Object> model = new LinkedHashMap<String, Object>();

                                Map<String, Object> globalModel = getValue(config, "model", Map.class);
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
                                    extension = ".html"; //temporary default until this can be configured at the global level.
                                }

                                String destFileRelPath = applyExtension(relPath, extension);
                                File destFile = new File(destDir, destFileRelPath);
                                ensureFile(destFile);

                                String markdown = readFile(f);
                                String html = pegDownProcessor.markdownToHtml(markdown);

                                model.put("content", html);
                                VelocityContext ctx = new VelocityContext(model);

                                FileWriter fw = new FileWriter(destFile);
                                BufferedWriter writer = new BufferedWriter(fw);

                                velocityEngine.mergeTemplate(templatePath, "UTF-8", ctx, writer);
                                writer.close();

                                rendered = true;
                                break;
                            }
                        }

                        if (!rendered) {
                            //no pattern matched - just copy the file over:
                            File destFile = new File(destDir, relPath);
                            ensureFile(destFile);
                            copy(f, destFile);
                        }
                    }
                }
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
            String msg = "Configuration property '" + name + "' is expected to be a " + type.getName() + " instance.";
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
                throw new IllegalStateException("File " + f + " was expected to be a file, not a directory.");
            }
            return;
        }

        f.getParentFile().mkdirs();
        f.createNewFile();
    }
}
