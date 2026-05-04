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
package org.flowable.engine.impl.bpmn.behavior;

/**
 * Implemented by an {@code ActivityBehavior} attached to an event-sub-process {@code StartEvent} that
 * registers a subscription, timer or waiting execution when the parent process or sub-process becomes
 * active. The {@code execute()} / {@code trigger()} half of the lifecycle continues to live on the
 * existing {@code ActivityBehavior} interface; this interface adds the "register at parent start"
 * half so each behavior owns its full lifecycle.
 * <p>
 * Custom integrations that supply their own {@code EventDefinition} + parse handler + behavior opt
 * into event-sub-process subscription registration by implementing this interface — no change in
 * {@code ProcessInstanceHelper} is required.
 */
public interface EventSubProcessStartEventActivityBehavior {

    void initializeEventSubProcessStart(EventSubProcessStartEventInitializerContext context);
}
