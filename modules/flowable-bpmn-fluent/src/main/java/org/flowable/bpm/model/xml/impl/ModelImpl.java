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
package org.flowable.bpm.model.xml.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.flowable.bpm.model.xml.Model;
import org.flowable.bpm.model.xml.impl.util.ModelUtil;
import org.flowable.bpm.model.xml.impl.util.QName;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;

/**
 * A model contains all defined types and the relationship between them.
 */
public class ModelImpl
        implements Model {

    private final Map<QName, ModelElementType> typesByName = new HashMap<>();
    private final Map<Class<? extends ModelElementInstance>, ModelElementType> typesByClass = new HashMap<>();
    private final String modelName;

    protected final Map<String, String> actualNsToAlternative = new HashMap<>();
    protected final Map<String, String> alternativeNsToActual = new HashMap<>();

    /**
     * Create a new {@link Model} with a model name.
     * 
     * @param modelName the model name to identify the model
     */
    public ModelImpl(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Declares an alternative namespace for an actual so that during lookup of elements/attributes both will be considered. This can be used if a
     * newer namespaces replaces an older one but XML files with the old one should still be parseable.
     * 
     * @param alternativeNs
     * @param actualNs
     * @throws IllegalArgumentException if the alternative is already used or if the actual namespace has an alternative
     */
    public void declareAlternativeNamespace(String alternativeNs, String actualNs) {
        if (actualNsToAlternative.containsKey(actualNs) || alternativeNsToActual.containsKey(alternativeNs)) {
            throw new IllegalArgumentException(
                    "Cannot register two alternatives for one namespace! Actual Ns: " + actualNs + " second alternative: " + alternativeNs);
        }
        actualNsToAlternative.put(actualNs, alternativeNs);
        alternativeNsToActual.put(alternativeNs, actualNs);
    }

    public void undeclareAlternativeNamespace(String alternativeNs) {
        if (!alternativeNsToActual.containsKey(alternativeNs)) {
            return;
        }
        String actual = alternativeNsToActual.remove(alternativeNs);
        actualNsToAlternative.remove(actual);
    }

    @Override
    public String getAlternativeNamespace(String actualNs) {
        return actualNsToAlternative.get(actualNs);
    }

    @Override
    public String getActualNamespace(String alternativeNs) {
        return alternativeNsToActual.get(alternativeNs);
    }

    @Override
    public Collection<ModelElementType> getTypes() {
        return new ArrayList<>(typesByName.values());
    }

    @Override
    public ModelElementType getType(Class<? extends ModelElementInstance> instanceClass) {
        return typesByClass.get(instanceClass);
    }

    @Override
    public ModelElementType getTypeForName(String typeName) {
        return getTypeForName(null, typeName);
    }

    @Override
    public ModelElementType getTypeForName(String namespaceUri, String typeName) {
        return typesByName.get(ModelUtil.getQName(namespaceUri, typeName));
    }

    /**
     * Registers a {@link ModelElementType} in this {@link Model}.
     *
     * @param modelElementType the element type to register
     * @param instanceType the instance class of the type to register
     */
    public void registerType(ModelElementType modelElementType, Class<? extends ModelElementInstance> instanceType) {
        QName qName = ModelUtil.getQName(modelElementType.getTypeNamespace(), modelElementType.getTypeName());
        typesByName.put(qName, modelElementType);
        typesByClass.put(instanceType, modelElementType);
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + ((modelName == null) ? 0 : modelName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ModelImpl other = (ModelImpl) obj;
        if (modelName == null) {
            if (other.modelName != null) {
                return false;
            }
        } else if (!modelName.equals(other.modelName)) {
            return false;
        }
        return true;
    }

}
