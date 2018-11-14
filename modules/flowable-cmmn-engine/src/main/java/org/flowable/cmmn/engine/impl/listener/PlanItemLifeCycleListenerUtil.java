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
package org.flowable.cmmn.engine.impl.listener;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class PlanItemLifeCycleListenerUtil {

    public static void callLifecycleListeners(CommandContext commandContext, PlanItemInstance planItemInstance, String oldState, String newState) {
        if (Objects.equals(oldState, newState)) {
            return;
        }

        // Lifecycle listeners defined on the cmmn engine configuration
        Map<String, List<PlanItemInstanceLifecycleListener>> planItemInstanceLifeCycleListeners
            = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getPlanItemInstanceLifecycleListeners();
        if (planItemInstanceLifeCycleListeners != null && !planItemInstanceLifeCycleListeners.isEmpty()) {

            List<PlanItemInstanceLifecycleListener> specificListeners = planItemInstanceLifeCycleListeners.get(planItemInstance.getPlanItemDefinitionType());
            executeListeners(specificListeners, planItemInstance, oldState, newState);

            List<PlanItemInstanceLifecycleListener> genericListeners = planItemInstanceLifeCycleListeners.get(null);
            executeListeners(genericListeners, planItemInstance, oldState, newState);

        }
    }

    public static void executeListeners(List<PlanItemInstanceLifecycleListener> listeners, PlanItemInstance planItemInstance, String oldState, String newState) {
        if (listeners != null) {
            for (PlanItemInstanceLifecycleListener lifecycleListener : listeners) {
                if (lifecycleListenerMatches(lifecycleListener, oldState, newState)) {
                    lifecycleListener.stateChanged(planItemInstance, oldState, newState);
                }
            }
        }
    }

    protected static boolean lifecycleListenerMatches(PlanItemInstanceLifecycleListener lifecycleListener, String oldState, String newState) {
        return stateMatches(lifecycleListener.getSourceState(), oldState) && stateMatches(lifecycleListener.getTargetState(), newState);
    }

    protected static boolean stateMatches(String listenerExpectedState, String actualState) {
        return listenerExpectedState == null || Objects.equals(actualState, listenerExpectedState);
    }

}
