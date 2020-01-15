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
package org.flowable.ui.task.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the Task UI App.
 *
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.task.app")
public class FlowableTaskAppProperties {

    /**
     * Enables the REST API (this is not the REST api used by the UI, but an api that's available over basic auth authentication).
     */
    private boolean restEnabled = true;
    
    /**
     * Enables the logic to migrate old app definitions from the process engine to the app engine
     */
    private boolean appMigrationEnabled = true;

    /**
     * Enables JMS for the task app.
     */
    private boolean jmsEnabled = false;

    /**
     * Enables Kafka for the task app.
     */
    private boolean kafkaEnabled = false;

    /**
     * Enables RabbitMQ for the task app.
     */
    private boolean rabbitEnabled = false;

    public boolean isRestEnabled() {
        return restEnabled;
    }

    public void setRestEnabled(boolean restEnabled) {
        this.restEnabled = restEnabled;
    }

    public boolean isAppMigrationEnabled() {
        return appMigrationEnabled;
    }

    public void setAppMigrationEnabled(boolean appMigrationEnabled) {
        this.appMigrationEnabled = appMigrationEnabled;
    }

    public boolean isJmsEnabled() {
        return jmsEnabled;
    }

    public void setJmsEnabled(boolean jmsEnabled) {
        this.jmsEnabled = jmsEnabled;
    }

    public boolean isKafkaEnabled() {
        return kafkaEnabled;
    }

    public void setKafkaEnabled(boolean kafkaEnabled) {
        this.kafkaEnabled = kafkaEnabled;
    }

    public boolean isRabbitEnabled() {
        return rabbitEnabled;
    }

    public void setRabbitEnabled(boolean rabbitEnabled) {
        this.rabbitEnabled = rabbitEnabled;
    }
}
