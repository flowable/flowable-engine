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
package org.flowable.cmmn.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Joram Barrez
 */
public class PlanFragment extends PlanItemDefinition {

    protected Case caze;
    protected Map<String, PlanItem> planItemMap = new LinkedHashMap<>();
    protected List<Sentry> sentries = new ArrayList<>();
    
    public PlanItem findPlanItemInPlanFragmentOrDownwards(String planItemId) {
        for (PlanItem planItem : planItemMap.values()) {
            if (planItem.getId().equals(planItemId)) {
                return planItem;
            }
            if (planItem.getPlanItemDefinition() instanceof PlanFragment) {
                PlanItem p = ((PlanFragment) planItem.getPlanItemDefinition()).findPlanItemInPlanFragmentOrDownwards(planItemId);
                if (p != null) {
                    return p;
                }
            }
        }
        return null;
    }
    
    public PlanItem findPlanItemInPlanFragmentOrUpwards(String planItemId) {
        if (planItemMap.containsKey(planItemId)) {
            return planItemMap.get(planItemId);
        }
        
        PlanFragment parentPlanFragment = getParent();
        if (parentPlanFragment != null) {
            return parentPlanFragment.findPlanItemInPlanFragmentOrUpwards(planItemId);
        }

        return null;
    }

    public Sentry findSentry(String sentryId) {
        for (Sentry sentry : sentries) {
            if (sentry.getId().equals(sentryId)) {
                return sentry;
            }
        }
        
        PlanFragment parentPlanFragment = getParent();
        if (parentPlanFragment != null) {
            return parentPlanFragment.findSentry(sentryId);
        }

        return null;
    }

    public void addPlanItem(PlanItem planItem) {
        planItemMap.put(planItem.getId(), planItem);
    }

    public void addSentry(Sentry sentry) {
        sentries.add(sentry);
    }

    public Case getCase() {
        return caze;
    }

    public void setCase(Case caze) {
        this.caze = caze;
    }

    public List<PlanItem> getPlanItems() {
        return new ArrayList<>(planItemMap.values());
    }

    public Map<String, PlanItem> getPlanItemMap() {
        return planItemMap;
    }

    public void setPlanItemMap(Map<String, PlanItem> planItemMap) {
        this.planItemMap = planItemMap;
    }

    public List<Sentry> getSentries() {
        return sentries;
    }

    public void setSentries(List<Sentry> sentries) {
        this.sentries = sentries;
    }

}
