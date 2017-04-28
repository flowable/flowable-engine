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
package org.flowable.bpm.model.bpmn.impl;

import org.flowable.bpm.model.bpmn.BpmnModelException;
import org.flowable.bpm.model.bpmn.Query;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QueryImpl<T extends ModelElementInstance>
        implements Query<T> {

    private final Collection<T> collection;

    public QueryImpl(Collection<T> collection) {
        this.collection = collection;
    }

    public List<T> list() {
        return new ArrayList<>(collection);
    }

    public int count() {
        return collection.size();
    }

    @SuppressWarnings("unchecked")
    public <V extends ModelElementInstance> Query<V> filterByType(ModelElementType elementType) {
        Class<V> elementClass = (Class<V>) elementType.getInstanceType();
        return filterByType(elementClass);
    }

    @SuppressWarnings("unchecked")
    public <V extends ModelElementInstance> Query<V> filterByType(Class<V> elementClass) {
        List<V> filtered = new ArrayList<>();
        for (T instance : collection) {
            if (elementClass.isAssignableFrom(instance.getClass())) {
                filtered.add((V) instance);
            }
        }
        return new QueryImpl<>(filtered);
    }

    public T singleResult() {
        if (collection.size() == 1) {
            return collection.iterator().next();
        } else {
            throw new BpmnModelException("Collection expected to have <1> entry but has <" + collection.size() + '>');
        }
    }
}
