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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.bpmn.converter.util.CommaSplitter;
import org.junit.jupiter.api.Test;

/**
 * @author Saeid Mirzaei
 */

public class CommaSplitterTest {

    @Test
    public void testNoComma() {
        String testString = "Test String";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertThat(result).containsExactly(testString);
    }

    @Test
    public void testOneComa() {
        String testString = "Test,String";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertThat(result).containsExactly("Test", "String");
    }

    @Test
    public void testManyCommas() {
        String testString = "does,anybody,really,read,this,nonsense";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertThat(result).containsExactly("does", "anybody", "really", "read", "this", "nonsense");
    }

    @Test
    public void testCommaAtStart() {
        String testString = ",first,second";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertThat(result).containsExactly("first", "second");
    }

    @Test
    public void testCommaAtEnd() {
        String testString = "first,second,";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertThat(result).containsExactly("first", "second");
    }

    @Test
    public void testCommaAtStartAndEnd() {
        String testString = ",first,second,";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertThat(result).containsExactly("first", "second");
    }

    @Test
    public void testOneComaInExpression() {
        String testString = "${first,second}";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertThat(result).containsExactly(testString);
    }

    @Test
    public void testOManyComaInExpression() {
        String testString = "${Everything,should,be,made,as,simple,as,possible},but,no,simpler";
        List<String> result = CommaSplitter.splitCommas(testString);
        assertThat(result).containsExactly("${Everything,should,be,made,as,simple,as,possible}", "but", "no", "simpler");
    }
}
