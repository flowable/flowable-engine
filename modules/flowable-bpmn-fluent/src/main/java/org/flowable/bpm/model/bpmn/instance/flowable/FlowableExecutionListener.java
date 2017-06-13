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
package org.flowable.bpm.model.bpmn.instance.flowable;

import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstance;

import java.util.Collection;

/**
 * The BPMN executionListener Flowable extension element.
 */
public interface FlowableExecutionListener
        extends BpmnModelElementInstance {

    String getFlowableEvent();

    void setFlowableEvent(String flowableEvent);

    String getFlowableClass();

    void setFlowableClass(String flowableClass);

    String getFlowableExpression();

    void setFlowableExpression(String flowableExpression);

    String getFlowableDelegateExpression();

    void setFlowableDelegateExpression(String flowableDelegateExpression);

    Collection<FlowableField> getFlowableFields();

    FlowableScript getFlowableScript();

    void setFlowableScript(FlowableScript flowableScript);
}
