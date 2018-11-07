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
package org.flowable.cmmn.engine.impl.behavior;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * Behavior interface, like the {@link CmmnActivityBehavior} and {@link CmmnTriggerableActivityBehavior} interfaces,
 * exposing even more low-level hookpoints to add custom logic.
 *
 * Implementations of this interface will also get notified of transitions between plan item
 * lifecycle states and can add custom logic if needed.
 * 
 * The transitions supported by the engine are found in the {@link PlanItemTransition} class.
 * 
 * @author Joram Barrez
 */
public interface PlanItemActivityBehavior extends CmmnTriggerableActivityBehavior {
 
    void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition);
    
}
