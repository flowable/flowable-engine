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
package flowable;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.spring.integration.FlowableInboundGateway;
import org.flowable.spring.integration.IntegrationActivityBehavior;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootApplication(proxyBeanMethods = false)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    IntegrationActivityBehavior flowableDelegate(FlowableInboundGateway flowableInboundGateway) {
        return new IntegrationActivityBehavior(flowableInboundGateway);
    }

    @Bean
    FlowableInboundGateway inboundGateway(ProcessEngine processEngine) {
        return new FlowableInboundGateway(processEngine, "customerId", "projectId", "orderId");
    }

    @Bean
    AnalysingService analysingService() {
        return new AnalysingService();
    }

    public static class AnalysingService {

        private final AtomicReference<String> stringAtomicReference = new AtomicReference<>();

        public void dump(String projectId) {
            this.stringAtomicReference.set(projectId);
        }

        public AtomicReference<String> getStringAtomicReference() {
            return stringAtomicReference;
        }
    }

    @Bean
    IntegrationFlow inboundProcess(FlowableInboundGateway inboundGateway) {
        return IntegrationFlows
                .from(inboundGateway)
                .handle(new GenericHandler<DelegateExecution>() {
                    @Override
                    public Object handle(DelegateExecution execution, MessageHeaders headers) {
                        return MessageBuilder.withPayload(execution)
                                .setHeader("projectId", "2143243")
                                .setHeader("orderId", "246")
                                .copyHeaders(headers).build();
                    }
                })
                .get();
    }

    @Bean
    CommandLineRunner init(
            final AnalysingService analysingService,
            final RuntimeService runtimeService) {
        return new CommandLineRunner() {
            @Override
            public void run(String... strings) throws Exception {

                String integrationGatewayProcess = "integrationGatewayProcess";

                runtimeService.startProcessInstanceByKey(
                        integrationGatewayProcess, Collections.singletonMap("customerId", (Object) 232L));

                System.out.println("projectId=" + analysingService.getStringAtomicReference().get());

            }
        };
    } // ...

}
