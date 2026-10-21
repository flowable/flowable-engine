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
package org.flowable.cmmn.engine.impl.deployer;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.deploy.CaseDefinitionCacheEntry;
import org.flowable.common.engine.impl.persistence.deploy.AbstractSameDeploymentDefinitionLookup;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;

/**
 * Looks up case definitions by key in the deployments of a parent deployment, caching the found definition ids.
 */
public class SameDeploymentCaseDefinitionLookup extends AbstractSameDeploymentDefinitionLookup<CaseDefinition> {

    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    public SameDeploymentCaseDefinitionLookup(DeploymentCache<String> cache, CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cache, cmmnEngineConfiguration);
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    protected boolean isVerifyCachedDefinitions() {
        return cmmnEngineConfiguration.isVerifyCachedSameDeploymentDefinitions();
    }

    @Override
    protected CaseDefinition findCachedDefinition(String definitionId) {
        CaseDefinitionCacheEntry cacheEntry = cmmnEngineConfiguration.getCaseDefinitionCache().get(definitionId);
        return cacheEntry != null ? cacheEntry.getCaseDefinition() : null;
    }

    @Override
    protected CaseDefinition findPersistedDefinition(String definitionId) {
        return cmmnEngineConfiguration.getCaseDefinitionEntityManager().findById(definitionId);
    }
}
