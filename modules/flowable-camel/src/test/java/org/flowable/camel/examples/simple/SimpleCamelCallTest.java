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

package org.flowable.camel.examples.simple;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@Tag("camel")
@ContextConfiguration("classpath:generic-camel-flowable-context.xml")
public class SimpleCamelCallTest extends SpringFlowableTestCase {

    @Autowired
    protected CamelContext camelContext;

    @BeforeEach
    public void setUp() throws Exception {
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("flowable:SimpleCamelCallProcess:simpleCall").to("log: org.flowable.camel.examples.SimpleCamelCall");
            }
        });
    }

    @Test
    @Deployment
    public void testSimpleCamelCall() {
        runtimeService.startProcessInstanceByKey("SimpleCamelCallProcess");
    }

}
