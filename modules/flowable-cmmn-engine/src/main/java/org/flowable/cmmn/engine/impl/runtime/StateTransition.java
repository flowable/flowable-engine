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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.PlanItemTransition;

/**
 * @author Joram Barrez
 */
public class StateTransition {
    
    public static Map<String, Set<String>> PLAN_ITEM_TRANSITIONS = new HashMap<>();
    
    // See 8.4.2 of CMMN 1.1 spec
    
    static {
        addPlanItemTransition(null, PlanItemTransition.CREATE);
        addPlanItemTransition(PlanItemInstanceState.WAITING_FOR_REPETITION, PlanItemTransition.CREATE);
        
        addPlanItemTransition(PlanItemInstanceState.AVAILABLE,
                PlanItemTransition.START, 
                PlanItemTransition.ENABLE, 
                PlanItemTransition.PARENT_SUSPEND, 
                PlanItemTransition.EXIT);
        
        addPlanItemTransition(PlanItemInstanceState.ENABLED,
                PlanItemTransition.DISABLE, 
                PlanItemTransition.MANUAL_START, 
                PlanItemTransition.PARENT_SUSPEND, 
                PlanItemTransition.EXIT);
        
        addPlanItemTransition(PlanItemInstanceState.DISABLED,
                PlanItemTransition.REENABLE, 
                PlanItemTransition.PARENT_SUSPEND, 
                PlanItemTransition.EXIT);
        
        addPlanItemTransition(PlanItemInstanceState.ACTIVE,
                PlanItemTransition.FAULT, 
                PlanItemTransition.COMPLETE, 
                PlanItemTransition.SUSPEND, 
                PlanItemTransition.TERMINATE, 
                PlanItemTransition.PARENT_SUSPEND, 
                PlanItemTransition.EXIT);
        
        addPlanItemTransition(PlanItemInstanceState.ASYNC_ACTIVE,
                PlanItemTransition.ASYNC_ACTIVATE);
        
        addPlanItemTransition(PlanItemInstanceState.FAILED,
                PlanItemTransition.REACTIVATE, 
                PlanItemTransition.EXIT);
        
        addPlanItemTransition(PlanItemInstanceState.SUSPENDED,
                PlanItemTransition.RESUME, 
                PlanItemTransition.PARENT_RESUME, 
                PlanItemTransition.EXIT);
        
        addPlanItemTransition(PlanItemInstanceState.COMPLETED);
        
        addPlanItemTransition(PlanItemInstanceState.TERMINATED);
    }

    public static Map<String, Set<String>> EVENT_LISTENER_TRANSITIONS = new HashMap<>();

    static {

        addEventListenerTransition(null, PlanItemTransition.CREATE);

        addEventListenerTransition(PlanItemInstanceState.UNAVAILABLE,
            PlanItemTransition.INITIATE,
            PlanItemTransition.TERMINATE,
            PlanItemTransition.EXIT,
            PlanItemTransition.SUSPEND);

        addEventListenerTransition(PlanItemInstanceState.AVAILABLE,
            PlanItemTransition.DISMISS,
            PlanItemTransition.TERMINATE,
            PlanItemTransition.OCCUR,
            PlanItemTransition.EXIT,
            PlanItemTransition.SUSPEND);

        addEventListenerTransition(PlanItemInstanceState.SUSPENDED,
            PlanItemTransition.RESUME,
            PlanItemTransition.EXIT,
            PlanItemTransition.TERMINATE);

        addEventListenerTransition(PlanItemInstanceState.COMPLETED);

        addEventListenerTransition(PlanItemInstanceState.TERMINATED);
    }
    
    protected static void addPlanItemTransition(String state, String...transitions) {
        PLAN_ITEM_TRANSITIONS.put(state, new HashSet<>(transitions.length));
        for (String transition : transitions) {
            PLAN_ITEM_TRANSITIONS.get(state).add(transition);
        }
    }

    protected static void addEventListenerTransition(String state, String...transitions) {
        EVENT_LISTENER_TRANSITIONS.put(state, new HashSet<>(transitions.length));
        for (String transition : transitions) {
            EVENT_LISTENER_TRANSITIONS.get(state).add(transition);
        }
    }
    
    public static boolean isPossible(PlanItemInstance planItemInstance, String transition) {
        if (((PlanItemInstanceEntity) planItemInstance).getPlanItem().getPlanItemDefinition() instanceof EventListener) {
            return isEventListenerTransitionPossible(planItemInstance.getState(), transition);
        } else {
            return isPlanItemTransitionPossible(planItemInstance.getState(), transition);
        }
    }
    
    protected static boolean isPlanItemTransitionPossible(String currentState, String transition) {
        return PLAN_ITEM_TRANSITIONS.get(currentState).contains(transition);
    }

    protected static boolean isEventListenerTransitionPossible(String currentState, String transition) {
        return EVENT_LISTENER_TRANSITIONS.get(currentState).contains(transition);
    }

}
