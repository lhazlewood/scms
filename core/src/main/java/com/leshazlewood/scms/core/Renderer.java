package com.leshazlewood.scms.core;

import java.io.IOException;

public interface Renderer {

    void render(RenderRequest request) throws IOException;

}
