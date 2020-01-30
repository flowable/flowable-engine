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

public class ParentCompletionRule extends PlanItemRule {
    
    public static final String DEFAULT = "default";
    public static final String IGNORE = "ignore";
    public static final String IGNORE_IF_AVAILABLE = "ignoreIfAvailable";
    public static final String IGNORE_IF_AVAILABLE_OR_ENABLED = "ignoreIfAvailableOrEnabled";
    public static final String IGNORE_AFTER_FIRST_COMPLETION = "ignoreAfterFirstCompletion";
    public static final String IGNORE_AFTER_FIRST_COMPLETION_IF_AVAILABLE_OR_ENABLED = "ignoreAfterFirstCompletionIfAvailableOrEnabled";

    protected String type;
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ParentCompletionRule{} " + super.toString();
    }
}
