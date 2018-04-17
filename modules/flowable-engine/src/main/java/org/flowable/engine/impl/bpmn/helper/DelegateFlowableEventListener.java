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
package org.flowable.engine.impl.bpmn.helper;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.util.ReflectUtil;

/**
 * An {@link FlowableEventListener} implementation which uses a classname to create a delegate {@link FlowableEventListener} instance to use for event notification. <br>
 * <br>
 * 
 * In case an entityClass was passed in the constructor, only events that are {@link FlowableEntityEvent}'s that target an entity of the given type, are dispatched to the delegate.
 * 
 * @author Frederik Heremans
 */
public class DelegateFlowableEventListener extends BaseDelegateEventListener {

    protected String className;
    protected FlowableEventListener delegateInstance;
    protected boolean failOnException;

    public DelegateFlowableEventListener(String className, Class<?> entityClass) {
        this.className = className;
        setEntityClass(entityClass);
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (isValidEvent(event)) {
            getDelegateInstance().onEvent(event);
        }
    }

    @Override
    public boolean isFailOnException() {
        if (delegateInstance != null) {
            return delegateInstance.isFailOnException();
        }
        return failOnException;
    }

    protected FlowableEventListener getDelegateInstance() {
        if (delegateInstance == null) {
            Object instance = ReflectUtil.instantiate(className);
            if (instance instanceof FlowableEventListener) {
                delegateInstance = (FlowableEventListener) instance;
            } else {
                // Force failing of the listener invocation, since the delegate
                // cannot be created
                failOnException = true;
                throw new FlowableIllegalArgumentException("Class " + className + " does not implement " + FlowableEventListener.class.getName());
            }
        }
        return delegateInstance;
    }
}
