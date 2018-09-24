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
package org.flowable.spring.boot;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * A base class that can be used by any configuration that needs to inject specific {@link EngineConfigurationConfigurer}(s).
 *
 * @author Filip Hrisafov
 */
public abstract class BaseEngineConfigurationWithConfigurers<T> {

    protected List<EngineConfigurationConfigurer<T>> engineConfigurers = new ArrayList<>();

    protected void invokeConfigurers(T engineConfiguration) {
        engineConfigurers.forEach(configurer -> configurer.configure(engineConfiguration));
    }

    @Autowired(required = false)
    public void setEngineConfigurers(List<EngineConfigurationConfigurer<T>> engineConfigurers) {
        this.engineConfigurers = engineConfigurers;
    }
}
