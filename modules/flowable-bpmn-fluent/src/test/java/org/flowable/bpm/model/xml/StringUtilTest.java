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
package org.flowable.bpm.model.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.flowable.bpm.model.xml.impl.util.StringUtil.joinCommaSeparatedList;
import static org.flowable.bpm.model.xml.impl.util.StringUtil.splitCommaSeparatedList;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class StringUtilTest {

    @Test
    public void stringListSplit() {
        assertThat(splitCommaSeparatedList("")).isEmpty();
        assertThat(splitCommaSeparatedList("  ")).isEmpty();
        assertThat(splitCommaSeparatedList("a")).containsExactly("a");
        assertThat(splitCommaSeparatedList("  a  ")).containsExactly("a");
        assertThat(splitCommaSeparatedList("a,b")).containsExactly("a", "b");
        assertThat(splitCommaSeparatedList("a , b, c ")).containsExactly("a", "b", "c");
        assertThat(splitCommaSeparatedList("${}")).containsExactly("${}");
        assertThat(splitCommaSeparatedList(" #{ } ")).containsExactly("#{ }");
        assertThat(splitCommaSeparatedList(" #{}, ${a}, #{b} ")).containsExactly("#{}", "${a}", "#{b}");
        assertThat(splitCommaSeparatedList(" a, ${b}, #{c} ")).containsExactly("a", "${b}", "#{c}");
        assertThat(splitCommaSeparatedList(" #{a}, b, ,c ,${d} ")).containsExactly("#{a}", "b", "c", "${d}");
        assertThat(splitCommaSeparatedList(" #{a(b,c)}, d, ,e ,${fg(h , i , j)} ")).containsExactly("#{a(b,c)}", "d", "e", "${fg(h , i , j)}");
        assertThat(splitCommaSeparatedList(" #{a == (b, c)}, d = e, f ,${fg(h , i , j)} ")).containsExactly("#{a == (b, c)}", "d = e", "f", "${fg(h , i , j)}");
        assertThat(splitCommaSeparatedList("accountancy, ${fakeLdapService.findManagers(execution, emp)}")).containsExactly("accountancy",
                "${fakeLdapService.findManagers(execution, emp)}");
    }

    @Test
    public void stringListJoin() {
        assertThat(joinCommaSeparatedList(null)).isNull();
        List<String> testList = new ArrayList<>();
        assertThat(joinCommaSeparatedList(testList)).isEqualTo("");
        testList.add("a");
        assertThat(joinCommaSeparatedList(testList)).isEqualTo("a");
        testList.add("b");
        assertThat(joinCommaSeparatedList(testList)).isEqualTo("a, b");
        testList.add("${a,b,c}");
        assertThat(joinCommaSeparatedList(testList)).isEqualTo("a, b, ${a,b,c}");
        testList.add("foo");
        assertThat(joinCommaSeparatedList(testList)).isEqualTo("a, b, ${a,b,c}, foo");
        testList.add("#{bar(e,f,g)}");
        assertThat(joinCommaSeparatedList(testList)).isEqualTo("a, b, ${a,b,c}, foo, #{bar(e,f,g)}");
        String testString = joinCommaSeparatedList(testList);
        assertThat(splitCommaSeparatedList(testString)).containsAll(testList);
    }

    @Test
    public void nullSplit() {
        assertThat(splitCommaSeparatedList(null)).isEmpty();
    }

    @Test
    public void nullJoin() {
        assertThat(joinCommaSeparatedList(null)).isNull();
    }

}
