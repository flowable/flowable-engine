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

import org.flowable.cmmn.api.event.FlowableCaseStartedEvent;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.event.FlowableEngineEventImpl;

/**
 * @author Filip Hrisafov
 */
public class FlowableCaseStartedEventImpl extends FlowableEngineEventImpl implements FlowableCaseStartedEvent {

    protected CaseInstance caseInstance;

    public FlowableCaseStartedEventImpl(CaseInstance caseInstance) {
        super(FlowableEngineEventType.CASE_STARTED, ScopeTypes.CMMN, caseInstance.getId(), null, caseInstance.getCaseDefinitionId());
        this.caseInstance = caseInstance;
    }

    @Override
    public CaseInstance getEntity() {
        return caseInstance;
    }
}
