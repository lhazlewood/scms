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
import io.github.scms.api.FileRendererFactory;
import java.io.File;
import org.apache.velocity.app.VelocityEngine;

public class VelocityRendererFactory implements FileRendererFactory {

  private File sourceDir;
  private File templateDir;

  public VelocityRendererFactory() {
    // sl
  }

  @Override
  public FileRendererFactory withSourceDir(File sourceDir) {
    this.sourceDir = sourceDir;
    return this;
  }

  @Override
  public FileRendererFactory withTemplateDir(File templateDir) {
    this.templateDir = templateDir;
    return this;
  }

  @Override
  public FileRenderer create() {
    VelocityEngine velocityEngine =
        new DefaultVelocityEngineFactory(sourceDir, templateDir).createVelocityEngine();

    return new VelocityRenderer(velocityEngine);
  }
}
