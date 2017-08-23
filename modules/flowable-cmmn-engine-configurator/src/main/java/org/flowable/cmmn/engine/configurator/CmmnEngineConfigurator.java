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
package org.flowable.cmmn.engine.configurator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.PlanItemInstanceCallbackType;
import org.flowable.cmmn.engine.configurator.impl.deployer.CmmnDeployer;
import org.flowable.cmmn.engine.configurator.impl.process.DefaultProcessInstanceService;
import org.flowable.cmmn.engine.impl.callback.ChildProcessInstanceStateChangeCallback;
import org.flowable.cmmn.engine.impl.cfg.StandaloneInMemCmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.db.EntityDependencyOrder;
import org.flowable.engine.cfg.AbstractEngineConfigurator;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.deploy.Deployer;

/**
 * @author Joram Barrez
 */
public class CmmnEngineConfigurator extends AbstractEngineConfigurator {

    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected CmmnEngine cmmnEngine;
    
    @Override
    public int getPriority() {
        return EngineConfigurationConstants.PRIORITY_ENGINE_CMMN;
    }
    
    @Override
    protected List<Deployer> getCustomDeployers() {
        return Arrays.<Deployer>asList(new CmmnDeployer(this));
    }
    
    @Override
    protected String getMybatisCfgPath() {
        return CmmnEngineConfiguration.DEFAULT_MYBATIS_MAPPING_FILE;
    }
    
    @Override
    public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (cmmnEngineConfiguration == null) {
            cmmnEngineConfiguration = new StandaloneInMemCmmnEngineConfiguration();
        }

        initialiseCommonProperties(processEngineConfiguration, cmmnEngineConfiguration);
        initProcessInstanceService(processEngineConfiguration);
        initProcessInstanceStateChangedCallbacks(processEngineConfiguration);
        
        this.cmmnEngine = initCmmnEngine();
    }
    
    protected void initProcessInstanceService(ProcessEngineConfigurationImpl processEngineConfiguration) {
        cmmnEngineConfiguration.setProcessInstanceService(new DefaultProcessInstanceService(processEngineConfiguration.getRuntimeService()));
    }

    protected void initProcessInstanceStateChangedCallbacks(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (processEngineConfiguration.getProcessInstanceStateChangedCallbacks() == null) {
            processEngineConfiguration.setProcessInstanceStateChangedCallbacks(new HashMap<String, List<RuntimeInstanceStateChangeCallback>>());
        }
        Map<String, List<RuntimeInstanceStateChangeCallback>> callbacks = processEngineConfiguration.getProcessInstanceStateChangedCallbacks();
        if (!callbacks.containsKey(PlanItemInstanceCallbackType.CHILD_PROCESS)) {
            callbacks.put(PlanItemInstanceCallbackType.CHILD_PROCESS, new ArrayList<RuntimeInstanceStateChangeCallback>());
        }
        callbacks.get(PlanItemInstanceCallbackType.CHILD_PROCESS).add(new ChildProcessInstanceStateChangeCallback(cmmnEngineConfiguration));
    }

    @Override
    protected List<Class<? extends Entity>> getEntityInsertionOrder() {
        return EntityDependencyOrder.INSERT_ORDER;
    }
    
    @Override
    protected List<Class<? extends Entity>> getEntityDeletionOrder() {
        return EntityDependencyOrder.DELETE_ORDER;
    }

    protected synchronized CmmnEngine initCmmnEngine() {
        if (cmmnEngineConfiguration == null) {
            throw new FlowableException("CmmnEngineConfiguration is required");
        }

        return cmmnEngineConfiguration.buildCmmnEngine();
    }

    public CmmnEngineConfiguration getCmmnEngineConfiguration() {
        return cmmnEngineConfiguration;
    }

    public CmmnEngineConfigurator setCmmnEngineConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        return this;
    }

    public CmmnEngine getCmmnEngine() {
        return cmmnEngine;
    }

    public void setCmmnEngine(CmmnEngine cmmnEngine) {
        this.cmmnEngine = cmmnEngine;
    }
    
}
