package com.leshazlewood.scms.core;

import java.io.Writer;
import java.util.Map;

public class DefaultRenderRequest implements RenderRequest {

    private final Map<String,?> model;
    private final Resource resource;
    private final Writer writer;

    public DefaultRenderRequest(Map<String, ?> model, Resource resource, Writer writer) {
        this.model = model;
        this.resource = resource;
        this.writer = writer;
    }

    @Override
    public Map<String, ?> getModel() {
        return this.model;
    }

    @Override
    public Resource getResource() {
        return this.resource;
    }

    @Override
    public Writer getWriter() {
        return this.writer;
    }
}
