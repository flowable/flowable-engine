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
package org.flowable.cmmn.api.listener;

import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemInstance;

/**
 * [Experimental]
 *
 * An interface for listener implementations that get notified when the state of a plan item instance changes.
 * The listener can be registered in the configuration of the engine.
 *
 * @author Joram Barrez
 */
public interface PlanItemInstanceLifeCycleListener {

    /**
     * @return The list of types (from {@link org.flowable.cmmn.api.runtime.PlanItemDefinitionType)} this listener listens for.
     *         If an empty list or null is returned, all types will be assumed.
     */
    List<String> getItemDefinitionTypes();

    /**
     * @return The type a plan item instance is changing from, use a value from {@link org.flowable.cmmn.api.runtime.PlanItemInstanceState}.
     *         This listener will only receive elements where the state changing from this value to another one.
     *         Return null or the empty String to listen to any state.
     */
    String getSourceState();

    /**
     * @return The type a plan item instance is changing to, use a value from {@link org.flowable.cmmn.api.runtime.PlanItemInstanceState}.
     *         This listener will only receive elements where the state changing from this value to another one.
     *         Return null or the empty String to listen to any state.
     */
    String getTargetState();

    /**
     * Will be called when the state of a {@link PlanItemInstance} changes, if the {@link #getItemDefinitionTypes()} and the
     * {@link #getSourceState()} and {@link #getTargetState()} match.
     */
    void stateChanged(PlanItemInstance planItemInstance, String oldState, String newState);

}
