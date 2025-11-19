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
public interface FlowableArrayNode extends FlowableJsonNode, Iterable<FlowableJsonNode> {

    void set(int index, String value);

    void set(int index, Boolean value);

    void set(int index, Short value);

    void set(int index, Integer value);

    void set(int index, Long value);

    void set(int index, Double value);

    void set(int index, BigDecimal value);

    void set(int index, BigInteger value);

    void setNull(int index);

    void set(int index, FlowableJsonNode value);

    void add(Short value);

    void add(Integer value);

    void add(Long value);

    void add(Float value);

    void add(Double value);

    void add(byte[] value);

    void add(String value);

    void add(Boolean value);

    void add(BigDecimal value);

    void add(BigInteger value);

    void add(FlowableJsonNode value);

    void addNull();
}
