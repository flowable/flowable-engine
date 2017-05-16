/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.editor.language.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.flowable.bpmn.converter.util.CommaSplitter;
import org.junit.Test;

/**
 * @author Saeid Mirzaei
 */

public class CommaSplitterTest {

    @Test
    public void testNoComma() {
        String testString = "Test String";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testString, result.get(0));
    }

    @Test
    public void testOneComa() {
        String testString = "Test,String";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test", result.get(0));
        assertEquals("String", result.get(1));
    }

    @Test
    public void testManyCommas() {
        String testString = "does,anybody,realy,reads,this,nonsense";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertNotNull(result);
        assertEquals(6, result.size());
        assertEquals("does", result.get(0));
        assertEquals("anybody", result.get(1));
        assertEquals("realy", result.get(2));
        assertEquals("reads", result.get(3));
        assertEquals("this", result.get(4));
        assertEquals("nonsense", result.get(5));
    }

    @Test
    public void testCommaAtStart() {
        String testString = ",first,second";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("first", result.get(0));
        assertEquals("second", result.get(1));

    }

    @Test
    public void testCommaAtEnd() {
        String testString = "first,second,";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("first", result.get(0));
        assertEquals("second", result.get(1));

    }

    @Test
    public void testCommaAtStartAndEnd() {
        String testString = ",first,second,";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("first", result.get(0));
        assertEquals("second", result.get(1));
    }

    @Test
    public void testOneComaInExpression() {
        String testString = "${first,second}";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testString, result.get(0));
    }

    @Test
    public void testOManyComaInExpression() {
        String testString = "${Everything,should,be,made,as,simple,as,possible},but,no,simpler";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("${Everything,should,be,made,as,simple,as,possible}", result.get(0));
        assertEquals("but", result.get(1));
        assertEquals("no", result.get(2));
        assertEquals("simpler", result.get(3));
    }
}
