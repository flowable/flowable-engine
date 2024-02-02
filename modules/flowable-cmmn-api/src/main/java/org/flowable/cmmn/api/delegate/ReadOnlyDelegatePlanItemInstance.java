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
package org.flowable.cmmn.api.delegate;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * @author Filip Hrisafov
 */
public interface ReadOnlyDelegatePlanItemInstance extends PlanItemInstance, VariableContainer {

    PlanItem getPlanItem();

    default PlanItemDefinition getPlanItemDefinition() {
        PlanItem planItem = getPlanItem();
        if (planItem != null) {
            return planItem.getPlanItemDefinition();
        }

        return null;
    }

    @Override
    default void setVariable(String variableName, Object variableValue) {
        throw new UnsupportedOperationException("Setting variable is not supported for read only delegate execution");
    }

    @Override
    default void setTransientVariable(String variableName, Object variableValue) {
        throw new UnsupportedOperationException("Setting transient variable is not supported for read only delegate execution");
    }

    @Override
    default void setLocalizedName(String localizedName) {
        throw new UnsupportedOperationException("Setting localized name is not supported for read only delegate execution");
    }
}
