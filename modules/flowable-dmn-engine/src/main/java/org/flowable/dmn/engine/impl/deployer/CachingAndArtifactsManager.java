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
package org.flowable.dmn.engine.impl.deployer;

import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.persistence.deploy.DecisionCacheEntry;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DmnDefinition;

/**
 * Updates caches and artifacts for a deployment and its decision (service)
 */
public class CachingAndArtifactsManager {

    /**
     * Ensures that the definition is cached in the appropriate places, including the deployment's collection of deployed artifacts and the deployment manager's cache.
     */
    public void updateCachingAndArtifacts(ParsedDeployment parsedDeployment) {
        final DmnEngineConfiguration dmnEngineConfiguration = CommandContextUtil.getDmnEngineConfiguration();
        DeploymentCache<DecisionCacheEntry> decisionCache = dmnEngineConfiguration.getDeploymentManager().getDecisionCache();
        DmnDeploymentEntity deployment = parsedDeployment.getDeployment();

        for (DecisionEntity decisionEntity : parsedDeployment.getAllDecisions()) {
            DmnDefinition dmnDefinition = parsedDeployment.getDmnDefinitionForDecision(decisionEntity);

            DecisionCacheEntry cacheEntry;
            if (!dmnDefinition.getDecisionServices().isEmpty()) {
                DecisionService decisionService = parsedDeployment.getDecisionServiceForDecisionEntity(decisionEntity);
                cacheEntry = new DecisionCacheEntry(decisionEntity, dmnDefinition, decisionService);
            } else {
                Decision decision = parsedDeployment.getDecisionForDecisionEntity(decisionEntity);
                cacheEntry = new DecisionCacheEntry(decisionEntity, dmnDefinition, decision);
            }

            decisionCache.add(decisionEntity.getId(), cacheEntry);

            // Add to deployment for further usage
            deployment.addDeployedArtifact(decisionEntity);
        }
    }
}
