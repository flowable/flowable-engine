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
 * Implemented by an {@code ActivityBehavior} attached to a process-level (i.e. NOT inside an
 * {@code EventSubProcess}) {@code StartEvent} that registers a deploy-time artifact — an event
 * subscription, a start timer job, etc. — and removes or updates that artifact when the process
 * definition is superseded by a new version.
 * <p>
 * Both {@link #deploy} and {@link #undeploy} must be implemented; a behavior that opts into the
 * deploy-time lifecycle owns both halves of it. Built-in start event types that don't register
 * anything (e.g. none / error) simply don't implement this interface.
 * <p>
 * Custom integrations supplying their own {@code EventDefinition} + parse handler + behavior pick
 * up deploy-time registration by implementing this interface — no change in
 * {@code BpmnDeploymentHelper} is required.
 */
public interface ProcessLevelStartEventActivityBehavior {

    /**
     * Register the deploy-time artifact (event subscription, timer job, etc.) for this start event
     * when its process definition is freshly deployed.
     */
    void deploy(ProcessLevelStartEventDeployContext context);

    /**
     * Remove or update the deploy-time artifact for this start event when its process definition is
     * superseded by a new version. Called on the previous (now-superseded) process definition's
     * start events.
     */
    void undeploy(ProcessLevelStartEventUndeployContext context);
}
