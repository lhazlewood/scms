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
package com.leshazlewood.scms.core;

import io.github.scms.api.RenderRequest;
import io.github.scms.api.Resource;
import java.io.Writer;
import java.util.Map;

public class DefaultRenderRequest implements RenderRequest {

  private final Map<String, Object> model;
  private final Resource resource;
  private final Writer writer;

  public DefaultRenderRequest(Map<String, Object> model, Resource resource, Writer writer) {
    this.model = model;
    this.resource = resource;
    this.writer = writer;
  }

  @Override
  public Map<String, Object> getModel() {
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
