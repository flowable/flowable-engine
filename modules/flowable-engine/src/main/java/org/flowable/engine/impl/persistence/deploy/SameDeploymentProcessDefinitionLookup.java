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
package org.flowable.engine.impl.persistence.deploy;

import org.flowable.common.engine.impl.persistence.deploy.AbstractSameDeploymentDefinitionLookup;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * Caches the id of the process definition with a given key in the deployments of a parent deployment (or in a deployment).
 */
public class SameDeploymentProcessDefinitionLookup extends AbstractSameDeploymentDefinitionLookup<ProcessDefinition> {

    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public SameDeploymentProcessDefinitionLookup(DeploymentCache<String> cache, ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(cache, processEngineConfiguration);
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    protected ProcessDefinition findPersistedDefinition(String definitionId) {
        return processEngineConfiguration.getProcessDefinitionEntityManager().findById(definitionId);
    }
}
