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
package org.flowable.dmn.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yvo Swillens
 */
public class DmnDefinition extends NamedElement {

    protected String expressionLanguage;
    protected String typeLanguage;
    protected String namespace;
    protected List<InputData> inputData = new ArrayList<>();
    protected List<ItemDefinition> itemDefinitions = new ArrayList<>();
    protected List<Decision> decisions = new ArrayList<>();
    protected List<DecisionService> decisionServices = new ArrayList<>();

    public String getExpressionLanguage() {
        return expressionLanguage;
    }

    public void setExpressionLanguage(String expressionLanguage) {
        this.expressionLanguage = expressionLanguage;
    }

    public String getTypeLanguage() {
        return typeLanguage;
    }

    public void setTypeLanguage(String typeLanguage) {
        this.typeLanguage = typeLanguage;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<InputData> getInputData() {
        return inputData;
    }
    public void setInputData(List<InputData> inputData) {
        this.inputData = inputData;
    }
    public void addInputData(InputData inputData) {
        this.inputData.add(inputData);
    }
    public void setItemDefinitions(List<ItemDefinition> itemDefinitions) {
        this.itemDefinitions = itemDefinitions;
    }
    public List<ItemDefinition> getItemDefinitions() {
        return itemDefinitions;
    }

    public void addItemDefinition(ItemDefinition itemDefinition) {
        this.itemDefinitions.add(itemDefinition);
    }

    public List<Decision> getDecisions() {
        return decisions;
    }

    public void addDecision(Decision decision) {
        this.decisions.add(decision);
    }

    public Decision getDecisionById(String id) {
        for (Decision decision : decisions) {
            if (id.equals(decision.getId())) {
                return decision;
            }
        }
        return null;
    }

    public List<DecisionService> getDecisionServices() {
        return decisionServices;
    }

    public void addDecisionService(DecisionService decisionService) {
        this.decisionServices.add(decisionService);
    }

    public DecisionService getDecisionServiceById(String id) {
        for (DecisionService decisionService : decisionServices) {
            if (id.equals((decisionService.getId()))) {
                return decisionService;
            }
        }
        return null;
    }
}
