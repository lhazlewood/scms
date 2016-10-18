package com.leshazlewood.scms.core;

import java.io.Reader;

public interface Resource {

    String getName();

    Reader getReader();
}
