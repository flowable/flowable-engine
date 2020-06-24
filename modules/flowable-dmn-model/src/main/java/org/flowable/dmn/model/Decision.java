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
public class Decision extends DRGElement {

    protected String question;
    protected String allowedAnswers;
    protected InformationItem variable;
    protected List<InformationRequirement> requiredDecisions = new ArrayList<>();
    protected List<InformationRequirement> requiredInputs = new ArrayList<>();
    protected List<AuthorityRequirement> authorityRequirements = new ArrayList<>();
    protected Expression expression;
    protected boolean forceDMN11;

    @JsonIgnore
    protected DmnDefinition dmnDefinition;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAllowedAnswers() {
        return allowedAnswers;
    }

    public void setAllowedAnswers(String allowedAnswers) {
        this.allowedAnswers = allowedAnswers;
    }

    public InformationItem getVariable() {
        return variable;
    }
    public void setVariable(InformationItem variable) {
        this.variable = variable;
    }

    public List<InformationRequirement> getRequiredDecisions() {
        return requiredDecisions;
    }
    public void setRequiredDecisions(List<InformationRequirement> requiredDecisions) {
        this.requiredDecisions = requiredDecisions;
    }
    public void addRequiredDecision(InformationRequirement requiredDecision) {
        this.requiredDecisions.add(requiredDecision);
    }
    public List<InformationRequirement> getRequiredInputs() {
        return requiredInputs;
    }
    public void setRequiredInputs(List<InformationRequirement> requiredInputs) {
        this.requiredInputs = requiredInputs;
    }
    public void addRequiredInput(InformationRequirement requiredInput) {
        this.requiredInputs.add(requiredInput);
    }
    public List<AuthorityRequirement> getAuthorityRequirements() {
        return authorityRequirements;
    }
    public void setAuthorityRequirements(List<AuthorityRequirement> authorityRequirements) {
        this.authorityRequirements = authorityRequirements;
    }

    public void addAuthorityRequirement(AuthorityRequirement authorityRequirement) {
        this.authorityRequirements.add(authorityRequirement);
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public boolean isForceDMN11() {
        return forceDMN11;
    }
    public void setForceDMN11(boolean forceDMN11) {
        this.forceDMN11 = forceDMN11;
    }

    @JsonIgnore
    public DmnDefinition getDmnDefinition() {
        return dmnDefinition;
    }
    public void setDmnDefinition(DmnDefinition dmnDefinition) {
        this.dmnDefinition = dmnDefinition;
    }
}
