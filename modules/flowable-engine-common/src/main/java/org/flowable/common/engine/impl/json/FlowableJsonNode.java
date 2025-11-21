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

/**
 * This is a Flowable JSON implementation that is for internal use by Flowable to support both Jackson 2 and Jackson 3
 *
 * @author Filip Hrisafov
 */
public interface FlowableJsonNode {

    Object getImplementationValue();

    String asString();

    boolean isValueNode();

    boolean isNull();

    boolean isString();

    boolean isLong();

    boolean isDouble();

    boolean isFloat();

    boolean isInt();

    boolean isShort();

    boolean isBoolean();

    boolean isNumber();

    boolean isBigDecimal();

    long longValue();

    double doubleValue();

    int intValue();

    boolean booleanValue();

    FlowableJsonNode get(String propertyName);

    FlowableJsonNode get(int index);

    FlowableJsonNode path(int index);

    int size();
}
