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

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.CommandExecutor;
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

    public FormInstanceQueryImpl id(String id) {
        this.id = id;
        return this;
    }

    public FormInstanceQueryImpl ids(Set<String> ids) {
        this.ids = ids;
        return this;
    }

    public FormInstanceQueryImpl formDefinitionId(String formDefinitionId) {
        this.formDefinitionId = formDefinitionId;
        return this;
    }

    public FormInstanceQueryImpl formDefinitionIdLike(String formDefinitionIdLike) {
        this.formDefinitionIdLike = formDefinitionIdLike;
        return this;
    }

    public FormInstanceQueryImpl taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public FormInstanceQueryImpl taskIdLike(String taskIdLike) {
        this.taskIdLike = taskIdLike;
        return this;
    }

    public FormInstanceQueryImpl processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    public FormInstanceQueryImpl processInstanceIdLike(String processInstanceIdLike) {
        this.processInstanceIdLike = processInstanceIdLike;
        return this;
    }

    public FormInstanceQueryImpl processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    public FormInstanceQueryImpl processDefinitionIdLike(String processDefinitionIdLike) {
        this.processDefinitionIdLike = processDefinitionIdLike;
        return this;
    }

    public FormInstanceQueryImpl submittedDate(Date submittedDate) {
        this.submittedDate = submittedDate;
        return this;
    }

    public FormInstanceQueryImpl submittedDateBefore(Date submittedDateBefore) {
        this.submittedDateBefore = submittedDateBefore;
        return this;
    }

    public FormInstanceQueryImpl submittedDateAfter(Date submittedDateAfter) {
        this.submittedDateAfter = submittedDateAfter;
        return this;
    }

    public FormInstanceQueryImpl submittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
        return this;
    }

    public FormInstanceQueryImpl submittedByLike(String submittedByLike) {
        this.submittedByLike = submittedByLike;
        return this;
    }

    public FormInstanceQueryImpl deploymentTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    public FormInstanceQueryImpl deploymentTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantIdLike is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    public FormInstanceQueryImpl deploymentWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting ////////////////////////////////////////////////////////

    public FormInstanceQuery orderBySubmittedDate() {
        return orderBy(FormInstanceQueryProperty.SUBMITTED_DATE);
    }

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
