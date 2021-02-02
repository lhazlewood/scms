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
package com.leshazlewood.scms.core

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue


/**
 * @since 0.1
 */
class SiteExporterTest {

  SiteExporter exporter

  @BeforeEach
  void setUp() {
    exporter = new SiteExporter()
  }

  @Test
  void testGetDirectoryPathWithNull() {
    assertThrows(
            IllegalArgumentException.class,
            { exporter.getRelativeDirectoryPath(null) } as Executable
    )
  }

  @Test
  void testGetDirectoryPathWithEmptyString() {
    assertEquals ".", exporter.getRelativeDirectoryPath("")
  }

  @Test
  void testGetDirectoryPathWithFlatPath() {
    assertEquals ".", exporter.getRelativeDirectoryPath('foo')
  }

  @Test
  void testGetDirectoryPathWithExplicitSubPath() {
    assertEquals "../..", exporter.getRelativeDirectoryPath('foo/bar/baz')
  }

  @Test
  void testStripMetadataWithNullModelArgument() {
    assertThrows(
            IllegalArgumentException.class,
            { exporter.stripMetadata('content', null) } as Executable
    )
  }

  @Test
  void testStripMetadataWithoutMetadata() {

    //simple test where this any semblance of metadata at all
    def md = '''foo

        bar'''

    Map<String,Object> model = new LinkedHashMap<String,Object>()

    String result = exporter.stripMetadata(md, model)

    assertEquals md, result
    assertTrue model.isEmpty()
  }

  @Test
  void testStripMetadataWithEmptyInitialLine() {

    //this should not be seen as metadata because the key/value pair starts on the second line:
    def md = '''
        key1: value2

        content'''

    Map<String,Object> model = new LinkedHashMap<String,Object>()

    String result = exporter.stripMetadata(md, model)

    assertEquals md, result
    assertTrue model.isEmpty()
  }

  @Test
  void testStripMetadataWithoutEmptySeparatorLine() {

    //this should not be seen as metadata because there is no empty line separating the supposed metadata and the
    //rest of the content:

    def md = '''key1: value1
        content'''

    Map<String,Object> model = new LinkedHashMap<String,Object>()

    String result = exporter.stripMetadata(md, model)

    assertEquals md, result
    assertTrue model.isEmpty()
  }

  @Test
  void testStripMetadataWithWellFormedMetadata() {

    def md = '''key1: value1
        key2: value2a
              value2b
        key3: value3

        content
        '''

    Map<String,Object> model = new LinkedHashMap<String,Object>()

    String result = exporter.stripMetadata(md, model)

    assertNotNull result
    assertEquals('content', result) //assert metadata was stripped out

    //assert metadata converted to a model map:
    assertEquals 3, model.size()
    assertEquals 'value1', model.key1
    assertTrue model.key2 instanceof List
    assertEquals 'value2a', model.key2.get(0)
    assertEquals 'value2b', model.key2.get(1)
    assertEquals 'value3', model.key3
  }
}
