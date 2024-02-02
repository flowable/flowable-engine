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

import org.flowable.cmmn.api.event.FlowableCaseStageStartedEvent;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.event.FlowableEngineEventImpl;

/**
 * Implements a CMMN case stage started event, holding the case and stage instances.
 *
 * @author Micha Kiener
 */
public class FlowableCaseStageStartedEventImpl extends FlowableEngineEventImpl implements FlowableCaseStageStartedEvent {

    protected CaseInstance caseInstance;
    protected PlanItemInstance stageInstance;

    public FlowableCaseStageStartedEventImpl(CaseInstance caseInstance, PlanItemInstance stageInstance) {
        super(FlowableEngineEventType.STAGE_STARTED, ScopeTypes.CMMN, caseInstance.getId(), stageInstance.getId(), caseInstance.getCaseDefinitionId());
        this.caseInstance = caseInstance;
        this.stageInstance = stageInstance;
    }

    @Override
    public CaseInstance getCaseInstance() {
        return caseInstance;
    }

    @Override
    public PlanItemInstance getEntity() {
        return stageInstance;
    }
}
