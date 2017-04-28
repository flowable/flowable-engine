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

import org.flowable.bpm.model.xml.Model;
import org.flowable.bpm.model.xml.UnsupportedModelOperationException;
import org.flowable.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.flowable.bpm.model.xml.impl.type.ModelElementTypeImpl;
import org.flowable.bpm.model.xml.impl.util.ModelUtil;
import org.flowable.bpm.model.xml.instance.DomElement;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;

import java.util.Collection;
import java.util.Iterator;

/**
 * This collection is a view on an the children of a Model Element.
 */
public class ChildElementCollectionImpl<T extends ModelElementInstance>
        implements ChildElementCollection<T> {

    protected final Class<T> childElementTypeClass;

    /** the containing type of the collection */
    private final ModelElementType parentElementType;

    /** the minimal count of child elements in the collection */
    private int minOccurs;

    /**
     * the maximum count of child elements in the collection. An unbounded collection has a negative maxOccurs.
     */
    protected int maxOccurs = -1;

    /** indicates whether this collection is mutable. */
    private boolean isMutable = true;

    public ChildElementCollectionImpl(Class<T> childElementTypeClass, ModelElementTypeImpl parentElementType) {
        this.childElementTypeClass = childElementTypeClass;
        this.parentElementType = parentElementType;
    }

    public void setImmutable() {
        setMutable(false);
    }

    public void setMutable(boolean isMutable) {
        this.isMutable = isMutable;
    }

    @Override
    public boolean isImmutable() {
        return !isMutable;
    }

    // view /////////////////////////////////////////////////////////

    /**
     * Internal method providing access to the view represented by this collection.
     *
     * @return the view represented by this collection
     */
    private Collection<DomElement> getView(ModelElementInstanceImpl modelElement) {
        return modelElement.getDomElement().getChildElementsByType(modelElement.getModelInstance(), childElementTypeClass);
    }

    @Override
    public int getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    @Override
    public int getMaxOccurs() {
        return maxOccurs;
    }

    @Override
    public ModelElementType getChildElementType(Model model) {
        return model.getType(childElementTypeClass);
    }

    @Override
    public Class<T> getChildElementTypeClass() {
        return childElementTypeClass;
    }

    @Override
    public ModelElementType getParentElementType() {
        return parentElementType;
    }

    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    /** the "add" operation used by the collection */
    private void performAddOperation(ModelElementInstanceImpl modelElement, T e) {
        modelElement.addChildElement(e);
    }

    /** the "remove" operation used by this collection */
    private boolean performRemoveOperation(ModelElementInstanceImpl modelElement, Object e) {
        return modelElement.removeChildElement((ModelElementInstanceImpl) e);
    }

    /** the "clear" operation used by this collection */
    private void performClearOperation(ModelElementInstanceImpl modelElement, Collection<DomElement> elementsToRemove) {
        Collection<ModelElementInstance> modelElements = ModelUtil.getModelElementCollection(elementsToRemove, modelElement.getModelInstance());
        for (ModelElementInstance element : modelElements) {
            modelElement.removeChildElement(element);
        }
    }

    @Override
    public Collection<T> get(ModelElementInstance element) {

        final ModelElementInstanceImpl modelElement = (ModelElementInstanceImpl) element;

        return new Collection<T>() {

            @Override
            public boolean contains(Object o) {
                if (!(o instanceof ModelElementInstanceImpl)) {
                    return false;

                }
                return getView(modelElement).contains(((ModelElementInstanceImpl) o).getDomElement());
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                for (Object elementToCheck : c) {
                    if (!contains(elementToCheck)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean isEmpty() {
                return getView(modelElement).isEmpty();
            }

            @Override
            public Iterator<T> iterator() {
                Collection<T> modelElementCollection = ModelUtil.getModelElementCollection(getView(modelElement), modelElement.getModelInstance());
                return modelElementCollection.iterator();
            }

            @Override
            public Object[] toArray() {
                Collection<T> modelElementCollection = ModelUtil.getModelElementCollection(getView(modelElement), modelElement.getModelInstance());
                return modelElementCollection.toArray();
            }

            @Override
            public <U> U[] toArray(U[] a) {
                Collection<T> modelElementCollection = ModelUtil.getModelElementCollection(getView(modelElement), modelElement.getModelInstance());
                return modelElementCollection.toArray(a);
            }

            @Override
            public int size() {
                return getView(modelElement).size();
            }

            @Override
            public boolean add(T e) {
                if (!isMutable) {
                    throw new UnsupportedModelOperationException("add()", "collection is immutable");
                }
                performAddOperation(modelElement, e);
                return true;
            }

            @Override
            public boolean addAll(Collection<? extends T> c) {
                if (!isMutable) {
                    throw new UnsupportedModelOperationException("addAll()", "collection is immutable");
                }
                boolean result = false;
                for (T t : c) {
                    result |= add(t);
                }
                return result;
            }

            @Override
            public void clear() {
                if (!isMutable) {
                    throw new UnsupportedModelOperationException("clear()", "collection is immutable");
                }
                Collection<DomElement> view = getView(modelElement);
                performClearOperation(modelElement, view);
            }

            @Override
            public boolean remove(Object e) {
                if (!isMutable) {
                    throw new UnsupportedModelOperationException("remove()", "collection is immutable");
                }
                ModelUtil.ensureInstanceOf(e, ModelElementInstanceImpl.class);
                return performRemoveOperation(modelElement, e);
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                if (!isMutable) {
                    throw new UnsupportedModelOperationException("removeAll()", "collection is immutable");
                }
                boolean result = false;
                for (Object t : c) {
                    result |= remove(t);
                }
                return result;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedModelOperationException("retainAll()", "not implemented");
            }

        };
    }

}
