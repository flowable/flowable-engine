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
package org.flowable.bpm.model.xml.impl.type.child;

import org.flowable.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.flowable.bpm.model.xml.impl.type.ModelElementTypeImpl;
import org.flowable.bpm.model.xml.impl.util.ModelUtil;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.child.ChildElement;

/**
 * Represents a single Child Element (ie. maxOccurs = 1);
 */
public class ChildElementImpl<T extends ModelElementInstance>
        extends ChildElementCollectionImpl<T>
        implements ChildElement<T> {

    public ChildElementImpl(Class<T> childElementTypeChild, ModelElementTypeImpl parentElementType) {
        super(childElementTypeChild, parentElementType);
        this.maxOccurs = 1;
    }

    /** the add operation replaces the child */
    private void performAddOperation(ModelElementInstanceImpl modelElement, T e) {
        modelElement.setUniqueChildElementByNameNs(e);
    }

    @Override
    public void setChild(ModelElementInstance element, T newChildElement) {
        performAddOperation((ModelElementInstanceImpl) element, newChildElement);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getChild(ModelElementInstance element) {
        ModelElementInstanceImpl elementInstanceImpl = (ModelElementInstanceImpl) element;

        ModelElementInstance childElement = elementInstanceImpl.getUniqueChildElementByType(childElementTypeClass);
        if (childElement != null) {
            ModelUtil.ensureInstanceOf(childElement, childElementTypeClass);
            return (T) childElement;
        } else {
            return null;
        }
    }

    @Override
    public boolean removeChild(ModelElementInstance element) {
        ModelElementInstanceImpl childElement = (ModelElementInstanceImpl) getChild(element);
        ModelElementInstanceImpl elementInstanceImpl = (ModelElementInstanceImpl) element;
        return elementInstanceImpl.removeChildElement(childElement);
    }
}
