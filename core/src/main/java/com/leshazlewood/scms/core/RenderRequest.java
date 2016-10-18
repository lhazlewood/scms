package com.leshazlewood.scms.core;

import java.io.Writer;
import java.util.Map;

public interface RenderRequest {

    Map<String,?> getModel();

    Resource getResource();

    Writer getWriter();
}
