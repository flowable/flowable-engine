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
package org.flowable.common.engine.impl.event;

import org.flowable.common.engine.api.delegate.event.FlowableChangeTenantIdEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;

/**
 * @author Filip Hrisafov
 */
public class FlowableChangeTenantIdEventImpl extends FlowableEventImpl implements FlowableChangeTenantIdEvent {

    protected String engineScopeType;
    protected String sourceTenantId;
    protected String targetTenantId;
    protected String definitionTenantId;

    public FlowableChangeTenantIdEventImpl(String engineScopeType, String sourceTenantId, String targetTenantId, String definitionTenantId) {
        super(FlowableEngineEventType.CHANGE_TENANT_ID);
        this.engineScopeType = engineScopeType;
        this.sourceTenantId = sourceTenantId;
        this.targetTenantId = targetTenantId;
        this.definitionTenantId = definitionTenantId;
    }

    @Override
    public String getEngineScopeType() {
        return engineScopeType;
    }

    @Override
    public String getSourceTenantId() {
        return sourceTenantId;
    }

    @Override
    public String getTargetTenantId() {
        return targetTenantId;
    }

    @Override
    public String getDefinitionTenantId() {
        return definitionTenantId;
    }

}
