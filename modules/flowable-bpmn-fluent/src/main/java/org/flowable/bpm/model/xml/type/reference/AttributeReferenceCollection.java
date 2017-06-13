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
package org.flowable.bpm.model.xml.type.reference;

import org.flowable.bpm.model.xml.ModelException;
import org.flowable.bpm.model.xml.UnsupportedModelOperationException;
import org.flowable.bpm.model.xml.impl.ModelInstanceImpl;
import org.flowable.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.flowable.bpm.model.xml.impl.type.attribute.AttributeImpl;
import org.flowable.bpm.model.xml.impl.type.reference.AttributeReferenceImpl;
import org.flowable.bpm.model.xml.impl.util.ModelUtil;
import org.flowable.bpm.model.xml.impl.util.StringUtil;
import org.flowable.bpm.model.xml.instance.DomDocument;
import org.flowable.bpm.model.xml.instance.DomElement;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class AttributeReferenceCollection<T extends ModelElementInstance>
        extends AttributeReferenceImpl<T> {

    protected String separator = " ";

    public AttributeReferenceCollection(AttributeImpl<String> referenceSourceAttribute) {
        super(referenceSourceAttribute);
    }

    @Override
    protected void updateReference(ModelElementInstance referenceSourceElement, String oldIdentifier, String newIdentifier) {
        String referencingIdentifier = getReferenceIdentifier(referenceSourceElement);
        List<String> references = StringUtil.splitListBySeparator(referencingIdentifier, separator);
        if (oldIdentifier != null && references.contains(oldIdentifier)) {
            // TODO: this looks wrong; where is referencingIdentifier used?
            referencingIdentifier = referencingIdentifier.replace(oldIdentifier, newIdentifier);
            setReferenceIdentifier(referenceSourceElement, newIdentifier);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void removeReference(ModelElementInstance referenceSourceElement, ModelElementInstance referenceTargetElement) {
        String identifier = getReferenceIdentifier(referenceSourceElement);
        List<String> references = StringUtil.splitListBySeparator(identifier, separator);
        String identifierToRemove = getTargetElementIdentifier((T) referenceTargetElement);
        references.remove(identifierToRemove);
        identifier = StringUtil.joinList(references, separator);
        setReferenceIdentifier(referenceSourceElement, identifier);
    }

    protected abstract String getTargetElementIdentifier(T referenceTargetElement);

    private Collection<DomElement> getView(ModelElementInstance referenceSourceElement) {
        DomDocument document = referenceSourceElement.getModelInstance().getDocument();

        String identifier = getReferenceIdentifier(referenceSourceElement);
        List<String> references = StringUtil.splitListBySeparator(identifier, separator);

        Collection<DomElement> referenceTargetElements = new ArrayList<>();
        for (String reference : references) {
            DomElement referenceTargetElement = document.getElementById(reference);
            if (referenceTargetElement != null) {
                referenceTargetElements.add(referenceTargetElement);
            } else {
                throw new ModelException("Unable to find a model element instance for id " + identifier);
            }
        }
        return referenceTargetElements;
    }

    public Collection<T> getReferenceTargetElements(final ModelElementInstance referenceSourceElement) {

        return new Collection<T>() {

            @Override
            public int size() {
                return getView(referenceSourceElement).size();
            }

            @Override
            public boolean isEmpty() {
                return getView(referenceSourceElement).isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                if (!(o instanceof ModelElementInstanceImpl)) {
                    return false;
                }
                return getView(referenceSourceElement).contains(((ModelElementInstanceImpl) o).getDomElement());
            }

            @Override
            public Iterator<T> iterator() {
                Collection<T> modelElementCollection =
                        ModelUtil.getModelElementCollection(getView(referenceSourceElement), (ModelInstanceImpl) referenceSourceElement.getModelInstance());
                return modelElementCollection.iterator();
            }

            @Override
            public Object[] toArray() {
                Collection<T> modelElementCollection =
                        ModelUtil.getModelElementCollection(getView(referenceSourceElement), (ModelInstanceImpl) referenceSourceElement.getModelInstance());
                return modelElementCollection.toArray();
            }

            @Override
            public <T1> T1[] toArray(T1[] a) {
                Collection<T> modelElementCollection =
                        ModelUtil.getModelElementCollection(getView(referenceSourceElement), (ModelInstanceImpl) referenceSourceElement.getModelInstance());
                return modelElementCollection.toArray(a);
            }

            @Override
            public boolean add(T t) {
                if (!contains(t)) {
                    performAddOperation(referenceSourceElement, t);
                }
                return true;
            }

            @Override
            public boolean remove(Object o) {
                ModelUtil.ensureInstanceOf(o, ModelElementInstanceImpl.class);
                performRemoveOperation(referenceSourceElement, o);
                return true;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                Collection<T> modelElementCollection =
                        ModelUtil.getModelElementCollection(getView(referenceSourceElement), (ModelInstanceImpl) referenceSourceElement.getModelInstance());
                return modelElementCollection.containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends T> c) {
                boolean result = false;
                for (T o : c) {
                    result |= add(o);
                }
                return result;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                boolean result = false;
                for (Object o : c) {
                    result |= remove(o);
                }
                return result;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedModelOperationException("retainAll()", "not implemented");
            }

            @Override
            public void clear() {
                performClearOperation(referenceSourceElement);
            }
        };

    }

    protected void performClearOperation(ModelElementInstance referenceSourceElement) {
        setReferenceIdentifier(referenceSourceElement, "");
    }

    @Override
    protected void setReferenceIdentifier(ModelElementInstance referenceSourceElement, String referenceIdentifier) {
        if (referenceIdentifier != null && !referenceIdentifier.isEmpty()) {
            super.setReferenceIdentifier(referenceSourceElement, referenceIdentifier);
        } else {
            referenceSourceAttribute.removeAttribute(referenceSourceElement);
        }
    }

    /**
     * @param referenceSourceElement
     * @param o
     */
    protected void performRemoveOperation(ModelElementInstance referenceSourceElement, Object o) {
        removeReference(referenceSourceElement, (ModelElementInstance) o);
    }

    protected void performAddOperation(ModelElementInstance referenceSourceElement, T referenceTargetElement) {
        String identifier = getReferenceIdentifier(referenceSourceElement);
        List<String> references = StringUtil.splitListBySeparator(identifier, separator);

        String targetIdentifier = getTargetElementIdentifier(referenceTargetElement);
        references.add(targetIdentifier);

        identifier = StringUtil.joinList(references, separator);

        setReferenceIdentifier(referenceSourceElement, identifier);
    }


}
