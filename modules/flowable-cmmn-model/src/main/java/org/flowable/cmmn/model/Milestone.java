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

/**
 * @author Joram Barrez
 */
public class Milestone extends PlanItemDefinition {
    
    protected Integer displayOrder;
    protected String includeInStageOverview;
    protected String milestoneVariable;
    
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getIncludeInStageOverview() {
        return includeInStageOverview;
    }

    public void setIncludeInStageOverview(String includeInStageOverview) {
        this.includeInStageOverview = includeInStageOverview;
    }

    public String getMilestoneVariable() {
        return milestoneVariable;
    }

    public void setMilestoneVariable(String milestoneVariable) {
        this.milestoneVariable = milestoneVariable;
    }

}
