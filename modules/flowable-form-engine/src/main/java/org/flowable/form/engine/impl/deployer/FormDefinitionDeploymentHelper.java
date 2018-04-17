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
package org.flowable.form.engine.impl.deployer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntity;
import org.flowable.form.engine.impl.util.CommandContextUtil;

/**
 * Methods for working with deployments. Much of the actual work of {@link FormDefinitionDeployer} is done by orchestrating the different pieces of work this class does; by having them here, we allow
 * other deployers to make use of them.
 */
public class FormDefinitionDeploymentHelper {

    /**
     * Verifies that no two decision tables share the same key, to prevent database unique index violation.
     * 
     * @throws FlowableException
     *             if any two decision tables have the same key
     */
    public void verifyFormsDoNotShareKeys(Collection<FormDefinitionEntity> forms) {
        Set<String> keySet = new LinkedHashSet<>();
        for (FormDefinitionEntity form : forms) {
            if (keySet.contains(form.getKey())) {
                throw new FlowableException("The deployment contains forms with the same key, this is not allowed");
            }
            keySet.add(form.getKey());
        }
    }

    /**
     * Updates all the decision table entities to match the deployment's values for tenant, engine version, and deployment id.
     */
    public void copyDeploymentValuesToForms(FormDeploymentEntity deployment, List<FormDefinitionEntity> formDefinitions) {
        String tenantId = deployment.getTenantId();
        String deploymentId = deployment.getId();

        for (FormDefinitionEntity formDefinition : formDefinitions) {

            // decision table inherits the tenant id
            if (tenantId != null) {
                formDefinition.setTenantId(tenantId);
            }

            formDefinition.setDeploymentId(deploymentId);
        }
    }

    /**
     * Updates all the decision table entities to have the correct resource names.
     */
    public void setResourceNamesOnFormDefinitions(ParsedDeployment parsedDeployment) {
        for (FormDefinitionEntity formDefinition : parsedDeployment.getAllFormDefinitions()) {
            String resourceName = parsedDeployment.getResourceForFormDefinition(formDefinition).getName();
            formDefinition.setResourceName(resourceName);
        }
    }

    /**
     * Gets the most recent persisted decision table that matches this one for tenant and key. If none is found, returns null. This method assumes that the tenant and key are properly set on the
     * decision table entity.
     */
    public FormDefinitionEntity getMostRecentVersionOfForm(FormDefinitionEntity formDefinition) {
        String key = formDefinition.getKey();
        String tenantId = formDefinition.getTenantId();
        FormDefinitionEntityManager formDefinitionEntityManager = CommandContextUtil.getFormEngineConfiguration().getFormDefinitionEntityManager();

        FormDefinitionEntity existingDefinition = null;

        if (tenantId != null && !tenantId.equals(FormEngineConfiguration.NO_TENANT_ID)) {
            existingDefinition = formDefinitionEntityManager.findLatestFormDefinitionByKeyAndTenantId(key, tenantId);
        } else {
            existingDefinition = formDefinitionEntityManager.findLatestFormDefinitionByKey(key);
        }

        return existingDefinition;
    }

    /**
     * Gets the persisted version of the already-deployed form. Note that this is different from {@link #getMostRecentVersionOfForm} as it looks specifically for a form that is already persisted and
     * attached to a particular deployment, rather than the latest version across all deployments.
     */
    public FormDefinitionEntity getPersistedInstanceOfFormDefinition(FormDefinitionEntity formDefinition) {
        String deploymentId = formDefinition.getDeploymentId();
        if (StringUtils.isEmpty(formDefinition.getDeploymentId())) {
            throw new FlowableIllegalArgumentException("Provided form definition must have a deployment id.");
        }

        FormDefinitionEntityManager formDefinitionEntityManager = CommandContextUtil.getFormEngineConfiguration().getFormDefinitionEntityManager();

        FormDefinitionEntity persistedFormDefinition = null;
        if (formDefinition.getTenantId() == null || FormEngineConfiguration.NO_TENANT_ID.equals(formDefinition.getTenantId())) {
            persistedFormDefinition = formDefinitionEntityManager.findFormDefinitionByDeploymentAndKey(deploymentId, formDefinition.getKey());
        } else {
            persistedFormDefinition = formDefinitionEntityManager.findFormDefinitionByDeploymentAndKeyAndTenantId(deploymentId,
                    formDefinition.getKey(), formDefinition.getTenantId());
        }

        return persistedFormDefinition;
    }
}
