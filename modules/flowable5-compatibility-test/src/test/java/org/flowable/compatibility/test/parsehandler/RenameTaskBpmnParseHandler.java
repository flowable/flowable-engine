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
package org.flowable.compatibility.test.parsehandler;

import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.handler.AbstractBpmnParseHandler;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.UserTask;

/**
 * Note the org.activiti5 package!
 * 
 * @author Joram Barrez
 */
public class RenameTaskBpmnParseHandler extends AbstractBpmnParseHandler<UserTask> {

    @Override
    protected Class<? extends BaseElement> getHandledType() {
        return UserTask.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, UserTask userTask) {
        if (bpmnParse.getCurrentProcessDefinition().getKey().equals("parseHandlerTestProcess")) {
            userTask.setName(userTask.getName() + "-activiti 5");
        }
    }

}
