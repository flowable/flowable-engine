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
 * The reactivation listener is a very specific user event listener available on a historic case instance in order to reactivate it again.
 *
 * @author Micha Kiener
 */
public class ReactivateEventListener extends UserEventListener {

    /**
     * The optional, default reactivation rule to be considered, if a plan item does not specify an explicit one, if this one is not provided either, such
     * a plan item will be ignored for reactivation.
     */
    protected ReactivationRule defaultReactivationRule;

    /**
     * If there is an available condition set for the reactivate event listener as part of the model, it will end up here as the default available
     * condition of the generic event listener will be predefined for a reactivate event listener making it unavailable as long as the case is active.
     */
    protected String reactivationAvailableConditionExpression;

    public ReactivationRule getDefaultReactivationRule() {
        return defaultReactivationRule;
    }

    public void setDefaultReactivationRule(ReactivationRule defaultReactivationRule) {
        this.defaultReactivationRule = defaultReactivationRule;
    }

    public String getReactivationAvailableConditionExpression() {
        return reactivationAvailableConditionExpression;
    }
    
    public void setReactivationAvailableConditionExpression(String reactivationAvailableConditionExpression) {
        this.reactivationAvailableConditionExpression = reactivationAvailableConditionExpression;
    }
}
