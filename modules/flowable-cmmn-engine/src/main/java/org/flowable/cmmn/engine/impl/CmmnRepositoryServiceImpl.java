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
package org.flowable.cmmn.engine.impl;

import java.io.InputStream;
import java.util.List;

import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder;
import org.flowable.cmmn.api.repository.CmmnDeploymentQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.cmd.AddIdentityLinkForCaseDefinitionCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteDeploymentCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteIdentityLinkForCaseDefinitionCmd;
import org.flowable.cmmn.engine.impl.cmd.DeployCmd;
import org.flowable.cmmn.engine.impl.cmd.GetCmmnModelCmd;
import org.flowable.cmmn.engine.impl.cmd.GetDecisionsForCaseDefinitionCmd;
import org.flowable.cmmn.engine.impl.cmd.GetDeploymentCaseDefinitionCmd;
import org.flowable.cmmn.engine.impl.cmd.GetDeploymentCaseDiagramCmd;
import org.flowable.cmmn.engine.impl.cmd.GetDeploymentResourceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetDeploymentResourceNamesCmd;
import org.flowable.cmmn.engine.impl.cmd.GetFormDefinitionsForCaseDefinitionCmd;
import org.flowable.cmmn.engine.impl.cmd.GetIdentityLinksForCaseDefinitionCmd;
import org.flowable.cmmn.engine.impl.cmd.SetCaseDefinitionCategoryCmd;
import org.flowable.cmmn.engine.impl.cmd.SetDeploymentParentDeploymentIdCmd;
import org.flowable.cmmn.engine.impl.repository.CmmnDeploymentBuilderImpl;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.form.api.FormDefinition;
import org.flowable.identitylink.api.IdentityLink;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class CmmnRepositoryServiceImpl extends CommonEngineServiceImpl<CmmnEngineConfiguration> implements CmmnRepositoryService {

    public CmmnRepositoryServiceImpl(CmmnEngineConfiguration engineConfiguration) {
        super(engineConfiguration);
    }

    @Override
    public CmmnDeploymentBuilder createDeployment() {
        return commandExecutor.execute(new Command<CmmnDeploymentBuilder>() {
            @Override
            public CmmnDeploymentBuilder execute(CommandContext commandContext) {
                return new CmmnDeploymentBuilderImpl();
            }
        });
    }
    
    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
       return commandExecutor.execute(new GetDeploymentResourceNamesCmd(deploymentId));
    }

    @Override
    public InputStream getResourceAsStream(String deploymentId, String resourceName) {
        return commandExecutor.execute(new GetDeploymentResourceCmd(deploymentId, resourceName));
    }
    
    public CmmnDeployment deploy(CmmnDeploymentBuilderImpl deploymentBuilder) {
        return commandExecutor.execute(new DeployCmd(deploymentBuilder));
    }
    
    @Override
    public CaseDefinition getCaseDefinition(String caseDefinitionId) {
        return commandExecutor.execute(new GetDeploymentCaseDefinitionCmd(caseDefinitionId));
    }
    
    @Override
    public CmmnModel getCmmnModel(String caseDefinitionId) {
        return commandExecutor.execute(new GetCmmnModelCmd(caseDefinitionId));
    }
    
    @Override
    public InputStream getCaseDiagram(String caseDefinitionId) {
        return commandExecutor.execute(new GetDeploymentCaseDiagramCmd(caseDefinitionId));
    }
    
    @Override
    public void deleteDeployment(String deploymentId, boolean cascade) {
        commandExecutor.execute(new DeleteDeploymentCmd(deploymentId, cascade));
    }
    
    @Override
    public CmmnDeploymentQuery createDeploymentQuery() {
        return configuration.getCmmnDeploymentEntityManager().createDeploymentQuery();
    }
    
    @Override
    public CaseDefinitionQuery createCaseDefinitionQuery() {
        return configuration.getCaseDefinitionEntityManager().createCaseDefinitionQuery();
    }
    
    @Override
    public void addCandidateStarterUser(String caseDefinitionId, String userId) {
        commandExecutor.execute(new AddIdentityLinkForCaseDefinitionCmd(caseDefinitionId, userId, null, configuration));
    }

    @Override
    public void addCandidateStarterGroup(String caseDefinitionId, String groupId) {
        commandExecutor.execute(new AddIdentityLinkForCaseDefinitionCmd(caseDefinitionId, null, groupId, configuration));
    }

    @Override
    public void deleteCandidateStarterGroup(String caseDefinitionId, String groupId) {
        commandExecutor.execute(new DeleteIdentityLinkForCaseDefinitionCmd(caseDefinitionId, null, groupId));
    }

    @Override
    public void deleteCandidateStarterUser(String caseDefinitionId, String userId) {
        commandExecutor.execute(new DeleteIdentityLinkForCaseDefinitionCmd(caseDefinitionId, userId, null));
    }

    @Override
    public List<IdentityLink> getIdentityLinksForCaseDefinition(String caseDefinitionId) {
        return commandExecutor.execute(new GetIdentityLinksForCaseDefinitionCmd(caseDefinitionId));
    }

    @Override
    public void setCaseDefinitionCategory(String caseDefinitionId, String category) {
        commandExecutor.execute(new SetCaseDefinitionCategoryCmd(caseDefinitionId, category));
    }
    
    @Override
    public void changeDeploymentParentDeploymentId(String deploymentId, String newParentDeploymentId) {
        commandExecutor.execute(new SetDeploymentParentDeploymentIdCmd(deploymentId, newParentDeploymentId));
    }

    @Override
    public List<DmnDecision> getDecisionsForCaseDefinition(String caseDefinitionId) {
        return commandExecutor.execute(new GetDecisionsForCaseDefinitionCmd(caseDefinitionId));
    }

    @Override
    @Deprecated
    public List<DmnDecision> getDecisionTablesForCaseDefinition(String caseDefinitionId) {
        return getDecisionsForCaseDefinition(caseDefinitionId);
    }
    
    @Override
    public List<FormDefinition> getFormDefinitionsForCaseDefinition(String caseDefinitionId) {
        return commandExecutor.execute(new GetFormDefinitionsForCaseDefinitionCmd(caseDefinitionId));
    }
}
