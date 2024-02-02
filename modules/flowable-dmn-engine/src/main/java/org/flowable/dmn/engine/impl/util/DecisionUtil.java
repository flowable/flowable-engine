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
package org.flowable.dmn.engine.impl.util;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.dmn.engine.impl.persistence.deploy.DecisionCacheEntry;
import org.flowable.dmn.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityManager;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DmnDefinition;

/**
 * A utility class that hides the complexity of {@link DecisionEntity} and {@link Decision} lookup. Use this class rather than accessing the decision table cache or {@link DeploymentManager}
 * directly.
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class DecisionUtil {

    public static DecisionEntity getDecisionTableEntity(String decisionTableId) {
        return getDecisionTableEntity(decisionTableId, false);
    }

    public static DecisionEntity getDecisionTableEntity(String decisionTableId, boolean checkCacheOnly) {
        if (checkCacheOnly) {
            DecisionCacheEntry cacheEntry = CommandContextUtil.getDmnEngineConfiguration().getDefinitionCache().get(decisionTableId);
            if (cacheEntry != null) {
                return cacheEntry.getDecisionEntity();
            }
            return null;
        } else {
            // This will check the cache in the findDeployedDecisionById method
            return CommandContextUtil.getDmnEngineConfiguration().getDeploymentManager().findDeployedDecisionById(decisionTableId);
        }
    }

    public static DmnDefinition getDmnDefinitionByDecisionId(String decisionId) {
        DeploymentManager deploymentManager = CommandContextUtil.getDmnEngineConfiguration().getDeploymentManager();

        // This will check the cache in the findDeployedDecisionById and resolveDecisionTable method
        DecisionEntity decisionTableEntity = deploymentManager.findDeployedDecisionById(decisionId);
        return deploymentManager.resolveDecision(decisionTableEntity).getDmnDefinition();
    }

    public static DmnDefinition getDmnDefinitionFromCache(String definitionId) {
        DecisionCacheEntry cacheEntry = CommandContextUtil.getDmnEngineConfiguration().getDefinitionCache().get(definitionId);
        if (cacheEntry != null) {
            return cacheEntry.getDmnDefinition();
        }
        return null;
    }

    public static DecisionEntity getDecisionTableFromDatabase(String decisionTableId) {
        DecisionEntityManager decisionTableEntityManager = CommandContextUtil.getDmnEngineConfiguration().getDecisionEntityManager();
        DecisionEntity decisionTable = decisionTableEntityManager.findById(decisionTableId);
        if (decisionTable == null) {
            throw new FlowableException("No decision table found with id " + decisionTableId);
        }

        return decisionTable;
    }

    public static DecisionService getDecisionService(String decisionId) {
        DmnDefinition dmnDefinition = getDmnDefinitionByDecisionId(decisionId);
        DecisionService decisionService = dmnDefinition.getDecisionServiceById(decisionId);

        if (decisionService == null) {
            throw new FlowableObjectNotFoundException("Could not find decision service with id: " + decisionId);
        }

        return decisionService;
    }
}
