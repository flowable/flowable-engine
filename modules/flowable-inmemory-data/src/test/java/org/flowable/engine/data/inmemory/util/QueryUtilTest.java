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
package org.flowable.engine.data.inmemory.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.test.AbstractTestCase;
import org.junit.jupiter.api.Test;

/**
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class QueryUtilTest extends AbstractTestCase {

    @Test
    public void testNullSafeEquals() {
        assertThat(QueryUtil.nullSafeEquals(null, null)).isTrue();
        assertThat(QueryUtil.nullSafeEquals(null, "foo")).isFalse();
        assertThat(QueryUtil.nullSafeEquals("foo", null)).isFalse();
        assertThat(QueryUtil.nullSafeEquals("foo", "foo")).isTrue();
    }

    @Test
    public void testMatchReturn() {
        assertThat(QueryUtil.matchReturn(null, false)).isNull();
        assertThat(QueryUtil.matchReturn(null, true)).isNull();
        assertThat(QueryUtil.matchReturn(false, true)).isNull();
        assertThat(QueryUtil.matchReturn(false, false)).isFalse();
        assertThat(QueryUtil.matchReturn(true, false)).isNull();
        assertThat(QueryUtil.matchReturn(true, true)).isTrue();
    }

    @Test
    public void testStripLike() {
        String l1 = "%like%";
        String l2 = "like%";
        String l3 = "%like";
        String l4 = "%%like%%";
        String l5 = "%";

        assertThat(QueryUtil.stripLike(l1)).isEqualTo("like");
        assertThat(QueryUtil.stripLike(l2)).isEqualTo("like");
        assertThat(QueryUtil.stripLike(l3)).isEqualTo("like");
        assertThat(QueryUtil.stripLike(l4)).isEqualTo("%like%");
        assertThat(QueryUtil.stripLike(l5)).isEqualTo("");
    }
}
