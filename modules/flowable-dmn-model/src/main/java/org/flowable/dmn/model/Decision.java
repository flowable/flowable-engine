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
public class Decision extends NamedElement {

    protected String question;
    protected String allowedAnswers;
    protected List<InformationRequirement> informationRequirements = new ArrayList<>();
    protected Expression expression;

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

    public void addInformationRequirement(InformationRequirement informationRequirement) {
        this.informationRequirements.add(informationRequirement);
    }

    public List<InformationRequirement> getInformationRequirements() {
        return this.informationRequirements;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }
}
