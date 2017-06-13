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
package org.flowable.bpm.model.xml.impl.type.attribute;

import org.flowable.bpm.model.xml.type.ModelElementType;

public class NamedEnumAttribute<T extends Enum<T>>
        extends AttributeImpl<T> {

    protected final Class<T> type;

    public NamedEnumAttribute(ModelElementType owningElementType, Class<T> type) {
        super(owningElementType);
        this.type = type;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T convertXmlValueToModelValue(String rawValue) {
        T[] enumConstants = type.getEnumConstants();
        if (rawValue != null && enumConstants != null) {
            for (T enumConstant : enumConstants) {
                if (rawValue.equals(enumConstant.toString())) {
                    return enumConstant;
                }
            }
        }
        return null;
    }

    @Override
    protected String convertModelValueToXmlValue(T modelValue) {
        return modelValue.toString();
    }

}
