package com.leshazlewood.scms.core;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;

import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

public class VelocityRenderer implements FileRenderer {

    private final VelocityEngine velocityEngine;

    public VelocityRenderer(VelocityEngine velocityEngine) {
        assert velocityEngine != null : "VelocityEngine argument cannot be null.";
        this.velocityEngine = velocityEngine;
    }

    @Override
    public boolean supports(String filename) {
        return filename.endsWith(".vtl");
    }

    @Override
    public String getInputFileExtension() {
        return "vtl";
    }

    @Override
    public String getOutputFileExtension() {
        return "html";
    }

    @Override
    public void render(RenderRequest request) {

        Map<String, ?> model = request.getModel();
        if (model == null) {
            model = Collections.emptyMap();
        }

        VelocityContext ctx = new VelocityContext(model);
        Writer outputWriter = request.getWriter();
        String sourceName = request.getResource().getName();
        Reader sourceReader = request.getResource().getReader();

        boolean successful;

        try {
            successful = velocityEngine.evaluate(ctx, outputWriter, sourceName, sourceReader);
        } catch ( ResourceNotFoundException notFoundException ) {
            throw new IllegalStateException("Unable to find resource ", notFoundException);
        }


        if (!successful) {
            throw new IllegalStateException("Unable to render resource " + sourceName);
        }
    }
}
