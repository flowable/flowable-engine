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
package org.flowable.bpm.model.xml;

import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;

import java.util.Collection;

/**
 * A model contains all defined types and the relationship between them. See {@link ModelBuilder#createInstance} to create a new model.
 */
public interface Model {

    /**
     * Gets the collection of all {@link ModelElementType} defined in the model.
     *
     * @return the list of all defined element types of this model
     */
    Collection<ModelElementType> getTypes();

    /**
     * Gets the defined {@link ModelElementType} of a {@link ModelElementInstance}.
     *
     * @param instanceClass the instance class to find the type for
     * @return the corresponding element type or null if no type is defined for the instance
     */
    ModelElementType getType(Class<? extends ModelElementInstance> instanceClass);

    /**
     * Gets the defined {@link ModelElementType} for a type by its name.
     *
     * @param typeName the name of the type
     * @return the element type or null if no type is defined for the name
     */
    ModelElementType getTypeForName(String typeName);

    /**
     * Gets the defined {@link ModelElementType} for a type by its name and namespace URI.
     *
     *
     * @param namespaceUri the namespace URI for the type
     * @param typeName the name of the type
     * @return the element type or null if no type is defined for the name and namespace URI
     */
    ModelElementType getTypeForName(String namespaceUri, String typeName);

    /**
     * Returns the model name, which is the identifier of this model.
     *
     * @return the model name
     */
    String getModelName();

    /**
     * Returns the actual namespace URI for an alternative namespace URI
     * 
     * @param alternativeNs the alternative namespace URI
     * @return the actual namespace URI or null if none is set
     */
    String getActualNamespace(String alternativeNs);

    /**
     * Returns the alternative namespace URI for a namespace URI
     * 
     * @param actualNs the actual namespace URI
     * @return the alternative namespace URI or null if none is set
     */
    String getAlternativeNamespace(String actualNs);

}
