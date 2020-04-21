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
package org.flowable.ui.task.conf;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Filip Hrisafov
 */
public class FlowableTaskEventRegistryCondition extends SpringBootCondition
    implements AutoConfigurationImportFilter, BeanFactoryAware, Condition, EnvironmentAware {

    protected BeanFactory beanFactory;

    protected Environment environment;

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        ConditionEvaluationReport report = ConditionEvaluationReport.find(this.beanFactory);
        Map<String, ConditionOutcome> conditions = getConditionOutcomes();

        ConditionOutcome[] outcomes = getOutcomes(autoConfigurationClasses, conditions);
        boolean[] match = new boolean[outcomes.length];
        for (int i = 0; i < outcomes.length; i++) {
            match[i] = (outcomes[i] == null || outcomes[i].isMatch());
            if (!match[i] && outcomes[i] != null) {
                logOutcome(autoConfigurationClasses[i], outcomes[i]);
                if (report != null) {
                    report.recordConditionEvaluation(autoConfigurationClasses[i], this, outcomes[i]);
                }
            }
        }
        return match;
    }

    protected Map<String, ConditionOutcome> getConditionOutcomes() {
        boolean jmsEnabled = environment.getProperty("flowable.task.app.jms-enabled", Boolean.class, false);
        boolean kafkaEnabled = environment.getProperty("flowable.task.app.kafka-enabled", Boolean.class, false);
        boolean rabbitEnabled = environment.getProperty("flowable.task.app.rabbit-enabled", Boolean.class, false);
        Map<String, ConditionOutcome> conditions = new HashMap<>();

        if (!jmsEnabled) {
            conditions.put("org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration",
                ConditionOutcome.noMatch("Property flowable.task.app.jms-enabled was not set to true")
            );
        }

        if (!kafkaEnabled) {
            conditions.put("org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                ConditionOutcome.noMatch("Property flowable.task.app.kafka-enabled was not set to true")
            );
        }

        if (!rabbitEnabled) {
            conditions.put("org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
                ConditionOutcome.noMatch("Property flowable.task.app.rabbit-enabled was not set to true")
            );
        }
        return conditions;
    }

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return ConditionOutcome.noMatch(ConditionMessage.empty());
    }

    protected ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses, Map<String, ConditionOutcome> conditionOutcomes) {
        ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];

        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            outcomes[i] = conditionOutcomes.get(autoConfigurationClasses[i]);
        }

        return outcomes;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
