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
package org.flowable.engine.test.impl.logger;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.engine.cfg.AbstractProcessEngineConfigurator;

/**
 * @author jbarrez
 */
public class ProcessExecutionLoggerConfigurator extends AbstractProcessEngineConfigurator {

    protected ProcessExecutionLogger processExecutionLogger;

    @Override
    public void beforeInit(AbstractEngineConfiguration engineConfiguration) {
        processExecutionLogger = new ProcessExecutionLogger();
        engineConfiguration.setCommandInvoker(new LoggingCommandInvoker(processExecutionLogger));
    }

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        engineConfiguration.getEventDispatcher().addEventListener(new DebugInfoEntityEventListener(processExecutionLogger));
    }

    public ProcessExecutionLogger getProcessExecutionLogger() {
        return processExecutionLogger;
    }

    public void setProcessExecutionLogger(ProcessExecutionLogger processExecutionLogger) {
        this.processExecutionLogger = processExecutionLogger;
    }

}
