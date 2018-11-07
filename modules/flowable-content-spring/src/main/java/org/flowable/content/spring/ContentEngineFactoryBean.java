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

package org.flowable.content.spring;

import org.flowable.common.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngineConfiguration;
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
public class ContentEngineFactoryBean implements FactoryBean<ContentEngine>, DisposableBean, ApplicationContextAware {

    protected ContentEngineConfiguration contentEngineConfiguration;

    protected ApplicationContext applicationContext;
    protected ContentEngine contentEngine;

    @Override
    public void destroy() throws Exception {
        if (contentEngine != null) {
            contentEngine.close();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public ContentEngine getObject() throws Exception {
        configureExternallyManagedTransactions();
        
        if (contentEngineConfiguration.getBeans() == null) {
            contentEngineConfiguration.setBeans(new SpringBeanFactoryProxyMap(applicationContext));
        }

        this.contentEngine = contentEngineConfiguration.buildContentEngine();
        return this.contentEngine;
    }

    protected void configureExternallyManagedTransactions() {
        if (contentEngineConfiguration instanceof SpringContentEngineConfiguration) { // remark: any config can be injected, so we cannot have SpringConfiguration as member
            SpringContentEngineConfiguration engineConfiguration = (SpringContentEngineConfiguration) contentEngineConfiguration;
            if (engineConfiguration.getTransactionManager() != null) {
                contentEngineConfiguration.setTransactionsExternallyManaged(true);
            }
        }
    }

    @Override
    public Class<ContentEngine> getObjectType() {
        return ContentEngine.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public ContentEngineConfiguration getContentEngineConfiguration() {
        return contentEngineConfiguration;
    }

    public void setContentEngineConfiguration(ContentEngineConfiguration contentEngineConfiguration) {
        this.contentEngineConfiguration = contentEngineConfiguration;
    }
}
