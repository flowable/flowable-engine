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
package org.flowable.idm.engine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.flowable.engine.common.AbstractEngineConfiguration;
import org.flowable.engine.common.api.delegate.event.FlowableEventDispatcher;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.common.impl.cfg.BeansConfigurationHelper;
import org.flowable.engine.common.impl.cfg.IdGenerator;
import org.flowable.engine.common.impl.cfg.standalone.StandaloneMybatisTransactionContextFactory;
import org.flowable.engine.common.impl.db.DbSqlSessionFactory;
import org.flowable.engine.common.impl.event.FlowableEventDispatcherImpl;
import org.flowable.engine.common.impl.interceptor.CommandConfig;
import org.flowable.engine.common.impl.interceptor.CommandContextFactory;
import org.flowable.engine.common.impl.interceptor.CommandContextInterceptor;
import org.flowable.engine.common.impl.interceptor.CommandInterceptor;
import org.flowable.engine.common.impl.interceptor.DefaultCommandInvoker;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.common.impl.interceptor.LogInterceptor;
import org.flowable.engine.common.impl.interceptor.SessionFactory;
import org.flowable.engine.common.impl.interceptor.TransactionContextInterceptor;
import org.flowable.engine.common.impl.persistence.GenericManagerFactory;
import org.flowable.engine.common.impl.persistence.cache.EntityCache;
import org.flowable.engine.common.impl.persistence.cache.EntityCacheImpl;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.flowable.engine.common.runtime.Clock;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.IdmManagementService;
import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;
import org.flowable.idm.api.event.FlowableIdmEventType;
import org.flowable.idm.engine.impl.IdmEngineImpl;
import org.flowable.idm.engine.impl.IdmIdentityServiceImpl;
import org.flowable.idm.engine.impl.IdmManagementServiceImpl;
import org.flowable.idm.engine.impl.ServiceImpl;
import org.flowable.idm.engine.impl.authentication.BlankSalt;
import org.flowable.idm.engine.impl.authentication.ClearTextPasswordEncoder;
import org.flowable.idm.engine.impl.cfg.StandaloneIdmEngineConfiguration;
import org.flowable.idm.engine.impl.cfg.StandaloneInMemIdmEngineConfiguration;
import org.flowable.idm.engine.impl.db.EntityDependencyOrder;
import org.flowable.idm.engine.impl.db.IdmDbSchemaManager;
import org.flowable.idm.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.ByteArrayEntityManagerImpl;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntityManagerImpl;
import org.flowable.idm.engine.impl.persistence.entity.IdentityInfoEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.IdentityInfoEntityManagerImpl;
import org.flowable.idm.engine.impl.persistence.entity.MembershipEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.MembershipEntityManagerImpl;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeEntityManagerImpl;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeMappingEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeMappingEntityManagerImpl;
import org.flowable.idm.engine.impl.persistence.entity.PropertyEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.PropertyEntityManagerImpl;
import org.flowable.idm.engine.impl.persistence.entity.TableDataManager;
import org.flowable.idm.engine.impl.persistence.entity.TableDataManagerImpl;
import org.flowable.idm.engine.impl.persistence.entity.TokenEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.TokenEntityManagerImpl;
import org.flowable.idm.engine.impl.persistence.entity.UserEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.UserEntityManagerImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.ByteArrayDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.GroupDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.IdentityInfoDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.MembershipDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.PrivilegeDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.PrivilegeMappingDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.PropertyDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.TokenDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.UserDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.impl.MybatisByteArrayDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.impl.MybatisGroupDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.impl.MybatisIdentityInfoDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.impl.MybatisMembershipDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.impl.MybatisPrivilegeDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.impl.MybatisPrivilegeMappingDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.impl.MybatisPropertyDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.impl.MybatisTokenDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.impl.MybatisUserDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdmEngineConfiguration extends AbstractEngineConfiguration {

    protected static final Logger LOGGER = LoggerFactory.getLogger(IdmEngineConfiguration.class);

    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/idm/db/mapping/mappings.xml";

    protected String idmEngineName = IdmEngines.NAME_DEFAULT;

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
    protected PrivilegeDataManager privilegeDataManager;
    protected PrivilegeMappingDataManager privilegeMappingDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////
    protected ByteArrayEntityManager byteArrayEntityManager;
    protected GroupEntityManager groupEntityManager;
    protected IdentityInfoEntityManager identityInfoEntityManager;
    protected MembershipEntityManager membershipEntityManager;
    protected PropertyEntityManager propertyEntityManager;
    protected TableDataManager tableDataManager;
    protected TokenEntityManager tokenEntityManager;
    protected UserEntityManager userEntityManager;
    protected PrivilegeEntityManager privilegeEntityManager;
    protected PrivilegeMappingEntityManager privilegeMappingEntityManager;

    protected PasswordEncoder passwordEncoder;
    protected PasswordSalt passwordSalt;

    public static IdmEngineConfiguration createIdmEngineConfigurationFromResourceDefault() {
        return createIdmEngineConfigurationFromResource("flowable.idm.cfg.xml", "idmEngineConfiguration");
    }

    public static IdmEngineConfiguration createIdmEngineConfigurationFromResource(String resource) {
        return createIdmEngineConfigurationFromResource(resource, "idmEngineConfiguration");
    }

    public static IdmEngineConfiguration createIdmEngineConfigurationFromResource(String resource, String beanName) {
        return (IdmEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromResource(resource, beanName);
    }

    public static IdmEngineConfiguration createIdmEngineConfigurationFromInputStream(InputStream inputStream) {
        return createIdmEngineConfigurationFromInputStream(inputStream, "idmEngineConfiguration");
    }

    public static IdmEngineConfiguration createIdmEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
        return (IdmEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromInputStream(inputStream, beanName);
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
            initDbSchemaManager();
        }

        initBeans();
        initTransactionFactory();
        initSqlSessionFactory();
        initSessionFactories();
        initPasswordEncoder();
        initServices();
        initDataManagers();
        initEntityManagers();
        initClock();
        initEventDispatcher();
    }
    
    public void initDbSchemaManager() {
        if (this.dbSchemaManager == null) {
            this.dbSchemaManager = new IdmDbSchemaManager();
        }
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
        if (privilegeDataManager == null) {
            privilegeDataManager = new MybatisPrivilegeDataManager(this);
        }
        if (privilegeMappingDataManager == null) {
            privilegeMappingDataManager = new MybatisPrivilegeMappingDataManager(getIdmEngineConfiguration());
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
        if (privilegeEntityManager == null) {
            privilegeEntityManager = new PrivilegeEntityManagerImpl(this, privilegeDataManager);
        }
        if (privilegeMappingEntityManager == null) {
            privilegeMappingEntityManager = new PrivilegeMappingEntityManagerImpl(this, privilegeMappingDataManager);
        }
    }

    // session factories ////////////////////////////////////////////////////////

    public void initSessionFactories() {
        if (sessionFactories == null) {
            sessionFactories = new HashMap<>();

            if (usingRelationalDatabase) {
                initDbSqlSessionFactory();
            }
            
            addSessionFactory(new GenericManagerFactory(EntityCache.class, EntityCacheImpl.class));
            
            commandContextFactory.setSessionFactories(sessionFactories);
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
            dbSqlSessionFactory.setDatabaseType(databaseType);
            dbSqlSessionFactory.setSqlSessionFactory(sqlSessionFactory);
            dbSqlSessionFactory.setIdGenerator(idGenerator);
            dbSqlSessionFactory.setDatabaseTablePrefix(databaseTablePrefix);
            dbSqlSessionFactory.setTablePrefixIsSchema(tablePrefixIsSchema);
            dbSqlSessionFactory.setDatabaseCatalog(databaseCatalog);
            dbSqlSessionFactory.setDatabaseSchema(databaseSchema);
            addSessionFactory(dbSqlSessionFactory);
        }
        initDbSqlSessionFactoryEntitySettings();
    }
    
    public DbSqlSessionFactory createDbSqlSessionFactory() {
        return new DbSqlSessionFactory();
    }
    
    protected void initDbSqlSessionFactoryEntitySettings() {
        for (Class<? extends Entity> clazz : EntityDependencyOrder.INSERT_ORDER) {
            dbSqlSessionFactory.getInsertionOrder().add(clazz);
        }
        
        for (Class<? extends Entity> clazz : EntityDependencyOrder.DELETE_ORDER) {
            dbSqlSessionFactory.getDeletionOrder().add(clazz);
        }
    }

    public void initPasswordEncoder() {
        if (passwordEncoder == null) {
            passwordEncoder = ClearTextPasswordEncoder.getInstance();
        }
        
        if (passwordSalt == null) {
            passwordSalt = BlankSalt.getInstance();
        }
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
            commandInvoker = new DefaultCommandInvoker();
        }
    }

    public void initCommandInterceptors() {
        if (commandInterceptors == null) {
            commandInterceptors = new ArrayList<>();
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
        if (defaultCommandInterceptors == null) {
            List<CommandInterceptor> interceptors = new ArrayList<>();
            interceptors.add(new LogInterceptor());
            
            CommandInterceptor transactionInterceptor = createTransactionInterceptor();
            if (transactionInterceptor != null) {
                interceptors.add(transactionInterceptor);
            }
    
            if (commandContextFactory != null) {
                CommandContextInterceptor commandContextInterceptor = new CommandContextInterceptor(commandContextFactory);
                engineConfigurations.put(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG, this);
                commandContextInterceptor.setEngineConfigurations(engineConfigurations);
                commandContextInterceptor.setCurrentEngineConfigurationKey(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
                interceptors.add(commandContextInterceptor);
            }
            
            if (transactionContextFactory != null) {
                interceptors.add(new TransactionContextInterceptor(transactionContextFactory));
            }
            defaultCommandInterceptors = interceptors;
        } 
        return defaultCommandInterceptors;
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
            this.eventDispatcher = new FlowableEventDispatcherImpl();
        }

        this.eventDispatcher.setEnabled(enableEventDispatcher);

        if (eventListeners != null) {
            for (FlowableEventListener listenerToAdd : eventListeners) {
                this.eventDispatcher.addEventListener(listenerToAdd);
            }
        }

        if (typedEventListeners != null) {
            for (Entry<String, List<FlowableEventListener>> listenersToAdd : typedEventListeners.entrySet()) {
                // Extract types from the given string
                FlowableIdmEventType[] types = FlowableIdmEventType.getTypesFromString(listenersToAdd.getKey());

                for (FlowableEventListener listenerToAdd : listenersToAdd.getValue()) {
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

    public IdmEngineConfiguration setBeans(Map<Object, Object> beans) {
        this.beans = beans;
        return this;
    }

    public IdmEngineConfiguration setDefaultCommandConfig(CommandConfig defaultCommandConfig) {
        this.defaultCommandConfig = defaultCommandConfig;
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

    public PrivilegeDataManager getPrivilegeDataManager() {
        return privilegeDataManager;
    }

    public IdmEngineConfiguration setPrivilegeDataManager(PrivilegeDataManager privilegeDataManager) {
        this.privilegeDataManager = privilegeDataManager;
        return this;
    }

    public PrivilegeMappingDataManager getPrivilegeMappingDataManager() {
        return privilegeMappingDataManager;
    }

    public IdmEngineConfiguration setPrivilegeMappingDataManager(PrivilegeMappingDataManager privilegeMappingDataManager) {
        this.privilegeMappingDataManager = privilegeMappingDataManager;
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

    public PrivilegeEntityManager getPrivilegeEntityManager() {
        return privilegeEntityManager;
    }

    public IdmEngineConfiguration setPrivilegeEntityManager(PrivilegeEntityManager privilegeEntityManager) {
        this.privilegeEntityManager = privilegeEntityManager;
        return this;
    }

    public PrivilegeMappingEntityManager getPrivilegeMappingEntityManager() {
        return privilegeMappingEntityManager;
    }

    public IdmEngineConfiguration setPrivilegeMappingEntityManager(PrivilegeMappingEntityManager privilegeMappingEntityManager) {
        this.privilegeMappingEntityManager = privilegeMappingEntityManager;
        return this;
    }

    public TableDataManager getTableDataManager() {
        return tableDataManager;
    }

    public IdmEngineConfiguration setTableDataManager(TableDataManager tableDataManager) {
        this.tableDataManager = tableDataManager;
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

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public IdmEngineConfiguration setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        return this;
    }

    public PasswordSalt getPasswordSalt() {
        return passwordSalt;
    }

    public IdmEngineConfiguration setPasswordSalt(PasswordSalt passwordSalt) {
        this.passwordSalt = passwordSalt;
        return this;
    }

    public IdmEngineConfiguration setSessionFactories(Map<Class<?>, SessionFactory> sessionFactories) {
        this.sessionFactories = sessionFactories;
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

    public IdmEngineConfiguration setEventDispatcher(FlowableEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
        return this;
    }

    public IdmEngineConfiguration setEventListeners(List<FlowableEventListener> eventListeners) {
        this.eventListeners = eventListeners;
        return this;
    }

    public IdmEngineConfiguration setTypedEventListeners(Map<String, List<FlowableEventListener>> typedEventListeners) {
        this.typedEventListeners = typedEventListeners;
        return this;
    }

    public IdmEngineConfiguration setClock(Clock clock) {
        this.clock = clock;
        return this;
    }
}
