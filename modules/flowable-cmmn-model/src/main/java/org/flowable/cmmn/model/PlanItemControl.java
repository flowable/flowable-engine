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
public class PlanItemControl extends CaseElement {
    
    protected RequiredRule requiredRule;
    protected RepetitionRule repetitionRule;
    protected ManualActivationRule manualActivationRule;
    protected CompletionNeutralRule completionNeutralRule;
    
    public RequiredRule getRequiredRule() {
        return requiredRule;
    }

    public void setRequiredRule(RequiredRule requiredRule) {
        this.requiredRule = requiredRule;
    }

    public RepetitionRule getRepetitionRule() {
        return repetitionRule;
    }

    public void setRepetitionRule(RepetitionRule repetitionRule) {
        this.repetitionRule = repetitionRule;
    }

    public ManualActivationRule getManualActivationRule() {
        return manualActivationRule;
    }

    public void setManualActivationRule(ManualActivationRule manualActivationRule) {
        this.manualActivationRule = manualActivationRule;
    }

    public CompletionNeutralRule getCompletionNeutralRule() {
        return completionNeutralRule;
    }

    public void setCompletionNeutralRule(CompletionNeutralRule completionNeutralRule) {
        this.completionNeutralRule = completionNeutralRule;
    }
}
