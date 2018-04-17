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
package org.flowable.app.engine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import javax.sql.DataSource;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.flowable.app.api.AppEngineConfigurationApi;
import org.flowable.app.api.AppManagementService;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppResourceConverter;
import org.flowable.app.engine.impl.AppEngineImpl;
import org.flowable.app.engine.impl.AppManagementServiceImpl;
import org.flowable.app.engine.impl.AppRepositoryServiceImpl;
import org.flowable.app.engine.impl.cfg.IdmEngineConfigurator;
import org.flowable.app.engine.impl.cfg.StandaloneInMemAppEngineConfiguration;
import org.flowable.app.engine.impl.db.AppDbSchemaManager;
import org.flowable.app.engine.impl.db.EntityDependencyOrder;
import org.flowable.app.engine.impl.deployer.AppDeployer;
import org.flowable.app.engine.impl.deployer.AppDeploymentManager;
import org.flowable.app.engine.impl.deployer.AppResourceConverterImpl;
import org.flowable.app.engine.impl.el.AppExpressionManager;
import org.flowable.app.engine.impl.interceptor.AppCommandInvoker;
import org.flowable.app.engine.impl.persistence.entity.AppDefinitionEntityManager;
import org.flowable.app.engine.impl.persistence.entity.AppDefinitionEntityManagerImpl;
import org.flowable.app.engine.impl.persistence.entity.AppDeploymentEntityManager;
import org.flowable.app.engine.impl.persistence.entity.AppDeploymentEntityManagerImpl;
import org.flowable.app.engine.impl.persistence.entity.AppResourceEntityManager;
import org.flowable.app.engine.impl.persistence.entity.AppResourceEntityManagerImpl;
import org.flowable.app.engine.impl.persistence.entity.data.AppDefinitionDataManager;
import org.flowable.app.engine.impl.persistence.entity.data.AppDeploymentDataManager;
import org.flowable.app.engine.impl.persistence.entity.data.AppResourceDataManager;
import org.flowable.app.engine.impl.persistence.entity.data.TableDataManager;
import org.flowable.app.engine.impl.persistence.entity.data.impl.MybatisAppDefinitionDataManager;
import org.flowable.app.engine.impl.persistence.entity.data.impl.MybatisAppDeploymentDataManager;
import org.flowable.app.engine.impl.persistence.entity.data.impl.MybatisResourceDataManager;
import org.flowable.app.engine.impl.persistence.entity.data.impl.TableDataManagerImpl;
import org.flowable.app.engine.impl.persistence.entity.deploy.AppDefinitionCacheEntry;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.EngineConfigurator;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.calendar.BusinessCalendarManager;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.calendar.DueDateBusinessCalendar;
import org.flowable.common.engine.impl.calendar.DurationBusinessCalendar;
import org.flowable.common.engine.impl.calendar.MapBusinessCalendarManager;
import org.flowable.common.engine.impl.cfg.BeansConfigurationHelper;
import org.flowable.common.engine.impl.db.DbSchemaManager;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.db.IdentityLinkDbSchemaManager;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.db.IbatisVariableTypeHandler;
import org.flowable.variable.service.impl.db.VariableDbSchemaManager;
import org.flowable.variable.service.impl.types.BooleanType;
import org.flowable.variable.service.impl.types.ByteArrayType;
import org.flowable.variable.service.impl.types.DateType;
import org.flowable.variable.service.impl.types.DefaultVariableTypes;
import org.flowable.variable.service.impl.types.DoubleType;
import org.flowable.variable.service.impl.types.IntegerType;
import org.flowable.variable.service.impl.types.JodaDateTimeType;
import org.flowable.variable.service.impl.types.JodaDateType;
import org.flowable.variable.service.impl.types.JsonType;
import org.flowable.variable.service.impl.types.LongJsonType;
import org.flowable.variable.service.impl.types.LongStringType;
import org.flowable.variable.service.impl.types.LongType;
import org.flowable.variable.service.impl.types.NullType;
import org.flowable.variable.service.impl.types.SerializableType;
import org.flowable.variable.service.impl.types.ShortType;
import org.flowable.variable.service.impl.types.StringType;
import org.flowable.variable.service.impl.types.UUIDType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AppEngineConfiguration extends AbstractEngineConfiguration implements AppEngineConfigurationApi {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AppEngineConfiguration.class);
    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/app/db/mapping/mappings.xml";
    public static final String LIQUIBASE_CHANGELOG_PREFIX = "ACT_APP_";

    protected String cmmnEngineName = AppEngines.NAME_DEFAULT;

    protected AppManagementService appManagementService = new AppManagementServiceImpl(this);
    protected AppRepositoryService appRepositoryService = new AppRepositoryServiceImpl(this);
    
    protected TableDataManager tableDataManager;
    protected AppDeploymentDataManager deploymentDataManager;
    protected AppResourceDataManager resourceDataManager;
    protected AppDefinitionDataManager appDefinitionDataManager;

    protected AppDeploymentEntityManager appDeploymentEntityManager;
    protected AppResourceEntityManager appResourceEntityManager;
    protected AppDefinitionEntityManager appDefinitionEntityManager;

    protected boolean disableIdmEngine;

    protected boolean executeServiceDbSchemaManagers = true;
    
    protected AppDeployer appDeployer;
    protected AppDeploymentManager deploymentManager;
    protected AppResourceConverter appResourceConverter;
    
    protected int appDefinitionCacheLimit = -1;
    protected DeploymentCache<AppDefinitionCacheEntry> appDefinitionCache;

    protected ExpressionManager expressionManager;
    protected DbSchemaManager identityLinkDbSchemaManager;
    protected DbSchemaManager variableDbSchemaManager;

    // CONFIGURATORS ////////////////////////////////////////////////////////////

    protected boolean enableConfiguratorServiceLoader = true; // Enabled by default. In certain environments this should be set to false (eg osgi)
    protected List<EngineConfigurator> configurators; // The injected configurators
    protected List<EngineConfigurator> allConfigurators; // Including auto-discovered configurators
    protected EngineConfigurator idmEngineConfigurator;

    // Identitylink support
    protected IdentityLinkServiceConfiguration identityLinkServiceConfiguration;

    // Variable support
    protected VariableTypes variableTypes;
    protected List<VariableType> customPreVariableTypes;
    protected List<VariableType> customPostVariableTypes;
    protected VariableServiceConfiguration variableServiceConfiguration;
    protected boolean serializableVariableTypeTrackDeserializedObjects = true;
    protected ObjectMapper objectMapper = new ObjectMapper();

    protected BusinessCalendarManager businessCalendarManager;

    public static AppEngineConfiguration createAppEngineConfigurationFromResourceDefault() {
        return createAppEngineConfigurationFromResource("flowable.app.cfg.xml", "appEngineConfiguration");
    }

    public static AppEngineConfiguration createAppEngineConfigurationFromResource(String resource) {
        return createAppEngineConfigurationFromResource(resource, "appEngineConfiguration");
    }

    public static AppEngineConfiguration createAppEngineConfigurationFromResource(String resource, String beanName) {
        return (AppEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromResource(resource, beanName);
    }

    public static AppEngineConfiguration createAppEngineConfigurationFromInputStream(InputStream inputStream) {
        return createAppEngineConfigurationFromInputStream(inputStream, "appEngineConfiguration");
    }

    public static AppEngineConfiguration createAppEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
        return (AppEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromInputStream(inputStream, beanName);
    }

    public static AppEngineConfiguration createStandaloneAppEngineConfiguration() {
        return new AppEngineConfiguration();
    }

    public static AppEngineConfiguration createStandaloneInMemAppEngineConfiguration() {
        return new StandaloneInMemAppEngineConfiguration();
    }

    public AppEngine buildAppEngine() {
        init();
        return new AppEngineImpl(this);
    }

    protected void init() {
        initConfigurators();
        configuratorsBeforeInit();
        initCommandContextFactory();
        initTransactionContextFactory();
        initCommandExecutors();
        initIdGenerator();
        initExpressionManager();
        
        if (usingRelationalDatabase) {
            initDataSource();
            initDbSchemaManager();
        }

        initVariableTypes();
        initBeans();
        initTransactionFactory();

        if (usingRelationalDatabase) {
            initSqlSessionFactory();
        }

        initSessionFactories();
        initServices();
        initDataManagers();
        initEntityManagers();
        initDeployers();
        initAppDefinitionCache();
        initAppResourceConverter();
        initDeploymentManager();
        initClock();
        initIdentityLinkServiceConfiguration();
        initVariableServiceConfiguration();
        configuratorsAfterInit();
        initBusinessCalendarManager();
    }

    @Override
    public void initDbSchemaManager() {
        super.initDbSchemaManager();
        initAppDbSchemaManager();

        if (executeServiceDbSchemaManagers) {
            initIdentityLinkDbSchemaManager();
            initVariableDbSchemaManager();
        }
    }

    protected void initAppDbSchemaManager() {
        if (this.dbSchemaManager == null) {
            this.dbSchemaManager = new AppDbSchemaManager();
        }
    }

    protected void initVariableDbSchemaManager() {
        if (this.variableDbSchemaManager == null) {
            this.variableDbSchemaManager = new VariableDbSchemaManager();
        }
    }

    protected void initIdentityLinkDbSchemaManager() {
        if (this.identityLinkDbSchemaManager == null) {
            this.identityLinkDbSchemaManager = new IdentityLinkDbSchemaManager();
        }
    }

    @Override
    public void initMybatisTypeHandlers(Configuration configuration) {
        configuration.getTypeHandlerRegistry().register(VariableType.class, JdbcType.VARCHAR, new IbatisVariableTypeHandler(variableTypes));
    }

    public void initExpressionManager() {
        if (expressionManager == null) {
            expressionManager = new AppExpressionManager(beans);
        }
    }

    @Override
    public void initCommandInvoker() {
        if (commandInvoker == null) {
            commandInvoker = new AppCommandInvoker();
        }
    }

    protected void initServices() {
        initService(appManagementService);
        initService(appRepositoryService);
    }

    public void initDataManagers() {
        if (tableDataManager == null) {
            tableDataManager = new TableDataManagerImpl();
        }
        if (deploymentDataManager == null) {
            deploymentDataManager = new MybatisAppDeploymentDataManager(this);
        }
        if (resourceDataManager == null) {
            resourceDataManager = new MybatisResourceDataManager(this);
        }
        if (appDefinitionDataManager == null) {
            appDefinitionDataManager = new MybatisAppDefinitionDataManager(this);
        }
    }

    public void initEntityManagers() {
        if (appDeploymentEntityManager == null) {
            appDeploymentEntityManager = new AppDeploymentEntityManagerImpl(this, deploymentDataManager);
        }
        if (appResourceEntityManager == null) {
            appResourceEntityManager = new AppResourceEntityManagerImpl(this, resourceDataManager);
        }
        if (appDefinitionEntityManager == null) {
            appDefinitionEntityManager = new AppDefinitionEntityManagerImpl(this, appDefinitionDataManager);
        }
    }

    protected void initDeployers() {
        if (this.appDeployer == null) {
            this.deployers = new ArrayList<>();
            if (customPreDeployers != null) {
                this.deployers.addAll(customPreDeployers);
            }
            this.deployers.addAll(getDefaultDeployers());
            if (customPostDeployers != null) {
                this.deployers.addAll(customPostDeployers);
            }
        }
    }

    public Collection<? extends EngineDeployer> getDefaultDeployers() {
        List<EngineDeployer> defaultDeployers = new ArrayList<>();

        if (appDeployer == null) {
            appDeployer = new AppDeployer();
        }

        defaultDeployers.add(appDeployer);
        return defaultDeployers;
    }

    protected void initAppDefinitionCache() {
        if (appDefinitionCache == null) {
            if (appDefinitionCacheLimit <= 0) {
                appDefinitionCache = new DefaultDeploymentCache<>();
            } else {
                appDefinitionCache = new DefaultDeploymentCache<>(appDefinitionCacheLimit);
            }
        }
    }
    
    protected void initAppResourceConverter() {
        if (appResourceConverter == null) {
            appResourceConverter = new AppResourceConverterImpl(objectMapper);
        }
    }

    protected void initDeploymentManager() {
        if (deploymentManager == null) {
            deploymentManager = new AppDeploymentManager();
            deploymentManager.setAppEngineConfiguration(this);
            deploymentManager.setAppDefinitionCache(appDefinitionCache);
            deploymentManager.setDeployers(deployers);
            deploymentManager.setAppDefinitionEntityManager(appDefinitionEntityManager);
            deploymentManager.setDeploymentEntityManager(appDeploymentEntityManager);
        }
    }

    @Override
    public String getEngineCfgKey() {
        return EngineConfigurationConstants.KEY_APP_ENGINE_CONFIG;
    }

    @Override
    public CommandInterceptor createTransactionInterceptor() {
        return null;
    }

    @Override
    public InputStream getMyBatisXmlConfigurationStream() {
        return getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
    }

    @Override
    protected void initDbSqlSessionFactoryEntitySettings() {
        defaultInitDbSqlSessionFactoryEntitySettings(EntityDependencyOrder.INSERT_ORDER, EntityDependencyOrder.DELETE_ORDER);
    }

    public void initConfigurators() {

        allConfigurators = new ArrayList<>();

        if (!disableIdmEngine) {
            if (idmEngineConfigurator != null) {
                allConfigurators.add(idmEngineConfigurator);
            } else {
                allConfigurators.add(new IdmEngineConfigurator());
            }
        }

        // Configurators that are explicitly added to the config
        if (configurators != null) {
            allConfigurators.addAll(configurators);
        }

        // Auto discovery through ServiceLoader
        if (enableConfiguratorServiceLoader) {
            ClassLoader classLoader = getClassLoader();
            if (classLoader == null) {
                classLoader = ReflectUtil.getClassLoader();
            }

            ServiceLoader<EngineConfigurator> configuratorServiceLoader = ServiceLoader.load(EngineConfigurator.class, classLoader);
            int nrOfServiceLoadedConfigurators = 0;
            for (EngineConfigurator configurator : configuratorServiceLoader) {
                allConfigurators.add(configurator);
                nrOfServiceLoadedConfigurators++;
            }

            if (nrOfServiceLoadedConfigurators > 0) {
                LOGGER.info("Found {} auto-discoverable Process Engine Configurator{}", nrOfServiceLoadedConfigurators++, nrOfServiceLoadedConfigurators > 1 ? "s" : "");
            }

            if (!allConfigurators.isEmpty()) {

                // Order them according to the priorities (useful for dependent
                // configurator)
                Collections.sort(allConfigurators, new Comparator<EngineConfigurator>() {
                    @Override
                    public int compare(EngineConfigurator configurator1, EngineConfigurator configurator2) {
                        int priority1 = configurator1.getPriority();
                        int priority2 = configurator2.getPriority();

                        if (priority1 < priority2) {
                            return -1;
                        } else if (priority1 > priority2) {
                            return 1;
                        }
                        return 0;
                    }
                });

                // Execute the configurators
                LOGGER.info("Found {} Engine Configurators in total:", allConfigurators.size());
                for (EngineConfigurator configurator : allConfigurators) {
                    LOGGER.info("{} (priority:{})", configurator.getClass(), configurator.getPriority());
                }

            }

        }
    }

    public void configuratorsBeforeInit() {
        for (EngineConfigurator configurator : allConfigurators) {
            LOGGER.info("Executing beforeInit() of {} (priority:{})", configurator.getClass(), configurator.getPriority());
            configurator.beforeInit(this);
        }
    }

    public void configuratorsAfterInit() {
        for (EngineConfigurator configurator : allConfigurators) {
            LOGGER.info("Executing configure() of {} (priority:{})", configurator.getClass(), configurator.getPriority());
            configurator.configure(this);
        }
    }

    public void initVariableTypes() {
        if (variableTypes == null) {
            variableTypes = new DefaultVariableTypes();
            if (customPreVariableTypes != null) {
                for (VariableType customVariableType : customPreVariableTypes) {
                    variableTypes.addType(customVariableType);
                }
            }
            variableTypes.addType(new NullType());
            variableTypes.addType(new StringType(getMaxLengthString()));
            variableTypes.addType(new LongStringType(getMaxLengthString() + 1));
            variableTypes.addType(new BooleanType());
            variableTypes.addType(new ShortType());
            variableTypes.addType(new IntegerType());
            variableTypes.addType(new LongType());
            variableTypes.addType(new DateType());
            variableTypes.addType(new JodaDateType());
            variableTypes.addType(new JodaDateTimeType());
            variableTypes.addType(new DoubleType());
            variableTypes.addType(new UUIDType());
            variableTypes.addType(new JsonType(getMaxLengthString(), objectMapper));
            variableTypes.addType(new LongJsonType(getMaxLengthString() + 1, objectMapper));
            variableTypes.addType(new ByteArrayType());
            variableTypes.addType(new SerializableType(serializableVariableTypeTrackDeserializedObjects));
            if (customPostVariableTypes != null) {
                for (VariableType customVariableType : customPostVariableTypes) {
                    variableTypes.addType(customVariableType);
                }
            }
        }
    }

    public void initVariableServiceConfiguration() {
        this.variableServiceConfiguration = new VariableServiceConfiguration();

        this.variableServiceConfiguration.setClock(this.clock);
        this.variableServiceConfiguration.setObjectMapper(this.objectMapper);
        this.variableServiceConfiguration.setEventDispatcher(this.eventDispatcher);

        this.variableServiceConfiguration.setVariableTypes(this.variableTypes);

        this.variableServiceConfiguration.setMaxLengthString(this.getMaxLengthString());
        this.variableServiceConfiguration.setSerializableVariableTypeTrackDeserializedObjects(this.isSerializableVariableTypeTrackDeserializedObjects());

        this.variableServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG, this.variableServiceConfiguration);
    }

    public void initIdentityLinkServiceConfiguration() {
        this.identityLinkServiceConfiguration = new IdentityLinkServiceConfiguration();
        this.identityLinkServiceConfiguration.setClock(this.clock);
        this.identityLinkServiceConfiguration.setObjectMapper(this.objectMapper);
        this.identityLinkServiceConfiguration.setEventDispatcher(this.eventDispatcher);

        this.identityLinkServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_IDENTITY_LINK_SERVICE_CONFIG, this.identityLinkServiceConfiguration);
    }

    public void initBusinessCalendarManager() {
        if (businessCalendarManager == null) {
            MapBusinessCalendarManager mapBusinessCalendarManager = new MapBusinessCalendarManager();
            mapBusinessCalendarManager.addBusinessCalendar(DurationBusinessCalendar.NAME, new DurationBusinessCalendar(this.clock));
            mapBusinessCalendarManager.addBusinessCalendar(DueDateBusinessCalendar.NAME, new DueDateBusinessCalendar(this.clock));
            mapBusinessCalendarManager.addBusinessCalendar(CycleBusinessCalendar.NAME, new CycleBusinessCalendar(this.clock));

            businessCalendarManager = mapBusinessCalendarManager;
        }
    }

    @Override
    public String getEngineName() {
        return cmmnEngineName;
    }

    public String getCmmnEngineName() {
        return cmmnEngineName;
    }

    public AppEngineConfiguration setCmmnEngineName(String cmmnEngineName) {
        this.cmmnEngineName = cmmnEngineName;
        return this;
    }

    @Override
    public AppManagementService getAppManagementService() {
        return appManagementService;
    }

    public AppEngineConfiguration setAppManagementService(AppManagementService appManagementService) {
        this.appManagementService = appManagementService;
        return this;
    }

    @Override
    public AppRepositoryService getAppRepositoryService() {
        return appRepositoryService;
    }

    public AppEngineConfiguration setAppRepositoryService(AppRepositoryService appRepositoryService) {
        this.appRepositoryService = appRepositoryService;
        return this;
    }

    public IdmIdentityService getIdmIdentityService() {
        return ((IdmEngineConfiguration) engineConfigurations.get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG)).getIdmIdentityService();
    }

    public TableDataManager getTableDataManager() {
        return tableDataManager;
    }

    public AppEngineConfiguration setTableDataManager(TableDataManager tableDataManager) {
        this.tableDataManager = tableDataManager;
        return this;
    }

    public AppDeploymentDataManager getDeploymentDataManager() {
        return deploymentDataManager;
    }

    public AppEngineConfiguration setDeploymentDataManager(AppDeploymentDataManager deploymentDataManager) {
        this.deploymentDataManager = deploymentDataManager;
        return this;
    }

    public AppResourceDataManager getResourceDataManager() {
        return resourceDataManager;
    }

    public AppEngineConfiguration setResourceDataManager(AppResourceDataManager resourceDataManager) {
        this.resourceDataManager = resourceDataManager;
        return this;
    }

    public AppDefinitionDataManager getAppDefinitionDataManager() {
        return appDefinitionDataManager;
    }

    public AppEngineConfiguration setAppDefinitionDataManager(AppDefinitionDataManager appDefinitionDataManager) {
        this.appDefinitionDataManager = appDefinitionDataManager;
        return this;
    }

    public AppDeploymentEntityManager getAppDeploymentEntityManager() {
        return appDeploymentEntityManager;
    }

    public AppEngineConfiguration setAppDeploymentEntityManager(AppDeploymentEntityManager appDeploymentEntityManager) {
        this.appDeploymentEntityManager = appDeploymentEntityManager;
        return this;
    }

    public AppResourceEntityManager getAppResourceEntityManager() {
        return appResourceEntityManager;
    }

    public AppEngineConfiguration setAppResourceEntityManager(AppResourceEntityManager appResourceEntityManager) {
        this.appResourceEntityManager = appResourceEntityManager;
        return this;
    }

    public AppDefinitionEntityManager getAppDefinitionEntityManager() {
        return appDefinitionEntityManager;
    }

    public AppEngineConfiguration setAppDefinitionEntityManager(AppDefinitionEntityManager appDefinitionEntityManager) {
        this.appDefinitionEntityManager = appDefinitionEntityManager;
        return this;
    }

    public AppDeployer getAppDeployer() {
        return appDeployer;
    }

    public AppEngineConfiguration setAppDeployer(AppDeployer appDeployer) {
        this.appDeployer = appDeployer;
        return this;
    }

    public AppResourceConverter getAppResourceConverter() {
        return appResourceConverter;
    }

    public AppEngineConfiguration setAppResourceConverter(AppResourceConverter appResourceConverter) {
        this.appResourceConverter = appResourceConverter;
        return this;
    }

    public AppDeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    public AppEngineConfiguration setDeploymentManager(AppDeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
        return this;
    }

    public int getAppDefinitionCacheLimit() {
        return appDefinitionCacheLimit;
    }

    public AppEngineConfiguration setAppDefinitionCacheLimit(int appDefinitionCacheLimit) {
        this.appDefinitionCacheLimit = appDefinitionCacheLimit;
        return this;
    }

    public DeploymentCache<AppDefinitionCacheEntry> getAppDefinitionCache() {
        return appDefinitionCache;
    }

    public AppEngineConfiguration setAppDefinitionCache(DeploymentCache<AppDefinitionCacheEntry> appDefinitionCache) {
        this.appDefinitionCache = appDefinitionCache;
        return this;
    }

    @Override
    public AppEngineConfiguration setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public boolean isExecuteServiceDbSchemaManagers() {
        return executeServiceDbSchemaManagers;
    }

    public void setExecuteServiceDbSchemaManagers(boolean executeServiceDbSchemaManagers) {
        this.executeServiceDbSchemaManagers = executeServiceDbSchemaManagers;
    }

    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    public AppEngineConfiguration setExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
        return this;
    }

    public DbSchemaManager getIdentityLinkDbSchemaManager() {
        return identityLinkDbSchemaManager;
    }

    public AppEngineConfiguration setIdentityLinkDbSchemaManager(DbSchemaManager identityLinkDbSchemaManager) {
        this.identityLinkDbSchemaManager = identityLinkDbSchemaManager;
        return this;
    }

    public DbSchemaManager getVariableDbSchemaManager() {
        return variableDbSchemaManager;
    }

    public AppEngineConfiguration setVariableDbSchemaManager(DbSchemaManager variableDbSchemaManager) {
        this.variableDbSchemaManager = variableDbSchemaManager;
        return this;
    }

    public VariableTypes getVariableTypes() {
        return variableTypes;
    }

    public AppEngineConfiguration setVariableTypes(VariableTypes variableTypes) {
        this.variableTypes = variableTypes;
        return this;
    }

    public List<VariableType> getCustomPreVariableTypes() {
        return customPreVariableTypes;
    }

    public AppEngineConfiguration setCustomPreVariableTypes(List<VariableType> customPreVariableTypes) {
        this.customPreVariableTypes = customPreVariableTypes;
        return this;
    }

    public List<VariableType> getCustomPostVariableTypes() {
        return customPostVariableTypes;
    }

    public AppEngineConfiguration setCustomPostVariableTypes(List<VariableType> customPostVariableTypes) {
        this.customPostVariableTypes = customPostVariableTypes;
        return this;
    }

    public IdentityLinkServiceConfiguration getIdentityLinkServiceConfiguration() {
        return identityLinkServiceConfiguration;
    }

    public AppEngineConfiguration setIdentityLinkServiceConfiguration(IdentityLinkServiceConfiguration identityLinkServiceConfiguration) {
        this.identityLinkServiceConfiguration = identityLinkServiceConfiguration;
        return this;
    }

    public VariableServiceConfiguration getVariableServiceConfiguration() {
        return variableServiceConfiguration;
    }

    public AppEngineConfiguration setVariableServiceConfiguration(VariableServiceConfiguration variableServiceConfiguration) {
        this.variableServiceConfiguration = variableServiceConfiguration;
        return this;
    }

    public boolean isSerializableVariableTypeTrackDeserializedObjects() {
        return serializableVariableTypeTrackDeserializedObjects;
    }

    public AppEngineConfiguration setSerializableVariableTypeTrackDeserializedObjects(boolean serializableVariableTypeTrackDeserializedObjects) {
        this.serializableVariableTypeTrackDeserializedObjects = serializableVariableTypeTrackDeserializedObjects;
        return this;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public AppEngineConfiguration setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public boolean isDisableIdmEngine() {
        return disableIdmEngine;
    }

    public AppEngineConfiguration setDisableIdmEngine(boolean disableIdmEngine) {
        this.disableIdmEngine = disableIdmEngine;
        return this;
    }

    public boolean isEnableConfiguratorServiceLoader() {
        return enableConfiguratorServiceLoader;
    }

    public AppEngineConfiguration setEnableConfiguratorServiceLoader(boolean enableConfiguratorServiceLoader) {
        this.enableConfiguratorServiceLoader = enableConfiguratorServiceLoader;
        return this;
    }

    public List<EngineConfigurator> getConfigurators() {
        return configurators;
    }

    public AppEngineConfiguration addConfigurator(EngineConfigurator configurator) {
        if (configurators == null) {
            configurators = new ArrayList<>();
        }
        configurators.add(configurator);
        return this;
    }

    public AppEngineConfiguration setConfigurators(List<EngineConfigurator> configurators) {
        this.configurators = configurators;
        return this;
    }

    public EngineConfigurator getIdmEngineConfigurator() {
        return idmEngineConfigurator;
    }

    public AppEngineConfiguration setIdmEngineConfigurator(EngineConfigurator idmEngineConfigurator) {
        this.idmEngineConfigurator = idmEngineConfigurator;
        return this;
    }

    public BusinessCalendarManager getBusinessCalendarManager() {
        return businessCalendarManager;
    }

    public AppEngineConfiguration setBusinessCalendarManager(BusinessCalendarManager businessCalendarManager) {
        this.businessCalendarManager = businessCalendarManager;
        return this;
    }
}
