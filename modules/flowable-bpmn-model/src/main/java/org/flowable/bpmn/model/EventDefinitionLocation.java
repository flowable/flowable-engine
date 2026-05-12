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
package org.flowable.bpmn.model;

/**
 * Where an {@link EventDefinition} is allowed to appear in a process model. Each {@link EventDefinition}
 * subclass declares its supported locations via {@link EventDefinition#getSupportedLocations()}; the BPMN
 * process validators consult this set instead of hard-coding the allowed types.
 */
public enum EventDefinitionLocation {

    /** Top-level start event of a {@code Process} (i.e. <em>not</em> inside an {@code EventSubProcess}). */
    START_EVENT,

    /** Start event inside an {@code EventSubProcess}. */
    EVENT_SUBPROCESS_START_EVENT,

    /** {@code IntermediateCatchEvent} body. */
    INTERMEDIATE_CATCH_EVENT,

    /** {@code BoundaryEvent} attached to an activity. */
    BOUNDARY_EVENT,

    /** {@code EndEvent} body — covers {@code Terminate}, {@code Cancel}, {@code Error}, {@code Escalation} end events. */
    END_EVENT,

    /** {@code IntermediateThrowEvent} body — covers {@code Compensate}, {@code Escalation}, {@code Signal} throw events. */
    INTERMEDIATE_THROW_EVENT
}
