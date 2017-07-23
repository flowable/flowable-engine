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
package org.flowable.engine.impl.bpmn.listener;

import org.flowable.engine.common.api.delegate.event.FlowableEvent;

import java.util.Map;

/**
 * @author Yvo Swillens
 */
@Deprecated
public class TransactionDependentEventListenerExecutionScope {

    protected final String processInstanceId;
    protected final String executionId;
    protected final FlowableEvent flowEvent;
    protected final Map<String, Object> executionVariables;
    protected final Map<String, Object> customPropertiesMap;

    public TransactionDependentEventListenerExecutionScope(String processInstanceId, String executionId,
                                                           FlowableEvent flowableEvent, Map<String, Object> executionVariables,
                                                           Map<String, Object> customPropertiesMap) {
        this.processInstanceId = processInstanceId;
        this.executionId = executionId;
        this.flowEvent = flowableEvent;
        this.executionVariables = executionVariables;
        this.customPropertiesMap = customPropertiesMap;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public FlowableEvent getFlowEvent() {
        return flowEvent;
    }

    public Map<String, Object> getExecutionVariables() {
        return executionVariables;
    }

    public Map<String, Object> getCustomPropertiesMap() {
        return customPropertiesMap;
    }
}
