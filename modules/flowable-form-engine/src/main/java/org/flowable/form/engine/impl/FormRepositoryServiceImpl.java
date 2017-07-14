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

import java.io.InputStream;
import java.util.List;

import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDefinitionQuery;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormDeploymentBuilder;
import org.flowable.form.api.FormDeploymentQuery;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.NativeFormDefinitionQuery;
import org.flowable.form.api.NativeFormDeploymentQuery;
import org.flowable.form.engine.impl.cmd.DeleteDeploymentCmd;
import org.flowable.form.engine.impl.cmd.DeployCmd;
import org.flowable.form.engine.impl.cmd.GetDeploymentResourceCmd;
import org.flowable.form.engine.impl.cmd.GetDeploymentResourceNamesCmd;
import org.flowable.form.engine.impl.cmd.GetFormDefinitionCmd;
import org.flowable.form.engine.impl.cmd.GetFormDefinitionResourceCmd;
import org.flowable.form.engine.impl.cmd.GetFormModelCmd;
import org.flowable.form.engine.impl.cmd.SetDeploymentCategoryCmd;
import org.flowable.form.engine.impl.cmd.SetDeploymentTenantIdCmd;
import org.flowable.form.engine.impl.cmd.SetFormDefinitionCategoryCmd;
import org.flowable.form.engine.impl.repository.FormDeploymentBuilderImpl;
import org.flowable.form.model.FormModel;

/**
 * @author Tijs Rademakers
 */
public class FormRepositoryServiceImpl extends ServiceImpl implements FormRepositoryService {

    public FormDeploymentBuilder createDeployment() {
        return commandExecutor.execute(new Command<FormDeploymentBuilder>() {
            @Override
            public FormDeploymentBuilder execute(CommandContext commandContext) {
                return new FormDeploymentBuilderImpl();
            }
        });
    }

    public FormDeployment deploy(FormDeploymentBuilderImpl deploymentBuilder) {
        return commandExecutor.execute(new DeployCmd<FormDeployment>(deploymentBuilder));
    }

    public void deleteDeployment(String deploymentId) {
        commandExecutor.execute(new DeleteDeploymentCmd(deploymentId));
    }

    public FormDefinitionQuery createFormDefinitionQuery() {
        return new FormDefinitionQueryImpl(commandExecutor);
    }

    public NativeFormDefinitionQuery createNativeFormDefinitionQuery() {
        return new NativeFormDefinitionQueryImpl(commandExecutor);
    }

    public List<String> getDeploymentResourceNames(String deploymentId) {
        return commandExecutor.execute(new GetDeploymentResourceNamesCmd(deploymentId));
    }

    public InputStream getResourceAsStream(String deploymentId, String resourceName) {
        return commandExecutor.execute(new GetDeploymentResourceCmd(deploymentId, resourceName));
    }

    public void setDeploymentCategory(String deploymentId, String category) {
        commandExecutor.execute(new SetDeploymentCategoryCmd(deploymentId, category));
    }

    public void setDeploymentTenantId(String deploymentId, String newTenantId) {
        commandExecutor.execute(new SetDeploymentTenantIdCmd(deploymentId, newTenantId));
    }

    public FormDeploymentQuery createDeploymentQuery() {
        return new FormDeploymentQueryImpl(commandExecutor);
    }

    public NativeFormDeploymentQuery createNativeDeploymentQuery() {
        return new NativeFormDeploymentQueryImpl(commandExecutor);
    }

    public FormDefinition getFormDefinition(String formDefinitionId) {
        return commandExecutor.execute(new GetFormDefinitionCmd(formDefinitionId));
    }

    public FormModel getFormModelById(String formId) {
        return commandExecutor.execute(new GetFormModelCmd(null, formId));
    }

    public FormModel getFormModelByKey(String formDefinitionKey) {
        return commandExecutor.execute(new GetFormModelCmd(formDefinitionKey, null));
    }

    public FormModel getFormModelByKey(String formDefinitionKey, String tenantId) {
        return commandExecutor.execute(new GetFormModelCmd(formDefinitionKey, null, tenantId));
    }

    public FormModel getFormModelByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId) {
        return commandExecutor.execute(new GetFormModelCmd(formDefinitionKey, null, null, parentDeploymentId));
    }

    public FormModel getFormModelByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId, String tenantId) {
        return commandExecutor.execute(new GetFormModelCmd(formDefinitionKey, null, tenantId, parentDeploymentId));
    }

    public InputStream getFormDefinitionResource(String formId) {
        return commandExecutor.execute(new GetFormDefinitionResourceCmd(formId));
    }

    public void setFormDefinitionCategory(String formId, String category) {
        commandExecutor.execute(new SetFormDefinitionCategoryCmd(formId, category));
    }
}
