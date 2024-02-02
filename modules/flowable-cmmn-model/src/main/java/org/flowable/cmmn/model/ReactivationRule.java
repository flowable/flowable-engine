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

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * The reactivation rule describes how a plan item is reactivated during phase two of case reactivation. There are three possible conditions:
 * <ul>
 *     <li><code>activateCondition</code> if evaluated to true, immediately activates the plan item, even if it contains conditions or entry sentries</li>
 *     <li><code>ignoreCondition</code> if evaluated to true, completely ignores the plan item</li>
 *     <li><code>defaultCondition</code> if evaluated to true, will treat the plan item the very same way as if the case was newly created</li>
 * </ul>
 * If the condition is <code>null</code>, it is evaluated as <code>false</code>. You might also just put <code>true</code> as the condition to permanently
 * activate it without runtime evaluation.
 * If more than one evaluates to <code>true</code>, they are treated in the following order: activate, ignore and then default, which means as an example,
 * if the activate condition evaluates to true, but the default one as well, the activate one has precedence, so the plan item immediately gets activated.
 *
 * @author Micha Kiener
 */
public class ReactivationRule extends PlanItemRule {

    protected String activateCondition;
    protected String ignoreCondition;
    protected String defaultCondition;

    public ReactivationRule() {
    }

    public ReactivationRule(String activateCondition, String ignoreCondition, String defaultCondition) {
        this.activateCondition = activateCondition;
        this.ignoreCondition = ignoreCondition;
        this.defaultCondition = defaultCondition;
    }

    public boolean hasActivationRule() {
        return StringUtils.isNotEmpty(activateCondition);
    }
    public boolean hasIgnoreRule() {
        return StringUtils.isNotEmpty(ignoreCondition);
    }
    public boolean hasDefaultRule() {
        return StringUtils.isNotEmpty(defaultCondition);
    }

    public boolean hasActivationCondition() {
        return hasActivationRule() && (activateCondition.contains("#") || activateCondition.contains("$"));
    }
    public boolean hasIgnoreCondition() {
        return hasIgnoreRule() && (ignoreCondition.contains("#") || ignoreCondition.contains("$"));
    }
    public boolean hasDefaultCondition() {
        return hasDefaultRule() && (defaultCondition.contains("#") || defaultCondition.contains("$"));
    }

    public String getDefaultCondition() {
        return defaultCondition;
    }
    public void setDefaultCondition(String defaultCondition) {
        this.defaultCondition = defaultCondition;
    }
    public String getActivateCondition() {
        return activateCondition;
    }
    public void setActivateCondition(String activateCondition) {
        this.activateCondition = activateCondition;
    }
    public String getIgnoreCondition() {
        return ignoreCondition;
    }
    public void setIgnoreCondition(String ignoreCondition) {
        this.ignoreCondition = ignoreCondition;
    }

    @Override
    public String toString() {
        return "ReactivationRule{} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReactivationRule that = (ReactivationRule) o;
        return Objects.equals(activateCondition, that.activateCondition) && Objects.equals(ignoreCondition, that.ignoreCondition)
            && Objects.equals(defaultCondition, that.defaultCondition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activateCondition, ignoreCondition, defaultCondition);
    }
}
