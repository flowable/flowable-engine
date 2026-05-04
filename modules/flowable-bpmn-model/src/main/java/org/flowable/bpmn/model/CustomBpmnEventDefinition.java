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
 * Marker for {@link EventDefinition} subclasses that are not part of the BPMN 2.0 specification and are
 * therefore serialized inside {@code <extensionElements>} rather than as direct children of the event
 * element. The Flowable-specific {@code EventRegistryEventDefinition} and {@code VariableListenerEventDefinition}
 * are built-in implementors; additional {@link EventDefinition} subclasses should also implement this marker
 * so the BPMN writer routes them through the extension-element path and round-trips correctly.
 */
public interface CustomBpmnEventDefinition {
}
