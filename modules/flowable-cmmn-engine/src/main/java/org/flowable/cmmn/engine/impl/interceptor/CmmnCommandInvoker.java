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
package org.flowable.cmmn.engine.impl.interceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgenda;
import org.flowable.cmmn.engine.impl.agenda.operation.CmmnOperation;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.agenda.AgendaOperationExecutionListener;
import org.flowable.common.engine.impl.agenda.AgendaOperationRunner;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.AbstractCommandInterceptor;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.util.ExceptionUtil;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSession;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CmmnCommandInvoker extends AbstractCommandInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnCommandInvoker.class);

    protected AgendaOperationRunner agendaOperationRunner;
    protected Collection<AgendaOperationExecutionListener> agendaOperationExecutionListeners;

    public CmmnCommandInvoker(AgendaOperationRunner agendaOperationRunner, Collection<AgendaOperationExecutionListener> agendaOperationExecutionListeners) {
        this.agendaOperationRunner = agendaOperationRunner;
        this.agendaOperationExecutionListeners = agendaOperationExecutionListeners;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T execute(final CommandConfig config, final Command<T> command, final CommandExecutor commandExecutor) {
        final CommandContext commandContext = Context.getCommandContext();
        final CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
        if (commandContext.isReused() && !agenda.isEmpty()) {
            commandContext.setResult(command.execute(commandContext));
        } else {

            executeExecutionListenersBeforeAll(commandContext);

            agenda.planOperation(() -> commandContext.setResult(command.execute(commandContext)));
            executeOperations(commandContext, true); // true -> always store the case instance id for the regular operation loop, even if it's a no-op operation.

            if (commandContext.isRootUsageOfCurrentEngine()) {

                Set<String> allHandledCaseInstanceIds = new HashSet<>();
                commandContext.addAttribute("ctx.attribute.allHandledCaseInstanceIds", allHandledCaseInstanceIds);

                evaluateUntilStable(commandContext, allHandledCaseInstanceIds);
                executeExecutionListenersAfterAll(commandContext);

            }
        }
        
        return (T) commandContext.getResult();
    }

    protected void executeOperations(CommandContext commandContext, boolean isStoreCaseInstanceIdOfNoOperation) {
        CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
        while (!agenda.isEmpty()) {
            Runnable runnable = agenda.getNextOperation();
            executeExecutionListenersBeforeExecute(commandContext, runnable);
            try {
                executeOperation(commandContext, isStoreCaseInstanceIdOfNoOperation, runnable);
            } catch (Throwable throwable) {
                executeExecutionListenersAfterException(commandContext, runnable, throwable);
                ExceptionUtil.sneakyThrow(throwable);
            }
            executeExecutionListenersAfterExecute(commandContext, runnable);
        }
    }

    protected void executeExecutionListenersBeforeAll(CommandContext commandContext) {
        if (agendaOperationExecutionListeners != null && !agendaOperationExecutionListeners.isEmpty()) {
            for (AgendaOperationExecutionListener listener : agendaOperationExecutionListeners) {
                listener.beforeAll(commandContext);
            }
        }
    }

    protected void executeExecutionListenersAfterAll(CommandContext commandContext) {
        if (agendaOperationExecutionListeners != null && !agendaOperationExecutionListeners.isEmpty()) {
            for (AgendaOperationExecutionListener listener : agendaOperationExecutionListeners) {
                listener.afterAll(commandContext);
            }
        }
    }

    protected void executeExecutionListenersBeforeExecute(CommandContext commandContext, Runnable runnable) {
        if (agendaOperationExecutionListeners != null && !agendaOperationExecutionListeners.isEmpty()) {
            for (AgendaOperationExecutionListener listener : agendaOperationExecutionListeners) {
                listener.beforeExecute(commandContext, runnable);
            }
        }
    }

    protected void executeExecutionListenersAfterExecute(CommandContext commandContext, Runnable runnable) {
        if (agendaOperationExecutionListeners != null && !agendaOperationExecutionListeners.isEmpty()) {
            for (AgendaOperationExecutionListener listener : agendaOperationExecutionListeners) {
                listener.afterExecute(commandContext, runnable);
            }
        }
    }

    protected void executeExecutionListenersAfterException(CommandContext commandContext, Runnable runnable, Throwable throwable) {
        if (agendaOperationExecutionListeners != null && !agendaOperationExecutionListeners.isEmpty()) {
            for (AgendaOperationExecutionListener listener : agendaOperationExecutionListeners) {
                listener.afterExecuteException(commandContext, runnable, throwable);
            }
        }
    }

    protected void executeOperation(CommandContext commandContext, boolean isStoreCaseInstanceIdOfNoOperation, Runnable runnable) {

        if (runnable instanceof CmmnOperation operation) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing agenda operation {}", runnable);
            }

            agendaOperationRunner.executeOperation(commandContext, runnable);

            // If the operation caused changes, a new evaluation needs to be planned,
            // as the operations could have changed the state and/or variables.
            if (isStoreCaseInstanceIdOfNoOperation || !operation.isNoop()) {

                String caseInstanceId = operation.getCaseInstanceId();
                if (caseInstanceId != null) {
                    CommandContextUtil.addInvolvedCaseInstanceId(commandContext, caseInstanceId);
                }

            }

        } else {
            runnable.run();

        }
    }

    protected void evaluateUntilStable(CommandContext commandContext, Set<String> allHandledCaseInstanceIds) {
        Set<String> involvedCaseInstanceIds = CommandContextUtil.getInvolvedCaseInstanceIds(commandContext);
        if (involvedCaseInstanceIds != null && !involvedCaseInstanceIds.isEmpty()) {

            allHandledCaseInstanceIds.addAll(involvedCaseInstanceIds);

            CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);

            for (String caseInstanceId : involvedCaseInstanceIds) {
                VariableListenerSession variableListenerSession = commandContext.getSession(VariableListenerSession.class);
                Map<String, List<VariableListenerSessionData>> variableSessionData = variableListenerSession.getVariableData();
                
                if (variableSessionData != null) {
                    List<String> variableListenerCaseInstanceIds = new ArrayList<>();
                    for (String variableName : variableSessionData.keySet()) {
                        List<VariableListenerSessionData> variableListenerDataList = variableSessionData.get(variableName);
                        for (VariableListenerSessionData variableListenerData : variableListenerDataList) {
                            if (!variableListenerCaseInstanceIds.contains(variableListenerData.getScopeId()) && 
                                    caseInstanceId.equals(variableListenerData.getScopeId())) {
                                
                                variableListenerCaseInstanceIds.add(variableListenerData.getScopeId());
                                agenda.planEvaluateVariableEventListenersOperation(variableListenerData.getScopeId());
                            }
                        }
                    }
                }
                
                agenda.planEvaluateCriteriaOperation(caseInstanceId, true);
            }

            involvedCaseInstanceIds.clear(); // Clearing after scheduling the evaluation. If anything changes, new operations will add ids again.
            executeOperations(commandContext, false); // false -> here, we're past the regular operation loop. Any operation now that is a no-op should not reschedule a new evaluation

            // If new involvedCaseInstanceIds have new entries, this means the evaluation has triggered new operations and data has changed.
            // Need to retrigger the evaluations to make sure no new things can fire now.
            if (!involvedCaseInstanceIds.isEmpty()) {
                evaluateUntilStable(commandContext, allHandledCaseInstanceIds);
            }
        }
    }

    @Override
    public void setNext(CommandInterceptor next) {
        throw new UnsupportedOperationException("CommandInvoker must be the last interceptor in the chain");
    }

    public AgendaOperationRunner getAgendaOperationRunner() {
        return agendaOperationRunner;
    }

    public void setAgendaOperationRunner(AgendaOperationRunner agendaOperationRunner) {
        this.agendaOperationRunner = agendaOperationRunner;
    }

}
