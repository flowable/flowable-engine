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

package org.flowable.engine;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.HasTaskIdGeneratorEngineConfiguration;
import org.flowable.common.engine.impl.cfg.BeansConfigurationHelper;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.cfg.HttpClientConfig;
import org.flowable.engine.cfg.MailServerInfo;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.task.service.TaskPostProcessor;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration information from which a process engine can be build.
 * 
 * <p>
 * Most common is to create a process engine based on the default configuration file:
 * 
 * <pre>
 * ProcessEngine processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResourceDefault().buildProcessEngine();
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * To create a process engine programmatic, without a configuration file, the first option is {@link #createStandaloneProcessEngineConfiguration()}
 * 
 * <pre>
 * ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration().buildProcessEngine();
 * </pre>
 * 
 * This creates a new process engine with all the defaults to connect to a remote h2 database (jdbc:h2:tcp://localhost/flowable) in standalone mode. Standalone mode means that the process engine will
 * manage the transactions on the JDBC connections that it creates. One transaction per service method. For a description of how to write the configuration files, see the userguide.
 * </p>
 * 
 * <p>
 * The second option is great for testing: {@link #createStandaloneInMemProcessEngineConfiguration()}
 * 
 * <pre>
 * ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration().buildProcessEngine();
 * </pre>
 * 
 * This creates a new process engine with all the defaults to connect to an memory h2 database (jdbc:h2:tcp://localhost/flowable) in standalone mode. The DB schema strategy default is in this case
 * <code>create-drop</code>. Standalone mode means that Flowable will manage the transactions on the JDBC connections that it creates. One transaction per service method.
 * </p>
 * 
 * <p>
 * On all forms of creating a process engine, you can first customize the configuration before calling the {@link #buildProcessEngine()} method by calling any of the setters like this:
 * 
 * <pre>
 * ProcessEngine processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResourceDefault().setMailServerHost(&quot;gmail.com&quot;).setJdbcUsername(&quot;mickey&quot;).setJdbcPassword(&quot;mouse&quot;)
 *         .buildProcessEngine();
 * </pre>
 * 
 * </p>
 * 
 * @see ProcessEngines
 * @author Tom Baeyens
 */
public abstract class ProcessEngineConfiguration extends AbstractEngineConfiguration implements HasTaskIdGeneratorEngineConfiguration {

    protected String processEngineName = ProcessEngines.NAME_DEFAULT;
    protected int idBlockSize = 2500;
    protected String history = HistoryLevel.AUDIT.getKey();
    protected boolean asyncExecutorActivate;
    protected boolean asyncHistoryExecutorActivate;

    protected String mailServerHost = "localhost";
    protected String mailServerUsername; // by default no name and password are provided, which
    protected String mailServerPassword; // means no authentication for mail server
    protected int mailServerPort = 25;
    protected boolean useSSL;
    protected boolean useTLS;
    protected String mailServerDefaultFrom = "flowable@localhost";
    protected String mailSessionJndi;
    protected Map<String, MailServerInfo> mailServers = new HashMap<>();
    protected Map<String, String> mailSessionsJndi = new HashMap<>();

    // Set Http Client config defaults
    protected HttpClientConfig httpClientConfig = new HttpClientConfig();

    protected HistoryLevel historyLevel;
    protected boolean enableProcessDefinitionHistoryLevel;

    protected String jpaPersistenceUnitName;
    protected Object jpaEntityManagerFactory;
    protected boolean jpaHandleTransaction;
    protected boolean jpaCloseEntityManager;

    protected AsyncExecutor asyncExecutor;
    protected AsyncExecutor asyncHistoryExecutor;
    /**
     * Define the default lock time for an async job in seconds. The lock time is used when creating an async job and when it expires the async executor assumes that the job has failed. It will be
     * retried again.
     */
    protected int lockTimeAsyncJobWaitTime = 60;
    /** define the default wait time for a failed job in seconds */
    protected int defaultFailedJobWaitTime = 10;
    /** define the default wait time for a failed async job in seconds */
    protected int asyncFailedJobWaitTime = 10;

    /**
     * Process diagram generator. Default value is DefaultProcessDiagramGenerator
     */
    protected ProcessDiagramGenerator processDiagramGenerator;

    protected boolean isCreateDiagramOnDeploy = true;

    protected String defaultCamelContext = "camelContext";

    protected String activityFontName = "Arial";
    protected String labelFontName = "Arial";
    protected String annotationFontName = "Arial";

    protected ProcessEngineLifecycleListener processEngineLifecycleListener;

    protected boolean enableProcessDefinitionInfoCache;

    /**
     * generator used to generate task ids
     */
    protected IdGenerator taskIdGenerator;

    /** postprocessor for a task builder */
    protected TaskPostProcessor taskPostProcessor = null;

    /** use one of the static createXxxx methods instead */
    protected ProcessEngineConfiguration() {
    }

    public abstract ProcessEngine buildProcessEngine();

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromResourceDefault() {
        return createProcessEngineConfigurationFromResource("flowable.cfg.xml", "processEngineConfiguration");
    }

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromResource(String resource) {
        return createProcessEngineConfigurationFromResource(resource, "processEngineConfiguration");
    }

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromResource(String resource, String beanName) {
        return (ProcessEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromResource(resource, beanName);
    }

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromInputStream(InputStream inputStream) {
        return createProcessEngineConfigurationFromInputStream(inputStream, "processEngineConfiguration");
    }

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
        return (ProcessEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromInputStream(inputStream, beanName);
    }

    public static ProcessEngineConfiguration createStandaloneProcessEngineConfiguration() {
        return new StandaloneProcessEngineConfiguration();
    }

    public static ProcessEngineConfiguration createStandaloneInMemProcessEngineConfiguration() {
        return new StandaloneInMemProcessEngineConfiguration();
    }

    // TODO add later when we have test coverage for this
    // public static ProcessEngineConfiguration
    // createJtaProcessEngineConfiguration() {
    // return new JtaProcessEngineConfiguration();
    // }

    public abstract RepositoryService getRepositoryService();

    public abstract RuntimeService getRuntimeService();

    public abstract FormService getFormService();

    public abstract TaskService getTaskService();

    public abstract HistoryService getHistoryService();

    public abstract IdentityService getIdentityService();

    public abstract ManagementService getManagementService();

    public abstract ProcessEngineConfiguration getProcessEngineConfiguration();

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public String getEngineName() {
        return processEngineName;
    }

    public ProcessEngineConfiguration setEngineName(String processEngineName) {
        this.processEngineName = processEngineName;
        return this;
    }

    public int getIdBlockSize() {
        return idBlockSize;
    }

    public ProcessEngineConfiguration setIdBlockSize(int idBlockSize) {
        this.idBlockSize = idBlockSize;
        return this;
    }

    public String getHistory() {
        return history;
    }

    public ProcessEngineConfiguration setHistory(String history) {
        this.history = history;
        return this;
    }

    public String getMailServerHost() {
        return mailServerHost;
    }

    public ProcessEngineConfiguration setMailServerHost(String mailServerHost) {
        this.mailServerHost = mailServerHost;
        return this;
    }

    public String getMailServerUsername() {
        return mailServerUsername;
    }

    public ProcessEngineConfiguration setMailServerUsername(String mailServerUsername) {
        this.mailServerUsername = mailServerUsername;
        return this;
    }

    public String getMailServerPassword() {
        return mailServerPassword;
    }

    public ProcessEngineConfiguration setMailServerPassword(String mailServerPassword) {
        this.mailServerPassword = mailServerPassword;
        return this;
    }

    public String getMailSessionJndi() {
        return mailSessionJndi;
    }

    public ProcessEngineConfiguration setMailSessionJndi(String mailSessionJndi) {
        this.mailSessionJndi = mailSessionJndi;
        return this;
    }

    public int getMailServerPort() {
        return mailServerPort;
    }

    public ProcessEngineConfiguration setMailServerPort(int mailServerPort) {
        this.mailServerPort = mailServerPort;
        return this;
    }

    public boolean getMailServerUseSSL() {
        return useSSL;
    }

    public ProcessEngineConfiguration setMailServerUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
        return this;
    }

    public boolean getMailServerUseTLS() {
        return useTLS;
    }

    public ProcessEngineConfiguration setMailServerUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
        return this;
    }

    public String getMailServerDefaultFrom() {
        return mailServerDefaultFrom;
    }

    public ProcessEngineConfiguration setMailServerDefaultFrom(String mailServerDefaultFrom) {
        this.mailServerDefaultFrom = mailServerDefaultFrom;
        return this;
    }

    public MailServerInfo getMailServer(String tenantId) {
        return mailServers.get(tenantId);
    }

    public Map<String, MailServerInfo> getMailServers() {
        return mailServers;
    }

    public ProcessEngineConfiguration setMailServers(Map<String, MailServerInfo> mailServers) {
        this.mailServers.putAll(mailServers);
        return this;
    }

    public String getMailSessionJndi(String tenantId) {
        return mailSessionsJndi.get(tenantId);
    }

    public Map<String, String> getMailSessionsJndi() {
        return mailSessionsJndi;
    }

    public ProcessEngineConfiguration setMailSessionsJndi(Map<String, String> mailSessionsJndi) {
        this.mailSessionsJndi.putAll(mailSessionsJndi);
        return this;
    }

    public HttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    public void setHttpClientConfig(HttpClientConfig httpClientConfig) {
        this.httpClientConfig.merge(httpClientConfig);
    }

    @Override
    public ProcessEngineConfiguration setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
        this.databaseSchemaUpdate = databaseSchemaUpdate;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setTransactionsExternallyManaged(boolean transactionsExternallyManaged) {
        this.transactionsExternallyManaged = transactionsExternallyManaged;
        return this;
    }

    public HistoryLevel getHistoryLevel() {
        return historyLevel;
    }

    public ProcessEngineConfiguration setHistoryLevel(HistoryLevel historyLevel) {
        this.historyLevel = historyLevel;
        return this;
    }

    public boolean isEnableProcessDefinitionHistoryLevel() {
        return enableProcessDefinitionHistoryLevel;
    }

    public ProcessEngineConfiguration setEnableProcessDefinitionHistoryLevel(boolean enableProcessDefinitionHistoryLevel) {
        this.enableProcessDefinitionHistoryLevel = enableProcessDefinitionHistoryLevel;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcMaxActiveConnections(int jdbcMaxActiveConnections) {
        this.jdbcMaxActiveConnections = jdbcMaxActiveConnections;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcMaxIdleConnections(int jdbcMaxIdleConnections) {
        this.jdbcMaxIdleConnections = jdbcMaxIdleConnections;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcMaxCheckoutTime(int jdbcMaxCheckoutTime) {
        this.jdbcMaxCheckoutTime = jdbcMaxCheckoutTime;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcMaxWaitTime(int jdbcMaxWaitTime) {
        this.jdbcMaxWaitTime = jdbcMaxWaitTime;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcPingEnabled(boolean jdbcPingEnabled) {
        this.jdbcPingEnabled = jdbcPingEnabled;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcPingQuery(String jdbcPingQuery) {
        this.jdbcPingQuery = jdbcPingQuery;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcPingConnectionNotUsedFor(int jdbcPingNotUsedFor) {
        this.jdbcPingConnectionNotUsedFor = jdbcPingNotUsedFor;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcDefaultTransactionIsolationLevel(int jdbcDefaultTransactionIsolationLevel) {
        this.jdbcDefaultTransactionIsolationLevel = jdbcDefaultTransactionIsolationLevel;
        return this;
    }

    public boolean isAsyncExecutorActivate() {
        return asyncExecutorActivate;
    }

    public ProcessEngineConfiguration setAsyncExecutorActivate(boolean asyncExecutorActivate) {
        this.asyncExecutorActivate = asyncExecutorActivate;
        return this;
    }
    
    public boolean isAsyncHistoryExecutorActivate() {
        return asyncHistoryExecutorActivate;
    }

    public ProcessEngineConfiguration setAsyncHistoryExecutorActivate(boolean asyncHistoryExecutorActivate) {
        this.asyncHistoryExecutorActivate = asyncHistoryExecutorActivate;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setUseClassForNameClassLoading(boolean useClassForNameClassLoading) {
        this.useClassForNameClassLoading = useClassForNameClassLoading;
        return this;
    }

    public Object getJpaEntityManagerFactory() {
        return jpaEntityManagerFactory;
    }

    public ProcessEngineConfiguration setJpaEntityManagerFactory(Object jpaEntityManagerFactory) {
        this.jpaEntityManagerFactory = jpaEntityManagerFactory;
        return this;
    }

    public boolean isJpaHandleTransaction() {
        return jpaHandleTransaction;
    }

    public ProcessEngineConfiguration setJpaHandleTransaction(boolean jpaHandleTransaction) {
        this.jpaHandleTransaction = jpaHandleTransaction;
        return this;
    }

    public boolean isJpaCloseEntityManager() {
        return jpaCloseEntityManager;
    }

    public ProcessEngineConfiguration setJpaCloseEntityManager(boolean jpaCloseEntityManager) {
        this.jpaCloseEntityManager = jpaCloseEntityManager;
        return this;
    }

    public String getJpaPersistenceUnitName() {
        return jpaPersistenceUnitName;
    }

    public ProcessEngineConfiguration setJpaPersistenceUnitName(String jpaPersistenceUnitName) {
        this.jpaPersistenceUnitName = jpaPersistenceUnitName;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDataSourceJndiName(String dataSourceJndiName) {
        this.dataSourceJndiName = dataSourceJndiName;
        return this;
    }

    public String getDefaultCamelContext() {
        return defaultCamelContext;
    }

    public ProcessEngineConfiguration setDefaultCamelContext(String defaultCamelContext) {
        this.defaultCamelContext = defaultCamelContext;
        return this;
    }

    public boolean isCreateDiagramOnDeploy() {
        return isCreateDiagramOnDeploy;
    }

    public ProcessEngineConfiguration setCreateDiagramOnDeploy(boolean createDiagramOnDeploy) {
        this.isCreateDiagramOnDeploy = createDiagramOnDeploy;
        return this;
    }

    public String getActivityFontName() {
        return activityFontName;
    }

    public ProcessEngineConfiguration setActivityFontName(String activityFontName) {
        this.activityFontName = activityFontName;
        return this;
    }

    public ProcessEngineConfiguration setProcessEngineLifecycleListener(ProcessEngineLifecycleListener processEngineLifecycleListener) {
        this.processEngineLifecycleListener = processEngineLifecycleListener;
        return this;
    }

    public ProcessEngineLifecycleListener getProcessEngineLifecycleListener() {
        return processEngineLifecycleListener;
    }

    public String getLabelFontName() {
        return labelFontName;
    }

    public ProcessEngineConfiguration setLabelFontName(String labelFontName) {
        this.labelFontName = labelFontName;
        return this;
    }

    public String getAnnotationFontName() {
        return annotationFontName;
    }

    public ProcessEngineConfiguration setAnnotationFontName(String annotationFontName) {
        this.annotationFontName = annotationFontName;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDatabaseTablePrefix(String databaseTablePrefix) {
        this.databaseTablePrefix = databaseTablePrefix;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setTablePrefixIsSchema(boolean tablePrefixIsSchema) {
        this.tablePrefixIsSchema = tablePrefixIsSchema;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDatabaseWildcardEscapeCharacter(String databaseWildcardEscapeCharacter) {
        this.databaseWildcardEscapeCharacter = databaseWildcardEscapeCharacter;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDatabaseCatalog(String databaseCatalog) {
        this.databaseCatalog = databaseCatalog;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDatabaseSchema(String databaseSchema) {
        this.databaseSchema = databaseSchema;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setXmlEncoding(String xmlEncoding) {
        this.xmlEncoding = xmlEncoding;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public ProcessDiagramGenerator getProcessDiagramGenerator() {
        return this.processDiagramGenerator;
    }

    public ProcessEngineConfiguration setProcessDiagramGenerator(ProcessDiagramGenerator processDiagramGenerator) {
        this.processDiagramGenerator = processDiagramGenerator;
        return this;
    }

    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }

    public ProcessEngineConfiguration setAsyncExecutor(AsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
        return this;
    }
    
    public AsyncExecutor getAsyncHistoryExecutor() {
        return asyncHistoryExecutor;
    }

    public ProcessEngineConfiguration setAsyncHistoryExecutor(AsyncExecutor asyncHistoryExecutor) {
        this.asyncHistoryExecutor = asyncHistoryExecutor;
        return this;
    }

    public int getLockTimeAsyncJobWaitTime() {
        return lockTimeAsyncJobWaitTime;
    }

    public ProcessEngineConfiguration setLockTimeAsyncJobWaitTime(int lockTimeAsyncJobWaitTime) {
        this.lockTimeAsyncJobWaitTime = lockTimeAsyncJobWaitTime;
        return this;
    }

    public int getDefaultFailedJobWaitTime() {
        return defaultFailedJobWaitTime;
    }

    public ProcessEngineConfiguration setDefaultFailedJobWaitTime(int defaultFailedJobWaitTime) {
        this.defaultFailedJobWaitTime = defaultFailedJobWaitTime;
        return this;
    }

    public int getAsyncFailedJobWaitTime() {
        return asyncFailedJobWaitTime;
    }

    public ProcessEngineConfiguration setAsyncFailedJobWaitTime(int asyncFailedJobWaitTime) {
        this.asyncFailedJobWaitTime = asyncFailedJobWaitTime;
        return this;
    }

    public boolean isEnableProcessDefinitionInfoCache() {
        return enableProcessDefinitionInfoCache;
    }

    public ProcessEngineConfiguration setEnableProcessDefinitionInfoCache(boolean enableProcessDefinitionInfoCache) {
        this.enableProcessDefinitionInfoCache = enableProcessDefinitionInfoCache;
        return this;
    }

    @Override
    public void initIdGenerator() {
        super.initIdGenerator();
        if (taskIdGenerator == null) {
            taskIdGenerator = idGenerator;
        }
    }

    @Override
    public IdGenerator getTaskIdGenerator() {
        return taskIdGenerator;
    }

    @Override
    public void setTaskIdGenerator(IdGenerator taskIdGenerator) {
        this.taskIdGenerator = taskIdGenerator;
    }

    public TaskPostProcessor getTaskPostProcessor() {
        return taskPostProcessor;
    }

    public void setTaskPostProcessor(TaskPostProcessor processor) {
        this.taskPostProcessor = processor;
    }
}
