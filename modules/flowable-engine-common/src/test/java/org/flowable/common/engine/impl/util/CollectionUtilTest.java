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
package org.flowable.common.engine.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class CollectionUtilTest {

    @Test
    void singletonMap() {
        assertThat(CollectionUtil.singletonMap("key", "value"))
                .containsExactly(entry("key", "value"));
    }

    @Test
    void mapOddNumberOfParameters() {
        assertThatThrownBy(() -> CollectionUtil.map("key"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The input should always be even since we expect a list of key-value pairs!");

        assertThatThrownBy(() -> CollectionUtil.map("key1", "value1", "key2"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The input should always be even since we expect a list of key-value pairs!");
    }

    @Test
    void mapEvenNumberOfParameters() {
        assertThat(CollectionUtil.map("key", "value"))
                .containsExactly(entry("key", "value"));

        assertThat(CollectionUtil.map("key1", "value1", "key2", "value2"))
                .containsOnly(
                        entry("key1", "value1"),
                        entry("key2", "value2")
                );
    }

    @Test
    void collectionIsEmpty() {
        assertThat(CollectionUtil.isEmpty(null)).isTrue();
        assertThat(CollectionUtil.isEmpty(Collections.emptyList())).isTrue();
        assertThat(CollectionUtil.isEmpty(Collections.singletonList("test"))).isFalse();
    }

    @Test
    void collectionIsNotEmpty() {
        assertThat(CollectionUtil.isNotEmpty(null)).isFalse();
        assertThat(CollectionUtil.isNotEmpty(Collections.emptyList())).isFalse();
        assertThat(CollectionUtil.isNotEmpty(Collections.singletonList("test"))).isTrue();
    }

    @Test
    void partitionNullCollection() {
        assertThat(CollectionUtil.partition(null, 10)).isNull();
    }

    @Test
    void partitionEmptyCollection() {
        assertThat(CollectionUtil.partition(Collections.emptySet(), 10)).isEmpty();
    }

    @Test
    void partitionSet() {
        Collection<String> set = new LinkedHashSet<>();
        set.add("1");
        set.add("2");
        set.add("3");
        set.add("4");
        set.add("5");
        assertThat(CollectionUtil.partition(set, 3))
                .containsExactly(
                        Arrays.asList("1", "2", "3"),
                        Arrays.asList("4", "5")
                );

        assertThat(CollectionUtil.partition(set, 5))
                .containsExactly(
                        Arrays.asList("1", "2", "3", "4", "5")
                );
    }

    @Test
    void partitionList() {
        Collection<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");
        list.add("5");
        assertThat(CollectionUtil.partition(list, 3))
                .containsExactly(
                        Arrays.asList("1", "2", "3"),
                        Arrays.asList("4", "5")
                );

        assertThat(CollectionUtil.partition(list, 5))
                .containsExactly(
                        Arrays.asList("1", "2", "3", "4", "5")
                );
    }
}
