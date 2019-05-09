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
package org.flowable.ui.task.conf;

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.api.runtime.CmmnDebugger;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgendaFactory;
import org.flowable.cmmn.engine.impl.agenda.DebugCmmnEngineAgendaFactory;
import org.flowable.cmmn.engine.impl.job.ActivateCmmnBreakpointJobHandler;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.engine.FlowableEngineAgendaFactory;
import org.flowable.engine.impl.agenda.DebugFlowableEngineAgendaFactory;
import org.flowable.engine.impl.event.BreakpointJobHandler;
import org.flowable.engine.runtime.ProcessDebugger;
import org.flowable.job.service.impl.asyncexecutor.AsyncRunnableExecutionExceptionHandler;
import org.flowable.job.service.impl.asyncexecutor.DefaultDebuggerExecutionExceptionHandler;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration for the process debugger.
 *
 * @author Filip Hrisafov
 */
@Configuration
@EnableConfigurationProperties(FlowableDebuggerProperties.class)
@ConditionalOnProperty(prefix = "flowable.experimental.debugger", name = "enabled", havingValue = "true")
@AutoConfigureBefore(ProcessEngineAutoConfiguration.class)
public class FlowableDebuggerConfiguration {

    @Bean
    public FlowableEngineAgendaFactory processDebuggerAgendaFactory(ProcessDebugger processDebugger) {
        DebugFlowableEngineAgendaFactory debugAgendaFactory = new DebugFlowableEngineAgendaFactory();
        debugAgendaFactory.setDebugger(processDebugger);
        return debugAgendaFactory;
    }

    @Bean
    public CmmnEngineAgendaFactory cmmnDebuggerAgendaFactory(CmmnDebugger debugger) {
        DebugCmmnEngineAgendaFactory debugAgendaFactory = new DebugCmmnEngineAgendaFactory(debugger);
        return debugAgendaFactory;
    }

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> debuggerProcessEngineConfigurationConfigurer(
        FlowableEngineAgendaFactory agendaFactory) {
        return processEngineConfiguration -> {
            processEngineConfiguration.setEnableDatabaseEventLogging(true);
            processEngineConfiguration.setAgendaFactory(agendaFactory);
            processEngineConfiguration.addCustomJobHandler(new BreakpointJobHandler());

            List<AsyncRunnableExecutionExceptionHandler> customAsyncRunnableExecutionExceptionHandlers = processEngineConfiguration.getCustomAsyncRunnableExecutionExceptionHandlers();
            ArrayList<AsyncRunnableExecutionExceptionHandler> exceptionHandlers;
            if (customAsyncRunnableExecutionExceptionHandlers == null) {
                exceptionHandlers = new ArrayList<>();
            } else {
                exceptionHandlers = new ArrayList<>(customAsyncRunnableExecutionExceptionHandlers);
            }
            exceptionHandlers.add(new DefaultDebuggerExecutionExceptionHandler());
            processEngineConfiguration.setCustomAsyncRunnableExecutionExceptionHandlers(exceptionHandlers);
        };
    }

    @Bean
    public EngineConfigurationConfigurer<SpringCmmnEngineConfiguration> debuggerCmmnEngineConfigurationConfigurer(
        CmmnEngineAgendaFactory agendaFactory) {
        return cmmnEngineConfiguration -> {
            cmmnEngineConfiguration.setCmmnEngineAgendaFactory(agendaFactory);
            cmmnEngineConfiguration.addCustomJobHandler(new ActivateCmmnBreakpointJobHandler());
        };
    }

}
