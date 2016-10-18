package com.leshazlewood.scms.core;

import java.io.File;

public interface Processor {

    void setSourceDir(File sourceDir);

    void setDestDir(File destDir);

    void setConfigFile(File configFile);

    void setEnvironment(String envName);

    void init();

    void run();
}
