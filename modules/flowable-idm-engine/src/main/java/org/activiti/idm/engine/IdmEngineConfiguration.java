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
package org.activiti.idm.engine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.sql.DataSource;

import org.activiti.engine.AbstractEngineConfiguration;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.event.ActivitiEventDispatcher;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.impl.cfg.IdGenerator;
import org.activiti.engine.impl.cfg.TransactionContextFactory;
import org.activiti.engine.impl.interceptor.CommandConfig;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.runtime.Clock;
import org.activiti.idm.api.IdmIdentityService;
import org.activiti.idm.api.IdmManagementService;
import org.activiti.idm.api.event.ActivitiIdmEventType;
import org.activiti.idm.engine.delegate.event.impl.ActivitiIdmEventDispatcherImpl;
import org.activiti.idm.engine.impl.IdmEngineImpl;
import org.activiti.idm.engine.impl.IdmIdentityServiceImpl;
import org.activiti.idm.engine.impl.IdmManagementServiceImpl;
import org.activiti.idm.engine.impl.ServiceImpl;
import org.activiti.idm.engine.impl.cfg.CommandExecutorImpl;
import org.activiti.idm.engine.impl.cfg.StandaloneIdmEngineConfiguration;
import org.activiti.idm.engine.impl.cfg.StandaloneInMemIdmEngineConfiguration;
import org.activiti.idm.engine.impl.cfg.TransactionListener;
import org.activiti.idm.engine.impl.cfg.standalone.StandaloneMybatisTransactionContextFactory;
import org.activiti.idm.engine.impl.db.DbSqlSessionFactory;
import org.activiti.idm.engine.impl.interceptor.CommandContext;
import org.activiti.idm.engine.impl.interceptor.CommandContextFactory;
import org.activiti.idm.engine.impl.interceptor.CommandContextInterceptor;
import org.activiti.idm.engine.impl.interceptor.CommandExecutor;
import org.activiti.idm.engine.impl.interceptor.CommandInterceptor;
import org.activiti.idm.engine.impl.interceptor.CommandInvoker;
import org.activiti.idm.engine.impl.interceptor.LogInterceptor;
import org.activiti.idm.engine.impl.interceptor.TransactionContextInterceptor;
import org.activiti.idm.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.ByteArrayEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.CapabilityEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.CapabilityEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.GroupEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.GroupEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.IdentityInfoEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.IdentityInfoEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.MembershipEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.MembershipEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.PropertyEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.PropertyEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.TableDataManager;
import org.activiti.idm.engine.impl.persistence.entity.TableDataManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.TokenEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.TokenEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.UserEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.UserEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.data.ByteArrayDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.CapabilityDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.GroupDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.IdentityInfoDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.MembershipDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.PropertyDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.TokenDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.UserDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisByteArrayDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisCapabilityDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisGroupDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisIdentityInfoDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisMembershipDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisPropertyDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisTokenDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisUserDataManager;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;

public class IdmEngineConfiguration extends AbstractEngineConfiguration {

  protected static final Logger logger = LoggerFactory.getLogger(IdmEngineConfiguration.class);

  public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/activiti/idm/db/mapping/mappings.xml";

  protected String idmEngineName = IdmEngines.NAME_DEFAULT;

  // COMMAND EXECUTORS ///////////////////////////////////////////////

  protected CommandInterceptor commandInvoker;

  /**
   * the configurable list which will be {@link #initInterceptorChain(java.util.List) processed} to build the {@link #commandExecutor}
   */
  protected List<CommandInterceptor> customPreCommandInterceptors;
  protected List<CommandInterceptor> customPostCommandInterceptors;

  protected List<CommandInterceptor> commandInterceptors;

  /** this will be initialized during the configurationComplete() */
  protected CommandExecutor commandExecutor;

  // SERVICES
  // /////////////////////////////////////////////////////////////////

  protected IdmIdentityService idmIdentityService = new IdmIdentityServiceImpl();
  protected IdmManagementService idmManagementService = new IdmManagementServiceImpl();
  
  // DATA MANAGERS ///////////////////////////////////////////////////

  protected ByteArrayDataManager byteArrayDataManager;
  protected GroupDataManager groupDataManager;
  protected IdentityInfoDataManager identityInfoDataManager;
  protected MembershipDataManager membershipDataManager;
  protected PropertyDataManager propertyDataManager;
  protected TokenDataManager tokenDataManager;
  protected UserDataManager userDataManager;
  protected CapabilityDataManager capabilityDataManager;

  // ENTITY MANAGERS /////////////////////////////////////////////////
  protected ByteArrayEntityManager byteArrayEntityManager;
  protected GroupEntityManager groupEntityManager;
  protected IdentityInfoEntityManager identityInfoEntityManager;
  protected MembershipEntityManager membershipEntityManager;
  protected PropertyEntityManager propertyEntityManager;
  protected TableDataManager tableDataManager;
  protected TokenEntityManager tokenEntityManager;
  protected UserEntityManager userEntityManager;
  protected CapabilityEntityManager capabilityEntityManager;

  protected CommandContextFactory commandContextFactory;
  protected TransactionContextFactory<TransactionListener, CommandContext> transactionContextFactory;

  // SESSION FACTORIES ///////////////////////////////////////////////
  protected DbSqlSessionFactory dbSqlSessionFactory;

  public static IdmEngineConfiguration createIdmEngineConfigurationFromResourceDefault() {
    return createIdmEngineConfigurationFromResource("activiti.idm.cfg.xml", "idmEngineConfiguration");
  }

  public static IdmEngineConfiguration createIdmEngineConfigurationFromResource(String resource) {
    return createIdmEngineConfigurationFromResource(resource, "idmEngineConfiguration");
  }

  public static IdmEngineConfiguration createIdmEngineConfigurationFromResource(String resource, String beanName) {
    return (IdmEngineConfiguration) parseEngineConfigurationFromResource(resource, beanName);
  }

  public static IdmEngineConfiguration createIdmEngineConfigurationFromInputStream(InputStream inputStream) {
    return createIdmEngineConfigurationFromInputStream(inputStream, "idmEngineConfiguration");
  }

  public static IdmEngineConfiguration createIdmEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
    return (IdmEngineConfiguration) parseEngineConfigurationFromInputStream(inputStream, beanName);
  }

  public static IdmEngineConfiguration createStandaloneIdmEngineConfiguration() {
    return new StandaloneIdmEngineConfiguration();
  }

  public static IdmEngineConfiguration createStandaloneInMemIdmEngineConfiguration() {
    return new StandaloneInMemIdmEngineConfiguration();
  }

  // buildProcessEngine
  // ///////////////////////////////////////////////////////

  public IdmEngine buildIdmEngine() {
    init();
    return new IdmEngineImpl(this);
  }

  // init
  // /////////////////////////////////////////////////////////////////////

  protected void init() {
    initCommandContextFactory();
    initTransactionContextFactory();
    initCommandExecutors();
    initIdGenerator();
    
    if (usingRelationalDatabase) {
      initDataSource();
    }
    
    initTransactionFactory();
    initSqlSessionFactory();
    initSessionFactories();
    initServices();
    initDataManagers();
    initEntityManagers();
    initClock();
    initEventDispatcher();
  }
  
  // services
  // /////////////////////////////////////////////////////////////////

  protected void initServices() {
    initService(idmIdentityService);
    initService(idmManagementService);
  }

  protected void initService(Object service) {
    if (service instanceof ServiceImpl) {
      ((ServiceImpl) service).setCommandExecutor(commandExecutor);
    }
  }
  
  // Data managers
  ///////////////////////////////////////////////////////////

  public void initDataManagers() {
    if (byteArrayDataManager == null) {
      byteArrayDataManager = new MybatisByteArrayDataManager(this);
    }
    if (groupDataManager == null) {
    	groupDataManager = new MybatisGroupDataManager(this);
    }
    if (identityInfoDataManager == null) {
    	identityInfoDataManager = new MybatisIdentityInfoDataManager(this);
    }
    if (membershipDataManager == null) {
    	membershipDataManager = new MybatisMembershipDataManager(this);
    }
    if (propertyDataManager == null) {
      propertyDataManager = new MybatisPropertyDataManager(this);
    }
    if (tokenDataManager == null) {
      tokenDataManager = new MybatisTokenDataManager(this);
    }
    if (userDataManager == null) {
    	userDataManager = new MybatisUserDataManager(this);
    }
    if (capabilityDataManager == null) {
      capabilityDataManager = new MybatisCapabilityDataManager(this);
    }
  }

  public void initEntityManagers() {
    if (byteArrayEntityManager == null) {
      byteArrayEntityManager = new ByteArrayEntityManagerImpl(this, byteArrayDataManager);
    }
    if (groupEntityManager == null) {
    	groupEntityManager = new GroupEntityManagerImpl(this, groupDataManager);
    }
    if (identityInfoEntityManager == null) {
    	identityInfoEntityManager = new IdentityInfoEntityManagerImpl(this, identityInfoDataManager);
    }
    if (membershipEntityManager == null) {
    	membershipEntityManager = new MembershipEntityManagerImpl(this, membershipDataManager);
    }
    if (propertyEntityManager == null) {
      propertyEntityManager = new PropertyEntityManagerImpl(this, propertyDataManager);
    }
    if (tableDataManager == null) {
      tableDataManager = new TableDataManagerImpl(this);
    }
    if (tokenEntityManager == null) {
      tokenEntityManager = new TokenEntityManagerImpl(this, tokenDataManager);
    }
    if (userEntityManager == null) {
      userEntityManager = new UserEntityManagerImpl(this, userDataManager);
    }
    if (capabilityEntityManager == null) {
      capabilityEntityManager = new CapabilityEntityManagerImpl(this, capabilityDataManager);
    }
  }
  
  // session factories ////////////////////////////////////////////////////////

  public void initSessionFactories() {
    if (sessionFactories == null) {
      sessionFactories = new HashMap<Class<?>, SessionFactory>();

      if (usingRelationalDatabase) {
        initDbSqlSessionFactory();
      }
    }

    if (customSessionFactories != null) {
      for (SessionFactory sessionFactory : customSessionFactories) {
        addSessionFactory(sessionFactory);
      }
    }
  }

  public void initDbSqlSessionFactory() {
    if (dbSqlSessionFactory == null) {
      dbSqlSessionFactory = createDbSqlSessionFactory();
    }
    dbSqlSessionFactory.setDatabaseType(databaseType);
    dbSqlSessionFactory.setSqlSessionFactory(sqlSessionFactory);
    dbSqlSessionFactory.setIdGenerator(idGenerator);
    dbSqlSessionFactory.setDatabaseTablePrefix(databaseTablePrefix);
    dbSqlSessionFactory.setTablePrefixIsSchema(tablePrefixIsSchema);
    dbSqlSessionFactory.setDatabaseCatalog(databaseCatalog);
    dbSqlSessionFactory.setDatabaseSchema(databaseSchema);
    addSessionFactory(dbSqlSessionFactory);
  }

  public DbSqlSessionFactory createDbSqlSessionFactory() {
    return new DbSqlSessionFactory();
  }
  
  public String pathToEngineDbProperties() {
    return "org/activiti/idm/db/properties/" + databaseType + ".properties";
  }

  // command executors
  // ////////////////////////////////////////////////////////

  public void initCommandExecutors() {
    initDefaultCommandConfig();
    initSchemaCommandConfig();
    initCommandInvoker();
    initCommandInterceptors();
    initCommandExecutor();
  }

  public void initCommandInvoker() {
    if (commandInvoker == null) {
      commandInvoker = new CommandInvoker();
    }
  }

  public void initCommandInterceptors() {
    if (commandInterceptors == null) {
      commandInterceptors = new ArrayList<CommandInterceptor>();
      if (customPreCommandInterceptors != null) {
        commandInterceptors.addAll(customPreCommandInterceptors);
      }
      commandInterceptors.addAll(getDefaultCommandInterceptors());
      if (customPostCommandInterceptors != null) {
        commandInterceptors.addAll(customPostCommandInterceptors);
      }
      commandInterceptors.add(commandInvoker);
    }
  }

  public Collection<? extends CommandInterceptor> getDefaultCommandInterceptors() {
    List<CommandInterceptor> interceptors = new ArrayList<CommandInterceptor>();
    interceptors.add(new LogInterceptor());
    
    CommandInterceptor transactionInterceptor = createTransactionInterceptor();
    if (transactionInterceptor != null) {
      interceptors.add(transactionInterceptor);
    }
    
    if (commandContextFactory != null) {
      interceptors.add(new CommandContextInterceptor(commandContextFactory, this));
    }
    
    if (transactionContextFactory != null) {
      interceptors.add(new TransactionContextInterceptor(transactionContextFactory));
    }
    
    return interceptors;
  }

  public void initCommandExecutor() {
    if (commandExecutor == null) {
      CommandInterceptor first = initInterceptorChain(commandInterceptors);
      commandExecutor = new CommandExecutorImpl(getDefaultCommandConfig(), first);
    }
  }

  public CommandInterceptor initInterceptorChain(List<CommandInterceptor> chain) {
    if (chain == null || chain.isEmpty()) {
      throw new ActivitiException("invalid command interceptor chain configuration: " + chain);
    }
    for (int i = 0; i < chain.size() - 1; i++) {
      chain.get(i).setNext(chain.get(i + 1));
    }
    return chain.get(0);
  }

  public CommandInterceptor createTransactionInterceptor() {
    // Should be overridden by subclasses
    return null;
  }

  // OTHER
  // ////////////////////////////////////////////////////////////////////

  public void initCommandContextFactory() {
    if (commandContextFactory == null) {
      commandContextFactory = new CommandContextFactory();
    }
    commandContextFactory.setIdmEngineConfiguration(this);
  }

  public void initTransactionContextFactory() {
    if (transactionContextFactory == null) {
      transactionContextFactory = new StandaloneMybatisTransactionContextFactory();
    }
  }

  public InputStream getMyBatisXmlConfigurationStream() {
    return getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
  }
  
  public void initEventDispatcher() {
    if (this.eventDispatcher == null) {
      this.eventDispatcher = new ActivitiIdmEventDispatcherImpl();
    }

    this.eventDispatcher.setEnabled(enableEventDispatcher);

    if (eventListeners != null) {
      for (ActivitiEventListener listenerToAdd : eventListeners) {
        this.eventDispatcher.addEventListener(listenerToAdd);
      }
    }

    if (typedEventListeners != null) {
      for (Entry<String, List<ActivitiEventListener>> listenersToAdd : typedEventListeners.entrySet()) {
        // Extract types from the given string
        ActivitiIdmEventType[] types = ActivitiIdmEventType.getTypesFromString(listenersToAdd.getKey());

        for (ActivitiEventListener listenerToAdd : listenersToAdd.getValue()) {
          this.eventDispatcher.addEventListener(listenerToAdd, types);
        }
      }
    }

  }

  // getters and setters
  // //////////////////////////////////////////////////////

  public String getEngineName() {
    return idmEngineName;
  }

  public IdmEngineConfiguration setEngineName(String idmEngineName) {
    this.idmEngineName = idmEngineName;
    return this;
  }

  public IdmEngineConfiguration setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  public IdmEngineConfiguration setUseClassForNameClassLoading(boolean useClassForNameClassLoading) {
    this.useClassForNameClassLoading = useClassForNameClassLoading;
    return this;
  }

  public IdmEngineConfiguration setDatabaseType(String databaseType) {
    this.databaseType = databaseType;
    return this;
  }

  public IdmEngineConfiguration setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
    return this;
  }

  public IdmEngineConfiguration setJdbcDriver(String jdbcDriver) {
    this.jdbcDriver = jdbcDriver;
    return this;
  }

  @Override
  public IdmEngineConfiguration setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
    return this;
  }

  public IdmEngineConfiguration setJdbcUsername(String jdbcUsername) {
    this.jdbcUsername = jdbcUsername;
    return this;
  }

  public IdmEngineConfiguration setJdbcPassword(String jdbcPassword) {
    this.jdbcPassword = jdbcPassword;
    return this;
  }

  public IdmEngineConfiguration setJdbcMaxActiveConnections(int jdbcMaxActiveConnections) {
    this.jdbcMaxActiveConnections = jdbcMaxActiveConnections;
    return this;
  }

  public IdmEngineConfiguration setJdbcMaxIdleConnections(int jdbcMaxIdleConnections) {
    this.jdbcMaxIdleConnections = jdbcMaxIdleConnections;
    return this;
  }

  public IdmEngineConfiguration setJdbcMaxCheckoutTime(int jdbcMaxCheckoutTime) {
    this.jdbcMaxCheckoutTime = jdbcMaxCheckoutTime;
    return this;
  }

  public IdmEngineConfiguration setJdbcMaxWaitTime(int jdbcMaxWaitTime) {
    this.jdbcMaxWaitTime = jdbcMaxWaitTime;
    return this;
  }

  public IdmEngineConfiguration setJdbcPingEnabled(boolean jdbcPingEnabled) {
    this.jdbcPingEnabled = jdbcPingEnabled;
    return this;
  }

  public IdmEngineConfiguration setJdbcPingConnectionNotUsedFor(int jdbcPingConnectionNotUsedFor) {
    this.jdbcPingConnectionNotUsedFor = jdbcPingConnectionNotUsedFor;
    return this;
  }

  public IdmEngineConfiguration setJdbcDefaultTransactionIsolationLevel(int jdbcDefaultTransactionIsolationLevel) {
    this.jdbcDefaultTransactionIsolationLevel = jdbcDefaultTransactionIsolationLevel;
    return this;
  }

  public IdmEngineConfiguration setJdbcPingQuery(String jdbcPingQuery) {
    this.jdbcPingQuery = jdbcPingQuery;
    return this;
  }

  public IdmEngineConfiguration setDataSourceJndiName(String dataSourceJndiName) {
    this.dataSourceJndiName = dataSourceJndiName;
    return this;
  }

  public IdmEngineConfiguration setSchemaCommandConfig(CommandConfig schemaCommandConfig) {
    this.schemaCommandConfig = schemaCommandConfig;
    return this;
  }

  public IdmEngineConfiguration setTransactionsExternallyManaged(boolean transactionsExternallyManaged) {
    this.transactionsExternallyManaged = transactionsExternallyManaged;
    return this;
  }

  public IdmEngineConfiguration setIdGenerator(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
    return this;
  }

  public IdmEngineConfiguration setXmlEncoding(String xmlEncoding) {
    this.xmlEncoding = xmlEncoding;
    return this;
  }

  public IdmEngineConfiguration setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    return this;
  }

  public IdmEngineConfiguration setDefaultCommandConfig(CommandConfig defaultCommandConfig) {
    this.defaultCommandConfig = defaultCommandConfig;
    return this;
  }

  public CommandInterceptor getCommandInvoker() {
    return commandInvoker;
  }

  public IdmEngineConfiguration setCommandInvoker(CommandInterceptor commandInvoker) {
    this.commandInvoker = commandInvoker;
    return this;
  }

  public List<CommandInterceptor> getCustomPreCommandInterceptors() {
    return customPreCommandInterceptors;
  }

  public IdmEngineConfiguration setCustomPreCommandInterceptors(List<CommandInterceptor> customPreCommandInterceptors) {
    this.customPreCommandInterceptors = customPreCommandInterceptors;
    return this;
  }

  public List<CommandInterceptor> getCustomPostCommandInterceptors() {
    return customPostCommandInterceptors;
  }

  public IdmEngineConfiguration setCustomPostCommandInterceptors(List<CommandInterceptor> customPostCommandInterceptors) {
    this.customPostCommandInterceptors = customPostCommandInterceptors;
    return this;
  }

  public List<CommandInterceptor> getCommandInterceptors() {
    return commandInterceptors;
  }

  public IdmEngineConfiguration setCommandInterceptors(List<CommandInterceptor> commandInterceptors) {
    this.commandInterceptors = commandInterceptors;
    return this;
  }

  public CommandExecutor getCommandExecutor() {
    return commandExecutor;
  }

  public IdmEngineConfiguration setCommandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
    return this;
  }

  public IdmIdentityService getIdmIdentityService() {
    return idmIdentityService;
  }
  
  public IdmEngineConfiguration setIdmIdentityService(IdmIdentityService idmIdentityService) {
    this.idmIdentityService = idmIdentityService;
    return this;
  }
  
  public IdmManagementService getIdmManagementService() {
    return idmManagementService;
  }
  
  public IdmEngineConfiguration setIdmManagementService(IdmManagementService idmManagementService) {
    this.idmManagementService = idmManagementService;
    return this;
  }

  public IdmEngineConfiguration getIdmEngineConfiguration() {
    return this;
  }

  public ByteArrayDataManager getByteArrayDataManager() {
    return byteArrayDataManager;
  }

  public IdmEngineConfiguration setByteArrayDataManager(ByteArrayDataManager byteArrayDataManager) {
    this.byteArrayDataManager = byteArrayDataManager;
    return this;
  }

  public GroupDataManager getGroupDataManager() {
    return groupDataManager;
  }

  public IdmEngineConfiguration setGroupDataManager(GroupDataManager groupDataManager) {
    this.groupDataManager = groupDataManager;
    return this;
  }
  
  public IdentityInfoDataManager getIdentityInfoDataManager() {
    return identityInfoDataManager;
  }

  public IdmEngineConfiguration setIdentityInfoDataManager(IdentityInfoDataManager identityInfoDataManager) {
    this.identityInfoDataManager = identityInfoDataManager;
    return this;
  }
  
  public MembershipDataManager getMembershipDataManager() {
    return membershipDataManager;
  }

  public IdmEngineConfiguration setMembershipDataManager(MembershipDataManager membershipDataManager) {
    this.membershipDataManager = membershipDataManager;
    return this;
  }
  
  public PropertyDataManager getPropertyDataManager() {
    return propertyDataManager;
  }

  public IdmEngineConfiguration setPropertyDataManager(PropertyDataManager propertyDataManager) {
    this.propertyDataManager = propertyDataManager;
    return this;
  }

  public TokenDataManager getTokenDataManager() {
    return tokenDataManager;
  }

  public IdmEngineConfiguration setTokenDataManager(TokenDataManager tokenDataManager) {
    this.tokenDataManager = tokenDataManager;
    return this;
  }

  public UserDataManager getUserDataManager() {
    return userDataManager;
  }

  public IdmEngineConfiguration setUserDataManager(UserDataManager userDataManager) {
    this.userDataManager = userDataManager;
    return this;
  }
  
  public CapabilityDataManager getCapabilityDataManager() {
    return capabilityDataManager;
  }

  public IdmEngineConfiguration setCapabilityDataManager(CapabilityDataManager capabilityDataManager) {
    this.capabilityDataManager = capabilityDataManager;
    return this;
  }

  public ByteArrayEntityManager getByteArrayEntityManager() {
    return byteArrayEntityManager;
  }

  public IdmEngineConfiguration setByteArrayEntityManager(ByteArrayEntityManager byteArrayEntityManager) {
    this.byteArrayEntityManager = byteArrayEntityManager;
    return this;
  }

  public GroupEntityManager getGroupEntityManager() {
    return groupEntityManager;
  }

  public IdmEngineConfiguration setGroupEntityManager(GroupEntityManager groupEntityManager) {
    this.groupEntityManager = groupEntityManager;
    return this;
  }

  public IdentityInfoEntityManager getIdentityInfoEntityManager() {
    return identityInfoEntityManager;
  }

  public IdmEngineConfiguration setIdentityInfoEntityManager(IdentityInfoEntityManager identityInfoEntityManager) {
    this.identityInfoEntityManager = identityInfoEntityManager;
    return this;
  }
  
  public MembershipEntityManager getMembershipEntityManager() {
    return membershipEntityManager;
  }

  public IdmEngineConfiguration setMembershipEntityManager(MembershipEntityManager membershipEntityManager) {
    this.membershipEntityManager = membershipEntityManager;
    return this;
  }
  
  public PropertyEntityManager getPropertyEntityManager() {
    return propertyEntityManager;
  }

  public IdmEngineConfiguration setPropertyEntityManager(PropertyEntityManager propertyEntityManager) {
    this.propertyEntityManager = propertyEntityManager;
    return this;
  }

  public TokenEntityManager getTokenEntityManager() {
    return tokenEntityManager;
  }

  public IdmEngineConfiguration setTokenEntityManager(TokenEntityManager tokenEntityManager) {
    this.tokenEntityManager = tokenEntityManager;
    return this;
  }

  public UserEntityManager getUserEntityManager() {
    return userEntityManager;
  }

  public IdmEngineConfiguration setUserEntityManager(UserEntityManager userEntityManager) {
    this.userEntityManager = userEntityManager;
    return this;
  }
  
  public CapabilityEntityManager getCapabilityEntityManager() {
    return capabilityEntityManager;
  }

  public IdmEngineConfiguration setCapabilityEntityManager(CapabilityEntityManager capabilityEntityManager) {
    this.capabilityEntityManager = capabilityEntityManager;
    return this;
  }

  public TableDataManager getTableDataManager() {
    return tableDataManager;
  }

  public IdmEngineConfiguration setTableDataManager(TableDataManager tableDataManager) {
    this.tableDataManager = tableDataManager;
    return this;
  }

  public CommandContextFactory getCommandContextFactory() {
    return commandContextFactory;
  }

  public IdmEngineConfiguration setCommandContextFactory(CommandContextFactory commandContextFactory) {
    this.commandContextFactory = commandContextFactory;
    return this;
  }

  public IdmEngineConfiguration setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }

  public IdmEngineConfiguration setTransactionFactory(TransactionFactory transactionFactory) {
    this.transactionFactory = transactionFactory;
    return this;
  }

  public IdmEngineConfiguration setCustomMybatisMappers(Set<Class<?>> customMybatisMappers) {
    this.customMybatisMappers = customMybatisMappers;
    return this;
  }

  public IdmEngineConfiguration setCustomMybatisXMLMappers(Set<String> customMybatisXMLMappers) {
    this.customMybatisXMLMappers = customMybatisXMLMappers;
    return this;
  }

  public IdmEngineConfiguration setCustomSessionFactories(List<SessionFactory> customSessionFactories) {
    this.customSessionFactories = customSessionFactories;
    return this;
  }

  public DbSqlSessionFactory getDbSqlSessionFactory() {
    return dbSqlSessionFactory;
  }

  public IdmEngineConfiguration setDbSqlSessionFactory(DbSqlSessionFactory dbSqlSessionFactory) {
    this.dbSqlSessionFactory = dbSqlSessionFactory;
    return this;
  }

  public IdmEngineConfiguration setUsingRelationalDatabase(boolean usingRelationalDatabase) {
    this.usingRelationalDatabase = usingRelationalDatabase;
    return this;
  }

  public IdmEngineConfiguration setDatabaseTablePrefix(String databaseTablePrefix) {
    this.databaseTablePrefix = databaseTablePrefix;
    return this;
  }

  public IdmEngineConfiguration setDatabaseWildcardEscapeCharacter(String databaseWildcardEscapeCharacter) {
    this.databaseWildcardEscapeCharacter = databaseWildcardEscapeCharacter;
    return this;
  }

  public IdmEngineConfiguration setDatabaseCatalog(String databaseCatalog) {
    this.databaseCatalog = databaseCatalog;
    return this;
  }

  public IdmEngineConfiguration setDatabaseSchema(String databaseSchema) {
    this.databaseSchema = databaseSchema;
    return this;
  }

  public IdmEngineConfiguration setTablePrefixIsSchema(boolean tablePrefixIsSchema) {
    this.tablePrefixIsSchema = tablePrefixIsSchema;
    return this;
  }

  public IdmEngineConfiguration setSessionFactories(Map<Class<?>, SessionFactory> sessionFactories) {
    this.sessionFactories = sessionFactories;
    return this;
  }

  public TransactionContextFactory<TransactionListener, CommandContext> getTransactionContextFactory() {
    return transactionContextFactory;
  }

  public IdmEngineConfiguration setTransactionContextFactory(TransactionContextFactory<TransactionListener, CommandContext> transactionContextFactory) {
    this.transactionContextFactory = transactionContextFactory;
    return this;
  }
  
  public IdmEngineConfiguration setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
    this.databaseSchemaUpdate = databaseSchemaUpdate;
    return this;
  }

  public IdmEngineConfiguration setEnableEventDispatcher(boolean enableEventDispatcher) {
    this.enableEventDispatcher = enableEventDispatcher;
    return this;
  }

  public IdmEngineConfiguration setEventDispatcher(ActivitiEventDispatcher eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
    return this;
  }

  public IdmEngineConfiguration setEventListeners(List<ActivitiEventListener> eventListeners) {
    this.eventListeners = eventListeners;
    return this;
  }

  public IdmEngineConfiguration setTypedEventListeners(Map<String, List<ActivitiEventListener>> typedEventListeners) {
    this.typedEventListeners = typedEventListeners;
    return this;
  }

  public IdmEngineConfiguration setClock(Clock clock) {
    this.clock = clock;
    return this;
  }
}
