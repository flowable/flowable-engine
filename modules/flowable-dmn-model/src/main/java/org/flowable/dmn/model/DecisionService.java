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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Yvo Swillens
 */
public class DecisionService extends Invocable {

    protected List<DmnElementReference> outputDecisions = new ArrayList<>();
    protected List<DmnElementReference> encapsulatedDecisions = new ArrayList<>();
    protected List<DmnElementReference> inputDecisions = new ArrayList<>();
    protected List<DmnElementReference> inputData = new ArrayList<>();

    @JsonIgnore
    protected DmnDefinition dmnDefinition;

    public List<DmnElementReference> getOutputDecisions() {
        return outputDecisions;
    }
    public void setOutputDecisions(List<DmnElementReference> outputDecisions) {
        this.outputDecisions = outputDecisions;
    }
    public void addOutputDecision(DmnElementReference outputDecision) {
        this.outputDecisions.add(outputDecision);
    }
    public List<DmnElementReference> getEncapsulatedDecisions() {
        return encapsulatedDecisions;
    }
    public void setEncapsulatedDecisions(List<DmnElementReference> encapsulatedDecisions) {
        this.encapsulatedDecisions = encapsulatedDecisions;
    }
    public void addEncapsulatedDecision(DmnElementReference encapsulatedDecision) {
        this.encapsulatedDecisions.add(encapsulatedDecision);
    }
    public List<DmnElementReference> getInputDecisions() {
        return inputDecisions;
    }
    public void setInputDecisions(List<DmnElementReference> inputDecisions) {
        this.inputDecisions = inputDecisions;
    }
    public void addInputDecision(DmnElementReference inputDecision) {
        this.inputDecisions.add(inputDecision);
    }
    public List<DmnElementReference> getInputData() {
        return inputData;
    }
    public void setInputData(List<DmnElementReference> inputData) {
        this.inputData = inputData;
    }
    public void addInputData(DmnElementReference inputData) {
        this.inputData.add(inputData);
    }

    @JsonIgnore
    public DmnDefinition getDmnDefinition() {
        return dmnDefinition;
    }

    public void setDmnDefinition(DmnDefinition dmnDefinition) {
        this.dmnDefinition = dmnDefinition;
    }
}
