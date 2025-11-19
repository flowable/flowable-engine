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
package org.flowable.common.engine.impl.json;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Filip Hrisafov
 */
public interface FlowableObjectNode extends FlowableJsonNode {

    void put(String propertyName, String value);

    void put(String propertyName, Boolean value);

    void put(String propertyName, Short value);

    void put(String propertyName, Integer value);

    void put(String propertyName, Long value);

    void put(String propertyName, Double value);

    void put(String propertyName, Float value);

    void put(String propertyName, BigDecimal value);

    void put(String propertyName, BigInteger value);

    void put(String propertyName, byte[] value);

    void putNull(String propertyName);

    FlowableArrayNode putArray(String propertyName);

    void set(String propertyName, FlowableJsonNode value);

    FlowableObjectNode putObject(String propertyName);
}
