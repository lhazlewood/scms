package com.leshazlewood.scms.core

import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*

/**
 * @since 0.1
 */
class SiteExporterTest {

    SiteExporter exporter;

    @Before
    void setUp() {
        exporter = new SiteExporter()
    }

    @Test(expected=IllegalArgumentException)
    void testGetDirectoryPathWithNull() {
        exporter.getRelativeDirectoryPath(null)
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

    @Test(expected=IllegalArgumentException)
    void testStripMetadataWithNullModelArgument() {
        exporter.stripMetadata('content', null);
    }

    @Test
    void testStripMetadataWithoutMetadata() {

        //simple test where this any semblance of metadata at all
        def md = '''foo

        bar'''

        Map<String,Object> model = new LinkedHashMap<String,Object>();

        String result = exporter.stripMetadata(md, model);

        assertEquals md, result
        assertTrue model.isEmpty()
    }

    @Test
    void testStripMetadataWithEmptyInitialLine() {

        //this should not be seen as metadata because the key/value pair starts on the second line:
        def md = '''
        key1: value2

        content'''

        Map<String,Object> model = new LinkedHashMap<String,Object>();

        String result = exporter.stripMetadata(md, model);

        assertEquals md, result
        assertTrue model.isEmpty()
    }

    @Test
    void testStripMetadataWithoutEmptySeparatorLine() {

        //this should not be seen as metadata because there is no empty line separating the supposed metadata and the
        //rest of the content:

        def md = '''key1: value1
        content'''

        Map<String,Object> model = new LinkedHashMap<String,Object>();

        String result = exporter.stripMetadata(md, model);

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

        Map<String,Object> model = new LinkedHashMap<String,Object>();

        String result = exporter.stripMetadata(md, model);

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
