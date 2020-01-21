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

package org.activiti.compatibility.spring;

import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.activiti.spring.SpringExpressionManager;
import org.flowable.compatibility.DefaultProcessEngineFactory;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.spring.SpringProcessEngineConfiguration;

public class DefaultSpringProcessEngineFactory extends DefaultProcessEngineFactory {

    /**
     * Takes in an V6 process engine config, gives back an V5 Process engine.
     */
    @Override
    public ProcessEngine buildProcessEngine(ProcessEngineConfigurationImpl v6Configuration) {

        org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl v5Configuration = null;
        if (v6Configuration instanceof SpringProcessEngineConfiguration) {
            v5Configuration = new org.activiti.spring.SpringProcessEngineConfiguration();
            super.copyConfigItems(v6Configuration, v5Configuration);
            copySpringConfigItems((SpringProcessEngineConfiguration) v6Configuration, (org.activiti.spring.SpringProcessEngineConfiguration) v5Configuration);
            return v5Configuration.buildProcessEngine();

        } else {
            return super.buildProcessEngine(v6Configuration);
        }

    }

    protected void copySpringConfigItems(SpringProcessEngineConfiguration v6Configuration, org.activiti.spring.SpringProcessEngineConfiguration v5Configuration) {
        v5Configuration.setApplicationContext(v6Configuration.getApplicationContext());
        v5Configuration.setTransactionManager(v6Configuration.getTransactionManager());

        Map<Object, Object> beans = v6Configuration.getBeans();

        if (!(beans instanceof org.flowable.common.engine.impl.cfg.SpringBeanFactoryProxyMap)) {
            v5Configuration.setBeans(new SpringBeanFactoryProxyMap(v6Configuration.getApplicationContext()));
        }
        if (v5Configuration.getExpressionManager() == null) {
            v5Configuration.setExpressionManager(new SpringExpressionManager(v6Configuration.getApplicationContext(), v6Configuration.getBeans()));
        }
    }

}
