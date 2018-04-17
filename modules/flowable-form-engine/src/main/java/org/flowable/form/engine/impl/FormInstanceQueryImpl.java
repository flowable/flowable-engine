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

package org.flowable.form.engine.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.form.api.FormInstance;
import org.flowable.form.api.FormInstanceQuery;
import org.flowable.form.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class FormInstanceQueryImpl extends AbstractQuery<FormInstanceQuery, FormInstance> implements FormInstanceQuery, Serializable {

    private static final long serialVersionUID = 1L;
    protected String id;
    protected Set<String> ids;
    protected String formDefinitionId;
    protected String formDefinitionIdLike;
    protected String taskId;
    protected String taskIdLike;
    protected String processInstanceId;
    protected String processInstanceIdLike;
    protected String processDefinitionId;
    protected String processDefinitionIdLike;
    protected String scopeId;
    protected String scopeType;
    protected String scopeDefinitionId;
    protected Date submittedDate;
    protected Date submittedDateBefore;
    protected Date submittedDateAfter;
    protected String submittedBy;
    protected String submittedByLike;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;

    public FormInstanceQueryImpl() {
    }

    public FormInstanceQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public FormInstanceQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public FormInstanceQueryImpl id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public FormInstanceQueryImpl ids(Set<String> ids) {
        this.ids = ids;
        return this;
    }

    @Override
    public FormInstanceQueryImpl formDefinitionId(String formDefinitionId) {
        this.formDefinitionId = formDefinitionId;
        return this;
    }

    @Override
    public FormInstanceQueryImpl formDefinitionIdLike(String formDefinitionIdLike) {
        this.formDefinitionIdLike = formDefinitionIdLike;
        return this;
    }

    @Override
    public FormInstanceQueryImpl taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    @Override
    public FormInstanceQueryImpl taskIdLike(String taskIdLike) {
        this.taskIdLike = taskIdLike;
        return this;
    }

    @Override
    public FormInstanceQueryImpl processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public FormInstanceQueryImpl processInstanceIdLike(String processInstanceIdLike) {
        this.processInstanceIdLike = processInstanceIdLike;
        return this;
    }

    @Override
    public FormInstanceQueryImpl processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public FormInstanceQueryImpl processDefinitionIdLike(String processDefinitionIdLike) {
        this.processDefinitionIdLike = processDefinitionIdLike;
        return this;
    }
    
    @Override
    public FormInstanceQueryImpl scopeId(String scopeId) {
        this.scopeId = scopeId;
        return this;
    }
    
    @Override
    public FormInstanceQueryImpl scopeType(String scopeType) {
        this.scopeType = scopeType;
        return this;
    }
    
    @Override
    public FormInstanceQueryImpl scopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
        return this;
    }

    @Override
    public FormInstanceQueryImpl submittedDate(Date submittedDate) {
        this.submittedDate = submittedDate;
        return this;
    }

    @Override
    public FormInstanceQueryImpl submittedDateBefore(Date submittedDateBefore) {
        this.submittedDateBefore = submittedDateBefore;
        return this;
    }

    @Override
    public FormInstanceQueryImpl submittedDateAfter(Date submittedDateAfter) {
        this.submittedDateAfter = submittedDateAfter;
        return this;
    }

    @Override
    public FormInstanceQueryImpl submittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
        return this;
    }

    @Override
    public FormInstanceQueryImpl submittedByLike(String submittedByLike) {
        this.submittedByLike = submittedByLike;
        return this;
    }

    @Override
    public FormInstanceQueryImpl deploymentTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public FormInstanceQueryImpl deploymentTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantIdLike is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public FormInstanceQueryImpl deploymentWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting ////////////////////////////////////////////////////////

    @Override
    public FormInstanceQuery orderBySubmittedDate() {
        return orderBy(FormInstanceQueryProperty.SUBMITTED_DATE);
    }

    @Override
    public FormInstanceQuery orderByTenantId() {
        return orderBy(FormInstanceQueryProperty.TENANT_ID);
    }

    // results ////////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getFormInstanceEntityManager(commandContext).findFormInstanceCountByQueryCriteria(this);
    }

    @Override
    public List<FormInstance> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getFormInstanceEntityManager(commandContext).findFormInstancesByQueryCriteria(this);
    }

    // getters ////////////////////////////////////////////////////////

    public String getId() {
        return id;
    }

    public Set<String> getIds() {
        return ids;
    }

    public String getFormDefinitionId() {
        return formDefinitionId;
    }

    public String getFormDefinitionIdLike() {
        return formDefinitionIdLike;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskIdLike() {
        return taskIdLike;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getProcessInstanceIdLike() {
        return processInstanceIdLike;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessDefinitionIdLike() {
        return processDefinitionIdLike;
    }

    public String getScopeId() {
        return scopeId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public Date getSubmittedDate() {
        return submittedDate;
    }

    public Date getSubmittedDateBefore() {
        return submittedDateBefore;
    }

    public Date getSubmittedDateAfter() {
        return submittedDateAfter;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public String getSubmittedByLike() {
        return submittedByLike;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }
}
