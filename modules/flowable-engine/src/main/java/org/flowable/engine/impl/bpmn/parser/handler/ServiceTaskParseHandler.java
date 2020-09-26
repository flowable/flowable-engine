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
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.engine.impl.bpmn.behavior.WebServiceActivityBehavior;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class ServiceTaskParseHandler extends AbstractActivityBpmnParseHandler<ServiceTask> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskParseHandler.class);

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return ServiceTask.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, ServiceTask serviceTask) {

        // Email, Mule, Http and Shell service tasks
        if (StringUtils.isNotEmpty(serviceTask.getType())) {

            if ("mail".equalsIgnoreCase(serviceTask.getType())) {
                serviceTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createMailActivityBehavior(serviceTask));

            } else if ("mule".equalsIgnoreCase(serviceTask.getType())) {
                serviceTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createMuleActivityBehavior(serviceTask));

            } else if ("camel".equalsIgnoreCase(serviceTask.getType())) {
                serviceTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createCamelActivityBehavior(serviceTask));

            } else if ("shell".equalsIgnoreCase(serviceTask.getType())) {
                serviceTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createShellActivityBehavior(serviceTask));

            } else if ("dmn".equalsIgnoreCase(serviceTask.getType())) {
                serviceTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createDmnActivityBehavior(serviceTask));

            } else if ("http".equalsIgnoreCase(serviceTask.getType())) {
                serviceTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createHttpActivityBehavior(serviceTask));

            } else {
                LOGGER.warn("Invalid type: '{}' for service task {}", serviceTask.getType(), serviceTask.getId());
            }

            // activiti:class
        } else if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(serviceTask.getImplementationType())) {

            serviceTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createClassDelegateServiceTask(serviceTask));

            // activiti:delegateExpression
        } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(serviceTask.getImplementationType())) {

            serviceTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createServiceTaskDelegateExpressionActivityBehavior(serviceTask));

            // activiti:expression
        } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equalsIgnoreCase(serviceTask.getImplementationType())) {

            serviceTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createServiceTaskExpressionActivityBehavior(serviceTask));

            // Webservice
        } else if (ImplementationType.IMPLEMENTATION_TYPE_WEBSERVICE.equalsIgnoreCase(serviceTask.getImplementationType()) && StringUtils.isNotEmpty(serviceTask.getOperationRef())) {

            WebServiceActivityBehavior webServiceActivityBehavior = bpmnParse.getActivityBehaviorFactory().createWebServiceActivityBehavior(serviceTask, bpmnParse.getBpmnModel());
            serviceTask.setBehavior(webServiceActivityBehavior);

        } else {
            LOGGER.warn("One of the attributes 'class', 'delegateExpression', 'type', 'operation', or 'expression' is mandatory on service task {}", serviceTask.getId());
        }

    }
}
