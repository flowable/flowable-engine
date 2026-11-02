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
package org.flowable.cmmn.engine.impl;

import java.util.function.Consumer;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.engine.EngineLifecycleListener;

/**
 * @author Filip Hrisafov
 */
public class CmmnEnginePostEngineBuildConsumer implements Consumer<CmmnEngine> {

    @Override
    public void accept(CmmnEngine cmmnEngine) {
        CmmnEngineConfiguration engineConfiguration = cmmnEngine.getCmmnEngineConfiguration();
        if (engineConfiguration.getEngineLifecycleListeners() != null) {
            for (EngineLifecycleListener engineLifecycleListener : engineConfiguration.getEngineLifecycleListeners()) {
                engineLifecycleListener.onEngineBuilt(cmmnEngine);
            }
        }

        if (engineConfiguration.isHandleCmmnEngineExecutorsAfterEngineCreate()) {
            cmmnEngine.startExecutors();
        }
    }
}
