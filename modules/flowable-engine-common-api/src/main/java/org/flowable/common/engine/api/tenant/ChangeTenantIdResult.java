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

 package org.flowable.common.engine.api.tenant;

import java.util.HashMap;
import java.util.Map;

/**
 * Container class to return the result of a change in a tenant id operation (produced by a {@link ChangeTenantIdBuilder})
 */
public class ChangeTenantIdResult {

    public static enum Key {
        ActivityInstances,
        CaseInstances,
        DeadLetterJobs,
        EventSubscriptions,
        Executions,
        ExternalWorkerJobs,
        HistoricActivityInstances,
        HistoricCaseInstances,
        HistoricDecisionExecutions,
        HistoricMilestoneInstances,
        HistoricPlanItemInstances,
        HistoricProcessInstances,
        HistoricTaskInstances,
        HistoricTaskLogEntries,
        HistoryJobs,
        Jobs,
        MilestoneInstances,
        PlanItemInstances,
        SuspendedJobs,
        Tasks,
        TimerJobs,
        FormInstances,
        ContentItemInstances
    }

    private final Map<Key,Long> resultMap;

    private ChangeTenantIdResult() {
        this.resultMap = new HashMap<>();
    }

    public static ChangeTenantIdResult builder() {
        return new ChangeTenantIdResult();
    }

    public ChangeTenantIdResult addResult(Key key, Long value) {
        this.resultMap.put(key, value);
        return this;
    }

    public ChangeTenantIdResult build() {
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((resultMap == null) ? 0 : resultMap.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ChangeTenantIdResult other = (ChangeTenantIdResult) obj;
        if (resultMap == null) {
            if (other.resultMap != null)
                return false;
        } else if (!resultMap.equals(other.resultMap))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ChangeTenantIdResult [resultMap=" + resultMap + "]";
    }

}
