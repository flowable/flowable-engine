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
package org.flowable.common.engine.impl.json.jackson3;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.flowable.common.engine.impl.json.FlowableArrayNode;
import org.flowable.common.engine.impl.json.FlowableJsonNode;
import org.flowable.common.engine.impl.json.FlowableObjectNode;

import tools.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class FlowableJackson3ObjectNode extends FlowableJackson3JsonNode<ObjectNode> implements FlowableObjectNode {

    public FlowableJackson3ObjectNode(ObjectNode jsonNode) {
        super(jsonNode);
    }

    @Override
    public void put(String propertyName, String value) {
        jsonNode.put(propertyName, value);
    }

    @Override
    public void put(String propertyName, Boolean value) {
        jsonNode.put(propertyName, value);
    }

    @Override
    public void put(String propertyName, Short value) {
        jsonNode.put(propertyName, value);
    }

    @Override
    public void put(String propertyName, Integer value) {
        jsonNode.put(propertyName, value);
    }

    @Override
    public void put(String propertyName, Long value) {
        jsonNode.put(propertyName, value);
    }

    @Override
    public void put(String propertyName, Double value) {
        jsonNode.put(propertyName, value);
    }

    @Override
    public void put(String propertyName, Float value) {
        jsonNode.put(propertyName, value);
    }

    @Override
    public void put(String propertyName, BigDecimal value) {
        jsonNode.put(propertyName, value);
    }

    @Override
    public void put(String propertyName, BigInteger value) {
        jsonNode.put(propertyName, value);
    }

    @Override
    public void put(String propertyName, byte[] value) {
        jsonNode.put(propertyName, value);
    }

    @Override
    public void putNull(String propertyName) {
        jsonNode.putNull(propertyName);
    }

    @Override
    public FlowableArrayNode putArray(String propertyName) {
        return new FlowableJackson3ArrayNode(jsonNode.putArray(propertyName));
    }

    @Override
    public void set(String propertyName, FlowableJsonNode value) {
        jsonNode.set(propertyName, asJsonNode(value));
    }

    @Override
    public FlowableObjectNode putObject(String propertyName) {
        return new FlowableJackson3ObjectNode(jsonNode.putObject(propertyName));
    }
}
