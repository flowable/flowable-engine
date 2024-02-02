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
package org.flowable.eventregistry.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.flowable.eventregistry.api.CorrelationKeyGenerator;
import org.flowable.eventregistry.impl.DefaultCorrelationKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class DefaultCorrelationKeyGeneratorTest {

    protected CorrelationKeyGenerator<Map<String, Object>> correlationKeyGenerator;

    @BeforeEach
    void setUp() {
        correlationKeyGenerator = new DefaultCorrelationKeyGenerator();
    }

    @Test
    void generateCorrelationKeyWithNullNullParameter() {
        Map<String, Object> data1 = new LinkedHashMap<>();
        data1.put("someKey", "value");
        data1.put("noValue", null);

        Map<String, Object> data2 = new LinkedHashMap<>();
        data2.put("someKey", "value");
        data2.put("noValue", "");

        String key1 = correlationKeyGenerator.generateKey(data1);
        String key2 = correlationKeyGenerator.generateKey(data2);
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void generateCorrelationKeyOrdering() {
        Map<String, Object> data1 = new LinkedHashMap<>();
        data1.put("someKey", "value");
        data1.put("otherKey", "other value");

        Map<String, Object> data2 = new LinkedHashMap<>();
        data2.put("otherKey", "other value");
        data2.put("someKey", "value");

        String key1 = correlationKeyGenerator.generateKey(data1);
        String key2 = correlationKeyGenerator.generateKey(data2);
        assertThat(key1).isEqualTo(key2);
    }
}
