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
import org.flowable.cmmn.model.PlanItemTransition;

/**
 * @author Joram Barrez
 */
public class StateTransition {
    
    public static Map<String, Set<String>> TRANSITIONS = new HashMap<>();
    
    // See 8.4.2 of CMMN 1.1 spec
    
    static {
        addTransition(null, PlanItemTransition.CREATE);
        addTransition(PlanItemInstanceState.WAITING_FOR_REPETITION, PlanItemTransition.CREATE);
        
        addTransition(PlanItemInstanceState.AVAILABLE, 
                PlanItemTransition.START, 
                PlanItemTransition.ENABLE, 
                PlanItemTransition.PARENT_SUSPEND, 
                PlanItemTransition.EXIT);
        
        addTransition(PlanItemInstanceState.ENABLED, 
                PlanItemTransition.DISABLE, 
                PlanItemTransition.MANUAL_START, 
                PlanItemTransition.PARENT_SUSPEND, 
                PlanItemTransition.EXIT);
        
        addTransition(PlanItemInstanceState.DISABLED, 
                PlanItemTransition.REENABLE, 
                PlanItemTransition.PARENT_SUSPEND, 
                PlanItemTransition.EXIT);
        
        addTransition(PlanItemInstanceState.ACTIVE, 
                PlanItemTransition.FAULT, 
                PlanItemTransition.COMPLETE, 
                PlanItemTransition.SUSPEND, 
                PlanItemTransition.TERMINATE, 
                PlanItemTransition.PARENT_SUSPEND, 
                PlanItemTransition.EXIT);
        
        addTransition(PlanItemInstanceState.ASYNC_ACTIVE, 
                PlanItemTransition.ASYNC_ACTIVATE);
        
        addTransition(PlanItemInstanceState.FAILED, 
                PlanItemTransition.REACTIVATE, 
                PlanItemTransition.EXIT);
        
        addTransition(PlanItemInstanceState.SUSPENDED, 
                PlanItemTransition.RESUME, 
                PlanItemTransition.PARENT_RESUME, 
                PlanItemTransition.EXIT);
        
        addTransition(PlanItemInstanceState.COMPLETED);
        
        addTransition(PlanItemInstanceState.TERMINATED);
    }
    
    protected static void addTransition(String state, String...transitions) {
        TRANSITIONS.put(state, new HashSet<>(transitions.length));
        for (String transition : transitions) {
            TRANSITIONS.get(state).add(transition);
        }
    }
    
    public static boolean isPossible(PlanItemInstance planItemInstance, String transition) {
        return isPossible(planItemInstance.getState(), transition);
    }
    
    public static boolean isPossible(String currentState, String transition) {
        return TRANSITIONS.get(currentState).contains(transition);
    }

}
