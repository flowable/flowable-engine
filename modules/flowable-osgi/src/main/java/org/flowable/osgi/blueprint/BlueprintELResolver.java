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
package org.flowable.osgi.blueprint;

import java.beans.FeatureDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see org.flowable.spring.ApplicationContextElResolver
 */
public class BlueprintELResolver extends ELResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintELResolver.class);
    private Map<String, JavaDelegate> delegateMap = new HashMap<>();
    private Map<String, ActivityBehavior> activityBehaviourMap = new HashMap<>();

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (base == null) {
            // according to javadoc, can only be a String
            String key = (String) property;
            for (String name : delegateMap.keySet()) {
                if (name.equalsIgnoreCase(key)) {
                    context.setPropertyResolved(true);
                    return delegateMap.get(name);
                }
            }
            for (String name : activityBehaviourMap.keySet()) {
                if (name.equalsIgnoreCase(key)) {
                    context.setPropertyResolved(true);
                    return activityBehaviourMap.get(name);
                }
            }
        }

        return null;
    }

    @SuppressWarnings("rawtypes")
    public void bindService(JavaDelegate delegate, Map props) {
        String name = (String) props.get("osgi.service.blueprint.compname");
        delegateMap.put(name, delegate);
        LOGGER.info("added Flowable service to delegate cache {}", name);
    }

    @SuppressWarnings("rawtypes")
    public void unbindService(JavaDelegate delegate, Map props) {
        String name = (String) props.get("osgi.service.blueprint.compname");
        if (delegateMap.containsKey(name)) {
            delegateMap.remove(name);
        }
        LOGGER.info("removed Flowable service from delegate cache {}", name);
    }

    @SuppressWarnings("rawtypes")
    public void bindActivityBehaviourService(ActivityBehavior delegate, Map props) {
        String name = (String) props.get("osgi.service.blueprint.compname");
        activityBehaviourMap.put(name, delegate);
        LOGGER.info("added Flowable service to activity behaviour cache {}", name);
    }

    @SuppressWarnings("rawtypes")
    public void unbindActivityBehaviourService(ActivityBehavior delegate, Map props) {
        String name = (String) props.get("osgi.service.blueprint.compname");
        if (activityBehaviourMap.containsKey(name)) {
            activityBehaviourMap.remove(name);
        }
        LOGGER.info("removed Flowable service from activity behaviour cache {}", name);
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return true;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
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
