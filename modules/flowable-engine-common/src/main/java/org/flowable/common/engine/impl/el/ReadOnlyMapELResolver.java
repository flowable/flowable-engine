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

package org.flowable.common.engine.impl.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;

/**
 * An {@link ELResolver} that exposed object values in the map, under the name of the entry's key. The values in the map are only returned when requested property has no 'base', meaning it's a
 * root-object.
 * 
 * @author Frederik Heremans
 */
public class ReadOnlyMapELResolver extends ELResolver {

    protected Map<Object, Object> wrappedMap;

    public ReadOnlyMapELResolver(Map<Object, Object> map) {
        this.wrappedMap = map;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (base == null) {
            if (wrappedMap.containsKey(property)) {
                context.setPropertyResolved(true);
                return wrappedMap.get(property);
            }
        }
        return null;
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return true;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        if (base == null) {
            if (wrappedMap.containsKey(property)) {
                throw new FlowableException("Cannot set value of '" + property + "', it's readonly!");
            }
        }
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object arg) {
        return Object.class;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object arg) {
        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object arg1, Object arg2) {
        return Object.class;
    }
}
