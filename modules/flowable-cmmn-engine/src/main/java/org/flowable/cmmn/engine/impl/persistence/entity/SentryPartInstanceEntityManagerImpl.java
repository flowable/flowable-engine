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
import org.flowable.cmmn.engine.impl.persistence.entity.data.SentryPartInstanceDataManager;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;

/**
 * @author Joram Barrez
 */
public class SentryPartInstanceEntityManagerImpl extends AbstractCmmnEntityManager<SentryPartInstanceEntity> implements SentryPartInstanceEntityManager {

    protected SentryPartInstanceDataManager sentryPartInstanceDataManager;

    public SentryPartInstanceEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, SentryPartInstanceDataManager sentryPartInstanceDataManager) {
        super(cmmnEngineConfiguration);
        this.sentryPartInstanceDataManager = sentryPartInstanceDataManager;
    }

    @Override
    protected DataManager<SentryPartInstanceEntity> getDataManager() {
        return sentryPartInstanceDataManager;
    }
    
    @Override
    public List<SentryPartInstanceEntity> findSentryPartInstancesByCaseInstanceIdAndNullPlanItemInstanceId(String caseInstanceId) {
        return sentryPartInstanceDataManager.findSentryPartInstancesByCaseInstanceIdAndNullPlanItemInstanceId(caseInstanceId);
    }

    @Override
    public List<SentryPartInstanceEntity> findSentryPartInstancesByPlanItemInstanceId(String planItemId) {
        return sentryPartInstanceDataManager.findSentryPartInstancesByPlanItemInstanceId(planItemId);
    }
    
    @Override
    public void deleteByCaseInstanceId(String caseInstanceId) {
        sentryPartInstanceDataManager.deleteByCaseInstanceId(caseInstanceId);
    }

}
