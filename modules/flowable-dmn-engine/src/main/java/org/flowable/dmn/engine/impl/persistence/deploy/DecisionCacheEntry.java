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
package org.flowable.dmn.engine.impl.persistence.deploy;

import java.io.Serializable;

import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DmnDefinition;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class DecisionCacheEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    protected DecisionEntity decisionEntity;
    protected DmnDefinition dmnDefinition;
    protected DecisionService decisionService;
    protected Decision decision;

    public DecisionCacheEntry(DecisionEntity decisionEntity, DmnDefinition dmnDefinition, DecisionService decisionService) {
        this.decisionEntity = decisionEntity;
        this.dmnDefinition = dmnDefinition;
        this.decisionService = decisionService;
    }

    public DecisionCacheEntry(DecisionEntity decisionEntity, DmnDefinition dmnDefinition, Decision decision) {
        this.decisionEntity = decisionEntity;
        this.dmnDefinition = dmnDefinition;
        this.decision = decision;
    }

    public DecisionEntity getDecisionEntity() {
        return decisionEntity;
    }

    public void setDecisionEntity(DecisionEntity decisionEntity) {
        this.decisionEntity = decisionEntity;
    }

    public DmnDefinition getDmnDefinition() {
        return dmnDefinition;
    }

    public void setDmnDefinition(DmnDefinition dmnDefinition) {
        this.dmnDefinition = dmnDefinition;
    }

    public DecisionService getDecisionService() {
        return decisionService;
    }
    public void setDecisionService(DecisionService decisionService) {
        this.decisionService = decisionService;
    }
    public Decision getDecision() {
        return decision;
    }
    public void setDecision(Decision decision) {
        this.decision = decision;
    }
}
