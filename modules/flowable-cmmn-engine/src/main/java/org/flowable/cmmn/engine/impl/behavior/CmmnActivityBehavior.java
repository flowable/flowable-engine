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
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgenda;
import org.flowable.cmmn.model.PlanItem;

/**
 * Root interface for all classes implementing logic when the CMMN engine 
 * decides the behavior for a plan item needs to be executed.
 * 
 * The main difference with the {@link PlanItemJavaDelegate} interface is 
 * that the implementation of this {@link #execute(DelegatePlanItemInstance)} methods
 * is expected to plan the next operation on the {@link CmmnEngineAgenda}, 
 * while for the {@link PlanItemJavaDelegate} the engine will automatically
 * plan a completion after execution the logic.
 * 
 * Concrete implementations of this class will be set on the {@link PlanItem}
 * in the case model during parsing.
 * 
 * Implementations of this class are assumed by the engine to not have wait state behavior,
 * use the {@link CmmnTriggerableActivityBehavior} when this behavior is needed.
 * 
 * @author Joram Barrez
 */
public interface CmmnActivityBehavior {
    
    void execute(DelegatePlanItemInstance delegatePlanItemInstance);
    
}
