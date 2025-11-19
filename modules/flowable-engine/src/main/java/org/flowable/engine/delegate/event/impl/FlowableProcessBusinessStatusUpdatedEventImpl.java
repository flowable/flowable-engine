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
package org.flowable.engine.delegate.event.impl;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.event.FlowableEngineEventImpl;
import org.flowable.engine.delegate.event.FlowableProcessBusinessStatusUpdatedEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

public class FlowableProcessBusinessStatusUpdatedEventImpl extends FlowableEngineEventImpl implements FlowableProcessBusinessStatusUpdatedEvent {
    protected ExecutionEntity execution;
    protected String oldBusinessStatus;
    protected String newBusinessStatus;

    public FlowableProcessBusinessStatusUpdatedEventImpl(ExecutionEntity execution, String oldBusinessStatus, String newBusinessStatus) {
        super(FlowableEngineEventType.BUSINESS_STATUS_UPDATED, ScopeTypes.BPMN, execution.getProcessInstanceId(), null, execution.getProcessDefinitionId());
        this.execution = execution;
        this.oldBusinessStatus = oldBusinessStatus;
        this.newBusinessStatus = newBusinessStatus;
    }

    @Override
    public ExecutionEntity getEntity() {
        return execution;
    }

    @Override
    public String getOldBusinessStatus() {
        return oldBusinessStatus;
    }

    @Override
    public String getNewBusinessStatus() {
        return newBusinessStatus;
    }
}
