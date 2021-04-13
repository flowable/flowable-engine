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

    public ReactivationRule getDefaultReactivationRule() {
        return defaultReactivationRule;
    }

    public void setDefaultReactivationRule(ReactivationRule defaultReactivationRule) {
        this.defaultReactivationRule = defaultReactivationRule;
    }
}
