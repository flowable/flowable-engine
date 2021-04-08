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
 * The reactivation rule describes how a plan item is reactivated during phase two of case reactivation.
 *
 * @author Micha Kiener
 */
public class ReactivationRule extends PlanItemRule {

    public static final String SKIP = "skip";
    public static final String REACTIVATE = "reactivate";
    public static final String REACTIVATE_IF_NOT_COMPLETED = "reactivateIfNotCompleted";

    protected String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ReactivationRule{} " + super.toString();
    }
}
