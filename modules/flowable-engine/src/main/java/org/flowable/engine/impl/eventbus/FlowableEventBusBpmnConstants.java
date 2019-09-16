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
package org.flowable.engine.impl.eventbus;

public interface FlowableEventBusBpmnConstants {

    String TYPE_SERVICETASK_EXCEPTION = "flowable-engine-servicetask-exception";
    
    String FLOW_ELEMENT_ID = "flowElementId";
    String FLOW_ELEMENT_NAME = "flowElementName";
    String FLOW_ELEMENT_TYPE = "flowElementType";
    String VARIABLE_MAP = "variableMap";
    String EXCEPTION_MESSAGE = "exceptionMessage";
    String EXCEPTION_STACKTRACE = "exceptionStacktrace";
}
