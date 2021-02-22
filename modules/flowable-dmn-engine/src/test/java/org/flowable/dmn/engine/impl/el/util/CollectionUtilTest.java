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
package org.flowable.dmn.engine.impl.el.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CollectionUtilTest {

    @Test
    public void noneOf() {
        assertThat(CollectionUtil.noneOf(Arrays.asList("group1", "group2"), Arrays.asList("group3", "group4"))).isTrue();
        assertThat(CollectionUtil.noneOf(Arrays.asList("group1", "group2"), Arrays.asList("group1", "group2"))).isFalse();
        assertThat(CollectionUtil.noneOf(Arrays.asList("group1", "group2"), Arrays.asList("group2", "group3"))).isFalse();

        assertThat(CollectionUtil.noneOf(Arrays.asList("group1", "group2"), "group3")).isTrue();
        assertThat(CollectionUtil.noneOf(Arrays.asList("group1", "group2"), "group2")).isFalse();

        assertThat(CollectionUtil.noneOf("group1, group2", "group3, group4")).isTrue();
        assertThat(CollectionUtil.noneOf("group1, group2", "group1, group2")).isFalse();
        assertThat(CollectionUtil.noneOf("group1, group2", "group2, group3")).isFalse();

        ObjectMapper mapper = new ObjectMapper();
        assertThat(CollectionUtil.noneOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group3", "group4"))))
                .isTrue();
        assertThat(CollectionUtil.noneOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group1", "group2"))))
                .isFalse();
        assertThat(CollectionUtil.noneOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group2", "group3"))))
                .isFalse();
    }


    @Test
    public void anyOf() {
        assertThat(CollectionUtil.anyOf(Arrays.asList("group1", "group2"), Arrays.asList("group3", "group4"))).isFalse();
        assertThat(CollectionUtil.anyOf(Arrays.asList("group1", "group2"), Arrays.asList("group1", "group2"))).isTrue();
        assertThat(CollectionUtil.anyOf(Arrays.asList("group1", "group2"), Arrays.asList("group2", "group3"))).isTrue();

        assertThat(CollectionUtil.anyOf(Arrays.asList("group1", "group2"), "group3")).isFalse();
        assertThat(CollectionUtil.anyOf(Arrays.asList("group1", "group2"), "group2")).isTrue();

        assertThat(CollectionUtil.anyOf("group1, group2", "group3, group4")).isFalse();
        assertThat(CollectionUtil.anyOf("group1, group2", "group1, group2")).isTrue();
        assertThat(CollectionUtil.anyOf("group1, group2", "group2, group3")).isTrue();

        ObjectMapper mapper = new ObjectMapper();
        assertThat(CollectionUtil.anyOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group3", "group4"))))
                .isFalse();
        assertThat(CollectionUtil.anyOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group1", "group2")))).isTrue();
        assertThat(CollectionUtil.anyOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group2", "group3")))).isTrue();
    }

    @Test
    public void notAllOf() {
        assertThat(CollectionUtil.notAllOf(Arrays.asList("group1", "group2"), Arrays.asList("group3", "group4"))).isTrue();
        assertThat(CollectionUtil.notAllOf(Arrays.asList("group1", "group2"), Arrays.asList("group1", "group2"))).isFalse();
        assertThat(CollectionUtil.notAllOf(Arrays.asList("group1", "group2"), Arrays.asList("group2", "group3"))).isTrue();

        assertThat(CollectionUtil.notAllOf(Arrays.asList("group1", "group2"), "group3")).isTrue();
        assertThat(CollectionUtil.notAllOf(Arrays.asList("group1", "group2"), "group2")).isFalse();

        assertThat(CollectionUtil.notAllOf("group1, group2", "group3, group4")).isTrue();
        assertThat(CollectionUtil.notAllOf("group1, group2", "group1, group2")).isFalse();
        assertThat(CollectionUtil.notAllOf("group1, group2", "group2, group3")).isTrue();

        ObjectMapper mapper = new ObjectMapper();
        assertThat(CollectionUtil.notAllOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group3", "group4"))))
                .isTrue();
        assertThat(CollectionUtil.notAllOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group1", "group2"))))
                .isFalse();
        assertThat(CollectionUtil.notAllOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group2", "group3"))))
                .isTrue();
    }

    @Test
    public void allOf() {
        assertThat(CollectionUtil.allOf(Arrays.asList("group1", "group2"), Arrays.asList("group3", "group4"))).isFalse();
        assertThat(CollectionUtil.allOf(Arrays.asList("group1", "group2"), Arrays.asList("group1", "group2"))).isTrue();
        assertThat(CollectionUtil.allOf(Arrays.asList("group1", "group2"), Arrays.asList("group2", "group3"))).isFalse();

        assertThat(CollectionUtil.allOf(Arrays.asList("group1", "group2"), "group3")).isFalse();
        assertThat(CollectionUtil.allOf(Arrays.asList("group1", "group2"), "group2")).isTrue();

        assertThat(CollectionUtil.allOf("group1, group2", "group3, group4")).isFalse();
        assertThat(CollectionUtil.allOf("group1, group2", "group1, group2")).isTrue();
        assertThat(CollectionUtil.allOf("group1, group2", "group2, group3")).isFalse();

        ObjectMapper mapper = new ObjectMapper();
        assertThat(CollectionUtil.allOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group3", "group4"))))
                .isFalse();
        assertThat(CollectionUtil.allOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group1", "group2")))).isTrue();
        assertThat(CollectionUtil.allOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group2", "group3"))))
                .isFalse();
    }

}
