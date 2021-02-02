package com.leshazlewood.scms.core

import org.apache.velocity.app.VelocityEngine
import org.pegdown.Extensions
import org.pegdown.PegDownProcessor

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption

@SuppressWarnings(["ChangeToOperator", "GrMethodMayBeStatic"])
class DefaultProcessor implements Processor {

    public static final String DEFAULT_CONFIG_FILE_NAME = '.scms.groovy'

    PatternMatcher patternMatcher = new AntPathMatcher()

    Renderer velocityRenderer;
    Renderer pegdownRenderer;
    Collection<Renderer> renderers;
    Map<String, Renderer> renderersByExtension;

    File sourceDir
    File destDir
    File configFile
    String envName
    Map config

    @Override
    public void setSourceDir(File sourceDir) {
        this.sourceDir = sourceDir
    }

    @Override
    public void setDestDir(File destDir) {
        this.destDir = destDir
    }

    @Override
    public void setConfigFile(File configFile) {
        this.configFile = configFile
    }

    @Override
    void setEnvironment(String envName) {
        this.envName = envName
    }

    @Override
    public void init() {

        if (sourceDir == null) {
            sourceDir = new File(System.getProperty("user.dir"));
        }
        ensureDirectory(sourceDir);

        if (destDir == null) {
            destDir = new File(sourceDir, "output");
        }
        ensureDirectory(destDir);

        if (sourceDir.getAbsolutePath() == destDir.getAbsolutePath()) {
            throw new IllegalArgumentException("Source directory and destination directory cannot be the same.");
        }

        VelocityEngine velocityEngine = new DefaultVelocityEngineFactory(sourceDir, new File(this.sourceDir, "templates")).createVelocityEngine();
        velocityRenderer = new VelocityRenderer(velocityEngine);

        pegdownRenderer = new PegdownRenderer(new PegDownProcessor(Extensions.ALL))

        renderers = []
        renderers << velocityRenderer
        renderers << pegdownRenderer

        renderersByExtension = asRendererMap(renderers)

        if (configFile == null) {
            configFile = new File(sourceDir, DEFAULT_CONFIG_FILE_NAME);
        }

        if (configFile.exists()) {
            if (configFile.isDirectory()) {
                throw new IllegalArgumentException("Expected configuration file " + configFile + " is a directory, not a file.");
            }

            def slurper

            if (envName) {
                slurper = new ConfigSlurper(envName);
            } else {
                slurper = new ConfigSlurper();
            }

            def cfgobj = slurper.parse(configFile.toURI().toURL())
            config = cfgobj.scms;
        } else {
            config = [:]
        }
    }

    private static Map<String, Renderer> asRendererMap(Collection<Renderer> c) {

        Map<String, Renderer> m = [:]
        for (Renderer r : c) {
            if (r instanceof FileRenderer) {
                m[r.inputFileExtension] = r
            }
        }

        return m
    }

    @Override
    public void run() {
        recurse(sourceDir);
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

    private static String getRelativePath(File parent, File child) {
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

    private static String getRelativeDirectoryPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path argument cannot be null.");
        }

        int lastSeparatorIndex = path.lastIndexOf(File.separatorChar as int);
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

        if (f.equals(configFile)) {
            return false;
        }

        String absPath = f.getAbsolutePath();

        /*if (absPath.startsWith(destDir.getAbsolutePath()) ||
                absPath.startsWith(templatesDir.getAbsolutePath()) ||
                f.equals(configFile)) {
            return false;
        }*/

        //only forcefully exclude the destDir (we require this so we avoid infinite recursion).
        //We don't however forcefully exclude the scms config and/or templatesDir in the produced
        //site in case the user wants to allow site viewers to see this information, e.g.
        //an open source community site might want to show their config and templates to help others.

        if (absPath.startsWith(destDir.getAbsolutePath())) {
            return false;
        }

        //now check excluded patterns:
        String relPath = getRelativePath(sourceDir, f);

        if (config.excludes instanceof Collection) {
            for (String pattern : config.excludes) {
                if (patternMatcher.matches(pattern, relPath)) {
                    return false;
                }
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private void recurse(File dir) throws IOException {

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (final File f : files) {

            if (f.equals(destDir) || !isIncluded(f)) {
                continue;
            }

            if (f.isDirectory()) {
                String relPath = getRelativePath(sourceDir, f);
                File copiedDir = new File(destDir, relPath);
                ensureDirectory(copiedDir);
                recurse(f);
            } else {
                try {
                    renderFile(f);
                } catch (Exception ioException) {
                    throw new IOException("Unable to render file $f: ${ioException.message}", ioException)
                }
            }
        }
    }

    private void renderFile(File f) throws IOException {

        String relPath = getRelativePath(sourceDir, f);

        Map<String, Object> config = (Map<String, Object>) deepcopy(this.config as Map)

        Map<String, Object> model = [:]

        if (config.containsKey('model') && config.model instanceof Map) {
            model = config.model as Map
        } else {
            config.model = model
        }

        String relDirPath = getRelativeDirectoryPath(relPath);
        if ("".equals(relDirPath)) {
            //still need to reference it with a separator char in the file:
            relDirPath = ".";
        }

        model.root = relDirPath

        Map patterns = Collections.emptyMap()

        if (config.containsKey('patterns')) {
            assert config.patterns instanceof Map: "scms.patterns must be a map"
            patterns = config.patterns
        }

        String action = 'render' //default unless overridden

        for (Map.Entry<String, ?> patternEntry : patterns.entrySet()) {

            String pattern = patternEntry.getKey();

            if (patternMatcher.matches(pattern, relPath)) {

                assert patternEntry.value instanceof Map: "Entry for pattern '$pattern' must be a map."
                Map patternConfig = patternEntry.value as Map
                config << patternConfig

                //pattern-specific model
                if (patternConfig.model && patternConfig.model instanceof Map) {
                    model << (patternConfig.model as Map)
                }

                if (patternConfig.containsKey('render')) {
                    action = patternConfig.render
                }

                break; //stop pattern iteration - first match always wins
            }
        }
        config.model = model

        if (action == 'skip') {
            return;
        } else if (action == 'copy') {
            File destFile = new File(destDir, relPath);
            ensureFile(destFile);
            copy(f, destFile);
            return;
        }

        //otherwise we need to render:
        Reader content = null
        String destRelPath = relPath; //assume same unless it is itself a template

        Renderer renderer = getRenderer(config, destRelPath)

        while (renderer) {

            String extension = getExtension(destRelPath)
            if (content == null) {
                content = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)
            }
            destRelPath = destRelPath.substring(0, destRelPath.length() - (extension.length() + 1))

            String destExtension = (renderer instanceof FileRenderer) ? renderer.outputFileExtension : extension;

            if (config.outputFileExtension) {
                destExtension = config.outputFileExtension
            }

            Renderer nextRenderer = getRenderer(config, destRelPath)

            // if this is the last renderer set the extension, otherwise, skip it
            if (nextRenderer == null && !destRelPath.endsWith(".$destExtension")) {
                destRelPath += ".$destExtension"
            }

            content = render(renderer, model, destRelPath, content)
            renderer = nextRenderer
        }

        if (config.template) { //a template will be used to render the contents
            String template = config.template as String
            File templateFile = new File(this.sourceDir, template);
            renderer = getRenderer(template)
            if (renderer) {
                if (content == null) {
                    content = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)
                }
                model.content = content.getText()
                content = Files.newBufferedReader(templateFile.toPath(), StandardCharsets.UTF_8)
                content = render(renderer, model, destRelPath, content)
            }
        }

        File destFile = new File(destDir, destRelPath);
        ensureFile(destFile);

        if (content != null) {
            //write out the rendered content to the destination file:
            BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));
            copy(content, writer)
            content.close()
            writer.close()
        } else {
            //just copy the file over:
            copy(f, destFile);
        }
    }

    def Map deepcopy(Map map) {
        if (map == null) {
            return null
        }
        Map copy = [:]
        for (Map.Entry e : map.entrySet()) {
            Object value = e.value
            if (value instanceof Collection) {
                value = deepcopy(value as Collection)
            }
            if (value instanceof Map) {
                value = deepcopy(value as Map)
            }
            copy[e.key] = value
        }
        return copy
    }

    def List deepcopy(Collection list) {
        if (list == null) {
            return list;
        }
        List copy = []
        for (Object o : list) {
            Object value = o;
            if (o instanceof Collection) {
                value = deepcopy(o as Collection)
            }
            if (o instanceof Map) {
                value = deepcopy(o as Map)
            }
            copy << value
        }
        return copy
    }


    private boolean hasExtension(String path) {
        return getExtension(path) != null
    }

    private String getExtension(String path) {
        int ch = '.' as char
        int i = path.lastIndexOf(ch)
        if (i > 0) {
            return path.substring(i + 1)
        }
        return null
    }

    Renderer getRenderer(String path) {
        String extension = getExtension(path)
        return renderersByExtension[extension]
    }

    Renderer getRenderer(Map config, String path) {
        if ('velocity'.equals(config.renderer)) {
            return velocityRenderer;
        } else if ('pegdown'.equals(config.renderer)) {
            return pegdownRenderer;
        }

        for (Renderer r : renderers) {
            if (r instanceof FileRenderer && r.supports(path)) {
                return r;
            }
        }

        return null
    }

    Reader render(Renderer renderer, Map<String, ?> model, String path, Reader reader) {
        Resource resource = new DefaultResource(path, reader);
        StringWriter resultWriter = new StringWriter(8192)
        RenderRequest request = new DefaultRenderRequest(model, resource, resultWriter)
        renderer.render(request);
        reader.close()
        resultWriter.close()
        return new StringReader(resultWriter.toString());
    }

    /**
     * Reads all characters from a Reader and writes them to a Writer.
     */
    private static long copy(Reader r, Writer w) throws IOException {
        long nread = 0L;
        char[] buf = new char[4096];
        int n;
        while ((n = r.read(buf)) > 0) {
            w.write(buf, 0, n);
            nread += n;
        }
        return nread;
    }

    private static void copy(File src, File dest) throws IOException {
        Files.copy(src.toPath(), dest.toPath(), LinkOption.NOFOLLOW_LINKS, StandardCopyOption.REPLACE_EXISTING)
    }
}
