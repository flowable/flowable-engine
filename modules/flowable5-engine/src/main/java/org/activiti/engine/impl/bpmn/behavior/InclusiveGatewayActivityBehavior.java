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
package org.activiti.engine.impl.bpmn.behavior;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.Condition;
import org.activiti.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Inclusive Gateway/OR gateway/inclusive data-based gateway as defined in the BPMN specification.
 *
 * @author Tijs Rademakers
 * @author Tom Van Buskirk
 * @author Joram Barrez
 */
public class InclusiveGatewayActivityBehavior extends GatewayActivityBehavior {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(InclusiveGatewayActivityBehavior.class.getName());

    @Override
    public void execute(DelegateExecution execution) {
        ActivityExecution activityExecution = (ActivityExecution) execution;
        activityExecution.inactivate();
        lockConcurrentRoot(activityExecution);

        PvmActivity activity = activityExecution.getActivity();
        if (!activeConcurrentExecutionsExist(activityExecution)) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("inclusive gateway '{}' activates", activity.getId());
            }

            List<ActivityExecution> joinedExecutions = activityExecution.findInactiveConcurrentExecutions(activity);
            String defaultSequenceFlow = (String) activityExecution.getActivity().getProperty("default");
            List<PvmTransition> transitionsToTake = new ArrayList<>();

            for (PvmTransition outgoingTransition : activityExecution.getActivity().getOutgoingTransitions()) {

                Expression skipExpression = outgoingTransition.getSkipExpression();
                if (!SkipExpressionUtil.isSkipExpressionEnabled(activityExecution, skipExpression)) {
                    if (defaultSequenceFlow == null || !outgoingTransition.getId().equals(defaultSequenceFlow)) {
                        Condition condition = (Condition) outgoingTransition.getProperty(BpmnParse.PROPERTYNAME_CONDITION);
                        if (condition == null || condition.evaluate(outgoingTransition.getId(), execution)) {
                            transitionsToTake.add(outgoingTransition);
                        }
                    }
                } else if (SkipExpressionUtil.shouldSkipFlowElement(activityExecution, skipExpression)) {
                    transitionsToTake.add(outgoingTransition);
                }
            }

            if (!transitionsToTake.isEmpty()) {
                activityExecution.takeAll(transitionsToTake, joinedExecutions);

            } else {

                if (defaultSequenceFlow != null) {
                    PvmTransition defaultTransition = activityExecution.getActivity().findOutgoingTransition(defaultSequenceFlow);
                    if (defaultTransition != null) {
                        activityExecution.take(defaultTransition);
                    } else {
                        throw new ActivitiException("Default sequence flow '"
                                + defaultSequenceFlow + "' could not be not found");
                    }
                } else {
                    // No sequence flow could be found, not even a default one
                    throw new ActivitiException(
                            "No outgoing sequence flow of the inclusive gateway '"
                                    + activityExecution.getActivity().getId()
                                    + "' could be selected for continuing the process");
                }
            }

        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Inclusive gateway '{}' does not activate", activity.getId());
            }
        }
    }

    List<? extends ActivityExecution> getLeaveExecutions(ActivityExecution parent) {
        List<ActivityExecution> executionlist = new ArrayList<>();
        List<? extends ActivityExecution> subExecutions = parent.getExecutions();
        if (subExecutions.isEmpty()) {
            executionlist.add(parent);
        } else {
            for (ActivityExecution concurrentExecution : subExecutions) {
                executionlist.addAll(getLeaveExecutions(concurrentExecution));
            }
        }
        return executionlist;
    }

    public boolean activeConcurrentExecutionsExist(ActivityExecution execution) {
        PvmActivity activity = execution.getActivity();
        if (execution.isConcurrent()) {
            for (ActivityExecution concurrentExecution : getLeaveExecutions(execution.getParent())) {
                if (concurrentExecution.isActive() && !concurrentExecution.getId().equals(execution.getId())) {
                    // TODO: when is transitionBeingTaken cleared? Should we clear it?
                    boolean reachable = false;
                    PvmTransition pvmTransition = ((ExecutionEntity) concurrentExecution).getTransitionBeingTaken();
                    if (pvmTransition != null) {
                        reachable = isReachable(pvmTransition.getDestination(), activity, new HashSet<>());
                    } else {
                        reachable = isReachable(concurrentExecution.getActivity(), activity, new HashSet<>());
                    }

                    if (reachable) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("an active concurrent execution found: '{}'", concurrentExecution.getActivity());
                        }
                        return true;
                    }
                }
            }
        } else if (execution.isActive()) { // is this ever true?
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("an active concurrent execution found: '{}'", execution.getActivity());
            }
            return true;
        }

        return false;
    }

    protected boolean isReachable(PvmActivity srcActivity,
                                  PvmActivity targetActivity, Set<PvmActivity> visitedActivities) {

        // if source has no outputs, it is the end of the process, and its parent process should be checked.
        if (srcActivity.getOutgoingTransitions().isEmpty()) {
            visitedActivities.add(srcActivity);
            if (!(srcActivity.getParent() instanceof PvmActivity)) {
                return false;
            }
            srcActivity = (PvmActivity) srcActivity.getParent();
        }

        if (srcActivity.equals(targetActivity)) {
            return true;
        }

        // To avoid infinite looping, we must capture every node we visit
        // and check before going further in the graph if we have already visited
        // the node.
        visitedActivities.add(srcActivity);

        List<PvmTransition> transitionList = srcActivity.getOutgoingTransitions();
        if (transitionList != null && !transitionList.isEmpty()) {
            for (PvmTransition pvmTransition : transitionList) {
                PvmActivity destinationActivity = pvmTransition.getDestination();
                if (destinationActivity != null && !visitedActivities.contains(destinationActivity)) {
                    boolean reachable = isReachable(destinationActivity, targetActivity, visitedActivities);

                    // If false, we should investigate other paths, and not yet return the
                    // result
                    if (reachable) {
                        return true;
                    }

                }
            }
        }
        return false;
    }

}
