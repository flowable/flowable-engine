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
package org.flowable.engine.test.impl.logger;

import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.BaseEntityEventListener;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 * @author jbarrez
 */
public class DebugInfoEntityEventListener extends BaseEntityEventListener {

    protected ProcessExecutionLogger processExecutionLogger;

    public DebugInfoEntityEventListener(ProcessExecutionLogger processExecutionLogger) {
        this.processExecutionLogger = processExecutionLogger;
    }

    @Override
    protected void onCreate(FlowableEvent event) {
        ExecutionEntity executionEntity = getExecutionEntity(event);
        if (executionEntity != null) {
            processExecutionLogger.executionCreated(executionEntity);
            processExecutionLogger.addDebugInfo(new DebugInfoExecutionCreated(executionEntity));
        }
    }

    @Override
    protected void onDelete(FlowableEvent event) {
        ExecutionEntity executionEntity = getExecutionEntity(event);
        if (executionEntity != null) {
            processExecutionLogger.executionDeleted(executionEntity);
            processExecutionLogger.addDebugInfo(new DebugInfoExecutionDeleted(executionEntity));
        }
    }

    protected ExecutionEntity getExecutionEntity(FlowableEvent event) {
        FlowableEntityEvent entityEvent = (FlowableEntityEvent) event;
        Object entity = entityEvent.getEntity();
        if (entity instanceof ExecutionEntity) {
            ExecutionEntity executionEntity = (ExecutionEntity) entity;
            return executionEntity;
        }
        return null;
    }

}
