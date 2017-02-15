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

package org.activiti.camel;

import org.activiti.camel.util.FlagJavaDelegate;
import org.activiti.spring.impl.test.SpringFlowableTestCase;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Saeid Mirzaei
 */
@ContextConfiguration("classpath:generic-camel-flowable-context.xml")
public class ErrorMapExceptionTest extends SpringFlowableTestCase {

    @Autowired
    protected CamelContext camelContext;

    @Deployment(resources = { "process/mapExceptionSingleMap.bpmn20.xml" })
    public void testCamelSingleDirectMap() throws Exception {
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("flowable:mapExceptionProcess:exceptionRoute").throwException(new MapExceptionParent("test exception"));
            }
        });

        FlagJavaDelegate.reset();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("mapExceptionProcess");
        assertTrue(FlagJavaDelegate.isFlagSet());
    }

    @Deployment(resources = { "process/mapExceptionDefaultMap.bpmn20.xml" })
    public void testCamelDefaultMap() throws Exception {
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("flowable:mapExceptionDefaultProcess:exceptionRoute").throwException(new NullPointerException("test exception"));
            }
        });
        FlagJavaDelegate.reset();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("mapExceptionDefaultProcess");
        assertTrue(FlagJavaDelegate.isFlagSet());
    }

    @Deployment(resources = { "process/mapExceptionParentMap.bpmn20.xml" })
    public void testCamelParentMap() throws Exception {
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("flowable:mapExceptionParentProcess:exceptionRoute").throwException(new MapExceptionChild("test exception"));
            }
        });
        FlagJavaDelegate.reset();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("mapExceptionParentProcess");
        assertTrue(FlagJavaDelegate.isFlagSet());
    }

}
