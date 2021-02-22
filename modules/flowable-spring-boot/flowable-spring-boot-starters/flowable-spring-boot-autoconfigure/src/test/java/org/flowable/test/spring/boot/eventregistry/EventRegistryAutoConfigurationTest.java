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
package org.flowable.test.spring.boot.eventregistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.flowable.test.spring.boot.util.DeploymentCleanerUtil.deleteDeployments;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.IterableAssert;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.management.DefaultEventRegistryChangeDetectionManager;
import org.flowable.eventregistry.impl.pipeline.DelegateExpressionInboundChannelModelProcessor;
import org.flowable.eventregistry.impl.pipeline.DelegateExpressionOutboundChannelModelProcessor;
import org.flowable.eventregistry.impl.pipeline.InboundChannelModelProcessor;
import org.flowable.eventregistry.impl.pipeline.OutboundChannelModelProcessor;
import org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration;
import org.flowable.eventregistry.spring.jms.JmsChannelModelProcessor;
import org.flowable.eventregistry.spring.kafka.KafkaChannelDefinitionProcessor;
import org.flowable.eventregistry.spring.management.DefaultSpringEventRegistryChangeDetectionExecutor;
import org.flowable.eventregistry.spring.rabbit.RabbitChannelDefinitionProcessor;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.eventregistry.EventRegistryAutoConfiguration;
import org.flowable.spring.boot.eventregistry.EventRegistryServicesAutoConfiguration;
import org.flowable.test.spring.boot.util.CustomUserEngineConfigurerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class EventRegistryAutoConfigurationTest {

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            EventRegistryServicesAutoConfiguration.class,
            EventRegistryAutoConfiguration.class
        ))
        .withUserConfiguration(CustomUserEngineConfigurerConfiguration.class);

    @Test
    public void standaloneEventRegistryWithBasicDataSource() {
        contextRunner.run(context -> {
            assertThat(context)
                .doesNotHaveBean(AppEngine.class)
                .doesNotHaveBean(ProcessEngine.class)
                .doesNotHaveBean(ChannelModelProcessor.class)
                .doesNotHaveBean("eventProcessEngineConfigurationConfigurer")
                .doesNotHaveBean("eventAppEngineConfigurationConfigurer");
            EventRegistryEngine eventRegistryEngine = context.getBean(EventRegistryEngine.class);
            assertThat(eventRegistryEngine).as("Event registry engine").isNotNull();
            assertAllServicesPresent(context, eventRegistryEngine);

            IterableAssert<ChannelModelProcessor> channelModelProcessorAssert = assertThat(
                eventRegistryEngine.getEventRegistryEngineConfiguration().getChannelModelProcessors());

            channelModelProcessorAssert
                .hasSize(4);

            channelModelProcessorAssert
                .element(0)
                .isInstanceOf(DelegateExpressionInboundChannelModelProcessor.class);

            channelModelProcessorAssert
                .element(1)
                .isInstanceOf(DelegateExpressionOutboundChannelModelProcessor.class);

            channelModelProcessorAssert
                .element(2)
                .isInstanceOf(InboundChannelModelProcessor.class);

            channelModelProcessorAssert
                .element(3)
                .isInstanceOf(OutboundChannelModelProcessor.class);

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringEventRegistryEngineConfiguration.class
                        );
                });

        });
    }

    @Test
    public void eventRegistryWithBasicDataSourceAndProcessEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).run(context -> {
            assertThat(context)
                .doesNotHaveBean(AppEngine.class)
                .doesNotHaveBean(ChannelModelProcessor.class)
                .hasBean("eventProcessEngineConfigurationConfigurer")
                .doesNotHaveBean("eventAppEngineConfigurationConfigurer");
            ProcessEngine processEngine = context.getBean(ProcessEngine.class);
            assertThat(processEngine).as("Process engine").isNotNull();
            EventRegistryEngineConfiguration eventRegistryEngineConfiguration = eventRegistryEngine(processEngine);

            EventRegistryEngine eventRegistryEngine = context.getBean(EventRegistryEngine.class);
            assertThat(eventRegistryEngine).as("Event registry engine").isNotNull();

            assertThat(eventRegistryEngine.getEventRegistryEngineConfiguration()).as("Event registry Configuration")
                .isEqualTo(eventRegistryEngineConfiguration);
            IterableAssert<ChannelModelProcessor> channelModelProcessorAssert = assertThat(
                eventRegistryEngine.getEventRegistryEngineConfiguration().getChannelModelProcessors());

            channelModelProcessorAssert
                .hasSize(4);

            channelModelProcessorAssert
                .element(0)
                .isInstanceOf(DelegateExpressionInboundChannelModelProcessor.class);

            channelModelProcessorAssert
                .element(1)
                .isInstanceOf(DelegateExpressionOutboundChannelModelProcessor.class);

            channelModelProcessorAssert
                .element(2)
                .isInstanceOf(InboundChannelModelProcessor.class);

            channelModelProcessorAssert
                .element(3)
                .isInstanceOf(OutboundChannelModelProcessor.class);

            assertAllServicesPresent(context, eventRegistryEngine);

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringEventRegistryEngineConfiguration.class,
                            SpringProcessEngineConfiguration.class
                        );
                });

            deleteDeployments(processEngine);
        });

    }
    
    @Test
    public void idmEngineWithBasicDataSourceAndAppEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            AppEngineServicesAutoConfiguration.class,
            AppEngineAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).run(context -> {
            assertThat(context)
                .doesNotHaveBean(ChannelModelProcessor.class)
                .doesNotHaveBean("eventProcessEngineConfigurationConfigurer")
                .hasBean("eventAppEngineConfigurationConfigurer");
            AppEngine appEngine = context.getBean(AppEngine.class);
            assertThat(appEngine).as("App engine").isNotNull();
            EventRegistryEngineConfiguration eventRegistryEngineConfiguration = eventRegistryEngine(appEngine);

            EventRegistryEngine eventRegistryEngine = context.getBean(EventRegistryEngine.class);
            assertThat(eventRegistryEngine).as("Idm engine").isNotNull();

            assertThat(eventRegistryEngine.getEventRegistryEngineConfiguration()).as("Event registry Configuration")
                .isEqualTo(eventRegistryEngineConfiguration);
            IterableAssert<ChannelModelProcessor> channelModelProcessorAssert = assertThat(
                eventRegistryEngine.getEventRegistryEngineConfiguration().getChannelModelProcessors());

            channelModelProcessorAssert
                .hasSize(4);

            channelModelProcessorAssert
                .element(0)
                .isInstanceOf(DelegateExpressionInboundChannelModelProcessor.class);

            channelModelProcessorAssert
                .element(1)
                .isInstanceOf(DelegateExpressionOutboundChannelModelProcessor.class);

            channelModelProcessorAssert
                .element(2)
                .isInstanceOf(InboundChannelModelProcessor.class);

            channelModelProcessorAssert
                .element(3)
                .isInstanceOf(OutboundChannelModelProcessor.class);

            assertAllServicesPresent(context, eventRegistryEngine);

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringProcessEngineConfiguration.class,
                            SpringEventRegistryEngineConfiguration.class,
                            SpringAppEngineConfiguration.class
                        );
                });

            deleteDeployments(appEngine);
            deleteDeployments(context.getBean(ProcessEngine.class));
        });
    }

    @Test
    public void eventRegistryWithJms() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(
                ActiveMQAutoConfiguration.class,
                JmsAutoConfiguration.class
            ))
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(JmsChannelModelProcessor.class)
                    .hasBean("jmsChannelDefinitionProcessor");
                EventRegistryEngine eventRegistryEngine = context.getBean(EventRegistryEngine.class);
                assertThat(eventRegistryEngine).as("Event registry engine").isNotNull();

                EventRegistryEngineConfiguration eventRegistryEngineConfiguration = eventRegistryEngine.getEventRegistryEngineConfiguration();

                IterableAssert<ChannelModelProcessor> channelModelProcessorAssert = assertThat(
                    eventRegistryEngineConfiguration.getChannelModelProcessors());

                channelModelProcessorAssert
                    .hasSize(5);

                channelModelProcessorAssert
                    .element(0)
                    .isEqualTo(context.getBean("jmsChannelDefinitionProcessor", ChannelModelProcessor.class));

                channelModelProcessorAssert
                    .element(1)
                    .isInstanceOf(DelegateExpressionInboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(2)
                    .isInstanceOf(DelegateExpressionOutboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(3)
                    .isInstanceOf(InboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(4)
                    .isInstanceOf(OutboundChannelModelProcessor.class);

            });
    }

    @Test
    public void eventRegistryWithRabbit() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(RabbitChannelDefinitionProcessor.class)
                    .hasBean("rabbitChannelDefinitionProcessor");
                EventRegistryEngine eventRegistryEngine = context.getBean(EventRegistryEngine.class);
                assertThat(eventRegistryEngine).as("Event registry engine").isNotNull();

                EventRegistryEngineConfiguration eventRegistryEngineConfiguration = eventRegistryEngine.getEventRegistryEngineConfiguration();
                IterableAssert<ChannelModelProcessor> channelModelProcessorAssert = assertThat(
                    eventRegistryEngineConfiguration.getChannelModelProcessors())
                    .hasSize(5);

                channelModelProcessorAssert
                    .element(0)
                    .isEqualTo(context.getBean("rabbitChannelDefinitionProcessor", ChannelModelProcessor.class));

                channelModelProcessorAssert
                    .element(1)
                    .isInstanceOf(DelegateExpressionInboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(2)
                    .isInstanceOf(DelegateExpressionOutboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(3)
                    .isInstanceOf(InboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(4)
                    .isInstanceOf(OutboundChannelModelProcessor.class);
            });
    }

    @Test
    public void eventRegistryWithKafka() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(KafkaChannelDefinitionProcessor.class)
                    .hasBean("kafkaChannelDefinitionProcessor");
                EventRegistryEngine eventRegistryEngine = context.getBean(EventRegistryEngine.class);
                assertThat(eventRegistryEngine).as("Event registry engine").isNotNull();

                IterableAssert<ChannelModelProcessor> channelModelProcessorAssert = assertThat(
                    eventRegistryEngine.getEventRegistryEngineConfiguration().getChannelModelProcessors());

                channelModelProcessorAssert
                    .hasSize(5);

                channelModelProcessorAssert
                    .element(0)
                    .isEqualTo(context.getBean("kafkaChannelDefinitionProcessor", ChannelModelProcessor.class));

                channelModelProcessorAssert
                    .element(1)
                    .isInstanceOf(DelegateExpressionInboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(2)
                    .isInstanceOf(DelegateExpressionOutboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(3)
                    .isInstanceOf(InboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(4)
                    .isInstanceOf(OutboundChannelModelProcessor.class);
            });
    }

    @Test
    public void eventRegistryWithJmsRabbitAndKafka() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(
                ActiveMQAutoConfiguration.class,
                JmsAutoConfiguration.class,
                RabbitAutoConfiguration.class,
                KafkaAutoConfiguration.class
            ))
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(JmsChannelModelProcessor.class)
                    .hasBean("jmsChannelDefinitionProcessor")
                    .hasSingleBean(RabbitChannelDefinitionProcessor.class)
                    .hasBean("rabbitChannelDefinitionProcessor")
                    .hasSingleBean(KafkaChannelDefinitionProcessor.class)
                    .hasBean("kafkaChannelDefinitionProcessor");
                EventRegistryEngine eventRegistryEngine = context.getBean(EventRegistryEngine.class);
                assertThat(eventRegistryEngine).as("Event registry engine").isNotNull();

                EventRegistryEngineConfiguration eventRegistryEngineConfiguration = eventRegistryEngine.getEventRegistryEngineConfiguration();
                IterableAssert<ChannelModelProcessor> channelModelProcessorAssert = assertThat(
                    eventRegistryEngineConfiguration.getChannelModelProcessors());
                channelModelProcessorAssert
                    .hasSize(7)
                    .contains(
                        context.getBean("jmsChannelDefinitionProcessor", JmsChannelModelProcessor.class),
                        context.getBean("rabbitChannelDefinitionProcessor", RabbitChannelDefinitionProcessor.class),
                        context.getBean("kafkaChannelDefinitionProcessor", KafkaChannelDefinitionProcessor.class)
                    );

                channelModelProcessorAssert
                    .element(3)
                    .isInstanceOf(DelegateExpressionInboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(4)
                    .isInstanceOf(DelegateExpressionOutboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(5)
                    .isInstanceOf(InboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(6)
                    .isInstanceOf(OutboundChannelModelProcessor.class);
            });
    }

    @Test
    public void eventRegistryWithCustomDefinitionProcessors() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(
                ActiveMQAutoConfiguration.class,
                JmsAutoConfiguration.class,
                RabbitAutoConfiguration.class,
                KafkaAutoConfiguration.class
            ))
            .withUserConfiguration(CustomChannelDefinitionProcessorsConfiguration.class)
            .run(context -> {
                assertThat(context)
                    .doesNotHaveBean(JmsChannelModelProcessor.class)
                    .hasBean("jmsChannelDefinitionProcessor")
                    .doesNotHaveBean(RabbitChannelDefinitionProcessor.class)
                    .hasBean("rabbitChannelDefinitionProcessor")
                    .doesNotHaveBean(KafkaChannelDefinitionProcessor.class)
                    .hasBean("kafkaChannelDefinitionProcessor")
                    .hasBean("customChannelDefinitionProcessor");
                EventRegistryEngine eventRegistryEngine = context.getBean(EventRegistryEngine.class);
                assertThat(eventRegistryEngine).as("Event registry engine").isNotNull();

                EventRegistryEngineConfiguration eventRegistryEngineConfiguration = eventRegistryEngine.getEventRegistryEngineConfiguration();
                IterableAssert<ChannelModelProcessor> channelModelProcessorAssert = assertThat(
                    eventRegistryEngineConfiguration.getChannelModelProcessors());
                channelModelProcessorAssert
                    .hasSize(8);

                channelModelProcessorAssert
                    .element(0)
                    .isEqualTo(context.getBean("customChannelDefinitionProcessor", ChannelModelProcessor.class));

                channelModelProcessorAssert
                    .element(1)
                    .isEqualTo(context.getBean("rabbitChannelDefinitionProcessor", ChannelModelProcessor.class));

                channelModelProcessorAssert
                    .element(2)
                    .isEqualTo(context.getBean("jmsChannelDefinitionProcessor", ChannelModelProcessor.class));

            channelModelProcessorAssert
                    .element(3)
                    .isEqualTo(context.getBean("kafkaChannelDefinitionProcessor", ChannelModelProcessor.class));

                channelModelProcessorAssert
                    .element(4)
                    .isInstanceOf(DelegateExpressionInboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(5)
                    .isInstanceOf(DelegateExpressionOutboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(6)
                    .isInstanceOf(InboundChannelModelProcessor.class);

                channelModelProcessorAssert
                    .element(7)
                    .isInstanceOf(OutboundChannelModelProcessor.class);
            });
    }

    @Test
    public void standaloneEventRegistryWithDefaultTaskScheduler() {
        contextRunner
            .withPropertyValues("flowable.eventregistry.enable-change-detection:true")
            .run(context -> {

                EventRegistryEngine eventRegistryEngine = context.getBean(EventRegistryEngine.class);
                assertThat(eventRegistryEngine).as("Event registry engine").isNotNull();
                assertAllServicesPresent(context, eventRegistryEngine);

                assertThat(eventRegistryEngine.getEventRegistryEngineConfiguration().getEventRegistryChangeDetectionManager())
                    .isInstanceOf(DefaultEventRegistryChangeDetectionManager.class);
                assertThat(eventRegistryEngine.getEventRegistryEngineConfiguration().getEventRegistryChangeDetectionExecutor())
                    .isInstanceOf(DefaultSpringEventRegistryChangeDetectionExecutor.class);

                DefaultSpringEventRegistryChangeDetectionExecutor executor = (DefaultSpringEventRegistryChangeDetectionExecutor)
                    eventRegistryEngine.getEventRegistryEngineConfiguration().getEventRegistryChangeDetectionExecutor();
                assertThat(executor.getTaskScheduler()).isInstanceOf(ThreadPoolTaskScheduler.class);
            });
    }

    @Test
    public void standaloneEventRegistryWithCustomTaskScheduler() {
        contextRunner
            .withBean(ThreadPoolTaskScheduler.class)
            .withPropertyValues("flowable.eventregistry.enable-change-detection:true")
            .run(context -> {

            EventRegistryEngine eventRegistryEngine = context.getBean(EventRegistryEngine.class);
            assertThat(eventRegistryEngine).as("Event registry engine").isNotNull();
            assertAllServicesPresent(context, eventRegistryEngine);

            assertThat(eventRegistryEngine.getEventRegistryEngineConfiguration().getEventRegistryChangeDetectionManager())
                .isInstanceOf(DefaultEventRegistryChangeDetectionManager.class);
                assertThat(eventRegistryEngine.getEventRegistryEngineConfiguration().getEventRegistryChangeDetectionExecutor())
                    .isInstanceOf(DefaultSpringEventRegistryChangeDetectionExecutor.class);

            DefaultSpringEventRegistryChangeDetectionExecutor executor = (DefaultSpringEventRegistryChangeDetectionExecutor)
                eventRegistryEngine.getEventRegistryEngineConfiguration().getEventRegistryChangeDetectionExecutor();
            assertThat(executor.getTaskScheduler()).isEqualTo(context.getBean(TaskScheduler.class));
        });
    }

    private void assertAllServicesPresent(ApplicationContext context, EventRegistryEngine eventRegistryEngine) {
        List<Method> methods = Stream.of(EventRegistryEngine.class.getDeclaredMethods())
            .filter(method -> !(method.getReturnType().equals(void.class)) || "close".equals(method.getName()) || "getName".equals(method.getName()))
            .collect(Collectors.toList());

        assertThat(methods).allSatisfy(method -> {
            try {
                assertThat(context.getBean(method.getReturnType()))
                    .as(method.getReturnType() + " bean")
                    .isEqualTo(method.invoke(eventRegistryEngine));
            } catch (IllegalAccessException | InvocationTargetException e) {
                fail("Failed to invoke method " + method, e);
            }
        });
    }

    private static EventRegistryEngineConfiguration eventRegistryEngine(ProcessEngine processEngine) {
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        return EngineServiceUtil.getEventRegistryEngineConfiguration(processEngineConfiguration);
    }

    private static EventRegistryEngineConfiguration eventRegistryEngine(AppEngine appEngine) {
        AppEngineConfiguration appEngineConfiguration = appEngine.getAppEngineConfiguration();
        return EngineServiceUtil.getEventRegistryEngineConfiguration(appEngineConfiguration);
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomChannelDefinitionProcessorsConfiguration {

        @Order(10)
        @Bean({
            "jmsChannelDefinitionProcessor",
            "customJmsChannelDefinitionProcessor"
        })
        public ChannelModelProcessor customJmsChannelDefinitionProcessor() {
            return mock(ChannelModelProcessor.class);
        }

        @Order(0)
        @Bean({
            "rabbitChannelDefinitionProcessor",
            "customRabbitChannelDefinitionProcessor"
        })
        public ChannelModelProcessor customRabbitChannelDefinitionProcessor() {
            return mock(ChannelModelProcessor.class);
        }

        @Order(20)
        @Bean({
            "kafkaChannelDefinitionProcessor",
            "customKafkaChannelDefinitionProcessor"
        })
        public ChannelModelProcessor customKafkaChannelDefinitionProcessor() {
            return mock(ChannelModelProcessor.class);
        }

        @Order(-20)
        @Bean
        public ChannelModelProcessor customChannelDefinitionProcessor() {
            return mock(ChannelModelProcessor.class);
        }
    }
}
