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

import org.flowable.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.flowable.bpm.model.xml.instance.DomDocument;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.validation.ModelElementValidator;
import org.flowable.bpm.model.xml.validation.ValidationResults;

import java.util.Collection;

/**
 * An instance of a model.
 */
public interface ModelInstance {

    /**
     * Returns the wrapped {@link DomDocument}.
     *
     * @return the DOM document
     */
    DomDocument getDocument();

    /**
     * Returns the {@link ModelElementInstanceImpl ModelElement} corresponding to the document element of this model or null if no document element
     * exists.
     *
     * @return the document element or null
     */
    ModelElementInstance getDocumentElement();

    /**
     * Updates the document element.
     *
     * @param documentElement the new document element to set
     */
    void setDocumentElement(ModelElementInstance documentElement);

    /**
     * Creates a new instance of type class.
     *
     * @param type the class of the type to create
     * @param <T> instance type
     * @return the new created instance
     */
    <T extends ModelElementInstance> T newInstance(Class<T> type);

    /**
     * Creates a new instance of type.
     *
     * @param type the type to create
     * @param <T> instance type
     * @return the new created instance
     */
    <T extends ModelElementInstance> T newInstance(ModelElementType type);

    /**
     * Returns the underlying model.
     *
     * @return the model
     */
    Model getModel();

    /**
     * Find a unique element of the model by id.
     *
     * @param id the id of the element
     * @return the element with the id or null
     */
    <T extends ModelElementInstance> T getModelElementById(String id);

    /**
     * Find all elements of a type.
     *
     * @param referencingType the type of the elements
     * @return the collection of elements of the type
     */
    Collection<ModelElementInstance> getModelElementsByType(ModelElementType referencingType);

    /**
     * Find all elements of a type.
     *
     * @param referencingClass the type class of the elements
     * @return the collection of elements of the type
     */
    <T extends ModelElementInstance> Collection<T> getModelElementsByType(Class<T> referencingClass);

    /**
     * Copies the model instance but not the model. So only the wrapped DOM document is cloned. Changes of the model are persistent between multiple
     * model instances.
     *
     * @return the new model instance
     */
    ModelInstance clone();

    /**
     * Validate semantic properties of this model instance using a collection of validators. ModelElementValidator is an SPI that can be implemented
     * by the user to execute custom validation logic on the model. The validation results are collected into a {@link ValidationResults} object which
     * is returned by this method.
     *
     * @param validators the validators to execute
     * @return the results of the validation.
     * @since 7.6
     */
    ValidationResults validate(Collection<ModelElementValidator<?>> validators);

}
