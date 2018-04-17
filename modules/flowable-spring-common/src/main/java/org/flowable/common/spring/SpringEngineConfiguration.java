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

package org.flowable.common.spring;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Tijs Rademakers
 */
public interface SpringEngineConfiguration extends ApplicationContextAware, SmartLifecycle {

    int PHASE = 0;

    int PHASE_DELTA = 10;

    PlatformTransactionManager getTransactionManager();

    void setTransactionManager(PlatformTransactionManager transactionManager);

    String getDeploymentName();

    void setDeploymentName(String deploymentName);

    Resource[] getDeploymentResources();

    void setDeploymentResources(Resource[] deploymentResources);

    ApplicationContext getApplicationContext();

    void setApplicationContext(ApplicationContext applicationContext);
    
    Map<Object, Object> getBeans();

    String getDeploymentMode();

    void setDeploymentMode(String deploymentMode);

    @Override
    default boolean isAutoStartup() {
        return true;
    }

    @Override
    default void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    default int getPhase() {
        return PHASE;
    }
}
