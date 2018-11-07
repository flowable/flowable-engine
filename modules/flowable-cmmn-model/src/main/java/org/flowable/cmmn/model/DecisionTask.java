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
 * @author martin.grofcik
 */
public class DecisionTask extends TaskWithFieldExtensions {

    protected String decisionRefExpression;
    protected String decisionRef;
    protected Decision decision;

    public String getDecisionRefExpression() {
        return decisionRefExpression;
    }

    public void setDecisionRefExpression(String decisionRefExpression) {
        this.decisionRefExpression = decisionRefExpression;
    }

    public String getDecisionRef() {
        return decisionRef;
    }

    public void setDecisionRef(String decisionRef) {
        this.decisionRef = decisionRef;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }
}
