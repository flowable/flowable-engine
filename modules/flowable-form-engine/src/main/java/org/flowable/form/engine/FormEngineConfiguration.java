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
package org.flowable.form.engine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.HasExpressionManagerEngineConfiguration;
import org.flowable.common.engine.impl.cfg.BeansConfigurationHelper;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.el.DefaultExpressionManager;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.common.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.common.engine.impl.persistence.entity.TableDataManager;
import org.flowable.common.engine.impl.tenant.ChangeTenantIdManager;
import org.flowable.common.engine.impl.tenant.MyBatisChangeTenantIdManager;
import org.flowable.editor.form.converter.FormJsonConverter;
import org.flowable.form.api.FormChangeTenantIdEntityTypes;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.impl.FormEngineImpl;
import org.flowable.form.engine.impl.FormManagementServiceImpl;
import org.flowable.form.engine.impl.FormRepositoryServiceImpl;
import org.flowable.form.engine.impl.FormServiceImpl;
import org.flowable.form.engine.impl.cfg.StandaloneFormEngineConfiguration;
import org.flowable.form.engine.impl.cfg.StandaloneInMemFormEngineConfiguration;
import org.flowable.form.engine.impl.cmd.SchemaOperationsFormEngineBuild;
import org.flowable.form.engine.impl.db.EntityDependencyOrder;
import org.flowable.form.engine.impl.db.FormDbSchemaManager;
import org.flowable.form.engine.impl.deployer.CachingAndArtifactsManager;
import org.flowable.form.engine.impl.deployer.FormDefinitionDeployer;
import org.flowable.form.engine.impl.deployer.FormDefinitionDeploymentHelper;
import org.flowable.form.engine.impl.deployer.ParsedDeploymentBuilderFactory;
import org.flowable.form.engine.impl.parser.FormDefinitionParseFactory;
import org.flowable.form.engine.impl.persistence.deploy.Deployer;
import org.flowable.form.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.form.engine.impl.persistence.deploy.FormDefinitionCacheEntry;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntityManagerImpl;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntityManagerImpl;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntityManagerImpl;
import org.flowable.form.engine.impl.persistence.entity.FormResourceEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormResourceEntityManagerImpl;
import org.flowable.form.engine.impl.persistence.entity.data.FormDefinitionDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.FormDeploymentDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.FormInstanceDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.FormResourceDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.impl.MybatisFormDefinitionDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.impl.MybatisFormDeploymentDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.impl.MybatisFormInstanceDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.impl.MybatisFormResourceDataManager;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;

public class FormEngineConfiguration extends AbstractEngineConfiguration
        implements FormEngineConfigurationApi, HasExpressionManagerEngineConfiguration {

    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/form/db/mapping/mappings.xml";

    public static final String LIQUIBASE_CHANGELOG_PREFIX = "ACT_FO_";

    protected String formEngineName = FormEngines.NAME_DEFAULT;

    // SERVICES
    // /////////////////////////////////////////////////////////////////

    protected FormManagementService formManagementService = new FormManagementServiceImpl(this);
    protected FormRepositoryService formRepositoryService = new FormRepositoryServiceImpl(this);
    protected FormService formService = new FormServiceImpl(this);
    protected ChangeTenantIdManager changeTenantIdManager;

    // DATA MANAGERS ///////////////////////////////////////////////////

    protected FormDeploymentDataManager deploymentDataManager;
    protected FormDefinitionDataManager formDefinitionDataManager;
    protected FormResourceDataManager resourceDataManager;
    protected FormInstanceDataManager formInstanceDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////
    protected FormDeploymentEntityManager deploymentEntityManager;
    protected FormDefinitionEntityManager formDefinitionEntityManager;
    protected FormResourceEntityManager resourceEntityManager;
    protected FormInstanceEntityManager formInstanceEntityManager;

    protected ExpressionManager expressionManager;
    protected Collection<ELResolver> preDefaultELResolvers;
    protected Collection<ELResolver> preBeanELResolvers;
    protected Collection<ELResolver> postDefaultELResolvers;

    protected FormJsonConverter formJsonConverter = new FormJsonConverter();

    // DEPLOYERS
    // ////////////////////////////////////////////////////////////////

    protected FormDefinitionDeployer formDeployer;
    protected FormDefinitionParseFactory formParseFactory;
    protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
    protected FormDefinitionDeploymentHelper formDeploymentHelper;
    protected CachingAndArtifactsManager cachingAndArtifactsManager;
    protected List<Deployer> customPreDeployers;
    protected List<Deployer> customPostDeployers;
    protected List<Deployer> deployers;
    protected DeploymentManager deploymentManager;

    protected int formDefinitionCacheLimit = -1; // By default, no limit
    protected DeploymentCache<FormDefinitionCacheEntry> formDefinitionCache;

    public static FormEngineConfiguration createFormEngineConfigurationFromResourceDefault() {
        return createFormEngineConfigurationFromResource("flowable.form.cfg.xml", "formEngineConfiguration");
    }

    public static FormEngineConfiguration createFormEngineConfigurationFromResource(String resource) {
        return createFormEngineConfigurationFromResource(resource, "formEngineConfiguration");
    }

    public static FormEngineConfiguration createFormEngineConfigurationFromResource(String resource, String beanName) {
        return (FormEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromResource(resource, beanName);
    }

    public static FormEngineConfiguration createFormEngineConfigurationFromInputStream(InputStream inputStream) {
        return createFormEngineConfigurationFromInputStream(inputStream, "formEngineConfiguration");
    }

    public static FormEngineConfiguration createFormEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
        return (FormEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromInputStream(inputStream, beanName);
    }

    public static FormEngineConfiguration createStandaloneFormEngineConfiguration() {
        return new StandaloneFormEngineConfiguration();
    }

    public static FormEngineConfiguration createStandaloneInMemFormEngineConfiguration() {
        return new StandaloneInMemFormEngineConfiguration();
    }

    // buildFormEngine
    // ///////////////////////////////////////////////////////

    public FormEngine buildFormEngine() {
        init();
        return new FormEngineImpl(this);
    }

    // init
    // /////////////////////////////////////////////////////////////////////

    protected void init() {
        initEngineConfigurations();
        initConfigurators();
        configuratorsBeforeInit();
        initClock();
        initObjectMapper();
        initBeans();
        initExpressionManager();
        initCommandContextFactory();
        initTransactionContextFactory();
        initCommandExecutors();
        initIdGenerator();

        if (usingRelationalDatabase) {
            initDataSource();
        }
        
        if (usingRelationalDatabase || usingSchemaMgmt) {
            initSchemaManager();
            initSchemaManagementCommand();
        }

        initTransactionFactory();

        if (usingRelationalDatabase) {
            initSqlSessionFactory();
        }

        initSessionFactories();
        initServices();
        initChangeTenantIdManager();
        configuratorsAfterInit();
        initDataManagers();
        initEntityManagers();
        initDeployers();
    }

    // services
    // /////////////////////////////////////////////////////////////////

    protected void initServices() {
        initService(formManagementService);
        initService(formRepositoryService);
        initService(formService);
    }

    public void initChangeTenantIdManager() {
        if (changeTenantIdManager == null) {
            changeTenantIdManager = new MyBatisChangeTenantIdManager(commandExecutor, ScopeTypes.FORM,
                    Collections.singleton(FormChangeTenantIdEntityTypes.FORM_INSTANCES));
        }
    }

    public void initExpressionManager() {
        if (expressionManager == null) {
            DefaultExpressionManager formExpressionManager = new DefaultExpressionManager(beans);
            if (preDefaultELResolvers != null) {
                preDefaultELResolvers.forEach(formExpressionManager::addPreDefaultResolver);
            }

            if (preBeanELResolvers != null) {
                preBeanELResolvers.forEach(formExpressionManager::addPreBeanResolver);
            }

            if (postDefaultELResolvers != null) {
                postDefaultELResolvers.forEach(formExpressionManager::addPostDefaultResolver);
            }
            expressionManager = formExpressionManager;
        }
    }

    // Data managers
    ///////////////////////////////////////////////////////////

    @Override
    public void initDataManagers() {
        super.initDataManagers();
        if (deploymentDataManager == null) {
            deploymentDataManager = new MybatisFormDeploymentDataManager(this);
        }
        if (formDefinitionDataManager == null) {
            formDefinitionDataManager = new MybatisFormDefinitionDataManager(this);
        }
        if (resourceDataManager == null) {
            resourceDataManager = new MybatisFormResourceDataManager(this);
        }
        if (formInstanceDataManager == null) {
            formInstanceDataManager = new MybatisFormInstanceDataManager(this);
        }
    }

    @Override
    public void initEntityManagers() {
        super.initEntityManagers();
        if (deploymentEntityManager == null) {
            deploymentEntityManager = new FormDeploymentEntityManagerImpl(this, deploymentDataManager);
        }
        if (formDefinitionEntityManager == null) {
            formDefinitionEntityManager = new FormDefinitionEntityManagerImpl(this, formDefinitionDataManager);
        }
        if (resourceEntityManager == null) {
            resourceEntityManager = new FormResourceEntityManagerImpl(this, resourceDataManager);
        }
        if (formInstanceEntityManager == null) {
            formInstanceEntityManager = new FormInstanceEntityManagerImpl(this, formInstanceDataManager);
        }
    }

    // data model ///////////////////////////////////////////////////////////////

    @Override
    public void initSchemaManager() {
        super.initSchemaManager();
        if (this.schemaManager == null) {
            this.schemaManager = new FormDbSchemaManager();
        }
    }
    
    public void initSchemaManagementCommand() {
        if (schemaManagementCmd == null) {
            if (usingRelationalDatabase && databaseSchemaUpdate != null) {
                this.schemaManagementCmd = new SchemaOperationsFormEngineBuild();
            }
        }
    }

    private void closeDatabase(Liquibase liquibase) {
        if (liquibase != null) {
            Database database = liquibase.getDatabase();
            if (database != null) {
                try {
                    database.close();
                } catch (DatabaseException e) {
                    logger.warn("Error closing database", e);
                }
            }
        }
    }

    // session factories ////////////////////////////////////////////////////////

    @Override
    public void initDbSqlSessionFactory() {
        if (dbSqlSessionFactory == null) {
            dbSqlSessionFactory = createDbSqlSessionFactory();
            dbSqlSessionFactory.setDatabaseType(databaseType);
            dbSqlSessionFactory.setSqlSessionFactory(sqlSessionFactory);
            dbSqlSessionFactory.setDatabaseTablePrefix(databaseTablePrefix);
            dbSqlSessionFactory.setTablePrefixIsSchema(tablePrefixIsSchema);
            dbSqlSessionFactory.setDatabaseCatalog(databaseCatalog);
            dbSqlSessionFactory.setDatabaseSchema(databaseSchema);
            addSessionFactory(dbSqlSessionFactory);
        }
        initDbSqlSessionFactoryEntitySettings();
    }

    @Override
    protected void initDbSqlSessionFactoryEntitySettings() {
        defaultInitDbSqlSessionFactoryEntitySettings(EntityDependencyOrder.INSERT_ORDER, EntityDependencyOrder.DELETE_ORDER);
    }

    @Override
    public DbSqlSessionFactory createDbSqlSessionFactory() {
        return new DbSqlSessionFactory(usePrefixId);
    }

    // command executors
    // ////////////////////////////////////////////////////////

    @Override
    public void initCommandExecutors() {
        initDefaultCommandConfig();
        initSchemaCommandConfig();
        initCommandInvoker();
        initCommandInterceptors();
        initCommandExecutor();
    }

    @Override
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

    @Override
    public String getEngineCfgKey() {
        return EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG;
    }
    
    @Override
    public String getEngineScopeType() {
        return ScopeTypes.FORM;
    }

    @Override
    public CommandInterceptor createTransactionInterceptor() {
        return null;
    }

    // deployers
    // ////////////////////////////////////////////////////////////////

    protected void initDeployers() {
        if (formParseFactory == null) {
            formParseFactory = new FormDefinitionParseFactory();
        }

        if (this.formDeployer == null) {
            this.deployers = new ArrayList<>();
            if (customPreDeployers != null) {
                this.deployers.addAll(customPreDeployers);
            }
            this.deployers.addAll(getDefaultDeployers());
            if (customPostDeployers != null) {
                this.deployers.addAll(customPostDeployers);
            }
        }

        // Decision cache
        if (formDefinitionCache == null) {
            if (formDefinitionCacheLimit <= 0) {
                formDefinitionCache = new DefaultDeploymentCache<>();
            } else {
                formDefinitionCache = new DefaultDeploymentCache<>(formDefinitionCacheLimit);
            }
        }

        deploymentManager = new DeploymentManager(formDefinitionCache, this);
        deploymentManager.setDeployers(deployers);
        deploymentManager.setDeploymentEntityManager(deploymentEntityManager);
        deploymentManager.setFormDefinitionEntityManager(formDefinitionEntityManager);
    }

    public Collection<? extends Deployer> getDefaultDeployers() {
        List<Deployer> defaultDeployers = new ArrayList<>();

        if (formDeployer == null) {
            formDeployer = new FormDefinitionDeployer();
        }

        initDmnDeployerDependencies();

        formDeployer.setIdGenerator(idGenerator);
        formDeployer.setParsedDeploymentBuilderFactory(parsedDeploymentBuilderFactory);
        formDeployer.setFormDeploymentHelper(formDeploymentHelper);
        formDeployer.setCachingAndArtifactsManager(cachingAndArtifactsManager);
        formDeployer.setUsePrefixId(usePrefixId);

        defaultDeployers.add(formDeployer);
        return defaultDeployers;
    }

    public void initDmnDeployerDependencies() {
        if (parsedDeploymentBuilderFactory == null) {
            parsedDeploymentBuilderFactory = new ParsedDeploymentBuilderFactory();
        }
        if (parsedDeploymentBuilderFactory.getFormParseFactory() == null) {
            parsedDeploymentBuilderFactory.setFormParseFactory(formParseFactory);
        }

        if (formDeploymentHelper == null) {
            formDeploymentHelper = new FormDefinitionDeploymentHelper();
        }

        if (cachingAndArtifactsManager == null) {
            cachingAndArtifactsManager = new CachingAndArtifactsManager();
        }
    }

    // myBatis SqlSessionFactory
    // ////////////////////////////////////////////////

    @Override
    public InputStream getMyBatisXmlConfigurationStream() {
        return getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public String getEngineName() {
        return formEngineName;
    }

    public FormEngineConfiguration setEngineName(String formEngineName) {
        this.formEngineName = formEngineName;
        return this;
    }

    @Override
    public FormManagementService getFormManagementService() {
        return formManagementService;
    }

    public FormEngineConfiguration setFormManagementService(FormManagementService formManagementService) {
        this.formManagementService = formManagementService;
        return this;
    }

    @Override
    public FormRepositoryService getFormRepositoryService() {
        return formRepositoryService;
    }

    public FormEngineConfiguration setFormRepositoryService(FormRepositoryService formRepositoryService) {
        this.formRepositoryService = formRepositoryService;
        return this;
    }

    @Override
    public FormService getFormService() {
        return formService;
    }

    public FormEngineConfiguration setFormService(FormService formService) {
        this.formService = formService;
        return this;
    }

    public ChangeTenantIdManager getChangeTenantIdManager() {
        return changeTenantIdManager;
    }

    public FormEngineConfiguration setChangeTenantIdManager(ChangeTenantIdManager changeTenantIdManager) {
        this.changeTenantIdManager = changeTenantIdManager;
        return this;
    }

    public DeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    public FormEngineConfiguration getFormEngineConfiguration() {
        return this;
    }

    public FormDefinitionDeployer getFormDeployer() {
        return formDeployer;
    }

    public FormEngineConfiguration setFormDeployer(FormDefinitionDeployer formDeployer) {
        this.formDeployer = formDeployer;
        return this;
    }

    public FormDefinitionParseFactory getFormParseFactory() {
        return formParseFactory;
    }

    public FormEngineConfiguration setFormParseFactory(FormDefinitionParseFactory formParseFactory) {
        this.formParseFactory = formParseFactory;
        return this;
    }

    public int getFormCacheLimit() {
        return formDefinitionCacheLimit;
    }

    public FormEngineConfiguration setFormDefinitionCacheLimit(int formDefinitionCacheLimit) {
        this.formDefinitionCacheLimit = formDefinitionCacheLimit;
        return this;
    }

    public DeploymentCache<FormDefinitionCacheEntry> getFormDefinitionCache() {
        return formDefinitionCache;
    }

    public FormEngineConfiguration setFormDefinitionCache(DeploymentCache<FormDefinitionCacheEntry> formDefinitionCache) {
        this.formDefinitionCache = formDefinitionCache;
        return this;
    }

    public FormDeploymentDataManager getDeploymentDataManager() {
        return deploymentDataManager;
    }

    public FormEngineConfiguration setDeploymentDataManager(FormDeploymentDataManager deploymentDataManager) {
        this.deploymentDataManager = deploymentDataManager;
        return this;
    }

    public FormDefinitionDataManager getFormDefinitionDataManager() {
        return formDefinitionDataManager;
    }

    public FormEngineConfiguration setFormDefinitionDataManager(FormDefinitionDataManager formDefinitionDataManager) {
        this.formDefinitionDataManager = formDefinitionDataManager;
        return this;
    }

    public FormResourceDataManager getResourceDataManager() {
        return resourceDataManager;
    }

    public FormEngineConfiguration setResourceDataManager(FormResourceDataManager resourceDataManager) {
        this.resourceDataManager = resourceDataManager;
        return this;
    }

    public FormInstanceDataManager getFormInstanceDataManager() {
        return formInstanceDataManager;
    }

    public FormEngineConfiguration setFormInstanceDataManager(FormInstanceDataManager formInstanceDataManager) {
        this.formInstanceDataManager = formInstanceDataManager;
        return this;
    }

    public FormDeploymentEntityManager getDeploymentEntityManager() {
        return deploymentEntityManager;
    }

    public FormEngineConfiguration setDeploymentEntityManager(FormDeploymentEntityManager deploymentEntityManager) {
        this.deploymentEntityManager = deploymentEntityManager;
        return this;
    }

    public FormDefinitionEntityManager getFormDefinitionEntityManager() {
        return formDefinitionEntityManager;
    }

    public FormEngineConfiguration setFormDefinitionEntityManager(FormDefinitionEntityManager formDefinitionEntityManager) {
        this.formDefinitionEntityManager = formDefinitionEntityManager;
        return this;
    }

    public FormResourceEntityManager getResourceEntityManager() {
        return resourceEntityManager;
    }

    public FormEngineConfiguration setResourceEntityManager(FormResourceEntityManager resourceEntityManager) {
        this.resourceEntityManager = resourceEntityManager;
        return this;
    }

    public FormInstanceEntityManager getFormInstanceEntityManager() {
        return formInstanceEntityManager;
    }

    public FormEngineConfiguration setFormInstanceEntityManager(FormInstanceEntityManager formInstanceEntityManager) {
        this.formInstanceEntityManager = formInstanceEntityManager;
        return this;
    }

    @Override
    public FormEngineConfiguration setTableDataManager(TableDataManager tableDataManager) {
        this.tableDataManager = tableDataManager;
        return this;
    }

    @Override
    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    @Override
    public FormEngineConfiguration setExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
        return this;
    }

    public Collection<ELResolver> getPreDefaultELResolvers() {
        return preDefaultELResolvers;
    }

    public FormEngineConfiguration setPreDefaultELResolvers(Collection<ELResolver> preDefaultELResolvers) {
        this.preDefaultELResolvers = preDefaultELResolvers;
        return this;
    }

    public FormEngineConfiguration addPreDefaultELResolver(ELResolver elResolver) {
        if (this.preDefaultELResolvers == null) {
            this.preDefaultELResolvers = new ArrayList<>();
        }

        this.preDefaultELResolvers.add(elResolver);
        return this;
    }

    public Collection<ELResolver> getPreBeanELResolvers() {
        return preBeanELResolvers;
    }

    public FormEngineConfiguration setPreBeanELResolvers(Collection<ELResolver> preBeanELResolvers) {
        this.preBeanELResolvers = preBeanELResolvers;
        return this;
    }

    public FormEngineConfiguration addPreBeanELResolver(ELResolver elResolver) {
        if (this.preBeanELResolvers == null) {
            this.preBeanELResolvers = new ArrayList<>();
        }

        this.preBeanELResolvers.add(elResolver);
        return this;
    }

    public Collection<ELResolver> getPostDefaultELResolvers() {
        return postDefaultELResolvers;
    }

    public FormEngineConfiguration setPostDefaultELResolvers(Collection<ELResolver> postDefaultELResolvers) {
        this.postDefaultELResolvers = postDefaultELResolvers;
        return this;
    }

    public FormEngineConfiguration addPostDefaultELResolver(ELResolver elResolver) {
        if (this.postDefaultELResolvers == null) {
            this.postDefaultELResolvers = new ArrayList<>();
        }

        this.postDefaultELResolvers.add(elResolver);
        return this;
    }

    public FormJsonConverter getFormJsonConverter() {
        return formJsonConverter;
    }

    public FormEngineConfiguration setFormJsonConverter(FormJsonConverter formJsonConverter) {
        this.formJsonConverter = formJsonConverter;
        return this;
    }
}
