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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ListIterator;
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
    private VelocityEngineFactory velocityEngineFactory;


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

    private void init() throws Exception {

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
            if (velocityEngineFactory != null) {
                velocityEngine = velocityEngineFactory.createVelocityEngine();
            } else {
                velocityEngine = new DefaultVelocityEngineFactory(sourceDir, templatesDir).createVelocityEngine();
            }
        }

        if (configFile == null) {
            File cfg = new File(sourceDir, "scms.cfg");
            if (cfg.exists()) {
                if (cfg.isDirectory()) {
                    throw new IllegalArgumentException("Expected configuration file " + cfg + " is a directory, not a file.");
                }
                configFile = cfg;
            } else {
                String msg = "Configuration file not found.  Create a default scms.cfg file in your source directory " +
                        "or specify the -c option to specify the file location.";
                throw new IllegalStateException(msg);
            }
        }

        config = new ConfigSlurper().parse(configFile.toURI().toURL());

        pegDownProcessor = new PegDownProcessor();
    }

    private static void ensureDirectory(File f) throws IOException {
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

    @Override
    public void run() {
        try {
            recurse(sourceDir);
        } catch (Exception e) {
            throw new Error(e);
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

    private void recurse(File dir) throws IOException {

        File[] files = dir.listFiles();

        if (files != null) {
            for (File f : files) {
                if (isIncluded(f)) {
                    if (f.isDirectory()) {
                        String relPath = getRelativePath(sourceDir, f);
                        File copiedDir = new File(destDir, relPath);
                        ensureDirectory(copiedDir);
                        recurse(f);
                    } else {
                        String relPath = getRelativePath(sourceDir, f);

                        Map<String, Object> patternCfg = (Map<String, Object>) config.get("patterns");
                        for (Map.Entry<String, Object> patternEntry : patternCfg.entrySet()) {
                            String pattern = patternEntry.getKey();

                            if (patternMatcher.matches(pattern, relPath)) {
                                Map<String, Object> value = (Map<String, Object>) patternEntry.getValue();
                                String templatePath = (String) value.get("template");

                                String destFileRelPath = applyExtension(relPath, ".html");
                                File destFile = new File(destDir, destFileRelPath);
                                ensureFile(destFile);

                                String markdown = readFile(f);
                                String html = pegDownProcessor.markdownToHtml(markdown);

                                Map<String, String> model = new HashMap<String, String>();
                                model.put("body", html);
                                VelocityContext ctx = new VelocityContext(model);

                                FileWriter fw = new FileWriter(destFile);
                                BufferedWriter writer = new BufferedWriter(fw);

                                velocityEngine.mergeTemplate(templatePath, "UTF-8", ctx, writer);
                                writer.close();
                            }
                        }
                    }
                }
            }
        }
    }

    private static String readFile(File file) throws IOException {
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

    private static void ensureFile(File f) throws IOException {
        if (f.exists()) {
            if (f.isDirectory()) {
                throw new IllegalStateException("File " + f + " was expected to be a file, not a directory.");
            }
            return;
        }

        f.getParentFile().mkdirs();
        f.createNewFile();
    }

    public static void main(String[] args) throws Exception {

        SiteExporter exporter = new SiteExporter();

        ListIterator<String> it = Arrays.asList(args).listIterator();

        while (it.hasNext()) {
            String s = it.next();
            if ("-d".equals(s)) {
                File outputDir = new File(it.next());
                exporter.setDestDir(outputDir);
            } else if ("-c".equals(s)) {
                File configFile = new File(it.next());
                if (!configFile.exists()) {
                    throw new IllegalArgumentException("Config file does not exist: " + configFile);
                }
                if (configFile.isDirectory()) {
                    throw new IllegalArgumentException("Specified config file is a directory and not a file: " + configFile);
                }
                exporter.setConfigFile(configFile);
            }
        }

        exporter.init();

        Thread t = new Thread(exporter);
        t.start();
        t.join();
    }
}
