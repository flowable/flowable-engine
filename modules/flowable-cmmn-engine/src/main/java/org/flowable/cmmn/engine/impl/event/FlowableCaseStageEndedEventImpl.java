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

import org.flowable.cmmn.api.event.FlowableCaseStageEndedEvent;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.event.FlowableEngineEventImpl;

/**
 * Implements a CMMN case stage ending event, holding the case and stage instances as well as the ending state of the stage.
 *
 * @author Micha Kiener
 */
public class FlowableCaseStageEndedEventImpl extends FlowableEngineEventImpl implements FlowableCaseStageEndedEvent {

    protected CaseInstance caseInstance;
    protected PlanItemInstance stageInstance;
    protected String endingState;

    public FlowableCaseStageEndedEventImpl(CaseInstance caseInstance, PlanItemInstance stageInstance, String endingState) {
        super(FlowableEngineEventType.STAGE_ENDED, ScopeTypes.CMMN, caseInstance.getId(), stageInstance.getId(), caseInstance.getCaseDefinitionId());
        this.caseInstance = caseInstance;
        this.stageInstance = stageInstance;
        this.endingState = endingState;
    }

    @Override
    public String getEndingState() {
        return endingState;
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
