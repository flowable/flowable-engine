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
package org.flowable.eventregistry.spring.kafka;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

import org.flowable.common.engine.api.FlowableException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ContainerPartitionPausingBackOffManagerFactory;
import org.springframework.kafka.listener.ContainerPausingBackOffHandler;
import org.springframework.kafka.listener.KafkaBackOffManagerFactory;
import org.springframework.kafka.listener.ListenerContainerPauseService;
import org.springframework.kafka.listener.ListenerContainerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.ClassUtils;

/**
 * This class is here only to help us create the right {@link KafkaBackOffManagerFactory} for a specific Spring Kafka version.
 *
 * @author Filip Hrisafov
 */
class KafkaBackOffManagerUtils {

    public static boolean SPRING_KAFKA_2_9_PRESENT = ClassUtils.isPresent("org.springframework.kafka.listener.ContainerPartitionPausingBackOffManagerFactory",
            null);

    public static KafkaBackOffManagerFactory createKafkaBackoffManagerFactory(KafkaListenerEndpointRegistry endpointRegistry,
            ApplicationContext applicationContext, Supplier<TaskScheduler> taskSchedulerSupplier) {
        if (SPRING_KAFKA_2_9_PRESENT) {
            return SpringKafka2_9.createBackOffManagerFactory(endpointRegistry, applicationContext, taskSchedulerSupplier);
        } else {
            return SpringKafkaPre2_9.createBackOffManagerFactory(endpointRegistry, applicationContext);

        }
    }

    private static class SpringKafka2_9 {

        private static KafkaBackOffManagerFactory createBackOffManagerFactory(KafkaListenerEndpointRegistry endpointRegistry,
                ApplicationContext applicationContext, Supplier<TaskScheduler> taskSchedulerSupplier) {
            ContainerPartitionPausingBackOffManagerFactory containerBackOffManagerFactory =
                    new ContainerPartitionPausingBackOffManagerFactory(endpointRegistry, applicationContext);
            containerBackOffManagerFactory.setBackOffHandler(new ContainerPausingBackOffHandler(
                    new ListenerContainerPauseService(endpointRegistry, taskSchedulerSupplier.get())));
            return containerBackOffManagerFactory;
        }

    }

    private static class SpringKafkaPre2_9 {

        private static KafkaBackOffManagerFactory createBackOffManagerFactory(KafkaListenerEndpointRegistry endpointRegistry,
                ApplicationContext applicationContext) {
            try {
                Class<?> backOfManagerFactory2_7 = ClassUtils.forName("org.springframework.kafka.listener.PartitionPausingBackOffManagerFactory", null);
                KafkaBackOffManagerFactory backOffManagerFactory = (KafkaBackOffManagerFactory) backOfManagerFactory2_7.getConstructor(
                                ListenerContainerRegistry.class)
                        .newInstance(endpointRegistry);
                if (backOffManagerFactory instanceof ApplicationContextAware) {
                    ((ApplicationContextAware) backOffManagerFactory).setApplicationContext(applicationContext);
                }
                return backOffManagerFactory;
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                throw new FlowableException("Failed to create KafkaBackOffManagerFactory with Spring Kafka prior to 2.9.x", ex);
            }
        }
    }

}
