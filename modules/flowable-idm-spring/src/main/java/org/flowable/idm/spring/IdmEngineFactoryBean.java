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

package org.flowable.idm.spring;

import org.flowable.common.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngineConfiguration;
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
public class IdmEngineFactoryBean implements FactoryBean<IdmEngine>, DisposableBean, ApplicationContextAware {

    protected IdmEngineConfiguration idmEngineConfiguration;

    protected ApplicationContext applicationContext;
    protected IdmEngine idmEngine;

    @Override
    public void destroy() throws Exception {
        if (idmEngine != null) {
            idmEngine.close();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public IdmEngine getObject() throws Exception {
        configureExternallyManagedTransactions();
        
        if (idmEngineConfiguration.getBeans() == null) {
            idmEngineConfiguration.setBeans(new SpringBeanFactoryProxyMap(applicationContext));
        }

        this.idmEngine = idmEngineConfiguration.buildIdmEngine();
        return this.idmEngine;
    }

    protected void configureExternallyManagedTransactions() {
        if (idmEngineConfiguration instanceof SpringIdmEngineConfiguration) { // remark: any config can be injected, so we cannot have SpringConfiguration as member
            SpringIdmEngineConfiguration engineConfiguration = (SpringIdmEngineConfiguration) idmEngineConfiguration;
            if (engineConfiguration.getTransactionManager() != null) {
                idmEngineConfiguration.setTransactionsExternallyManaged(true);
            }
        }
    }

    @Override
    public Class<IdmEngine> getObjectType() {
        return IdmEngine.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public IdmEngineConfiguration getIdmEngineConfiguration() {
        return idmEngineConfiguration;
    }

    public void setIdmEngineConfiguration(IdmEngineConfiguration idmEngineConfiguration) {
        this.idmEngineConfiguration = idmEngineConfiguration;
    }
}
