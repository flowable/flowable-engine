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
package org.flowable.form.engine.impl.cmd;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormInfo;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.FormDeploymentQueryImpl;
import org.flowable.form.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.form.engine.impl.persistence.deploy.FormDefinitionCacheEntry;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntityManager;
import org.flowable.form.engine.impl.util.CommandContextUtil;
import org.flowable.form.model.SimpleFormModel;

/**
 * @author Tijs Rademakers
 */
public class GetFormModelCmd implements Command<FormInfo>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String formDefinitionKey;
    protected String formDefinitionId;
    protected String tenantId;
    protected String parentDeploymentId;
    protected boolean fallbackToDefaultTenant;

    public GetFormModelCmd(String formDefinitionKey, String formDefinitionId) {
        this.formDefinitionKey = formDefinitionKey;
        this.formDefinitionId = formDefinitionId;
    }

    public GetFormModelCmd(String formDefinitionKey, String formDefinitionId, String tenantId, boolean fallbackToDefaultTenant) {
        this(formDefinitionKey, formDefinitionId);
        this.tenantId = tenantId;
        this.fallbackToDefaultTenant = fallbackToDefaultTenant;
    }

    public GetFormModelCmd(String formDefinitionKey, String formDefinitionId, String tenantId, String parentDeploymentId, boolean fallbackToDefaultTenant) {
        this(formDefinitionKey, formDefinitionId, tenantId, fallbackToDefaultTenant);
        this.parentDeploymentId = parentDeploymentId;
    }

    @Override
    public FormInfo execute(CommandContext commandContext) {
        FormEngineConfiguration formEngineConfiguration = CommandContextUtil.getFormEngineConfiguration();
        DeploymentManager deploymentManager = formEngineConfiguration.getDeploymentManager();
        FormDefinitionEntityManager formDefinitionEntityManager = formEngineConfiguration.getFormDefinitionEntityManager();

        // Find the form definition
        FormDefinitionEntity formDefinitionEntity = null;
        if (formDefinitionId != null) {

            formDefinitionEntity = deploymentManager.findDeployedFormDefinitionById(formDefinitionId);
            if (formDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No form definition found for id = '" + formDefinitionId + "'", FormDefinitionEntity.class);
            }

        } else if (formDefinitionKey != null && (tenantId == null || FormEngineConfiguration.NO_TENANT_ID.equals(tenantId)) && 
                        (parentDeploymentId == null || formEngineConfiguration.isAlwaysLookupLatestDefinitionVersion())) {

            formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKey(formDefinitionKey);
            if (formDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No form definition found for key '" + formDefinitionKey + "'", FormDefinitionEntity.class);
            }

        } else if (formDefinitionKey != null && tenantId != null && !FormEngineConfiguration.NO_TENANT_ID.equals(tenantId) && 
                        (parentDeploymentId == null || formEngineConfiguration.isAlwaysLookupLatestDefinitionVersion())) {

            formDefinitionEntity = formDefinitionEntityManager.findLatestFormDefinitionByKeyAndTenantId(formDefinitionKey, tenantId);
            
            if (formDefinitionEntity == null && (fallbackToDefaultTenant || formEngineConfiguration.isFallbackToDefaultTenant())) {
                if (StringUtils.isNotEmpty(formEngineConfiguration.getDefaultTenantValue())) {
                    formDefinitionEntity = formDefinitionEntityManager.findLatestFormDefinitionByKeyAndTenantId(formDefinitionKey, formEngineConfiguration.getDefaultTenantValue());
                } else {
                    formDefinitionEntity = formDefinitionEntityManager.findLatestFormDefinitionByKey(formDefinitionKey);
                }
            }
            
            if (formDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No form definition found for key '" + formDefinitionKey + "' for tenant identifier " + tenantId, FormDefinitionEntity.class);
            }

        } else if (formDefinitionKey != null && (tenantId == null || FormEngineConfiguration.NO_TENANT_ID.equals(tenantId)) && parentDeploymentId != null) {

            List<FormDeployment> formDeployments = deploymentManager.getDeploymentEntityManager().findDeploymentsByQueryCriteria(
                            new FormDeploymentQueryImpl().parentDeploymentId(parentDeploymentId));
            
            if (formDeployments != null && formDeployments.size() > 0) {
                formDefinitionEntity = formDefinitionEntityManager.findFormDefinitionByDeploymentAndKey(formDeployments.get(0).getId(), formDefinitionKey);
            }
            
            if (formDefinitionEntity == null) {
                formDefinitionEntity = formDefinitionEntityManager.findLatestFormDefinitionByKey(formDefinitionKey);
            }
            
            if (formDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No form definition found for key '" + formDefinitionKey +
                        "' for parent deployment id " + parentDeploymentId, FormDefinitionEntity.class);
            }

        } else if (formDefinitionKey != null && tenantId != null && !FormEngineConfiguration.NO_TENANT_ID.equals(tenantId) && parentDeploymentId != null) {

            List<FormDeployment> formDeployments = deploymentManager.getDeploymentEntityManager().findDeploymentsByQueryCriteria(
                            new FormDeploymentQueryImpl().parentDeploymentId(parentDeploymentId).deploymentTenantId(tenantId));
            
            if (formDeployments != null && formDeployments.size() > 0) {
                formDefinitionEntity = formDefinitionEntityManager.findFormDefinitionByDeploymentAndKeyAndTenantId(
                                formDeployments.get(0).getId(), formDefinitionKey, tenantId);
            }
            
            if (formDefinitionEntity == null) {
                formDefinitionEntity = formDefinitionEntityManager.findLatestFormDefinitionByKeyAndTenantId(formDefinitionKey, tenantId);
            }
            
            if (formDefinitionEntity == null && (fallbackToDefaultTenant || formEngineConfiguration.isFallbackToDefaultTenant())) {
                if (StringUtils.isNotEmpty(formEngineConfiguration.getDefaultTenantValue())) {
                    formDefinitionEntity = formDefinitionEntityManager.findLatestFormDefinitionByKeyAndTenantId(formDefinitionKey, formEngineConfiguration.getDefaultTenantValue());
                } else {
                    formDefinitionEntity = formDefinitionEntityManager.findLatestFormDefinitionByKey(formDefinitionKey);
                }
            }
            
            if (formDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No form definition found for key '" + formDefinitionKey +
                        " for parent deployment id '" + parentDeploymentId + "' and for tenant identifier " + tenantId, FormDefinitionEntity.class);
            }

        } else {
            throw new FlowableObjectNotFoundException("formDefinitionKey and formDefinitionId are null");
        }

        FormDefinitionCacheEntry formDefinitionCacheEntry = deploymentManager.resolveFormDefinition(formDefinitionEntity);
        SimpleFormModel formModel = CommandContextUtil.getFormEngineConfiguration(commandContext).getFormJsonConverter().convertToFormModel(formDefinitionCacheEntry.getFormDefinitionJson());
        FormInfo formInfo = new FormInfo();
        formInfo.setId(formDefinitionEntity.getId());
        formInfo.setName(formDefinitionEntity.getName());
        formInfo.setKey(formDefinitionEntity.getKey());
        formInfo.setVersion(formDefinitionEntity.getVersion());
        formInfo.setFormModel(formModel);
        return formInfo;
    }
}
