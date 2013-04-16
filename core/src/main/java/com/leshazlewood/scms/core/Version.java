package com.leshazlewood.scms.core;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * @since 0.1
 */
public class Version {

    private static final String VERSION = lookupVersion();

    public static String getVersion() {
        return VERSION;
    }

    private static String lookupVersion() {
        Class clazz = Version.class;
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (!classPath.startsWith("jar")) {
            // Class not from JAR
            return "NOT-FROM-JAR";
        }
        String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
        Manifest manifest = getManifest(manifestPath);
        Attributes attr = manifest.getMainAttributes();
        String value = attr.getValue("Implementation-Version");
        if (value == null) {
            throw new IllegalStateException("Unable to obtain 'Implementation-Version' property from manifest.");
        }
        return value;
    }

    private static Manifest getManifest(String path) {
        try {
            return new Manifest(new URL(path).openStream());
        } catch (IOException e) {
            throw new RuntimeException("Unable to obtain version from manifest path [" + path + "]");
        }
    }
}
