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
import java.util.ListIterator;
import java.util.Map;

/**
 * Even though plan fragments don't have a runtime behavior, they still need to be stored in the {@link CmmnModel},
 * as they are needed when exporting the XML.
 *
 * From the CMMN spec:
 *
 * "Unlike other PlanItemDefinitions, a PlanFragment does not have a representation in run-time,
 * i.e., there is no notion of lifecycle tracking of a PlanFragment (not being a Stage) in the context of a Case instance.
 * Just the PlanItems that are contained in it are instantiated and have their lifecyles that are tracked.
 *
 * @author Joram Barrez
 */
public class PlanFragment extends PlanItemDefinition {

    protected Case caze;
    protected PlanItem planItem;
    protected Map<String, PlanItem> planItemMap = new LinkedHashMap<>();
    protected Map<String, PlanItem> planItemDefinitionToItemMap = new LinkedHashMap<>();
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
            PlanItem p = parentPlanFragment.findPlanItemInPlanFragmentOrUpwards(planItemId);
            if (p != null) {
                return p;
            }
        }

        return null;
    }

    public PlanItem findPlanItemForPlanItemDefinitionInPlanFragmentOrUpwards(String planItemDefinitionId) {
        if (planItemDefinitionToItemMap.containsKey(planItemDefinitionId)) {
            return planItemDefinitionToItemMap.get(planItemDefinitionId);
        }

        PlanFragment parentPlanFragment = getParent();
        if (parentPlanFragment != null) {
            PlanItem p = parentPlanFragment.findPlanItemForPlanItemDefinitionInPlanFragmentOrUpwards(planItemDefinitionId);
            if (p != null) {
                return p;
            }
        }

        return null;
    }

    public PlanItem findPlanItemForPlanItemDefinitionInPlanFragmentOrDownwards(String planItemDefinitionId) {
        if (planItemDefinitionToItemMap.containsKey(planItemDefinitionId)) {
            return planItemDefinitionToItemMap.get(planItemDefinitionId);
        }

        for (PlanItem planItem : planItemMap.values()) {
            if (planItem.getPlanItemDefinition() instanceof PlanFragment) {
                PlanItem p = ((PlanFragment) planItem.getPlanItemDefinition()).findPlanItemForPlanItemDefinitionInPlanFragmentOrDownwards(planItemDefinitionId);
                if (p != null) {
                    return p;
                }
            }
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
        planItemDefinitionToItemMap.put(planItem.getDefinitionRef(), planItem);
    }

    public void removePlanItem(PlanItem planItem) {
        planItemMap.remove(planItem.getId());
        planItemDefinitionToItemMap.get(planItem.getId());
    }

    public List<PlanItem> getDirectChildPlanItemsWithLifecycleEnabled() {
        List<PlanItem> childPlanItems = new ArrayList<>(planItemMap.values());
        ListIterator<PlanItem> planItemIterator = childPlanItems.listIterator(childPlanItems.size());
        while (planItemIterator.hasPrevious()) { // ListIterator#add will add it before the cursor, hence the reverse traversal
            PlanItem planItem = planItemIterator.previous();
            if (!planItem.isInstanceLifecycleEnabled()) {
                planItemIterator.remove();

                PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
                if (planItemDefinition instanceof PlanFragment planFragment) {

                    for (PlanItem planFragmentPlanItem : planFragment.getPlanItems()) {
                        planItemIterator.add(planFragmentPlanItem);
                    }
                }
            }
        }
        return childPlanItems;
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

    public PlanItem getPlanItem() {
        return planItem;
    }

    public void setPlanItem(PlanItem planItem) {
        this.planItem = planItem;
    }

    public List<PlanItem> getPlanItems() {
        return new ArrayList<>(planItemMap.values());
    }

    public void setPlanItemMap(Map<String, PlanItem> planItemMap) {
        this.planItemMap = planItemMap;
    }

    public PlanItem getPlanItem(String planItemId) {
        return planItemMap.get(planItemId);
    }

    public List<Sentry> getSentries() {
        return sentries;
    }

    public void setSentries(List<Sentry> sentries) {
        this.sentries = sentries;
    }

}
