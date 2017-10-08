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
package org.flowable.cmmn.engine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.flowable.cmmn.engine.impl.CmmnEngineImpl;
import org.flowable.cmmn.engine.impl.CmmnHistoryServiceImpl;
import org.flowable.cmmn.engine.impl.CmmnManagementServiceImpl;
import org.flowable.cmmn.engine.impl.CmmnRepositoryServiceImpl;
import org.flowable.cmmn.engine.impl.CmmnTaskServiceImpl;
import org.flowable.cmmn.engine.impl.ServiceImpl;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgendaFactory;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgendaSessionFactory;
import org.flowable.cmmn.engine.impl.agenda.DefaultCmmnEngineAgendaFactory;
import org.flowable.cmmn.engine.impl.callback.ChildCaseInstanceStateChangeCallback;
import org.flowable.cmmn.engine.impl.cfg.DelegateExpressionFieldInjectionMode;
import org.flowable.cmmn.engine.impl.cfg.StandaloneInMemCmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.db.CmmnDbSchemaManager;
import org.flowable.cmmn.engine.impl.db.EntityDependencyOrder;
import org.flowable.cmmn.engine.impl.delegate.CmmnClassDelegateFactory;
import org.flowable.cmmn.engine.impl.delegate.DefaultCmmnClassDelegateFactory;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeployer;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.deployer.Deployer;
import org.flowable.cmmn.engine.impl.el.CmmnExpressionManager;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryTaskManager;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryVariableManager;
import org.flowable.cmmn.engine.impl.history.DefaultCmmnHistoryManager;
import org.flowable.cmmn.engine.impl.interceptor.CmmnCommandInvoker;
import org.flowable.cmmn.engine.impl.parser.CmmnActivityBehaviorFactory;
import org.flowable.cmmn.engine.impl.parser.CmmnParser;
import org.flowable.cmmn.engine.impl.parser.CmmnParserImpl;
import org.flowable.cmmn.engine.impl.parser.DefaultCmmnActivityBehaviorFactory;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseDefinitionDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CmmnDeploymentDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CmmnResourceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.HistoricCaseInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.HistoricMilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.MilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.PlanItemInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.SentryPartInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.TableDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisCaseDefinitionDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisCaseInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisCmmnDeploymentDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisHistoricCaseInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisHistoricMilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisMilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisPlanItemInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisResourceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisSentryPartInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.TableDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.deploy.CaseDefinitionCacheEntry;
import org.flowable.cmmn.engine.impl.process.ProcessInstanceService;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelper;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelperImpl;
import org.flowable.cmmn.engine.impl.runtime.CmmnRuntimeServiceImpl;
import org.flowable.cmmn.engine.impl.task.DefaultCmmnTaskVariableScopeResolver;
import org.flowable.engine.common.AbstractEngineConfiguration;
import org.flowable.engine.common.api.delegate.FlowableFunctionDelegate;
import org.flowable.engine.common.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.engine.common.impl.cfg.BeansConfigurationHelper;
import org.flowable.engine.common.impl.db.DbSchemaManager;
import org.flowable.engine.common.impl.el.ExpressionManager;
import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.engine.common.impl.interceptor.CommandInterceptor;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.common.impl.interceptor.SessionFactory;
import org.flowable.engine.common.impl.persistence.GenericManagerFactory;
import org.flowable.engine.common.impl.persistence.cache.EntityCache;
import org.flowable.engine.common.impl.persistence.cache.EntityCacheImpl;
import org.flowable.engine.common.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.engine.common.impl.persistence.deploy.DeploymentCache;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.db.IdentityLinkDbSchemaManager;
import org.flowable.task.service.InternalTaskVariableScopeResolver;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.history.InternalHistoryTaskManager;
import org.flowable.task.service.impl.db.TaskDbSchemaManager;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.history.InternalHistoryVariableManager;
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
import org.flowable.variable.service.impl.types.VariableType;
import org.flowable.variable.service.impl.types.VariableTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CmmnEngineConfiguration extends AbstractEngineConfiguration implements CmmnEngineConfigurationApi {

    protected static final Logger LOGGER = LoggerFactory.getLogger(CmmnEngineConfiguration.class);
    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/cmmn/db/mapping/mappings.xml";
    public static final String LIQUIBASE_CHANGELOG_PREFIX = "ACT_CMMN_";

    protected String cmmnEngineName = "default";

    protected CmmnEngineAgendaFactory cmmnEngineAgendaFactory;

    protected CmmnRuntimeService cmmnRuntimeService = new CmmnRuntimeServiceImpl();
    protected CmmnTaskService cmmnTaskService = new CmmnTaskServiceImpl();
    protected CmmnManagementService cmmnManagementService = new CmmnManagementServiceImpl();
    protected CmmnRepositoryService cmmnRepositoryService = new CmmnRepositoryServiceImpl();
    protected CmmnHistoryService cmmnHistoryService = new CmmnHistoryServiceImpl();

    protected TableDataManager tableDataManager;
    protected CmmnDeploymentDataManager deploymentDataManager;
    protected CmmnResourceDataManager resourceDataManager;
    protected CaseDefinitionDataManager caseDefinitionDataManager;
    protected CaseInstanceDataManager caseInstanceDataManager;
    protected PlanItemInstanceDataManager planItemInstanceDataManager;
    protected SentryPartInstanceDataManager sentryPartInstanceDataManager;
    protected MilestoneInstanceDataManager milestoneInstanceDataManager;
    protected HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager;
    protected HistoricMilestoneInstanceDataManager historicMilestoneInstanceDataManager;

    protected CmmnDeploymentEntityManager cmmnDeploymentEntityManager;
    protected CmmnResourceEntityManager cmmnResourceEntityManager;
    protected CaseDefinitionEntityManager caseDefinitionEntityManager;
    protected CaseInstanceEntityManager caseInstanceEntityManager;
    protected PlanItemInstanceEntityManager planItemInstanceEntityManager;
    protected SentryPartInstanceEntityManager sentryPartInstanceEntityManager;
    protected MilestoneInstanceEntityManager milestoneInstanceEntityManager;
    protected HistoricCaseInstanceDataManager historicCaseInstanceDataManager;
    protected HistoricMilestoneInstanceEntityManager historicMilestoneInstanceEntityManager;

    protected CaseInstanceHelper caseInstanceHelper;
    protected CmmnHistoryManager cmmnHistoryManager;
    protected ProcessInstanceService processInstanceService;
    protected Map<String, List<RuntimeInstanceStateChangeCallback>> caseInstanceStateChangeCallbacks;

    protected boolean enableSafeCmmnXml;
    protected CmmnActivityBehaviorFactory activityBehaviorFactory;
    protected CmmnClassDelegateFactory classDelegateFactory;
    protected CmmnParser cmmnParser;
    protected CmmnDeployer cmmnDeployer;
    protected List<Deployer> customPreDeployers;
    protected List<Deployer> customPostDeployers;
    protected List<Deployer> deployers;
    protected CmmnDeploymentManager deploymentManager;

    protected int caseDefinitionCacheLimit = -1;
    protected DeploymentCache<CaseDefinitionCacheEntry> caseDefinitionCache;
    
    protected HistoryLevel historyLevel = HistoryLevel.AUDIT;
    
    protected ExpressionManager expressionManager;
    protected List<FlowableFunctionDelegate> flowableFunctionDelegates;
    protected List<FlowableFunctionDelegate> customFlowableFunctionDelegates;
    
    /**
     * Using field injection together with a delegate expression for a service task / execution listener / task listener is not thread-sade , see user guide section 'Field Injection' for more
     * information.
     * <p>
     * Set this flag to false to throw an exception at runtime when a field is injected and a delegateExpression is used.
     */
    protected DelegateExpressionFieldInjectionMode delegateExpressionFieldInjectionMode = DelegateExpressionFieldInjectionMode.MIXED;

    protected DbSchemaManager identityLinkDbSchemaManager;
    protected DbSchemaManager variableDbSchemaManager;
    protected DbSchemaManager taskDbSchemaManager;
    
    // Identitylink support
    protected IdentityLinkServiceConfiguration identityLinkServiceConfiguration;
    
    // Task support
    protected TaskServiceConfiguration taskServiceConfiguration;
    protected InternalHistoryTaskManager internalHistoryTaskManager;
    protected InternalTaskVariableScopeResolver internalTaskVariableScopeResolver;
    protected boolean isEnableTaskRelationshipCounts;
    protected int taskQueryLimit;
    protected int historicTaskQueryLimit;
    
    // Variable support
    protected VariableTypes variableTypes;
    protected List<VariableType> customPreVariableTypes;
    protected List<VariableType> customPostVariableTypes;
    protected VariableServiceConfiguration variableServiceConfiguration;
    protected InternalHistoryVariableManager internalHistoryVariableManager;
    protected boolean serializableVariableTypeTrackDeserializedObjects = true;
    protected ObjectMapper objectMapper = new ObjectMapper();

    public static CmmnEngineConfiguration createCmmnEngineConfigurationFromResourceDefault() {
        return createCmmnEngineConfigurationFromResource("flowable.cmmn.cfg.xml", "cmmnEngineConfiguration");
    }

    public static CmmnEngineConfiguration createCmmnEngineConfigurationFromResource(String resource) {
        return createCmmnEngineConfigurationFromResource(resource, "cmmnEngineConfiguration");
    }

    public static CmmnEngineConfiguration createCmmnEngineConfigurationFromResource(String resource, String beanName) {
        return (CmmnEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromResource(resource, beanName);
    }

    public static CmmnEngineConfiguration createCmmnEngineConfigurationFromInputStream(InputStream inputStream) {
        return createCmmnEngineConfigurationFromInputStream(inputStream, "cmmnEngineConfiguration");
    }

    public static CmmnEngineConfiguration createCmmnEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
        return (CmmnEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromInputStream(inputStream, beanName);
    }

    public static CmmnEngineConfiguration createStandaloneCmmnEngineConfiguration() {
        return new CmmnEngineConfiguration();
    }

    public static CmmnEngineConfiguration createStandaloneInMemCmmnEngineConfiguration() {
        return new StandaloneInMemCmmnEngineConfiguration();
    }

    public CmmnEngine buildCmmnEngine() {
        init();
        return new CmmnEngineImpl(this);
    }

    protected void init() {
        initCommandContextFactory();
        initTransactionContextFactory();
        initCommandExecutors();
        initIdGenerator();
        initExpressionManager();
        initCmmnEngineAgendaFactory();

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
        initClassDelegateFactory();
        initActivityBehaviorFactory();
        initDeployers();
        initCaseDefinitionCache();
        initDeploymentManager();
        initCaseInstanceHelper();
        initHistoryManager();
        initCaseInstanceCallbacks();
        initClock();
        initIdentityLinkServiceConfiguration();
        initVariableServiceConfiguration();
        initTaskServiceConfiguration();
    }

    @Override

    public void initDbSchemaManager() {
        super.initDbSchemaManager();
        initCmmnDbSchemaManager();
        initIdentityLinkDbSchemaManager();
        initVariableDbSchemaManager();
        initTaskDbSchemaManager();
    }

    protected void initCmmnDbSchemaManager() {
        if (this.dbSchemaManager == null) {
            this.dbSchemaManager = new CmmnDbSchemaManager();
        }
    }

    protected void initVariableDbSchemaManager() {
        if (this.variableDbSchemaManager == null) {
            this.variableDbSchemaManager = new VariableDbSchemaManager();
        }
    }

    protected void initTaskDbSchemaManager() {
        if (this.taskDbSchemaManager == null) {
            this.taskDbSchemaManager = new TaskDbSchemaManager();
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
            expressionManager = new CmmnExpressionManager(beans);
        }
        if (flowableFunctionDelegates == null) {
            flowableFunctionDelegates = new ArrayList<>();
        }
        if (customFlowableFunctionDelegates != null) {
            flowableFunctionDelegates.addAll(customFlowableFunctionDelegates);
        }
        expressionManager.setFunctionDelegates(flowableFunctionDelegates);
    }

    public void initCmmnEngineAgendaFactory() {
        if (cmmnEngineAgendaFactory == null) {
            cmmnEngineAgendaFactory = new DefaultCmmnEngineAgendaFactory();
        }
    }

    @Override
    public void initCommandInvoker() {
        if (commandInvoker == null) {
            commandInvoker = new CmmnCommandInvoker();
        }
    }

    public void initSessionFactories() {
        if (sessionFactories == null) {
            sessionFactories = new HashMap<>();

            if (usingRelationalDatabase) {
                initDbSqlSessionFactory();
            }

            addSessionFactory(new GenericManagerFactory(EntityCache.class, EntityCacheImpl.class));
            commandContextFactory.setSessionFactories(sessionFactories);
        }

        addSessionFactory(new CmmnEngineAgendaSessionFactory(cmmnEngineAgendaFactory));

        if (customSessionFactories != null) {
            for (SessionFactory sessionFactory : customSessionFactories) {
                addSessionFactory(sessionFactory);
            }
        }
    }

    protected void initServices() {
        initService(cmmnRuntimeService);
        initService(cmmnTaskService);;
        initService(cmmnManagementService);
        initService(cmmnRepositoryService);
        initService(cmmnHistoryService);
    }

    protected void initService(Object service) {
        if (service instanceof ServiceImpl) {
            ((ServiceImpl) service).setEngineConfig(this);
            ((ServiceImpl) service).setCommandExecutor(commandExecutor);
        }
    }

    public void initDataManagers() {
        if (tableDataManager == null) {
            tableDataManager = new TableDataManagerImpl();
        }
        if (deploymentDataManager == null) {
            deploymentDataManager = new MybatisCmmnDeploymentDataManager(this);
        }
        if (resourceDataManager == null) {
            resourceDataManager = new MybatisResourceDataManager(this);
        }
        if (caseDefinitionDataManager == null) {
            caseDefinitionDataManager = new MybatisCaseDefinitionDataManager(this);
        }
        if (caseInstanceDataManager == null) {
            caseInstanceDataManager = new MybatisCaseInstanceDataManagerImpl(this);
        }
        if (planItemInstanceDataManager == null) {
            planItemInstanceDataManager = new MybatisPlanItemInstanceDataManagerImpl(this);
        }
        if (sentryPartInstanceDataManager == null) {
            sentryPartInstanceDataManager = new MybatisSentryPartInstanceDataManagerImpl(this);
        }
        if (milestoneInstanceDataManager == null) {
            milestoneInstanceDataManager = new MybatisMilestoneInstanceDataManager(this);
        }
        if (historicCaseInstanceDataManager == null) {
            historicCaseInstanceDataManager = new MybatisHistoricCaseInstanceDataManagerImpl(this);
        }
        if (historicMilestoneInstanceDataManager == null) {
            historicMilestoneInstanceDataManager = new MybatisHistoricMilestoneInstanceDataManager(this);
        }
    }

    public void initEntityManagers() {
        if (cmmnDeploymentEntityManager == null) {
            cmmnDeploymentEntityManager = new CmmnDeploymentEntityManagerImpl(this, deploymentDataManager);
        }
        if (cmmnResourceEntityManager == null) {
            cmmnResourceEntityManager = new CmmnResourceEntityManagerImpl(this, resourceDataManager);
        }
        if (caseDefinitionEntityManager == null) {
            caseDefinitionEntityManager = new CaseDefinitionEntityManagerImpl(this, caseDefinitionDataManager);
        }
        if (caseInstanceEntityManager == null) {
            caseInstanceEntityManager = new CaseInstanceEntityManagerImpl(this, caseInstanceDataManager);
        }
        if (planItemInstanceEntityManager == null) {
            planItemInstanceEntityManager = new PlanItemInstanceEntityManagerImpl(this, planItemInstanceDataManager);
        }
        if (sentryPartInstanceEntityManager == null) {
            sentryPartInstanceEntityManager = new SentryPartInstanceEntityManagerImpl(this, sentryPartInstanceDataManager);
        }
        if (milestoneInstanceEntityManager == null) {
            milestoneInstanceEntityManager = new MilestoneInstanceEntityManagerImpl(this, milestoneInstanceDataManager);
        }
        if (historicCaseInstanceEntityManager == null) {
            historicCaseInstanceEntityManager = new HistoricCaseInstanceEntityManagerImpl(this, historicCaseInstanceDataManager);
        }
        if (historicMilestoneInstanceEntityManager == null) {
            historicMilestoneInstanceEntityManager = new HistoricMilestoneInstanceEntityManagerImpl(this, historicMilestoneInstanceDataManager);
        }
    }

    protected void initClassDelegateFactory() {
        if (classDelegateFactory == null) {
            classDelegateFactory = new DefaultCmmnClassDelegateFactory();
        }
    }

    protected void initActivityBehaviorFactory() {
        if (activityBehaviorFactory == null) {
            DefaultCmmnActivityBehaviorFactory defaultCmmnActivityBehaviorFactory = new DefaultCmmnActivityBehaviorFactory();
            defaultCmmnActivityBehaviorFactory.setClassDelegateFactory(classDelegateFactory);
            defaultCmmnActivityBehaviorFactory.setExpressionManager(expressionManager);
            activityBehaviorFactory = defaultCmmnActivityBehaviorFactory;
        }
    }

    protected void initDeployers() {
        if (this.cmmnDeployer == null) {
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

    public Collection<? extends Deployer> getDefaultDeployers() {
        List<Deployer> defaultDeployers = new ArrayList<>();

        if (cmmnDeployer == null) {
            cmmnDeployer = new CmmnDeployer();
        }

        initCmmnParser();

        cmmnDeployer.setIdGenerator(idGenerator);
        cmmnDeployer.setCmmnParser(cmmnParser);

        defaultDeployers.add(cmmnDeployer);
        return defaultDeployers;
    }

    protected void initCaseDefinitionCache() {
        if (caseDefinitionCache == null) {
            if (caseDefinitionCacheLimit <= 0) {
                caseDefinitionCache = new DefaultDeploymentCache<>();
            } else {
                caseDefinitionCache = new DefaultDeploymentCache<>(caseDefinitionCacheLimit);
            }
        }
    }

    protected void initDeploymentManager() {
        if (deploymentManager == null) {
            deploymentManager = new CmmnDeploymentManager();
            deploymentManager.setCmmnEngineConfiguration(this);
            deploymentManager.setCaseDefinitionCache(caseDefinitionCache);
            deploymentManager.setDeployers(deployers);
            deploymentManager.setCaseDefinitionEntityManager(caseDefinitionEntityManager);
            deploymentManager.setDeploymentEntityManager(cmmnDeploymentEntityManager);
        }
    }

    public void initCmmnParser() {
        if (cmmnParser == null) {
            CmmnParserImpl cmmnParserImpl = new CmmnParserImpl();
            cmmnParserImpl.setActivityBehaviorFactory(activityBehaviorFactory);
            cmmnParserImpl.setExpressionManager(expressionManager);
            cmmnParser = cmmnParserImpl;
        }
    }

    public void initCaseInstanceHelper() {
        if (caseInstanceHelper == null) {
            caseInstanceHelper = new CaseInstanceHelperImpl();
        }
    }

    public void initHistoryManager() {
        if (cmmnHistoryManager == null) {
            cmmnHistoryManager = new DefaultCmmnHistoryManager(this);
        }
    }

    public void initCaseInstanceCallbacks() {
        if (this.caseInstanceStateChangeCallbacks == null) {
            this.caseInstanceStateChangeCallbacks = new HashMap<>();
        }
        initDefaultCaseInstanceCallbacks();
    }

    protected void initDefaultCaseInstanceCallbacks() {
        this.caseInstanceStateChangeCallbacks.put(PlanItemInstanceCallbackType.CHILD_CASE,
                Collections.<RuntimeInstanceStateChangeCallback>singletonList(new ChildCaseInstanceStateChangeCallback()));
    }

    @Override
    public String getEngineCfgKey() {
        return EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG;
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
        for (Class<? extends Entity> clazz : EntityDependencyOrder.INSERT_ORDER) {
            dbSqlSessionFactory.getInsertionOrder().add(clazz);
        }

        for (Class<? extends Entity> clazz : EntityDependencyOrder.DELETE_ORDER) {
            dbSqlSessionFactory.getDeletionOrder().add(clazz);
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
        
        this.variableServiceConfiguration.setHistoryLevel(this.historyLevel);
        this.variableServiceConfiguration.setClock(this.clock);
        this.variableServiceConfiguration.setObjectMapper(this.objectMapper);
        this.variableServiceConfiguration.setEventDispatcher(this.eventDispatcher);

        this.variableServiceConfiguration.setVariableTypes(this.variableTypes);
        
        if (this.internalHistoryVariableManager != null) {
            this.variableServiceConfiguration.setInternalHistoryVariableManager(this.internalHistoryVariableManager);
        } else {
            this.variableServiceConfiguration.setInternalHistoryVariableManager(new CmmnHistoryVariableManager(cmmnHistoryManager));
        }

        this.variableServiceConfiguration.setMaxLengthString(this.getMaxLengthString());
        this.variableServiceConfiguration.setSerializableVariableTypeTrackDeserializedObjects(this.isSerializableVariableTypeTrackDeserializedObjects());

        this.variableServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG, this.variableServiceConfiguration);
    }
    
    public void initTaskServiceConfiguration() {
        this.taskServiceConfiguration = new TaskServiceConfiguration();
        this.taskServiceConfiguration.setHistoryLevel(this.historyLevel);
        this.taskServiceConfiguration.setClock(this.clock);
        this.taskServiceConfiguration.setObjectMapper(this.objectMapper);
        this.taskServiceConfiguration.setEventDispatcher(this.eventDispatcher);

        if (this.internalHistoryTaskManager != null) {
            this.taskServiceConfiguration.setInternalHistoryTaskManager(this.internalHistoryTaskManager);
        } else {
            this.taskServiceConfiguration.setInternalHistoryTaskManager(new CmmnHistoryTaskManager(cmmnHistoryManager));
        }

        if (this.internalTaskVariableScopeResolver != null) {
            this.taskServiceConfiguration.setInternalTaskVariableScopeResolver(this.internalTaskVariableScopeResolver);
        } else {
            this.taskServiceConfiguration.setInternalTaskVariableScopeResolver(new DefaultCmmnTaskVariableScopeResolver(this));
        }

        this.taskServiceConfiguration.setEnableTaskRelationshipCounts(this.isEnableTaskRelationshipCounts);
        this.taskServiceConfiguration.setTaskQueryLimit(this.taskQueryLimit);
        this.taskServiceConfiguration.setHistoricTaskQueryLimit(this.historicTaskQueryLimit);

        this.taskServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG, this.taskServiceConfiguration);
    }
    
    public void initIdentityLinkServiceConfiguration() {
        this.identityLinkServiceConfiguration = new IdentityLinkServiceConfiguration();
        this.identityLinkServiceConfiguration.setHistoryLevel(this.historyLevel);
        this.identityLinkServiceConfiguration.setClock(this.clock);
        this.identityLinkServiceConfiguration.setObjectMapper(this.objectMapper);
        this.identityLinkServiceConfiguration.setEventDispatcher(this.eventDispatcher);

        this.identityLinkServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_IDENTITY_LINK_SERVICE_CONFIG, this.identityLinkServiceConfiguration);
    }

    @Override
    public String getEngineName() {
        return cmmnEngineName;
    }

    public String getCmmnEngineName() {
        return cmmnEngineName;
    }

    public CmmnEngineConfiguration setCmmnEngineName(String cmmnEngineName) {
        this.cmmnEngineName = cmmnEngineName;
        return this;
    }

    public CmmnRuntimeService getCmmnRuntimeService() {
        return cmmnRuntimeService;
    }

    public CmmnEngineConfiguration setCmmnRuntimeService(CmmnRuntimeService cmmnRuntimeService) {
        this.cmmnRuntimeService = cmmnRuntimeService;
        return this;
    }
    
    public CmmnTaskService getCmmnTaskService() {
        return cmmnTaskService;
    }

    public CmmnEngineConfiguration setCmmnTaskService(CmmnTaskService cmmnTaskService) {
        this.cmmnTaskService = cmmnTaskService;
        return this;
    }

    public CmmnManagementService getCmmnManagementService() {
        return cmmnManagementService;
    }

    public CmmnEngineConfiguration setCmmnManagementService(CmmnManagementService cmmnManagementService) {
        this.cmmnManagementService = cmmnManagementService;
        return this;
    }

    public CmmnRepositoryService getCmmnRepositoryService() {
        return cmmnRepositoryService;
    }

    public CmmnEngineConfiguration setCmmnRepositoryService(CmmnRepositoryService cmmnRepositoryService) {
        this.cmmnRepositoryService = cmmnRepositoryService;
        return this;
    }

    public CmmnHistoryService getCmmnHistoryService() {
        return cmmnHistoryService;
    }

    public CmmnEngineConfiguration setCmmnHistoryService(CmmnHistoryService cmmnHistoryService) {
        this.cmmnHistoryService = cmmnHistoryService;
        return this;
    }

    public CmmnEngineAgendaFactory getCmmnEngineAgendaFactory() {
        return cmmnEngineAgendaFactory;
    }

    public CmmnEngineConfiguration setCmmnEngineAgendaFactory(CmmnEngineAgendaFactory cmmnEngineAgendaFactory) {
        this.cmmnEngineAgendaFactory = cmmnEngineAgendaFactory;
        return this;
    }

    public TableDataManager getTableDataManager() {
        return tableDataManager;
    }

    public CmmnEngineConfiguration setTableDataManager(TableDataManager tableDataManager) {
        this.tableDataManager = tableDataManager;
        return this;
    }

    public CmmnDeploymentDataManager getDeploymentDataManager() {
        return deploymentDataManager;
    }

    public CmmnEngineConfiguration setDeploymentDataManager(CmmnDeploymentDataManager deploymentDataManager) {
        this.deploymentDataManager = deploymentDataManager;
        return this;
    }

    public CmmnResourceDataManager getResourceDataManager() {
        return resourceDataManager;
    }

    public CmmnEngineConfiguration setResourceDataManager(CmmnResourceDataManager resourceDataManager) {
        this.resourceDataManager = resourceDataManager;
        return this;
    }

    public CaseDefinitionDataManager getCaseDefinitionDataManager() {
        return caseDefinitionDataManager;
    }

    public CmmnEngineConfiguration setCaseDefinitionDataManager(CaseDefinitionDataManager caseDefinitionDataManager) {
        this.caseDefinitionDataManager = caseDefinitionDataManager;
        return this;
    }

    public CaseInstanceDataManager getCaseInstanceDataManager() {
        return caseInstanceDataManager;
    }

    public CmmnEngineConfiguration setCaseInstanceDataManager(CaseInstanceDataManager caseInstanceDataManager) {
        this.caseInstanceDataManager = caseInstanceDataManager;
        return this;
    }

    public PlanItemInstanceDataManager getPlanItemInstanceDataManager() {
        return planItemInstanceDataManager;
    }

    public CmmnEngineConfiguration setPlanItemInstanceDataManager(PlanItemInstanceDataManager planItemInstanceDataManager) {
        this.planItemInstanceDataManager = planItemInstanceDataManager;
        return this;
    }

    public SentryPartInstanceDataManager getSentryPartInstanceDataManager() {
        return sentryPartInstanceDataManager;
    }

    public CmmnEngineConfiguration setSentryPartInstanceDataManager(SentryPartInstanceDataManager sentryPartInstanceDataManager) {
        this.sentryPartInstanceDataManager = sentryPartInstanceDataManager;
        return this;
    }

    public MilestoneInstanceDataManager getMilestoneInstanceDataManager() {
        return milestoneInstanceDataManager;
    }

    public CmmnEngineConfiguration setMilestoneInstanceDataManager(MilestoneInstanceDataManager milestoneInstanceDataManager) {
        this.milestoneInstanceDataManager = milestoneInstanceDataManager;
        return this;
    }

    public HistoricCaseInstanceDataManager getHistoricCaseInstanceDataManager() {
        return historicCaseInstanceDataManager;
    }

    public CmmnEngineConfiguration setHistoricCaseInstanceDataManager(HistoricCaseInstanceDataManager historicCaseInstanceDataManager) {
        this.historicCaseInstanceDataManager = historicCaseInstanceDataManager;
        return this;
    }

    public HistoricMilestoneInstanceDataManager getHistoricMilestoneInstanceDataManager() {
        return historicMilestoneInstanceDataManager;
    }

    public CmmnEngineConfiguration setHistoricMilestoneInstanceDataManager(HistoricMilestoneInstanceDataManager historicMilestoneInstanceDataManager) {
        this.historicMilestoneInstanceDataManager = historicMilestoneInstanceDataManager;
        return this;
    }

    public CmmnDeploymentEntityManager getCmmnDeploymentEntityManager() {
        return cmmnDeploymentEntityManager;
    }

    public CmmnEngineConfiguration setCmmnDeploymentEntityManager(CmmnDeploymentEntityManager cmmnDeploymentEntityManager) {
        this.cmmnDeploymentEntityManager = cmmnDeploymentEntityManager;
        return this;
    }

    public CmmnResourceEntityManager getCmmnResourceEntityManager() {
        return cmmnResourceEntityManager;
    }

    public CmmnEngineConfiguration setCmmnResourceEntityManager(CmmnResourceEntityManager cmmnResourceEntityManager) {
        this.cmmnResourceEntityManager = cmmnResourceEntityManager;
        return this;
    }

    public CaseDefinitionEntityManager getCaseDefinitionEntityManager() {
        return caseDefinitionEntityManager;
    }

    public CmmnEngineConfiguration setCaseDefinitionEntityManager(CaseDefinitionEntityManager caseDefinitionEntityManager) {
        this.caseDefinitionEntityManager = caseDefinitionEntityManager;
        return this;
    }

    public CaseInstanceEntityManager getCaseInstanceEntityManager() {
        return caseInstanceEntityManager;
    }

    public CmmnEngineConfiguration setCaseInstanceEntityManager(CaseInstanceEntityManager caseInstanceEntityManager) {
        this.caseInstanceEntityManager = caseInstanceEntityManager;
        return this;
    }

    public PlanItemInstanceEntityManager getPlanItemInstanceEntityManager() {
        return planItemInstanceEntityManager;
    }

    public CmmnEngineConfiguration setPlanItemInstanceEntityManager(PlanItemInstanceEntityManager planItemInstanceEntityManager) {
        this.planItemInstanceEntityManager = planItemInstanceEntityManager;
        return this;
    }

    public SentryPartInstanceEntityManager getSentryPartInstanceEntityManager() {
        return sentryPartInstanceEntityManager;
    }

    public CmmnEngineConfiguration setSentryPartInstanceEntityManager(SentryPartInstanceEntityManager sentryPartInstanceEntityManager) {
        this.sentryPartInstanceEntityManager = sentryPartInstanceEntityManager;
        return this;
    }

    public MilestoneInstanceEntityManager getMilestoneInstanceEntityManager() {
        return milestoneInstanceEntityManager;
    }

    public CmmnEngineConfiguration setMilestoneInstanceEntityManager(MilestoneInstanceEntityManager milestoneInstanceEntityManager) {
        this.milestoneInstanceEntityManager = milestoneInstanceEntityManager;
        return this;
    }

    public HistoricCaseInstanceEntityManager getHistoricCaseInstanceEntityManager() {
        return historicCaseInstanceEntityManager;
    }

    public CmmnEngineConfiguration setHistoricCaseInstanceEntityManager(HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager) {
        this.historicCaseInstanceEntityManager = historicCaseInstanceEntityManager;
        return this;
    }

    public HistoricMilestoneInstanceEntityManager getHistoricMilestoneInstanceEntityManager() {
        return historicMilestoneInstanceEntityManager;
    }

    public CmmnEngineConfiguration setHistoricMilestoneInstanceEntityManager(HistoricMilestoneInstanceEntityManager historicMilestoneInstanceEntityManager) {
        this.historicMilestoneInstanceEntityManager = historicMilestoneInstanceEntityManager;
        return this;
    }

    public CaseInstanceHelper getCaseInstanceHelper() {
        return caseInstanceHelper;
    }

    public CmmnEngineConfiguration setCaseInstanceHelper(CaseInstanceHelper caseInstanceHelper) {
        this.caseInstanceHelper = caseInstanceHelper;
        return this;
    }

    public CmmnHistoryManager getCmmnHistoryManager() {
        return cmmnHistoryManager;
    }

    public CmmnEngineConfiguration setCmmnHistoryManager(CmmnHistoryManager cmmnHistoryManager) {
        this.cmmnHistoryManager = cmmnHistoryManager;
        return this;
    }

    public boolean isEnableSafeCmmnXml() {
        return enableSafeCmmnXml;
    }

    public CmmnEngineConfiguration setEnableSafeCmmnXml(boolean enableSafeCmmnXml) {
        this.enableSafeCmmnXml = enableSafeCmmnXml;
        return this;
    }

    public CmmnParser getCmmnParser() {
        return cmmnParser;
    }

    public CmmnEngineConfiguration setCmmnParser(CmmnParser cmmnParser) {
        this.cmmnParser = cmmnParser;
        return this;
    }

    public CmmnDeployer getCmmnDeployer() {
        return cmmnDeployer;
    }

    public CmmnEngineConfiguration setCmmnDeployer(CmmnDeployer cmmnDeployer) {
        this.cmmnDeployer = cmmnDeployer;
        return this;
    }

    public List<Deployer> getCustomPreDeployers() {
        return customPreDeployers;
    }

    public CmmnEngineConfiguration setCustomPreDeployers(List<Deployer> customPreDeployers) {
        this.customPreDeployers = customPreDeployers;
        return this;
    }

    public List<Deployer> getCustomPostDeployers() {
        return customPostDeployers;
    }

    public CmmnEngineConfiguration setCustomPostDeployers(List<Deployer> customPostDeployers) {
        this.customPostDeployers = customPostDeployers;
        return this;
    }

    public List<Deployer> getDeployers() {
        return deployers;
    }

    public CmmnEngineConfiguration setDeployers(List<Deployer> deployers) {
        this.deployers = deployers;
        return this;
    }

    public CmmnDeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    public CmmnEngineConfiguration setDeploymentManager(CmmnDeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
        return this;
    }

    public CmmnActivityBehaviorFactory getActivityBehaviorFactory() {
        return activityBehaviorFactory;
    }

    public CmmnEngineConfiguration setActivityBehaviorFactory(CmmnActivityBehaviorFactory activityBehaviorFactory) {
        this.activityBehaviorFactory = activityBehaviorFactory;
        return this;
    }

    public CmmnClassDelegateFactory getClassDelegateFactory() {
        return classDelegateFactory;
    }

    public CmmnEngineConfiguration setClassDelegateFactory(CmmnClassDelegateFactory classDelegateFactory) {
        this.classDelegateFactory = classDelegateFactory;
        return this;
    }

    public int getCaseDefinitionCacheLimit() {
        return caseDefinitionCacheLimit;
    }

    public CmmnEngineConfiguration setCaseDefinitionCacheLimit(int caseDefinitionCacheLimit) {
        this.caseDefinitionCacheLimit = caseDefinitionCacheLimit;
        return this;
    }

    public DeploymentCache<CaseDefinitionCacheEntry> getCaseDefinitionCache() {
        return caseDefinitionCache;
    }

    public CmmnEngineConfiguration setCaseDefinitionCache(DeploymentCache<CaseDefinitionCacheEntry> caseDefinitionCache) {
        this.caseDefinitionCache = caseDefinitionCache;
        return this;
    }

    public ProcessInstanceService getProcessInstanceService() {
        return processInstanceService;
    }

    public CmmnEngineConfiguration setProcessInstanceService(ProcessInstanceService processInstanceService) {
        this.processInstanceService = processInstanceService;
        return this;
    }

    public Map<String, List<RuntimeInstanceStateChangeCallback>> getCaseInstanceStateChangeCallbacks() {
        return caseInstanceStateChangeCallbacks;
    }

    public CmmnEngineConfiguration setCaseInstanceStateChangeCallbacks(Map<String, List<RuntimeInstanceStateChangeCallback>> caseInstanceStateChangeCallbacks) {
        this.caseInstanceStateChangeCallbacks = caseInstanceStateChangeCallbacks;
        return this;
    }
    
    @Override
    public CmmnEngineConfiguration setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public HistoryLevel getHistoryLevel() {
        return historyLevel;
    }

    public CmmnEngineConfiguration setHistoryLevel(HistoryLevel historyLevel) {
        this.historyLevel = historyLevel;
        return this;
    }
    
    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    public CmmnEngineConfiguration setExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
        return this;
    }

    public DelegateExpressionFieldInjectionMode getDelegateExpressionFieldInjectionMode() {
        return delegateExpressionFieldInjectionMode;
    }

    public CmmnEngineConfiguration setDelegateExpressionFieldInjectionMode(DelegateExpressionFieldInjectionMode delegateExpressionFieldInjectionMode) {
        this.delegateExpressionFieldInjectionMode = delegateExpressionFieldInjectionMode;
        return this;
    }

    public List<FlowableFunctionDelegate> getFlowableFunctionDelegates() {
        return flowableFunctionDelegates;
    }

    public CmmnEngineConfiguration setFlowableFunctionDelegates(List<FlowableFunctionDelegate> flowableFunctionDelegates) {
        this.flowableFunctionDelegates = flowableFunctionDelegates;
        return this;
    }

    public List<FlowableFunctionDelegate> getCustomFlowableFunctionDelegates() {
        return customFlowableFunctionDelegates;
    }

    public CmmnEngineConfiguration setCustomFlowableFunctionDelegates(List<FlowableFunctionDelegate> customFlowableFunctionDelegates) {
        this.customFlowableFunctionDelegates = customFlowableFunctionDelegates;
        return this;
    }
    
    public DbSchemaManager getIdentityLinkDbSchemaManager() {
        return identityLinkDbSchemaManager;
    }

    public CmmnEngineConfiguration setIdentityLinkDbSchemaManager(DbSchemaManager identityLinkDbSchemaManager) {
        this.identityLinkDbSchemaManager = identityLinkDbSchemaManager;
        return this;
    }

    public DbSchemaManager getVariableDbSchemaManager() {
        return variableDbSchemaManager;
    }

    public CmmnEngineConfiguration setVariableDbSchemaManager(DbSchemaManager variableDbSchemaManager) {
        this.variableDbSchemaManager = variableDbSchemaManager;
        return this;
    }
    
    public DbSchemaManager getTaskDbSchemaManager() {
        return taskDbSchemaManager;
    }

    public CmmnEngineConfiguration setTaskDbSchemaManager(DbSchemaManager taskDbSchemaManager) {
        this.taskDbSchemaManager = taskDbSchemaManager;
        return this;
    }

    public VariableTypes getVariableTypes() {
        return variableTypes;
    }

    public CmmnEngineConfiguration setVariableTypes(VariableTypes variableTypes) {
        this.variableTypes = variableTypes;
        return this;
    }
    
    public List<VariableType> getCustomPreVariableTypes() {
        return customPreVariableTypes;
    }

    public CmmnEngineConfiguration setCustomPreVariableTypes(List<VariableType> customPreVariableTypes) {
        this.customPreVariableTypes = customPreVariableTypes;
        return this;
    }

    public List<VariableType> getCustomPostVariableTypes() {
        return customPostVariableTypes;
    }

    public CmmnEngineConfiguration setCustomPostVariableTypes(List<VariableType> customPostVariableTypes) {
        this.customPostVariableTypes = customPostVariableTypes;
        return this;
    }
    
    public IdentityLinkServiceConfiguration getIdentityLinkServiceConfiguration() {
        return identityLinkServiceConfiguration;
    }

    public CmmnEngineConfiguration setIdentityLinkServiceConfiguration(IdentityLinkServiceConfiguration identityLinkServiceConfiguration) {
        this.identityLinkServiceConfiguration = identityLinkServiceConfiguration;
        return this;
    }

    public VariableServiceConfiguration getVariableServiceConfiguration() {
        return variableServiceConfiguration;
    }

    public CmmnEngineConfiguration setVariableServiceConfiguration(VariableServiceConfiguration variableServiceConfiguration) {
        this.variableServiceConfiguration = variableServiceConfiguration;
        return this;
    }
    
    public TaskServiceConfiguration getTaskServiceConfiguration() {
        return taskServiceConfiguration;
    }

    public CmmnEngineConfiguration setTaskServiceConfiguration(TaskServiceConfiguration taskServiceConfiguration) {
        this.taskServiceConfiguration = taskServiceConfiguration;
        return this;
    }
    
    public InternalHistoryTaskManager getInternalHistoryTaskManager() {
        return internalHistoryTaskManager;
    }

    public CmmnEngineConfiguration setInternalHistoryTaskManager(InternalHistoryTaskManager internalHistoryTaskManager) {
        this.internalHistoryTaskManager = internalHistoryTaskManager;
        return this;
    }
    
    public InternalTaskVariableScopeResolver getInternalTaskVariableScopeResolver() {
        return internalTaskVariableScopeResolver;
    }

    public CmmnEngineConfiguration setInternalTaskVariableScopeResolver(InternalTaskVariableScopeResolver internalTaskVariableScopeResolver) {
        this.internalTaskVariableScopeResolver = internalTaskVariableScopeResolver;
        return this;
    }

    public boolean isEnableTaskRelationshipCounts() {
        return isEnableTaskRelationshipCounts;
    }

    public CmmnEngineConfiguration setEnableTaskRelationshipCounts(boolean isEnableTaskRelationshipCounts) {
        this.isEnableTaskRelationshipCounts = isEnableTaskRelationshipCounts;
        return this;
    }

    public int getTaskQueryLimit() {
        return taskQueryLimit;
    }

    public CmmnEngineConfiguration setTaskQueryLimit(int taskQueryLimit) {
        this.taskQueryLimit = taskQueryLimit;
        return this;
    }

    public int getHistoricTaskQueryLimit() {
        return historicTaskQueryLimit;
    }

    public CmmnEngineConfiguration setHistoricTaskQueryLimit(int historicTaskQueryLimit) {
        this.historicTaskQueryLimit = historicTaskQueryLimit;
        return this;
    }

    public InternalHistoryVariableManager getInternalHistoryVariableManager() {
        return internalHistoryVariableManager;
    }

    public CmmnEngineConfiguration setInternalHistoryVariableManager(InternalHistoryVariableManager internalHistoryVariableManager) {
        this.internalHistoryVariableManager = internalHistoryVariableManager;
        return this;
    }

    public boolean isSerializableVariableTypeTrackDeserializedObjects() {
        return serializableVariableTypeTrackDeserializedObjects;
    }

    public CmmnEngineConfiguration setSerializableVariableTypeTrackDeserializedObjects(boolean serializableVariableTypeTrackDeserializedObjects) {
        this.serializableVariableTypeTrackDeserializedObjects = serializableVariableTypeTrackDeserializedObjects;
        return this;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public CmmnEngineConfiguration setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

}
