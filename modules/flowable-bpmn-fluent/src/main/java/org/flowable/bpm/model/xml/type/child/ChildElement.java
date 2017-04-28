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
package org.flowable.bpm.model.xml.type.child;

import org.flowable.bpm.model.xml.instance.ModelElementInstance;

import java.util.Collection;

/**
 * A single child element (child Element collection where {@link ChildElementCollection#getMaxOccurs()} returns 1.
 *
 * The {@link Collection#add(Object)} operation provided by this collection has special behavior: it will replace an existing element if it exists.
 *
 * @param <T> the type of the child element
 */
public interface ChildElement<T extends ModelElementInstance>
        extends ChildElementCollection<T> {

    /**
     * Sets the child element, potentially replacing an existing child element.
     *
     * @param element the parent element of the child element
     * @param newChildElement the new child element to set
     */
    void setChild(ModelElementInstance element, T newChildElement);

    /**
     * Returns the child element.
     *
     * @param element the parent element of the child element
     * @return the child element of the parent, or null if not exist
     */
    T getChild(ModelElementInstance element);

    /**
     * Removes the child element.
     *
     * @param element the parent element of the child element
     * @return true if the child was remove otherwise false
     */
    boolean removeChild(ModelElementInstance element);
}
