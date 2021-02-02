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
import io.github.scms.api.RenderRequest;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.pegdown.PegDownProcessor;

@SuppressWarnings({"Duplicates", "unchecked"})
public class PegdownRenderer implements FileRenderer {

  private static final String METADATA_KV_PAIR_DELIMITER = ":";

  private final PegDownProcessor pegDownProcessor;

  public PegdownRenderer(PegDownProcessor pegDownProcessor) {
    this.pegDownProcessor = pegDownProcessor;
  }

  @Override
  public boolean supports(String filename) {
    return filename != null && (filename.endsWith("md") || filename.endsWith("markdown"));
  }

  @Override
  public String getInputFileExtension() {
    return "md";
  }

  @Override
  public String getOutputFileExtension() {
    return "html";
  }

  @Override
  public void render(RenderRequest request) throws IOException {

    Map<String, Object> model = (Map<String, Object>) request.getModel();
    if (model == null) {
      model = new LinkedHashMap<>();
    }

    Reader reader = request.getResource().getReader();
    BufferedReader breader =
        (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);

    Writer writer = request.getWriter();
    BufferedWriter bwriter =
        (writer instanceof BufferedWriter) ? (BufferedWriter) writer : new BufferedWriter(writer);

    String content = new Scanner(breader).useDelimiter("\\Z").next();
    content = stripMetadata(content, model);
    content = pegDownProcessor.markdownToHtml(content);

    bwriter.write(content);
    bwriter.flush();
  }

  protected String stripMetadata(String markdown, Map<String, Object> model) {
    if (model == null) {
      throw new IllegalArgumentException("model argument cannot be null.");
    }

    Scanner scanner = new Scanner(markdown);
    int lineCount = 0;
    int charCount = 0; // counter for determining where to cut the metadata from non-metadata

    String key = null;
    List<String> value = new ArrayList<String>();

    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      lineCount++;
      charCount +=
          line.length() + 1; // +1 is to account for the newline character that the scanner stripped
      line = line.trim();

      if (lineCount == 1) {
        if (line.equals("") || !line.contains(METADATA_KV_PAIR_DELIMITER)) {
          // does not conform to Markdown Metadata expectations:
          // - cannot be any blank lines above first line of content
          // - first line of content must be a ':' delimited key/value pair
          return markdown;
        }
      } else { // 2nd line or more
        if ("".equals(line)) {
          // we found the end of metadata - add last key/value pair and stop looping:
          applyValue(model, key, value);
          break;
        }
      }

      int index = line.indexOf(METADATA_KV_PAIR_DELIMITER);
      if (index > 0) {
        applyValue(model, key, value);
        key = line.substring(0, index).trim();
        String valueString = line.substring(index + 1).trim();
        value = new ArrayList<>();
        value.add(valueString);
      } else {
        value.add(line);
      }
    }

    if (charCount < markdown.length()) {
      return markdown.substring(charCount).trim();
    }

    return markdown;
  }

  private void applyValue(Map<String, Object> model, String key, List<String> value) {
    if (key != null && value != null && !value.isEmpty()) {
      if (value.size() == 1) {
        model.put(key, value.get(0));
      } else {
        model.put(key, value);
      }
    }
  }
}
