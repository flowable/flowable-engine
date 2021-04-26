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

import java.util.Collection;
import java.util.Collections;

import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

/**
 * @author Filip Hrisafov
 */
public class EmptyCollectionType implements VariableType {

    public static final String TYPE_NAME = "emptyCollection";

    private static final String SET = "set";
    private static final String LIST = "list";
    private static final Class<?> EMPTY_LIST_CLASS = Collections.emptyList().getClass();
    private static final Class<?> EMPTY_SET_CLASS = Collections.emptySet().getClass();

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    @Override
    public boolean isAbleToStore(Object value) {
        if (value instanceof Collection) {
            return EMPTY_LIST_CLASS.isInstance(value) || EMPTY_SET_CLASS.isInstance(value);
        }

        return false;
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        if (EMPTY_LIST_CLASS.isInstance(value)) {
            valueFields.setTextValue("list");
        } else {
            valueFields.setTextValue("set");
        }
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        String value = valueFields.getTextValue();
        if (LIST.equals(value)) {
            return Collections.emptyList();
        } else if (SET.equals(value)) {
            return Collections.emptySet();
        }

        return null;
    }
}
