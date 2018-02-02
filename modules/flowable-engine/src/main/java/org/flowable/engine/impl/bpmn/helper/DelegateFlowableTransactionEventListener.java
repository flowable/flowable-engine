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

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.common.api.delegate.event.TransactionFlowableEventListener;
import org.flowable.engine.common.impl.util.ReflectUtil;

/**
 * An {@link FlowableEventListener} implementation which uses a classname to create a delegate
 * {@link FlowableEventListener} instance to use for event notification. <br> <br>
 *
 * In case an entityClass was passed in the constructor, only events that are {@link
 * FlowableEntityEvent}'s that target an entity of the given type, are dispatched to the delegate.
 *
 * @author Frederik Heremans
 */
public class DelegateFlowableTransactionEventListener extends BaseDelegateTransactionEventListener {

    protected String className;
    protected TransactionFlowableEventListener delegateInstance;
    protected String onTransaction;
    protected boolean failOnException;

    public DelegateFlowableTransactionEventListener(String className, Class<?> entityClass, String transaction) {
        this.className = className;
        this.onTransaction = transaction;
        setEntityClass(entityClass);
    }
    
    @Override
    public void onEvent(FlowableEvent event) {
        if (isValidEvent(event)) {
            getDelegateInstance().onEvent(event);
        }
    }

    @Override
    public String getOnTransaction() {
        return onTransaction;
    }

    @Override
    public void setOnTransaction(String onTransaction) {
        this.onTransaction = onTransaction;
    }

    @Override
    public boolean isFailOnException() {
        if (delegateInstance != null) {
            return delegateInstance.isFailOnException();
        }
        return failOnException;
    }

    protected TransactionFlowableEventListener getDelegateInstance() {
        if (delegateInstance == null) {
            Object instance = ReflectUtil.instantiate(className);
            if (instance instanceof TransactionFlowableEventListener) {
                delegateInstance = (TransactionFlowableEventListener) instance;
            } else {
                // Force failing of the listener invocation, since the delegate
                // cannot be created
                failOnException = true;
                throw new FlowableIllegalArgumentException("Class " + className + " does not implement " + TransactionFlowableEventListener.class.getName());
            }
        }
        return delegateInstance;
    }
}
