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

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * Implement this behavior interface if you want to hook into the end semantics of a plan item depending on its parent ending transition. As an example, the
 * case page behavior implements this interface to control the delegated ending whenever its parent (e.g. a stage or the root plan model) gets completed or
 * terminated.
 * By implementing this interface, you overwrite the default behavior which delegates the ending to its children by terminating them, regardless, if it got
 * completed or terminated. It also means you MUST either complete, terminate or exit the plan item, otherwise it would be left in a wrong state when its
 * parent gets ended.
 *
 * @author Micha Kiener
 */
public interface OnParentEndDependantActivityBehavior {

    /**
     * This method will be triggered on a child plan item instance whenever its parent transitions to an ending state with all necessary information to
     * implement the necessary behavior. MAKE SURE that you will put the provided plan item instance to an ending state by triggering the appropriate
     * operation on the agenda like
     * {@link org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgenda#planTerminatePlanItemInstanceOperation(PlanItemInstanceEntity, String, String)} or
     * {@link org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgenda#planCompletePlanItemInstanceOperation(PlanItemInstanceEntity)}.
     *
     * @param commandContext the command context under which this hook gets invoked
     * @param planItemInstanceEntity the plan item instance to put into an ending state
     * @param parentEndTransition the transition of the parent plan item instance to its ending state as it might have an impact on how to end this plan item instance
     * @param exitEventType the optional exit event type (e.g. {@link org.flowable.cmmn.model.Criterion#EXIT_EVENT_TYPE_COMPLETE}, etc) if the parent was ended
     *      through an exit sentry, the exit event type will contain information on how exactly the exit was triggered (e.g. exit or complete, etc)
     */
    void onParentEnd(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, String parentEndTransition, String exitEventType);
}
