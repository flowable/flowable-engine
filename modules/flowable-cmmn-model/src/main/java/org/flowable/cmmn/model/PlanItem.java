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
public class PlanItem extends CaseElement implements HasEntryCriteria, HasExitCriteria {
    
    protected String name;
    protected String definitionRef;
    protected PlanItemDefinition planItemDefinition;
    protected List<Criterion> entryCriteria = new ArrayList<>();
    protected List<Criterion> exitCriteria = new ArrayList<>();
    
    protected Object behavior;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
    
    public List<Criterion> getEntryCriteria() {
        return entryCriteria;
    }

    public void setEntryCriteria(List<Criterion> entryCriteria) {
        this.entryCriteria = entryCriteria;
    }
    
    public List<Criterion> getExitCriteria() {
        return exitCriteria;
    }

    public void setExitCriteria(List<Criterion> exitCriteria) {
        this.exitCriteria = exitCriteria;
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
