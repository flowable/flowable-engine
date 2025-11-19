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
package org.flowable.common.engine.impl.json.jackson2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

import org.flowable.common.engine.impl.json.FlowableArrayNode;
import org.flowable.common.engine.impl.json.FlowableJsonNode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Filip Hrisafov
 */
public class FlowableJackson2ArrayNode extends FlowableJackson2JsonNode<ArrayNode> implements FlowableArrayNode {

    public FlowableJackson2ArrayNode(ArrayNode arrayNode) {
        super(arrayNode);
    }

    @Override
    public Iterator<FlowableJsonNode> iterator() {
        Iterator<JsonNode> delegate = jsonNode.elements();
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public FlowableJsonNode next() {
                return FlowableJackson2JsonNode.wrap(delegate.next());
            }

            @Override
            public void remove() {
                delegate.remove();
            }
        };
    }

    @Override
    public void set(int index, String value) {
        jsonNode.set(index, value);
    }

    @Override
    public void set(int index, Boolean value) {
        jsonNode.set(index, value);
    }

    @Override
    public void set(int index, Short value) {
        jsonNode.set(index, value);
    }

    @Override
    public void set(int index, Integer value) {
        jsonNode.set(index, value);
    }

    @Override
    public void set(int index, Long value) {
        jsonNode.set(index, value);
    }

    @Override
    public void set(int index, Double value) {
        jsonNode.set(index, value);
    }

    @Override
    public void set(int index, BigDecimal value) {
        jsonNode.set(index, value);
    }

    @Override
    public void set(int index, BigInteger value) {
        jsonNode.set(index, value);
    }

    @Override
    public void setNull(int index) {
        jsonNode.setNull(index);
    }

    @Override
    public void set(int index, FlowableJsonNode value) {
        jsonNode.set(index, asJsonNode(value));
    }

    @Override
    public void add(Short value) {
        jsonNode.add(value);
    }

    @Override
    public void add(Integer value) {
        jsonNode.add(value);
    }

    @Override
    public void add(Long value) {
        jsonNode.add(value);
    }

    @Override
    public void add(Float value) {
        jsonNode.add(value);
    }

    @Override
    public void add(Double value) {
        jsonNode.add(value);
    }

    @Override
    public void add(byte[] value) {
        jsonNode.add(value);
    }

    @Override
    public void add(String value) {
        jsonNode.add(value);
    }

    @Override
    public void add(Boolean value) {
        jsonNode.add(value);
    }

    @Override
    public void add(BigDecimal value) {
        jsonNode.add(value);
    }

    @Override
    public void add(BigInteger value) {
        jsonNode.add(value);
   }

    @Override
    public void add(FlowableJsonNode value) {
        jsonNode.add(asJsonNode(value));
    }

    @Override
    public void addNull() {
        jsonNode.addNull();
    }
}
