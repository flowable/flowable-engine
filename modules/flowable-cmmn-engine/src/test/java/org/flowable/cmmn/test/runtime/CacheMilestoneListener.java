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
package org.flowable.cmmn.test.runtime;

import java.util.List;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.MilestoneInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;

public class CacheMilestoneListener implements PlanItemInstanceLifecycleListener {
    
    public static String milestoneInstanceId;
    public static String historicMilestoneInstanceId;

    public static void reset() {
        milestoneInstanceId = null;
        historicMilestoneInstanceId = null;
    }

    @Override
    public String getSourceState() {
        return null;
    }

    @Override
    public String getTargetState() {
        return null;
    }

    @Override
    public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        CmmnRuntimeService runtimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        EntityCache entityCache = CommandContextUtil.getCommandContext().getSession(EntityCache.class);
        List<MilestoneInstanceEntityImpl> cacheMilestoneInstances = entityCache.findInCache(MilestoneInstanceEntityImpl.class);
        String cacheMilestoneInstanceId = cacheMilestoneInstances.get(0).getId();
        
        MilestoneInstance milestoneInstance = runtimeService.createMilestoneInstanceQuery().milestoneInstanceId(cacheMilestoneInstanceId).singleResult();
        if (milestoneInstance != null) {
            milestoneInstanceId = milestoneInstance.getId();
        }
        
        CmmnHistoryService historyService = cmmnEngineConfiguration.getCmmnHistoryService();
        HistoricMilestoneInstance historicMilestoneInstance = historyService.createHistoricMilestoneInstanceQuery().milestoneInstanceId(cacheMilestoneInstanceId).singleResult();
        if (historicMilestoneInstance != null) {
            historicMilestoneInstanceId = historicMilestoneInstance.getId();
        }
    }

}
