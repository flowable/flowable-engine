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

/**
 * Represents a structure encapsulated in a class
 * 
 * @author Esteban Robles Luna
 */
public class ClassStructureDefinition implements FieldBaseStructureDefinition {

    protected String id;

    protected Class<?> classStructure;

    public ClassStructureDefinition(Class<?> classStructure) {
        this(classStructure.getName(), classStructure);
    }

    public ClassStructureDefinition(String id, Class<?> classStructure) {
        this.id = id;
        this.classStructure = classStructure;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public int getFieldSize() {
        // TODO
        return 0;
    }

    @Override
    public String getFieldNameAt(int index) {
        // TODO
        return null;
    }

    @Override
    public Class<?> getFieldTypeAt(int index) {
        // TODO
        return null;
    }

    @Override
    public StructureInstance createInstance() {
        return new FieldBaseStructureInstance(this);
    }
}
