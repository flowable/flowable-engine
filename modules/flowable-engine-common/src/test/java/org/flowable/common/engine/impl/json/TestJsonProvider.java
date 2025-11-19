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
public interface TestJsonProvider {

    FlowableJsonNode create(String value);

    FlowableJsonNode create(Long value);

    FlowableJsonNode create(Integer value);

    FlowableJsonNode create(Double value);

    FlowableJsonNode create(Boolean value);

    FlowableJsonNode create(Short value);

    FlowableJsonNode create(Float value);

    FlowableJsonNode create(BigDecimal value);

    FlowableJsonNode create(BigInteger value);

    FlowableJsonNode createNull();

    FlowableJsonNode createMissing();

    FlowableJsonNode wrapNull();

    FlowableObjectNode createObjectNode(String json);

    FlowableArrayNode createArrayNode(String json);

    FlowableJsonNode createOtherTypeJson(String json);

}
