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
package org.flowable.cdi;

import org.flowable.cdi.impl.CdiCommandInvoker;
import org.flowable.engine.impl.cfg.JtaProcessEngineConfiguration;

/**
 * @author Daniel Meyer
 */
public class CdiJtaProcessEngineConfiguration extends JtaProcessEngineConfiguration {

    @Override
    public void initExpressionManager() {
        expressionManager = new CdiExpressionManager(this);
    }

    @Override
    public void initCommandInvoker() {
        if (commandInvoker == null) {
            commandInvoker = new CdiCommandInvoker();
        }
    }

}
