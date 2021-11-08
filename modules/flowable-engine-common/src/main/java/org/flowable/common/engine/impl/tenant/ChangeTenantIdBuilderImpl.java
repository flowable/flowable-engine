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
package org.flowable.common.engine.impl.tenant;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;

/**
 * @author Filip Hrisafov
 */
public class ChangeTenantIdBuilderImpl implements ChangeTenantIdBuilder {

    protected final String sourceTenantId;
    protected final String targetTenantId;
    protected final ChangeTenantIdManager changeTenantIdManager;

    protected String definitionTenantId;

    public ChangeTenantIdBuilderImpl(String sourceTenantId, String targetTenantId, ChangeTenantIdManager changeTenantIdManager) {
        if (sourceTenantId == null) {
            throw new FlowableIllegalArgumentException("The source tenant id must not be null.");
        }
        if (targetTenantId == null) {
            throw new FlowableIllegalArgumentException("The target tenant id must not be null.");
        }
        this.sourceTenantId = sourceTenantId;
        this.targetTenantId = targetTenantId;
        if (sourceTenantId.equals(targetTenantId)) {
            throw new FlowableIllegalArgumentException("The source and the target tenant ids must be different.");
        }
        this.changeTenantIdManager = changeTenantIdManager;
    }

    @Override
    public ChangeTenantIdBuilder definitionTenantId(String definitionTenantId) {
        if (definitionTenantId == null) {
            throw new FlowableIllegalArgumentException("definitionTenantId must not be null");
        }
        this.definitionTenantId = definitionTenantId;
        return this;
    }

    @Override
    public ChangeTenantIdResult simulate() {
        return changeTenantIdManager.simulate(this);
    }

    @Override
    public ChangeTenantIdResult complete() {
        return changeTenantIdManager.complete(this);
    }

    public String getSourceTenantId() {
        return sourceTenantId;
    }

    public String getTargetTenantId() {
        return targetTenantId;
    }

    public String getDefinitionTenantId() {
        return definitionTenantId;
    }
}
