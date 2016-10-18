package com.leshazlewood.scms.core;

import java.io.Reader;

public class DefaultResource implements Resource {

    private final String name;
    private final Reader reader;

    public DefaultResource(String name, Reader reader) {
        this.name = name;
        this.reader = reader;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Reader getReader() {
        return this.reader;
    }
}
