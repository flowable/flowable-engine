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
package org.flowable.engine.impl.bpmn.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a simple in memory structure
 * 
 * @author Esteban Robles Luna
 */
public class SimpleStructureDefinition implements FieldBaseStructureDefinition {

    protected String id;

    protected List<String> fieldNames;

    protected List<Class<?>> fieldTypes;

    protected List<Class<?>> fieldParameterTypes;

    public SimpleStructureDefinition(String id) {
        this.id = id;
        this.fieldNames = new ArrayList<>();
        this.fieldTypes = new ArrayList<>();
        this.fieldParameterTypes = new ArrayList<>();
    }

    @Override
    public int getFieldSize() {
        return this.fieldNames.size();
    }

    @Override
    public String getId() {
        return this.id;
    }

    public void setFieldName(int index, String fieldName, Class<?> type, Class<?> parameterType) {
        this.growListToContain(index, this.fieldNames);
        this.growListToContain(index, this.fieldTypes);
        this.growListToContain(index, this.fieldParameterTypes);
        this.fieldNames.set(index, fieldName);
        this.fieldTypes.set(index, type);
        this.fieldParameterTypes.set(index, parameterType);
    }

    private void growListToContain(int index, List<?> list) {
        if (!(list.size() - 1 >= index)) {
            for (int i = list.size(); i <= index; i++) {
                list.add(null);
            }
        }
    }

    @Override
    public String getFieldNameAt(int index) {
        return this.fieldNames.get(index);
    }

    @Override
    public Class<?> getFieldTypeAt(int index) {
        return this.fieldTypes.get(index);
    }

    @Override
    public Class<?> getFieldParameterTypeAt(int index) {
        return this.fieldParameterTypes.get(index);
    }

    @Override
    public StructureInstance createInstance() {
        return new FieldBaseStructureInstance(this);
    }
}
