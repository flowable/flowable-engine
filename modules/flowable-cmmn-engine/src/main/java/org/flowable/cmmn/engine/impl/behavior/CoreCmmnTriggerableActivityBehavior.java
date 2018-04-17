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
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * Superclass for {@link CmmnTriggerableActivityBehavior} implementations that are part of the 
 * core CMMN behaviors of the CMMN Engine.
 *
 * More specifically, subclasses needed to implement {@link #trigger(CommandContext, DelegatePlanItemInstance)}
 * instead of {@link #trigger(DelegatePlanItemInstance)} and thus avoid an extra lookup of the thread-local {@link CommandContext}.
 * Instead of passing a {@link DelegatePlanItemInstance}, a 'raw' {@link PlanItemInstanceEntity} is also passed.
 * 
 * @author Joram Barrez
 */
public abstract class CoreCmmnTriggerableActivityBehavior extends CoreCmmnActivityBehavior implements CmmnTriggerableActivityBehavior {

    @Override
    public void trigger(DelegatePlanItemInstance planItemInstance) {
        trigger(CommandContextUtil.getCommandContext(), (PlanItemInstanceEntity) planItemInstance);
    }
    
    public abstract void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstance);
    
}
