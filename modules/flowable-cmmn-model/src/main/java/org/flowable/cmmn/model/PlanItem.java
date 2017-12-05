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
import java.util.List;

/**
 * @author Joram Barrez
 */
public class PlanItem extends CaseElement implements HasEntryCriteria, HasExitCriteria, HasAssociations {
    
    protected String definitionRef;
    protected PlanItemDefinition planItemDefinition;
    protected PlanItemControl itemControl;
    protected List<String> criteriaRefs = new ArrayList<>();
    protected List<Criterion> entryCriteria = new ArrayList<>();
    protected List<Criterion> exitCriteria = new ArrayList<>();
    protected List<Association> incomingAssociations = new ArrayList<>();
    protected List<Association> outgoingAssociations = new ArrayList<>();
    
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
    
    public List<String> getCriteriaRefs() {
        return criteriaRefs;
    }

    public void setCriteriaRefs(List<String> criteriaRefs) {
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
    
    @Override
    public String toString() {
        return "PlanItem " + id + (name != null ? (" " + name) : "");
    }
    
}
