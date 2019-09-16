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
package org.flowable.variable.service.impl.eventbus;

public interface FlowableEventBusVariableConstants {

    String TYPE_VARIABLE_CREATED = "flowable-engine-variable-created";
    String TYPE_VARIABLE_UPDATED = "flowable-engine-variable-updated";
    
    String VARIABLE_ID = "variableId";
    String VARIABLE_NAME = "variableName";
    String VARIABLE_TYPE = "variableType";
    String VARIABLE_VALUE = "variableValue";
    String OLD_VARIABLE_TYPE = "oldVariableType";
    String OLD_VARIABLE_VALUE = "oldVariableValue";
}
