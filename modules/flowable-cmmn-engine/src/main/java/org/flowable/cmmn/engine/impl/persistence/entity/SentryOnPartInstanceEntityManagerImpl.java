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

package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.data.SentryOnPartInstanceDataManager;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;

/**
 * @author Joram Barrez
 */
public class SentryOnPartInstanceEntityManagerImpl extends AbstractCmmnEntityManager<SentryOnPartInstanceEntity> implements SentryOnPartInstanceEntityManager {

    protected SentryOnPartInstanceDataManager sentryOnPartInstanceDataManager;

    public SentryOnPartInstanceEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, SentryOnPartInstanceDataManager sentryOnPartInstanceDataManager) {
        super(cmmnEngineConfiguration);
        this.sentryOnPartInstanceDataManager = sentryOnPartInstanceDataManager;
    }

    @Override
    protected DataManager<SentryOnPartInstanceEntity> getDataManager() {
        return sentryOnPartInstanceDataManager;
    }
    
    @Override
    public List<SentryOnPartInstanceEntity> findSentryOnPartInstancesByCaseInstanceIdAndNullPlanItemInstanceId(String caseInstanceId) {
        return sentryOnPartInstanceDataManager.findSentryOnPartInstancesByCaseInstanceIdAndNullPlanItemInstanceId(caseInstanceId);
    }

    @Override
    public List<SentryOnPartInstanceEntity> findSentryOnPartInstancesByPlanItemInstanceId(String planItemId) {
        return sentryOnPartInstanceDataManager.findSentryOnPartInstancesByPlanItemInstanceId(planItemId);
    }

}
