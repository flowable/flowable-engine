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
import java.util.HashMap;
import java.util.List;

import org.flowable.cmmn.engine.impl.CmmnEngineImpl;
import org.flowable.cmmn.engine.impl.CmmnManagementServiceImpl;
import org.flowable.cmmn.engine.impl.CmmnRepositoryServiceImpl;
import org.flowable.cmmn.engine.impl.ServiceImpl;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgendaFactory;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgendaSessionFactory;
import org.flowable.cmmn.engine.impl.agenda.DefaultCmmnEngineAgendaFactory;
import org.flowable.cmmn.engine.impl.cfg.StandaloneInMemCmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.db.CmmnDbSchemaManager;
import org.flowable.cmmn.engine.impl.db.EntityDependencyOrder;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeployer;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.deployer.Deployer;
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
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseDefinitionDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CmmnDeploymentDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CmmnResourceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.MilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.PlanItemInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.TableDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisCaseDefinitionDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisCaseInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisCmmnDeploymentDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisMilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisPlanItemInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisResourceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.TableDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.deploy.CaseDefinitionCacheEntry;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelper;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelperImpl;
import org.flowable.cmmn.engine.impl.runtime.CmmnRuntimeServiceImpl;
import org.flowable.engine.common.AbstractEngineConfiguration;
import org.flowable.engine.common.impl.cfg.BeansConfigurationHelper;
import org.flowable.engine.common.impl.interceptor.CommandInterceptor;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.common.impl.interceptor.SessionFactory;
import org.flowable.engine.common.impl.persistence.GenericManagerFactory;
import org.flowable.engine.common.impl.persistence.cache.EntityCache;
import org.flowable.engine.common.impl.persistence.cache.EntityCacheImpl;
import org.flowable.engine.common.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.engine.common.impl.persistence.deploy.DeploymentCache;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmmnEngineConfiguration extends AbstractEngineConfiguration {
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(CmmnEngineConfiguration.class);
    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/cmmn/db/mapping/mappings.xml";
    public static final String LIQUIBASE_CHANGELOG_PREFIX = "ACT_CMMN_";
    
    protected String cmmnEngineName = "default";
    
    protected CmmnEngineAgendaFactory cmmnEngineAgendaFactory;
    
    protected CmmnRuntimeService cmmnRuntimeService = new CmmnRuntimeServiceImpl();
    protected CmmnManagementService cmmnManagementService = new CmmnManagementServiceImpl();
    protected CmmnRepositoryService cmmnRepositoryService = new CmmnRepositoryServiceImpl();
    
    protected CmmnDeploymentDataManager deploymentDataManager;
    protected CmmnResourceDataManager resourceDataManager;
    protected CaseDefinitionDataManager caseDefinitionDataManager;
    protected CaseInstanceDataManager caseInstanceDataManager;
    protected PlanItemInstanceDataManager planItemInstanceDataManager;
    protected MilestoneInstanceDataManager milestoneInstanceDataManager;
    protected TableDataManager tableDataManager;
    
    protected CmmnDeploymentEntityManager cmmnDeploymentEntityManager;
    protected CmmnResourceEntityManager cmmnResourceEntityManager;
    protected CaseDefinitionEntityManager caseDefinitionEntityManager;
    protected CaseInstanceEntityManager caseInstanceEntityManager;
    protected PlanItemInstanceEntityManager planItemInstanceEntityManager;
    protected MilestoneInstanceEntityManager milestoneInstanceEntityManager;
    
    protected CaseInstanceHelper caseInstanceHelper;
    
    protected boolean enableSafeCmmnXml;
    protected CmmnActivityBehaviorFactory activityBehaviorFactory;
    protected CmmnParser cmmnParser;
    protected CmmnDeployer cmmnDeployer;
    protected List<Deployer> customPreDeployers;
    protected List<Deployer> customPostDeployers;
    protected List<Deployer> deployers;
    protected CmmnDeploymentManager deploymentManager;
    
    protected int caseDefinitionCacheLimit = -1;
    protected DeploymentCache<CaseDefinitionCacheEntry> caseDefinitionCache;
    
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
        initCmmnEngineAgendaFactory();

        if (usingRelationalDatabase) {
            initDataSource();
            initDbSchemaManager();
            initDbSchema();
        }

        initBeans();
        initTransactionFactory();
        
        if (usingRelationalDatabase) {
            initSqlSessionFactory();
        }
        
        initSessionFactories();
        initServices();
        initDataManagers();
        initEntityManagers();
        initActivityBehaviorFactory();
        initDeployers();
        initCaseDefinitionCache();
        initDeploymentManager();
        initCaseInstanceHelper();
        initClock();
    }
    
    public void initDbSchemaManager() {
        if (this.dbSchemaManager == null) {
            this.dbSchemaManager = new CmmnDbSchemaManager();
        }
    }
    
    public void initDbSchema() {
        ((CmmnDbSchemaManager) this.dbSchemaManager).initSchema(this);
    }
    
    public void initCmmnEngineAgendaFactory() {
        if (cmmnEngineAgendaFactory == null) {
            cmmnEngineAgendaFactory = new DefaultCmmnEngineAgendaFactory();
        }
    }
    
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
            
            addSessionFactory(new CmmnEngineAgendaSessionFactory(cmmnEngineAgendaFactory));

            addSessionFactory(new GenericManagerFactory(EntityCache.class, EntityCacheImpl.class));
            commandContextFactory.setSessionFactories(sessionFactories);
        }

        if (customSessionFactories != null) {
            for (SessionFactory sessionFactory : customSessionFactories) {
                addSessionFactory(sessionFactory);
            }
        }
    }
    
    protected void initServices() {
        initService(cmmnRuntimeService);
        initService(cmmnManagementService);
        initService(cmmnRepositoryService);
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
        if (milestoneInstanceDataManager == null) {
            milestoneInstanceDataManager = new MybatisMilestoneInstanceDataManager(this);
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
        if (milestoneInstanceEntityManager == null) {
            milestoneInstanceEntityManager = new MilestoneInstanceEntityManagerImpl(this, milestoneInstanceDataManager);
        }
    }
    
    protected void initActivityBehaviorFactory() {
        if (activityBehaviorFactory == null) {
            activityBehaviorFactory = new DefaultCmmnActivityBehaviorFactory();
        }
    }
    
    protected void initDeployers() {
        if (this.cmmnDeployer == null) {
            this.deployers = new ArrayList<Deployer>();
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
        List<Deployer> defaultDeployers = new ArrayList<Deployer>();

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
                caseDefinitionCache = new DefaultDeploymentCache<CaseDefinitionCacheEntry>();
            } else {
                caseDefinitionCache = new DefaultDeploymentCache<CaseDefinitionCacheEntry>(caseDefinitionCacheLimit);
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
            cmmnParser = cmmnParserImpl;
        }
    }
    
    public void initCaseInstanceHelper() {
        if (caseInstanceHelper == null) {
            caseInstanceHelper = new CaseInstanceHelperImpl();
        }
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
    
    public MilestoneInstanceDataManager getMilestoneInstanceDataManager() {
        return milestoneInstanceDataManager;
    }

    public CmmnEngineConfiguration setMilestoneInstanceDataManager(MilestoneInstanceDataManager milestoneInstanceDataManager) {
        this.milestoneInstanceDataManager = milestoneInstanceDataManager;
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
    
    public MilestoneInstanceEntityManager getMilestoneInstanceEntityManager() {
        return milestoneInstanceEntityManager;
    }

    public void setMilestoneInstanceEntityManager(MilestoneInstanceEntityManager milestoneInstanceEntityManager) {
        this.milestoneInstanceEntityManager = milestoneInstanceEntityManager;
    }

    public CaseInstanceHelper getCaseInstanceHelper() {
        return caseInstanceHelper;
    }

    public CmmnEngineConfiguration setCaseInstanceHelper(CaseInstanceHelper caseInstanceHelper) {
        this.caseInstanceHelper = caseInstanceHelper;
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

}
