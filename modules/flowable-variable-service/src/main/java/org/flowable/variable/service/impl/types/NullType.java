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
package org.flowable.variable.service.impl.types;

import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

/**
 * @author Tom Baeyens
 */
public class NullType implements VariableType {

    public static final String TYPE_NAME = "null";

    private static final long serialVersionUID = 1L;

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        return null;
    }

    @Override
    public boolean isAbleToStore(Object value) {
        return (value == null);
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
    }
}
