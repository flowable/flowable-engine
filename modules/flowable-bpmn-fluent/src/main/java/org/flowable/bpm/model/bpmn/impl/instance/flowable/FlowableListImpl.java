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
package org.flowable.bpm.model.bpmn.impl.instance.flowable;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_LIST;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableList;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.UnsupportedModelOperationException;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.impl.util.ModelUtil;
import org.flowable.bpm.model.xml.instance.DomElement;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class FlowableListImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableList {

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableList.class, FLOWABLE_ELEMENT_LIST)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableList>() {
                    @Override
                    public FlowableList newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableListImpl(instanceContext);
                    }
                });

        typeBuilder.build();
    }

    public FlowableListImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BpmnModelElementInstance> Collection<T> getValues() {

        return new Collection<T>() {

            protected Collection<T> getElements() {
                return ModelUtil.getModelElementCollection(getDomElement().getChildElements(), getModelInstance());
            }

            @Override
            public int size() {
                return getElements().size();
            }

            @Override
            public boolean isEmpty() {
                return getElements().isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return getElements().contains(o);
            }

            @Override
            public Iterator<T> iterator() {
                return getElements().iterator();
            }

            @Override
            public Object[] toArray() {
                return getElements().toArray();
            }

            @Override
            public <T1> T1[] toArray(T1[] a) {
                return getElements().toArray(a);
            }

            @Override
            public boolean add(T t) {
                getDomElement().appendChild(t.getDomElement());
                return true;
            }

            @Override
            public boolean remove(Object o) {
                ModelUtil.ensureInstanceOf(o, BpmnModelElementInstance.class);
                return getDomElement().removeChild(((BpmnModelElementInstance) o).getDomElement());
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                for (Object o : c) {
                    if (!contains(o)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean addAll(Collection<? extends T> c) {
                for (T element : c) {
                    add(element);
                }
                return true;
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
                DomElement domElement = getDomElement();
                List<DomElement> childElements = domElement.getChildElements();
                for (DomElement childElement : childElements) {
                    domElement.removeChild(childElement);
                }
            }
        };
    }

}
