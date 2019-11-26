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
package org.flowable.engine.runtime;

/**
 * Represent a 'path of execution' in a process instance.
 * 
 * Note that a {@link ProcessInstance} also is an execution.
 * 
 * @author Joram Barrez
 */
public interface Execution {

    /**
     * The unique identifier of the execution.
     */
    String getId();

    /**
     * Indicates if the execution is suspended.
     */
    boolean isSuspended();

    /**
     * Indicates if the execution is ended.
     */
    boolean isEnded();

    /**
     * Returns the id of the activity where the execution currently is at. Returns null if the execution is not a 'leaf' execution (eg concurrent parent).
     */
    String getActivityId();

    /**
     * Id of the root of the execution tree representing the process instance. It is the same as {@link #getId()} if this execution is the process instance.
     */
    String getProcessInstanceId();

    /**
     * Gets the id of the parent of this execution. If null, the execution represents a process-instance.
     */
    String getParentId();

    /**
     * Gets the id of the super execution of this execution.
     */
    String getSuperExecutionId();

    /**
     * Id of the root of the execution tree representing the process instance that has no super execution.
     */
    String getRootProcessInstanceId();

    /**
     * The tenant identifier of this process instance
     */
    String getTenantId();

    /**
     * Returns the name of this execution.
     */
    String getName();

    /**
     * Returns the description of this execution.
     */
    String getDescription();

    /**
     * If this execution has created a case (through a case task), this will return the referenced case instance id.
     *
     * @return the id of the optionally referenced case instance, if this execution created a case
     */
    String getReferenceId();

    /**
     * If this execution has created a case (through a case task), this will return the referenced case type (e.g. bpmn-x-to-cmmn-y type).
     *
     * @return the type of the optionally referenced case instance, if this execution created a case
     */
    String getReferenceType();

    /**
     * If this execution runs in the context of a case and stage, this method returns it's closest parent stage instance id (the stage plan item instance id to be
     * precise).
     *
     * @return the stage instance id this execution belongs to or null, if this execution is not part of a case at all or is not a child element of a stage
     */
    String getPropagatedStageInstanceId();
}
