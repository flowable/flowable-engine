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

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.model.PlanItem;

/**
 * Behavior interface, like {@link CmmnActivityBehavior}, when the CMMN engine 
 * decides the behavior for a plan item needs to be executed and the behavior 
 * acts as a wait state. 
 * 
 * This means that after the {@link #execute(DelegatePlanItemInstance)} method is called,
 * the engine will not automatically complete the corresponding {@link PlanItemInstance}
 * as happens for the {@link CmmnActivityBehavior} implementations.
 * 
 * Note that 'triggering' a plan item that acts as a wait state is not part 
 * of the CMMN specification, but has been added as an explicit concept to mimic
 * the concept of 'triggering' used by the process engine.
 * 
 * Any plan item that implements this interface should be triggereable programmatically 
 * through the {@link CmmnRuntimeService#triggerPlanItemInstance(String)} method.
 *
 * Concrete implementations of this class will be set on the {@link PlanItem}
 * in the case model during parsing.
 * 
 * @author Joram Barrez
 */
public interface CmmnTriggerableActivityBehavior extends CmmnActivityBehavior {
    
    void trigger(DelegatePlanItemInstance planItemInstance);

}
