package org.flowable.dmn.engine.impl.agenda.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.engine.impl.ExecuteDecisionContext;
import org.flowable.dmn.engine.impl.audit.DecisionExecutionAuditUtil;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.InformationRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecuteDecisionServiceOperation extends DmnOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteDecisionServiceOperation.class);
    protected final DecisionService decisionService;
    protected final ExecuteDecisionContext executeDecisionContext;

    public ExecuteDecisionServiceOperation(CommandContext commandContext, ExecuteDecisionContext executeDecisionContext, DecisionService decisionService) {
        super(commandContext);
        this.executeDecisionContext = executeDecisionContext;
        this.decisionService = decisionService;
    }

    @Override
    public void run() {
        LOGGER.debug("#### Executing decision service operation {}", decisionService.getId());

        planExecuteDecisionOperationsForDecisionService();

        LOGGER.debug("#### Finished decision service operation {}", decisionService.getId());
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

        orderedOutputDecisions.forEach(decision -> {
            ExecuteDecisionContext childExecuteDecisionContext = new ExecuteDecisionContext(decision, executeDecisionContext);

            LOGGER.debug("#### Planning execute decision operation for decision {}", decision.getId());
            CommandContextUtil.getAgenda(commandContext).planExecuteDecisionOperation(childExecuteDecisionContext, decision);
        });
    }

    protected List<Decision> determineDecisionExecutionOrder(List<Decision> encapsulatedDecisions, List<Decision> outputDecisions) {
        List<Decision> combinedDecisions = Stream.of(encapsulatedDecisions, outputDecisions)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        return determineDecisionExecutionOrder(combinedDecisions);
    }

    protected List<Decision> determineDecisionExecutionOrder(List<Decision> sortDecisions) {
        List<Decision> order = new ArrayList<>();

        Map<String, Decision> decisionsById = new HashMap<>();
        Map<String, Boolean> visited = new HashMap<>();

        for (Decision sortDecision : sortDecisions) {
            decisionsById.put(sortDecision.getId(), sortDecision);
            visited.put(sortDecision.getId(), false);
        }

        for (Decision decision : sortDecisions) {
            if (!visited.get(decision.getId()))
                executeSort(decisionsById, decision.getId(), visited, order);
        }

        Collections.reverse(order);
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
            if (!visited.get(requiredDecision.getRequiredDecision().getParsedId()))
                executeSort(decisions, requiredDecision.getRequiredDecision().getParsedId(), visited, order);
        }

        // Put the current node in the array
        order.add(decisions.get(decisionId));
    }
}
