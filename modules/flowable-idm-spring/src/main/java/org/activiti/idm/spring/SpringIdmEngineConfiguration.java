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

package org.activiti.idm.spring;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.interceptor.CommandConfig;
import org.activiti.idm.engine.IdmEngineConfiguration;
import org.activiti.idm.engine.impl.cfg.StandaloneIdmEngineConfiguration;
import org.activiti.idm.engine.impl.interceptor.CommandInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Tijs Rademakers
 * @author David Syer
 * @author Joram Barrez
 */
public class SpringIdmEngineConfiguration extends IdmEngineConfiguration implements ApplicationContextAware {

  protected PlatformTransactionManager transactionManager;
  protected ApplicationContext applicationContext;
  protected Integer transactionSynchronizationAdapterOrder = null;

  public SpringIdmEngineConfiguration() {
    this.transactionsExternallyManaged = true;
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
      throw new ActivitiException("transactionManager is required property for SpringIdmEngineConfiguration, use " + StandaloneIdmEngineConfiguration.class.getName() + " otherwise");
    }

    return new SpringTransactionInterceptor(transactionManager);
  }

  @Override
  public void initTransactionContextFactory() {
    if (transactionContextFactory == null && transactionManager != null) {
      transactionContextFactory = new SpringTransactionContextFactory(transactionManager, transactionSynchronizationAdapterOrder);
    }
  }

  public PlatformTransactionManager getTransactionManager() {
    return transactionManager;
  }

  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
