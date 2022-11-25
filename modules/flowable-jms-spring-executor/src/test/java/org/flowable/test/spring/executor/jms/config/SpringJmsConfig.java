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
package org.flowable.test.spring.executor.jms.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.TransportConstants;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.JobManager;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.executor.jms.JobMessageListener;
import org.flowable.spring.executor.jms.MessageBasedJobManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

@Configuration(proxyBeanMethods = false)
public class SpringJmsConfig {

    private static final AtomicInteger serverIdCounter = new AtomicInteger();

    protected final int serverId = serverIdCounter.getAndIncrement();

    @Value("${jdbc.url:jdbc:h2:mem:flowable-spring-jms-test;DB_CLOSE_DELAY=1000}")
    protected String jdbcUrl;

    @Value("${jdbc.driver:org.h2.Driver}")
    protected String jdbcDriverClassName;

    @Value("${jdbc.username:sa}")
    protected String jdbcUsername;

    @Value("${jdbc.password:}")
    protected String jdbcPassword;

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setDriverClassName(jdbcDriverClassName);
        dataSource.setUsername(jdbcUsername);
        dataSource.setPassword(jdbcPassword);
        return dataSource;
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);
        return transactionManager;
    }

    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
        JobManager jobManager) {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        configuration.setDatabaseSchemaUpdate(SpringProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        configuration.setAsyncExecutorMessageQueueMode(true);
        configuration.setAsyncExecutorActivate(true);
        configuration.setJobManager(jobManager);
        return configuration;
    }

    @Bean
    public ProcessEngine processEngine(ProcessEngineConfiguration processEngineConfiguration) {
        return processEngineConfiguration.buildProcessEngine();
    }

    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public MessageBasedJobManager jobManager(JmsTemplate jmsTemplate) {
        MessageBasedJobManager jobManager = new MessageBasedJobManager();
        jobManager.setJmsTemplate(jmsTemplate);
        return jobManager;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public EmbeddedActiveMQ embeddedActiveMQ() {
        ConfigurationImpl configuration = new ConfigurationImpl();
        configuration.setSecurityEnabled(false);
        configuration.setPersistenceEnabled(false);
        TransportConfiguration transportConfiguration = new TransportConfiguration(InVMAcceptorFactory.class.getName(), generateTransportParameter());
        configuration.getAcceptorConfigurations().add(transportConfiguration);
        configuration.setClusterPassword("flowable");
        EmbeddedActiveMQ embeddedActiveMQ = new EmbeddedActiveMQ();
        embeddedActiveMQ.setConfiguration(configuration);

        return embeddedActiveMQ;
    }

    @Bean
    @DependsOn("embeddedActiveMQ")
    public ConnectionFactory connectionFactory() {
        // configuration properties are Spring Boot defaults
        TransportConfiguration transportConfiguration = new TransportConfiguration(InVMConnectorFactory.class.getName(), generateTransportParameter());
        ServerLocator serverLocator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(serverLocator);

        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(activeMQConnectionFactory);
        cachingConnectionFactory.setCacheConsumers(false);
        cachingConnectionFactory.setCacheProducers(true);
        cachingConnectionFactory.setSessionCacheSize(1);

        return cachingConnectionFactory;
    }

    protected Map<String, Object> generateTransportParameter() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(TransportConstants.SERVER_ID_PROP_NAME, serverId);
        return parameters;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setDefaultDestination(new ActiveMQQueue("flowable-jobs"));
        jmsTemplate.setConnectionFactory(connectionFactory);
        return jmsTemplate;
    }

    @Bean
    public MessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory, JobMessageListener jobMessageListener) {
        DefaultMessageListenerContainer messageListenerContainer = new DefaultMessageListenerContainer();
        messageListenerContainer.setConnectionFactory(connectionFactory);
        messageListenerContainer.setDestinationName("flowable-jobs");
        messageListenerContainer.setMessageListener(jobMessageListener);
        messageListenerContainer.setConcurrentConsumers(2);
        messageListenerContainer.start();
        return messageListenerContainer;
    }

    @Bean
    public JobMessageListener jobMessageListener(ProcessEngine processEngine) {
        JobMessageListener jobMessageListener = new JobMessageListener();
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        JobServiceConfiguration jobServiceConfiguration = (JobServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
        jobMessageListener.setJobServiceConfiguration(jobServiceConfiguration);
        return jobMessageListener;
    }

}
