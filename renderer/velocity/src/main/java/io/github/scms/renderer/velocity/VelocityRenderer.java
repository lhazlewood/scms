/*
 * Copyright 2021 Les Hazlewood, scms contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.scms.renderer.velocity;

import io.github.scms.api.FileRenderer;
import io.github.scms.api.RenderRequest;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;

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

    Map<String, Object> model = request.getModel();
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
    } catch (ResourceNotFoundException notFoundException) {
      throw new IllegalStateException("Unable to find resource ", notFoundException);
    }

    if (!successful) {
      throw new IllegalStateException("Unable to render resource " + sourceName);
    }
  }
}
