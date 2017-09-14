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
package org.flowable.engine.impl.persistence.entity.data;

import org.flowable.engine.common.impl.db.AbstractDataManager;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.flowable.engine.common.runtime.Clock;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * @author Joram Barrez
 */
public abstract class AbstractProcessDataManager<EntityImpl extends Entity> extends AbstractDataManager<EntityImpl> {
    
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public AbstractProcessDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }
    
    protected ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }
    
    protected Clock getClock() {
        return processEngineConfiguration.getClock();
    }

}
