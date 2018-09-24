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

package org.flowable.app.spring;

import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.common.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Dave Syer
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Josh Long
 */
public class AppEngineFactoryBean implements FactoryBean<AppEngine>, DisposableBean, ApplicationContextAware {

    protected AppEngineConfiguration appEngineConfiguration;

    protected ApplicationContext applicationContext;
    protected AppEngine appEngine;

    @Override
    public void destroy() throws Exception {
        if (appEngine != null) {
            appEngine.close();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public AppEngine getObject() throws Exception {
        configureExpressionManager();
        configureExternallyManagedTransactions();
        
        if (appEngineConfiguration.getBeans() == null) {
            appEngineConfiguration.setBeans(new SpringBeanFactoryProxyMap(applicationContext));
        }

        this.appEngine = appEngineConfiguration.buildAppEngine();
        return this.appEngine;
    }
    
    protected void configureExpressionManager() {
        if (appEngineConfiguration.getExpressionManager() == null && applicationContext != null) {
            appEngineConfiguration.setExpressionManager(new SpringAppExpressionManager(applicationContext, appEngineConfiguration.getBeans()));
        }
    }

    protected void configureExternallyManagedTransactions() {
        if (appEngineConfiguration instanceof SpringAppEngineConfiguration) { // remark: any config can be injected, so we cannot have SpringConfiguration as member
            SpringAppEngineConfiguration engineConfiguration = (SpringAppEngineConfiguration) appEngineConfiguration;
            if (engineConfiguration.getTransactionManager() != null) {
                appEngineConfiguration.setTransactionsExternallyManaged(true);
            }
        }
    }

    @Override
    public Class<AppEngine> getObjectType() {
        return AppEngine.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public AppEngineConfiguration getAppEngineConfiguration() {
        return appEngineConfiguration;
    }

    public void setAppEngineConfiguration(AppEngineConfiguration appEngineConfiguration) {
        this.appEngineConfiguration = appEngineConfiguration;
    }
}
