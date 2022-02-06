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
package org.flowable.dmn.engine.impl.agenda.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.DecisionServiceExecutionAuditContainer;
import org.flowable.dmn.api.ExecuteDecisionContext;
import org.flowable.dmn.engine.impl.audit.DecisionExecutionAuditUtil;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.InformationRequirement;

public class ExecuteDecisionServiceOperation extends DmnOperation {

    protected final DecisionService decisionService;
    protected final ExecuteDecisionContext executeDecisionContext;

    public ExecuteDecisionServiceOperation(CommandContext commandContext, ExecuteDecisionContext executeDecisionContext, DecisionService decisionService) {
        super(commandContext);
        this.executeDecisionContext = executeDecisionContext;
        this.decisionService = decisionService;
    }

    @Override
    public void run() {
        DecisionServiceExecutionAuditContainer auditContainer = DecisionExecutionAuditUtil.initializeDecisionServiceExecutionAudit(decisionService, executeDecisionContext);
        executeDecisionContext.setDecisionExecution(auditContainer);

        planExecuteDecisionOperationsForDecisionService();
    }

    protected void planExecuteDecisionOperationsForDecisionService() {
        List<Decision> encapsulatedDecisions = decisionService.getEncapsulatedDecisions()
            .stream()
            .map(ref -> decisionService.getDmnDefinition().getDecisionById(ref.getParsedId()))
            .collect(Collectors.toList());

        List<Decision> outputDecisions = decisionService.getOutputDecisions()
            .stream()
            .map(ref -> decisionService.getDmnDefinition().getDecisionById(ref.getParsedId()))
            .collect(Collectors.toList());

        List<Decision> orderedOutputDecisions = determineDecisionExecutionOrder(encapsulatedDecisions, outputDecisions);

        orderedOutputDecisions.forEach(decision -> CommandContextUtil.getAgenda(commandContext).planExecuteDecisionOperation(executeDecisionContext, decision));
    }

    protected List<Decision> determineDecisionExecutionOrder(List<Decision> encapsulatedDecisions, List<Decision> outputDecisions) {
        List<Decision> combinedDecisions = Stream.of(encapsulatedDecisions, outputDecisions)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        return determineDecisionExecutionOrder(combinedDecisions);
    }

    protected List<Decision> determineDecisionExecutionOrder(List<Decision> allDecisions) {
        List<Decision> order = new ArrayList<>();
        LinkedList<Decision> sortDecisions = new LinkedList<>();

        Map<String, Decision> decisionsById = new HashMap<>();
        Map<String, Boolean> visited = new HashMap<>();

        for (Decision sortDecision : allDecisions) {
            decisionsById.put(sortDecision.getId(), sortDecision);
            visited.put(sortDecision.getId(), false);
            if (sortDecision.getRequiredDecisions().isEmpty()) {
                sortDecisions.addFirst(sortDecision);
            } else {
                sortDecisions.addLast(sortDecision);
            }
        }

        for (Decision decision : sortDecisions) {
            if (!visited.get(decision.getId())) {
                executeSort(decisionsById, decision.getId(), visited, order);
            }
        }

        return order;
    }

    private void executeSort(Map<String, Decision> decisions, String decisionId, Map<String, Boolean> visited, List<Decision> order) {
        if (!decisions.containsKey(decisionId)) {
            throw new FlowableObjectNotFoundException("Required decision " + decisionId + " is not available");
        }

        // Mark the current node as visited
        visited.replace(decisionId, true);

        // We reuse the algorithm on all adjacent nodes to the current node
        for (InformationRequirement requiredDecision : decisions.get(decisionId).getRequiredDecisions()) {
            if (!visited.get(requiredDecision.getRequiredDecision().getParsedId())) {
                executeSort(decisions, requiredDecision.getRequiredDecision().getParsedId(), visited, order);
            }
        }

        // Put the current node in the array
        order.add(decisions.get(decisionId));
    }
}
