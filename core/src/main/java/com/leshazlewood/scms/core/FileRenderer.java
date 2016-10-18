package com.leshazlewood.scms.core;

public interface FileRenderer extends Renderer {

    boolean supports(String filename);

    String getInputFileExtension();

    String getOutputFileExtension();
}
