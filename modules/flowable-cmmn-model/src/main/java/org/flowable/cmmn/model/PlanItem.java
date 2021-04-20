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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Joram Barrez
 */
public class PlanItem extends CaseElement implements HasEntryCriteria, HasExitCriteria, HasAssociations {
    
    protected String definitionRef;
    protected PlanItemDefinition planItemDefinition;
    protected boolean instanceLifecycleEnabled = true;
    protected PlanItemControl itemControl;
    protected Set<String> criteriaRefs = new HashSet<>();
    protected List<Criterion> entryCriteria = new ArrayList<>();
    protected List<Criterion> exitCriteria = new ArrayList<>();
    protected List<Association> incomingAssociations = new ArrayList<>();
    protected List<Association> outgoingAssociations = new ArrayList<>();

    /**
     * A list of {@link PlanItem}s to which this plan item is dependent on through its entry criteria.
     * Said differently: this list of plan items will influence entry criteria on this plan item.
     */
    protected List<PlanItem> entryDependencies = new ArrayList<>();

    /**
     * A list of {@link PlanItem}s to which this plan item is dependent on through its exit criteria.
     * Said differently: this list of plan items will influence exit criteria on this plan item.
     */
    protected List<PlanItem> exitDependencies = new ArrayList<>();
    
    /**
     * A list of all {@link PlanItem}s that are dependent on this plan item through their entry criteria.
     * Said differently: this list of plan items have an entry criteria that references this plan item.
     */
    protected List<PlanItem> entryDependentPlanItems = new ArrayList<>();

    /**
     * A list of all {@link PlanItem}s that are dependent on this plan item through their exit criteria.
     * Said differently: this list of plan items have an exit criteria that references this plan item.
     */
    protected List<PlanItem> exitDependentPlanItems = new ArrayList<>();
    
    protected Object behavior;

    public String getDefinitionRef() {
        return definitionRef;
    }

    public void setDefinitionRef(String definitionRef) {
        this.definitionRef = definitionRef;
    }

    public PlanItemDefinition getPlanItemDefinition() {
        return planItemDefinition;
    }

    public void setPlanItemDefinition(PlanItemDefinition planItemDefinition) {
        this.planItemDefinition = planItemDefinition;
        setInstanceLifecycleEnabled(!(planItemDefinition instanceof PlanFragment) || (planItemDefinition instanceof Stage));
    }

    public boolean isInstanceLifecycleEnabled() {
        return instanceLifecycleEnabled;
    }

    public void setInstanceLifecycleEnabled(boolean instanceLifecycleEnabled) {
        this.instanceLifecycleEnabled = instanceLifecycleEnabled;
    }

    public PlanItemControl getItemControl() {
        return itemControl;
    }

    public void setItemControl(PlanItemControl itemControl) {
        this.itemControl = itemControl;
    }

    public void addCriteriaRef(String entryCriteriaRef) {
        this.criteriaRefs.add(entryCriteriaRef);
    }
    
    public Set<String> getCriteriaRefs() {
        return criteriaRefs;
    }

    public void setCriteriaRefs(Set<String> criteriaRefs) {
        this.criteriaRefs = criteriaRefs;
    }
    
    @Override
    public void addEntryCriterion(Criterion entryCriterion) {
        this.entryCriteria.add(entryCriterion);
    }

    @Override
    public List<Criterion> getEntryCriteria() {
        return entryCriteria;
    }

    @Override
    public void setEntryCriteria(List<Criterion> entryCriteria) {
        this.entryCriteria = entryCriteria;
    }
    
    @Override
    public void addExitCriterion(Criterion exitCriterion) {
        this.exitCriteria.add(exitCriterion);
    }
    
    @Override
    public List<Criterion> getExitCriteria() {
        return exitCriteria;
    }

    @Override
    public void setExitCriteria(List<Criterion> exitCriteria) {
        this.exitCriteria = exitCriteria;
    }
    
    @Override
    public void addIncomingAssociation(Association association) {
        this.incomingAssociations.add(association);
    }
    
    @Override
    public List<Association> getIncomingAssociations() {
        return incomingAssociations;
    }
    
    @Override
    public void setIncomingAssociations(List<Association> incomingAssociations) {
        this.incomingAssociations = incomingAssociations;
    }
    
    @Override
    public void addOutgoingAssociation(Association association) {
        this.outgoingAssociations.add(association);
    }
    
    @Override
    public List<Association> getOutgoingAssociations() {
        return outgoingAssociations;
    }
    
    @Override
    public void setOutgoingAssociations(List<Association> outgoingAssociations) {
        this.outgoingAssociations = outgoingAssociations;
    }

    public Object getBehavior() {
        return behavior;
    }

    public void setBehavior(Object behavior) {
        this.behavior = behavior;
    }

    public List<PlanItem> getEntryDependencies() {
        return entryDependencies;
    }

    public void setEntryDependencies(List<PlanItem> entryDependencies) {
        this.entryDependencies = entryDependencies;
    }

    public List<PlanItem> getExitDependencies() {
        return exitDependencies;
    }

    public void setExitDependencies(List<PlanItem> exitDependencies) {
        this.exitDependencies = exitDependencies;
    }

    public List<PlanItem> getEntryDependentPlanItems() {
        return entryDependentPlanItems;
    }

    public void setEntryDependentPlanItems(List<PlanItem> entryDependentPlanItems) {
        this.entryDependentPlanItems = entryDependentPlanItems;
    }

    public void addEntryDependentPlanItem(PlanItem planItem) {
        Optional<PlanItem> planItemWithSameId = entryDependentPlanItems.stream().filter(p -> p.getId().equals(planItem.getId())).findFirst();
        if (!planItemWithSameId.isPresent()) {
            entryDependentPlanItems.add(planItem);
        }
    }

    public List<PlanItem> getExitDependentPlanItems() {
        return exitDependentPlanItems;
    }

    public void setExitDependentPlanItems(List<PlanItem> exitDependentPlanItems) {
        this.exitDependentPlanItems = exitDependentPlanItems;
    }

    public void addExitDependentPlanItem(PlanItem planItem) {
        Optional<PlanItem> planItemWithSameId = exitDependentPlanItems.stream().filter(p -> p.getId().equals(planItem.getId())).findFirst();
        if (!planItemWithSameId.isPresent()) {
            exitDependentPlanItems.add(planItem);
        }
    }

    public List<PlanItem> getAllDependentPlanItems() {
        List<PlanItem> allDependentPlanItems = new ArrayList<>(entryDependentPlanItems.size() + exitDependentPlanItems.size());
        allDependentPlanItems.addAll(entryDependentPlanItems);
        allDependentPlanItems.addAll(exitDependentPlanItems);
        return allDependentPlanItems;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("PlanItem ");
        if (getName() != null) {
            stringBuilder.append("'").append(getName()).append("'");
        }

        stringBuilder.append(" (id: ");
        stringBuilder.append(getId());

        if (getPlanItemDefinition() != null) {
            stringBuilder.append(", definitionId: ").append(getPlanItemDefinition().getId());
        }

        stringBuilder.append(")");
        return stringBuilder.toString();
    }
    
}
