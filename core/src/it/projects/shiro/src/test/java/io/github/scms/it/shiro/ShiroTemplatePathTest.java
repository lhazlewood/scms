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
package io.github.scms.it.shiro;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.leshazlewood.scms.core.DefaultProcessor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class ShiroTemplatePathTest {

  public File destDir;

  private File sourceDir;

  @BeforeEach
  public void setup(TestInfo testInfo) throws IOException {
    this.sourceDir = new File("target/test-classes/shiro-site");
    String testMethodName =
        testInfo.getTestMethod().orElseThrow(NoSuchElementException::new).getName();
    this.destDir = new File("target/tmp/", testMethodName);
    if (this.destDir.exists()) {
      Files.walk(this.destDir.toPath())
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);

      Files.deleteIfExists(this.destDir.toPath());
    }
    boolean mkdirs = this.destDir.mkdirs();
    if (!mkdirs) {
      throw new IllegalStateException("Could not initialize tmp dir: " + destDir.getAbsolutePath());
    }
    System.out.println("source dir:" + sourceDir.getAbsolutePath());
    System.out.println("dest dir:" + destDir.getAbsolutePath());
  }

  @Test
  public void testNestedTemplatePath() {
    // given
    DefaultProcessor processor = new com.leshazlewood.scms.core.DefaultProcessor();
    processor.setSourceDir(this.sourceDir);
    processor.setDestDir(this.destDir);
    processor.init();

    // when
    processor.run();

    // then
    File coreHtml = new File(this.destDir, "core.html");
    assertTrue(coreHtml.exists());
  }
}
