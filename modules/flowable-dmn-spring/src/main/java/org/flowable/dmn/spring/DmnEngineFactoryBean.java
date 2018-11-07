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

package org.flowable.dmn.spring;

import org.flowable.common.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
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
public class DmnEngineFactoryBean implements FactoryBean<DmnEngine>, DisposableBean, ApplicationContextAware {

    protected DmnEngineConfiguration dmnEngineConfiguration;

    protected ApplicationContext applicationContext;
    protected DmnEngine dmnEngine;

    @Override
    public void destroy() throws Exception {
        if (dmnEngine != null) {
            dmnEngine.close();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public DmnEngine getObject() throws Exception {
        configureExpressionManager();
        configureExternallyManagedTransactions();
        
        if (dmnEngineConfiguration.getBeans() == null) {
            dmnEngineConfiguration.setBeans(new SpringBeanFactoryProxyMap(applicationContext));
        }

        this.dmnEngine = dmnEngineConfiguration.buildDmnEngine();
        return this.dmnEngine;
    }
    
    protected void configureExpressionManager() {
        if (dmnEngineConfiguration.getExpressionManager() == null && applicationContext != null) {
            dmnEngineConfiguration.setExpressionManager(new SpringDmnExpressionManager(applicationContext, dmnEngineConfiguration.getBeans()));
        }
    }

    protected void configureExternallyManagedTransactions() {
        if (dmnEngineConfiguration instanceof SpringDmnEngineConfiguration) { // remark: any config can be injected, so we cannot have SpringConfiguration as member
            SpringDmnEngineConfiguration engineConfiguration = (SpringDmnEngineConfiguration) dmnEngineConfiguration;
            if (engineConfiguration.getTransactionManager() != null) {
                dmnEngineConfiguration.setTransactionsExternallyManaged(true);
            }
        }
    }

    @Override
    public Class<DmnEngine> getObjectType() {
        return DmnEngine.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public DmnEngineConfiguration getDmnEngineConfiguration() {
        return dmnEngineConfiguration;
    }

    public void setDmnEngineConfiguration(DmnEngineConfiguration dmnEngineConfiguration) {
        this.dmnEngineConfiguration = dmnEngineConfiguration;
    }
}
