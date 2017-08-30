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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceQuery;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.AbstractQuery;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.CommandExecutor;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceQueryImpl extends AbstractQuery<PlanItemInstanceQuery, PlanItemInstance> implements PlanItemInstanceQuery {
    
    protected String caseDefinitionId;
    protected String caseInstanceId;
    protected String stageInstanceId;
    protected String elementId;
    protected String name;
    protected String state;
    protected Date startedBefore;
    protected Date startedAfter;
    protected String startUserId;
    protected String referenceId;
    protected String referenceType;
    protected String tenantId;
    protected boolean withoutTenantId;
    
    public PlanItemInstanceQueryImpl() {
        
    }
    
    public PlanItemInstanceQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public PlanItemInstanceQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public PlanItemInstanceQuery caseDefinitionId(String caseDefinitionId) {
        if (caseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Case definition id is null");
        }
        this.caseDefinitionId = caseDefinitionId;
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseInstanceId(String caseInstanceId) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("Case instance id is null");
        }
        this.caseInstanceId = caseInstanceId;
        return this;
    }

    @Override
    public PlanItemInstanceQuery stageInstanceId(String stageInstanceId) {
        if (stageInstanceId == null) {
            throw new FlowableIllegalArgumentException("Stage instance id is null");
        }
        this.stageInstanceId = stageInstanceId;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceElementId(String elementId) {
        if (elementId == null) {
            throw new FlowableIllegalArgumentException("Element id is null");
        }
        this.elementId = elementId;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("Name is null");
        }
        this.name = name;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceState(String state) {
        if (state == null) {
            throw new FlowableIllegalArgumentException("State is null");
        }
        this.state = state;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceStartedBefore(Date startedBefore) {
        if (startedBefore == null) {
            throw new FlowableIllegalArgumentException("StartedBefore is null");
        }
        this.startedBefore = startedBefore;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceStarterAfter(Date startedAfer) {
        if (startedAfer == null) {
            throw new FlowableIllegalArgumentException("StartedAfter is null");
        }
        this.startedAfter = startedAfer;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceStartUserId(String startUserId) {
        if (startUserId == null) {
            throw new FlowableIllegalArgumentException("Start user id is null");
        }
        this.startUserId = startUserId;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceReferenceId(String referenceId) {
        this.referenceId = referenceId;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceReferenceType(String referenceType) {
        this.referenceType = referenceType;
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("Tenant id is null");
        }
        this.tenantId = tenantId;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }
    
    @Override
    public long executeCount(CommandContext commandContext) {
        return CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).countByCriteria(this);
    }

    @Override
    public List<PlanItemInstance> executeList(CommandContext commandContext) {
        return CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findByCriteria(this);
    }
    
    @Override
    public PlanItemInstanceQuery orderByStartTime() {
        this.orderProperty = PlanItemInstanceQueryProperty.START_TIME;
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery orderByName() {
        this.orderProperty = PlanItemInstanceQueryProperty.NAME;
        return this;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public String getStageInstanceId() {
        return stageInstanceId;
    }

    public String getElementId() {
        return elementId;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    public Date getStartedAfter() {
        return startedAfter;
    }

    public String getStartUserId() {
        return startUserId;
    }
    
    public String getReferenceId() {
        return referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

}
