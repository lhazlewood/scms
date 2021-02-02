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
package io.github.scms.renderer.markdown;

import io.github.scms.api.FileRenderer;
import io.github.scms.api.FileRendererFactory;
import java.io.File;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

public class MarkdownRendererFactory implements FileRendererFactory {

  @Override
  public FileRendererFactory withSourceDir(File sourceDir) {
    return this;
  }

  @Override
  public FileRendererFactory withTemplateDir(File templateDir) {
    return this;
  }

  @Override
  public FileRenderer create() {
    PegDownProcessor pegDownProcessor = new PegDownProcessor(Extensions.ALL);

    return new PegdownRenderer(pegDownProcessor);
  }
}
