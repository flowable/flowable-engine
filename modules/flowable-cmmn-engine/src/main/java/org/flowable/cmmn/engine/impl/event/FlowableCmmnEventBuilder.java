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
package org.flowable.cmmn.engine.impl.event;

import org.flowable.cmmn.api.event.FlowableCaseEndedEvent;
import org.flowable.cmmn.api.event.FlowableCaseStageEndedEvent;
import org.flowable.cmmn.api.event.FlowableCaseStartedEvent;
import org.flowable.cmmn.api.event.FlowableCaseStageStartedEvent;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.impl.event.FlowableEntityEventImpl;
import org.flowable.task.api.Task;

/**
 * @author Filip Hrisafov
 * @author Micha Kiener
 */
public class FlowableCmmnEventBuilder {

    public static FlowableCaseStartedEvent createCaseStartedEvent(CaseInstance caseInstance) {
        return new FlowableCaseStartedEventImpl(caseInstance);
    }

    public static FlowableCaseEndedEvent createCaseEndedEvent(CaseInstance caseInstance, String endingState) {
        return new FlowableCaseEndedEventImpl(caseInstance, endingState);
    }

    public static FlowableCaseStageStartedEvent createStageStartedEvent(CaseInstance caseInstance, PlanItemInstance stageInstance) {
        return new FlowableCaseStageStartedEventImpl(caseInstance, stageInstance);
    }

    public static FlowableCaseStageEndedEvent createStageEndedEvent(CaseInstance caseInstance, PlanItemInstance stageInstance, String endingState) {
        return new FlowableCaseStageEndedEventImpl(caseInstance, stageInstance, endingState);
    }

    public static FlowableEntityEvent createTaskCreatedEvent(Task task) {
        FlowableEntityEventImpl event = new FlowableEntityEventImpl(task, FlowableEngineEventType.TASK_CREATED);

        event.setScopeId(task.getScopeId());
        event.setScopeDefinitionId(task.getScopeDefinitionId());
        event.setScopeType(task.getScopeType());
        event.setSubScopeId(task.getId());

        return event;
    }

    public static FlowableEntityEvent createTaskAssignedEvent(Task task) {
        FlowableEntityEventImpl event = new FlowableEntityEventImpl(task, FlowableEngineEventType.TASK_ASSIGNED);

        event.setScopeId(task.getScopeId());
        event.setScopeDefinitionId(task.getScopeDefinitionId());
        event.setScopeType(task.getScopeType());
        event.setSubScopeId(task.getId());

        return event;
    }

    public static FlowableEntityEvent createTaskCompletedEvent(Task task) {
        FlowableEntityEventImpl event = new FlowableEntityEventImpl(task, FlowableEngineEventType.TASK_COMPLETED);

        event.setScopeId(task.getScopeId());
        event.setScopeDefinitionId(task.getScopeDefinitionId());
        event.setScopeType(task.getScopeType());
        event.setSubScopeId(task.getId());

        return event;
    }
}
