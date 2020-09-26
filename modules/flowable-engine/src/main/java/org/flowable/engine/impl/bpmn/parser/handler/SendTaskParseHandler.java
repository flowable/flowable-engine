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
package org.flowable.engine.impl.bpmn.parser.handler;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.SendTask;
import org.flowable.engine.impl.bpmn.behavior.WebServiceActivityBehavior;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class SendTaskParseHandler extends AbstractActivityBpmnParseHandler<SendTask> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendTaskParseHandler.class);

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return SendTask.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, SendTask sendTask) {

        if (StringUtils.isNotEmpty(sendTask.getType())) {

            if ("mail".equalsIgnoreCase(sendTask.getType())) {
                sendTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createMailActivityBehavior(sendTask));
            } else if ("mule".equalsIgnoreCase(sendTask.getType())) {
                sendTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createMuleActivityBehavior(sendTask));
            } else if ("camel".equalsIgnoreCase(sendTask.getType())) {
                sendTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createCamelActivityBehavior(sendTask));
            } else if ("dmn".equalsIgnoreCase(sendTask.getType())) {
                sendTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createDmnActivityBehavior(sendTask));
            }

        } else if (ImplementationType.IMPLEMENTATION_TYPE_WEBSERVICE.equalsIgnoreCase(sendTask.getImplementationType()) && StringUtils.isNotEmpty(sendTask.getOperationRef())) {

            WebServiceActivityBehavior webServiceActivityBehavior = bpmnParse.getActivityBehaviorFactory().createWebServiceActivityBehavior(sendTask, bpmnParse.getBpmnModel());
            sendTask.setBehavior(webServiceActivityBehavior);

        } else {
            LOGGER.warn("One of the attributes 'type' or 'operation' is mandatory on sendTask {}", sendTask.getId());
        }
    }

}
