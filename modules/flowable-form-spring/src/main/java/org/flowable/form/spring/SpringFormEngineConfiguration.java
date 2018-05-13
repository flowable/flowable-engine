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

package org.flowable.form.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.spring.SpringEngineConfiguration;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.FormEngines;
import org.flowable.form.engine.impl.cfg.StandaloneFormEngineConfiguration;
import org.flowable.form.spring.autodeployment.AutoDeploymentStrategy;
import org.flowable.form.spring.autodeployment.DefaultAutoDeploymentStrategy;
import org.flowable.form.spring.autodeployment.ResourceParentFolderAutoDeploymentStrategy;
import org.flowable.form.spring.autodeployment.SingleResourceAutoDeploymentStrategy;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Tijs Rademakers
 * @author David Syer
 * @author Joram Barrez
 */
public class SpringFormEngineConfiguration extends FormEngineConfiguration implements SpringEngineConfiguration {

    protected PlatformTransactionManager transactionManager;
    protected String deploymentName = "SpringAutoDeployment";
    protected Resource[] deploymentResources = new Resource[0];
    protected String deploymentMode = "default";
    protected ApplicationContext applicationContext;
    protected Integer transactionSynchronizationAdapterOrder;
    private Collection<AutoDeploymentStrategy> deploymentStrategies = new ArrayList<>();
    protected volatile boolean running = false;
    protected List<String> enginesBuild = new ArrayList<>();
    protected final Object lifeCycleMonitor = new Object();

    public SpringFormEngineConfiguration() {
        this.transactionsExternallyManaged = true;
        deploymentStrategies.add(new DefaultAutoDeploymentStrategy());
        deploymentStrategies.add(new SingleResourceAutoDeploymentStrategy());
        deploymentStrategies.add(new ResourceParentFolderAutoDeploymentStrategy());
    }

    @Override
    public FormEngine buildFormEngine() {
        FormEngine formEngine = super.buildFormEngine();
        FormEngines.setInitialized(true);
        enginesBuild.add(formEngine.getName());
        return formEngine;
    }

    public void setTransactionSynchronizationAdapterOrder(Integer transactionSynchronizationAdapterOrder) {
        this.transactionSynchronizationAdapterOrder = transactionSynchronizationAdapterOrder;
    }

    @Override
    public void initDefaultCommandConfig() {
        if (defaultCommandConfig == null) {
            defaultCommandConfig = new CommandConfig().setContextReusePossible(true);
        }
    }

    @Override
    public CommandInterceptor createTransactionInterceptor() {
        if (transactionManager == null) {
            throw new FlowableException("transactionManager is required property for SpringFormEngineConfiguration, use " + StandaloneFormEngineConfiguration.class.getName() + " otherwise");
        }

        return new SpringTransactionInterceptor(transactionManager);
    }

    @Override
    public void initTransactionContextFactory() {
        if (transactionContextFactory == null && transactionManager != null) {
            transactionContextFactory = new SpringTransactionContextFactory(transactionManager, transactionSynchronizationAdapterOrder);
        }
    }

    protected void autoDeployResources(FormEngine formEngine) {
        if (deploymentResources != null && deploymentResources.length > 0) {
            final AutoDeploymentStrategy strategy = getAutoDeploymentStrategy(deploymentMode);
            strategy.deployResources(deploymentName, deploymentResources, formEngine.getFormRepositoryService());
        }
    }

    @Override
    public FormEngineConfiguration setDataSource(DataSource dataSource) {
        if (dataSource instanceof TransactionAwareDataSourceProxy) {
            return (FormEngineConfiguration) super.setDataSource(dataSource);
        } else {
            // Wrap datasource in Transaction-aware proxy
            DataSource proxiedDataSource = new TransactionAwareDataSourceProxy(dataSource);
            return (FormEngineConfiguration) super.setDataSource(proxiedDataSource);
        }
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public String getDeploymentName() {
        return deploymentName;
    }

    @Override
    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    @Override
    public Resource[] getDeploymentResources() {
        return deploymentResources;
    }

    @Override
    public void setDeploymentResources(Resource[] deploymentResources) {
        this.deploymentResources = deploymentResources;
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public String getDeploymentMode() {
        return deploymentMode;
    }

    @Override
    public void setDeploymentMode(String deploymentMode) {
        this.deploymentMode = deploymentMode;
    }

    /**
     * Gets the {@link AutoDeploymentStrategy} for the provided mode. This method may be overridden to implement custom deployment strategies if required, but implementors should take care not to
     * return <code>null</code>.
     * 
     * @param mode
     *            the mode to get the strategy for
     * @return the deployment strategy to use for the mode. Never <code>null</code>
     */
    protected AutoDeploymentStrategy getAutoDeploymentStrategy(final String mode) {
        AutoDeploymentStrategy result = new DefaultAutoDeploymentStrategy();
        for (final AutoDeploymentStrategy strategy : deploymentStrategies) {
            if (strategy.handlesMode(mode)) {
                result = strategy;
                break;
            }
        }
        return result;
    }

    @Override
    public void start() {
        synchronized (lifeCycleMonitor) {
            if (!isRunning()) {
                enginesBuild.forEach(name -> autoDeployResources(FormEngines.getFormEngine(name)));
                running = true;
            }
        }
    }

    @Override
    public void stop() {
        synchronized (lifeCycleMonitor) {
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return SpringEngineConfiguration.super.getPhase() - SpringEngineConfiguration.PHASE_DELTA * 2;
    }
}
