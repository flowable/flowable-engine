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

package org.activiti.engine;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.activiti.engine.impl.cfg.BeansConfigurationHelper;
import org.activiti.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.runtime.JobProcessor;
import org.flowable.common.engine.impl.cfg.mail.MailServerInfo;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.mail.common.api.client.FlowableMailClient;

/**
 * Configuration information from which a process engine can be build.
 * <p>
 * <p>
 * Most common is to create a process engine based on the default configuration file:
 * <p>
 * <pre>
 * ProcessEngine processEngine = ProcessEngineConfiguration
 *         .createProcessEngineConfigurationFromResourceDefault()
 *         .buildProcessEngine();
 * </pre>
 * </p>
 * <p>
 * <p>
 * To create a process engine programatic, without a configuration file, the first option is {@link #createStandaloneProcessEngineConfiguration()}
 * <p>
 * <pre>
 * ProcessEngine processEngine = ProcessEngineConfiguration
 *         .createStandaloneProcessEngineConfiguration()
 *         .buildProcessEngine();
 * </pre>
 * <p>
 * This creates a new process engine with all the defaults to connect to a remote h2 database (jdbc:h2:tcp://localhost/activiti) in standalone mode. Standalone mode means that Activiti will manage the
 * transactions on the JDBC connections that it creates. One transaction per service method. For a description of how to write the configuration files, see the userguide.
 * </p>
 * <p>
 * <p>
 * The second option is great for testing: {@link #createStandaloneInMemProcessEngineConfiguration()}
 * <p>
 * <pre>
 * ProcessEngine processEngine = ProcessEngineConfiguration
 *         .createStandaloneInMemProcessEngineConfiguration()
 *         .buildProcessEngine();
 * </pre>
 * <p>
 * This creates a new process engine with all the defaults to connect to an memory h2 database (jdbc:h2:tcp://localhost/activiti) in standalone mode. The DB schema strategy default is in this case
 * <code>create-drop</code>. Standalone mode means that Activiti will manage the transactions on the JDBC connections that it creates. One transaction per service method.
 * </p>
 * <p>
 * <p>
 * On all forms of creating a process engine, you can first customize the configuration before calling the {@link #buildProcessEngine()} method by calling any of the setters like this:
 * <p>
 * <pre>
 * ProcessEngine processEngine = ProcessEngineConfiguration
 *         .createProcessEngineConfigurationFromResourceDefault()
 *         .setMailServerHost("gmail.com")
 *         .setJdbcUsername("mickey")
 *         .setJdbcPassword("mouse")
 *         .buildProcessEngine();
 * </pre>
 * </p>
 *
 * @author Tom Baeyens
 * @see ProcessEngines
 */
public abstract class ProcessEngineConfiguration {

    /**
     * Checks the version of the DB schema against the library when the process engine is being created and throws an exception if the versions don't match.
     */
    public static final String DB_SCHEMA_UPDATE_FALSE = "false";

    /**
     * Creates the schema when the process engine is being created and drops the schema when the process engine is being closed.
     */
    public static final String DB_SCHEMA_UPDATE_CREATE_DROP = "create-drop";

    /**
     * Upon building of the process engine, a check is performed and an update of the schema is performed if it is necessary.
     */
    public static final String DB_SCHEMA_UPDATE_TRUE = "true";

    /**
     * The tenant id indicating 'no tenant'
     */
    public static final String NO_TENANT_ID = "";

    protected String processEngineName = ProcessEngines.NAME_DEFAULT;
    protected int idBlockSize = 2500;
    protected String history = HistoryLevel.AUDIT.getKey();
    protected boolean asyncExecutorActivate;

    protected FlowableMailClient defaultMailClient;
    protected MailServerInfo defaultMailServer;
    protected String mailSessionJndi;
    protected Map<String, MailServerInfo> mailServers = new HashMap<>();
    protected Map<String, FlowableMailClient> mailClients = new HashMap<>();
    protected Map<String, String> mailSessionsJndi = new HashMap<>();

    protected String databaseType;
    protected String databaseSchemaUpdate = DB_SCHEMA_UPDATE_FALSE;
    protected String jdbcDriver = "org.h2.Driver";
    protected String jdbcUrl = "jdbc:h2:tcp://localhost/~/activiti";
    protected String jdbcUsername = "sa";
    protected String jdbcPassword = "";
    protected String dataSourceJndiName;
    protected boolean isDbHistoryUsed = true;
    protected HistoryLevel historyLevel;
    protected int jdbcMaxActiveConnections;
    protected int jdbcMaxIdleConnections;
    protected int jdbcMaxCheckoutTime;
    protected int jdbcMaxWaitTime;
    protected boolean jdbcPingEnabled;
    protected String jdbcPingQuery;
    protected int jdbcPingConnectionNotUsedFor;
    protected int jdbcDefaultTransactionIsolationLevel;
    protected DataSource dataSource;
    protected boolean transactionsExternallyManaged;

    protected String jpaPersistenceUnitName;
    protected Object jpaEntityManagerFactory;
    protected boolean jpaHandleTransaction;
    protected boolean jpaCloseEntityManager;

    protected Clock clock;
    protected AsyncExecutor asyncExecutor;

    /**
     * Define the default lock time for an async job in seconds. The lock time is used when creating an async job and when it expires the async executor assumes that the job has failed. It will be
     * retried again.
     */
    protected int lockTimeAsyncJobWaitTime = 60;
    /**
     * define the default wait time for a failed job in seconds
     */
    protected int defaultFailedJobWaitTime = 10;
    /**
     * define the default wait time for a failed async job in seconds
     */
    protected int asyncFailedJobWaitTime = 10;

    /**
     * process diagram generator. Default value is DefaultProcessDiagramGenerator
     */
    protected ProcessDiagramGenerator processDiagramGenerator;

    /**
     * Allows configuring a database table prefix which is used for all runtime operations of the process engine. For example, if you specify a prefix named 'PRE1.', activiti will query for executions
     * in a table named 'PRE1.ACT_RU_EXECUTION_'.
     * <p>
     * <p/>
     * <strong>NOTE: the prefix is not respected by automatic database schema management. If you use {@link ProcessEngineConfiguration#DB_SCHEMA_UPDATE_CREATE_DROP} or
     * {@link ProcessEngineConfiguration#DB_SCHEMA_UPDATE_TRUE}, activiti will create the database tables using the default names, regardless of the prefix configured here.</strong>
     *
     * @since 5.9
     */
    protected String databaseTablePrefix = "";

    /**
     * Escape character for doing wildcard searches.
     * <p>
     * This will be added at then end of queries that include for example a LIKE clause. For example: SELECT * FROM table WHERE column LIKE '%\%%' ESCAPE '\';
     */
    protected String databaseWildcardEscapeCharacter;

    /**
     * database catalog to use
     */
    protected String databaseCatalog = "";

    /**
     * In some situations you want to set the schema to use for table checks / generation if the database metadata doesn't return that correctly, see https://activiti.atlassian.net/browse/ACT-1220,
     * https://activiti.atlassian.net/browse/ACT-1062
     */
    protected String databaseSchema;

    /**
     * Set to true in case the defined databaseTablePrefix is a schema-name, instead of an actual table name prefix. This is relevant for checking if Activiti-tables exist, the databaseTablePrefix
     * will not be used here - since the schema is taken into account already, adding a prefix for the table-check will result in wrong table-names.
     *
     * @since 5.15
     */
    protected boolean tablePrefixIsSchema;

    protected boolean isCreateDiagramOnDeploy = true;
    
    /**
     *  include the sequence flow name in case there's no Label DI, 
     */
    protected boolean drawSequenceFlowNameWithNoLabelDI = false;

    protected String xmlEncoding = "UTF-8";

    protected String defaultCamelContext = "camelContext";

    protected String activityFontName = "Arial";
    protected String labelFontName = "Arial";
    protected String annotationFontName = "Arial";

    protected ClassLoader classLoader;
    /**
     * Either use Class.forName or ClassLoader.loadClass for class loading. See http://forums.activiti.org/content/reflectutilloadclass-and-custom-classloader
     */
    protected boolean useClassForNameClassLoading = true;
    protected ProcessEngineLifecycleListener processEngineLifecycleListener;

    protected boolean enableProcessDefinitionInfoCache;

    protected List<JobProcessor> jobProcessors = Collections.emptyList();

    /**
     * use one of the static createXxxx methods instead
     */
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
        return BeansConfigurationHelper.parseProcessEngineConfigurationFromResource(resource, beanName);
    }

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromInputStream(InputStream inputStream) {
        return createProcessEngineConfigurationFromInputStream(inputStream, "processEngineConfiguration");
    }

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
        return BeansConfigurationHelper.parseProcessEngineConfigurationFromInputStream(inputStream, beanName);
    }

    public static ProcessEngineConfiguration createStandaloneProcessEngineConfiguration() {
        return new StandaloneProcessEngineConfiguration();
    }

    public static ProcessEngineConfiguration createStandaloneInMemProcessEngineConfiguration() {
        return new StandaloneInMemProcessEngineConfiguration();
    }

    // TODO add later when we have test coverage for this
    // public static ProcessEngineConfiguration createJtaProcessEngineConfiguration() {
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

    // getters and setters //////////////////////////////////////////////////////

    public String getProcessEngineName() {
        return processEngineName;
    }

    public ProcessEngineConfiguration setProcessEngineName(String processEngineName) {
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

    public FlowableMailClient getDefaultMailClient() {
        return defaultMailClient;
    }

    public ProcessEngineConfiguration setDefaultMailClient(FlowableMailClient defaultMailClient) {
        this.defaultMailClient = defaultMailClient;
        return this;
    }

    public MailServerInfo getDefaultMailServer() {
        return getOrCreateDefaultMaiLServer();
    }

    public ProcessEngineConfiguration setDefaultMailServer(MailServerInfo defaultMailServer) {
        this.defaultMailServer = defaultMailServer;
        return this;
    }

    protected MailServerInfo getOrCreateDefaultMaiLServer() {
        if (defaultMailServer == null) {
            defaultMailServer = new MailServerInfo();
            defaultMailServer.setMailServerHost("localhost");
            defaultMailServer.setMailServerPort(25);
            defaultMailServer.setMailServerSSLPort(465);
            defaultMailServer.setMailServerDefaultFrom("flowable@localhost");
        }
        return defaultMailServer;
    }

    public String getMailServerHost() {
        return getOrCreateDefaultMaiLServer().getMailServerHost();
    }

    public ProcessEngineConfiguration setMailServerHost(String mailServerHost) {
        getOrCreateDefaultMaiLServer().setMailServerHost(mailServerHost);
        return this;
    }

    public String getMailServerUsername() {
        return getOrCreateDefaultMaiLServer().getMailServerUsername();
    }

    public ProcessEngineConfiguration setMailServerUsername(String mailServerUsername) {
        getOrCreateDefaultMaiLServer().setMailServerUsername(mailServerUsername);
        return this;
    }

    public String getMailServerPassword() {
        return getOrCreateDefaultMaiLServer().getMailServerPassword();
    }

    public ProcessEngineConfiguration setMailServerPassword(String mailServerPassword) {
        getOrCreateDefaultMaiLServer().setMailServerPassword(mailServerPassword);
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
        return getOrCreateDefaultMaiLServer().getMailServerPort();
    }

    public ProcessEngineConfiguration setMailServerPort(int mailServerPort) {
        getOrCreateDefaultMaiLServer().setMailServerPort(mailServerPort);
        return this;
    }

    public int getMailServerSSLPort() {
        return getOrCreateDefaultMaiLServer().getMailServerSSLPort();
    }

    public ProcessEngineConfiguration setMailServerSSLPort(int mailServerSSLPort) {
        getOrCreateDefaultMaiLServer().setMailServerSSLPort(mailServerSSLPort);
        return this;
    }

    public boolean getMailServerUseSSL() {
        return getOrCreateDefaultMaiLServer().isMailServerUseSSL();
    }

    public ProcessEngineConfiguration setMailServerUseSSL(boolean useSSL) {
        getOrCreateDefaultMaiLServer().setMailServerUseSSL(useSSL);
        return this;
    }

    public boolean getMailServerUseTLS() {
        return getOrCreateDefaultMaiLServer().isMailServerUseTLS();
    }

    public ProcessEngineConfiguration setMailServerUseTLS(boolean useTLS) {
        getOrCreateDefaultMaiLServer().setMailServerUseTLS(useTLS);
        return this;
    }

    public String getMailServerDefaultFrom() {
        return getOrCreateDefaultMaiLServer().getMailServerDefaultFrom();
    }

    public ProcessEngineConfiguration setMailServerDefaultFrom(String mailServerDefaultFrom) {
        getOrCreateDefaultMaiLServer().setMailServerDefaultFrom(mailServerDefaultFrom);
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

    public FlowableMailClient getMailClient(String tenantId) {
        return mailClients.get(tenantId);
    }

    public Map<String, FlowableMailClient> getMailClients() {
        return mailClients;
    }

    public ProcessEngineConfiguration setMailClients(Map<String, FlowableMailClient> mailClients) {
        this.mailClients.putAll(mailClients);
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

    public String getDatabaseType() {
        return databaseType;
    }

    public ProcessEngineConfiguration setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
        return this;
    }

    public String getDatabaseSchemaUpdate() {
        return databaseSchemaUpdate;
    }

    public ProcessEngineConfiguration setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
        this.databaseSchemaUpdate = databaseSchemaUpdate;
        return this;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public ProcessEngineConfiguration setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public ProcessEngineConfiguration setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
        return this;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public ProcessEngineConfiguration setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        return this;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public ProcessEngineConfiguration setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
        return this;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public ProcessEngineConfiguration setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
        return this;
    }

    public boolean isTransactionsExternallyManaged() {
        return transactionsExternallyManaged;
    }

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

    public boolean isDbHistoryUsed() {
        return isDbHistoryUsed;
    }

    public ProcessEngineConfiguration setDbHistoryUsed(boolean isDbHistoryUsed) {
        this.isDbHistoryUsed = isDbHistoryUsed;
        return this;
    }

    public int getJdbcMaxActiveConnections() {
        return jdbcMaxActiveConnections;
    }

    public ProcessEngineConfiguration setJdbcMaxActiveConnections(int jdbcMaxActiveConnections) {
        this.jdbcMaxActiveConnections = jdbcMaxActiveConnections;
        return this;
    }

    public int getJdbcMaxIdleConnections() {
        return jdbcMaxIdleConnections;
    }

    public ProcessEngineConfiguration setJdbcMaxIdleConnections(int jdbcMaxIdleConnections) {
        this.jdbcMaxIdleConnections = jdbcMaxIdleConnections;
        return this;
    }

    public int getJdbcMaxCheckoutTime() {
        return jdbcMaxCheckoutTime;
    }

    public ProcessEngineConfiguration setJdbcMaxCheckoutTime(int jdbcMaxCheckoutTime) {
        this.jdbcMaxCheckoutTime = jdbcMaxCheckoutTime;
        return this;
    }

    public int getJdbcMaxWaitTime() {
        return jdbcMaxWaitTime;
    }

    public ProcessEngineConfiguration setJdbcMaxWaitTime(int jdbcMaxWaitTime) {
        this.jdbcMaxWaitTime = jdbcMaxWaitTime;
        return this;
    }

    public boolean isJdbcPingEnabled() {
        return jdbcPingEnabled;
    }

    public ProcessEngineConfiguration setJdbcPingEnabled(boolean jdbcPingEnabled) {
        this.jdbcPingEnabled = jdbcPingEnabled;
        return this;
    }

    public String getJdbcPingQuery() {
        return jdbcPingQuery;
    }

    public ProcessEngineConfiguration setJdbcPingQuery(String jdbcPingQuery) {
        this.jdbcPingQuery = jdbcPingQuery;
        return this;
    }

    public int getJdbcPingConnectionNotUsedFor() {
        return jdbcPingConnectionNotUsedFor;
    }

    public ProcessEngineConfiguration setJdbcPingConnectionNotUsedFor(int jdbcPingNotUsedFor) {
        this.jdbcPingConnectionNotUsedFor = jdbcPingNotUsedFor;
        return this;
    }

    public int getJdbcDefaultTransactionIsolationLevel() {
        return jdbcDefaultTransactionIsolationLevel;
    }

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

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public ProcessEngineConfiguration setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }
    
    public boolean isDrawSequenceFlowNameWithNoLabelDI() {
        return drawSequenceFlowNameWithNoLabelDI;
    }
    
    public ProcessEngineConfiguration setDrawSequenceFlowNameWithNoLabelDI(boolean drawSequenceFlowNameWithNoLabelDI) {
        this.drawSequenceFlowNameWithNoLabelDI = drawSequenceFlowNameWithNoLabelDI;
        return this;
    }
    
    public boolean isUseClassForNameClassLoading() {
        return useClassForNameClassLoading;
    }

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

    public String getDataSourceJndiName() {
        return dataSourceJndiName;
    }

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

    public String getDatabaseTablePrefix() {
        return databaseTablePrefix;
    }

    public ProcessEngineConfiguration setDatabaseTablePrefix(String databaseTablePrefix) {
        this.databaseTablePrefix = databaseTablePrefix;
        return this;
    }

    public String getDatabaseWildcardEscapeCharacter() {
        return databaseWildcardEscapeCharacter;
    }

    public ProcessEngineConfiguration setDatabaseWildcardEscapeCharacter(String databaseWildcardEscapeCharacter) {
        this.databaseWildcardEscapeCharacter = databaseWildcardEscapeCharacter;
        return this;
    }

    public ProcessEngineConfiguration setTablePrefixIsSchema(boolean tablePrefixIsSchema) {
        this.tablePrefixIsSchema = tablePrefixIsSchema;
        return this;
    }

    public boolean isTablePrefixIsSchema() {
        return tablePrefixIsSchema;
    }

    public String getDatabaseCatalog() {
        return databaseCatalog;
    }

    public ProcessEngineConfiguration setDatabaseCatalog(String databaseCatalog) {
        this.databaseCatalog = databaseCatalog;
        return this;
    }

    public String getDatabaseSchema() {
        return databaseSchema;
    }

    public ProcessEngineConfiguration setDatabaseSchema(String databaseSchema) {
        this.databaseSchema = databaseSchema;
        return this;
    }

    public String getXmlEncoding() {
        return xmlEncoding;
    }

    public ProcessEngineConfiguration setXmlEncoding(String xmlEncoding) {
        this.xmlEncoding = xmlEncoding;
        return this;
    }

    public Clock getClock() {
        return clock;
    }

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

    public List<JobProcessor> getJobProcessors() {
        return jobProcessors;
    }

    public ProcessEngineConfiguration setJobProcessors(List<JobProcessor> jobProcessors) {
        this.jobProcessors = jobProcessors;
        return this;
    }

}
