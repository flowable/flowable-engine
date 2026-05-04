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
package org.flowable.engine.impl.jobexecutor;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author Tijs Rademakers
 */
public class AsyncSendEventJobHandler implements JobHandler {

    public static final String TYPE = "async-send-event";

    /**
     * CommandContext attribute key under which the parsed snapshot ({@link JsonNode}) is exposed
     * to {@code SendEventTaskActivityBehavior}. The behavior dispatches from the snapshot instead of
     * re-evaluating expressions in the worker thread.
     */
    public static final String SNAPSHOT_ATTRIBUTE = "async-send-event.snapshot";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ExecutionEntity executionEntity = (ExecutionEntity) variableScope;
        FlowElement flowElement = executionEntity.getCurrentFlowElement();

        if (!(flowElement instanceof SendEventServiceTask)) {
            throw new FlowableException("Unexpected activity type found for " + job + " at " + executionEntity);
        }

        Object behavior = ((SendEventServiceTask) flowElement).getBehavior();
        if (!(behavior instanceof ActivityBehavior activityBehavior)) {
            throw new FlowableException(
                    "Unexpected activity behavior (" + behavior.getClass() + ") found for " + job + " at " + executionEntity);
        }

        // The snapshot was captured at scheduling time when the original variables, authenticated
        // user, beans and any other thread-local context were available. Hand it to the behavior so
        // it dispatches the resolved values verbatim — no expression is re-evaluated in this worker
        // thread. Jobs scheduled by an older engine version do not carry a snapshot; for those we
        // fall back to the legacy behavior of re-resolving in the worker (matches pre-fix semantics).
        JsonNode snapshot = readSnapshot(job, commandContext);
        boolean snapshotAttached = false;
        if (snapshot != null) {
            commandContext.addAttribute(SNAPSHOT_ATTRIBUTE, snapshot);
            snapshotAttached = true;
        }

        try {
            commandContext.addAttribute(TYPE, true); // Will be read in the SendEventTaskActivityBehavior
            activityBehavior.execute(executionEntity);
        } finally {
            commandContext.removeAttribute(TYPE);
            if (snapshotAttached) {
                commandContext.removeAttribute(SNAPSHOT_ATTRIBUTE);
            }
        }
    }

    protected JsonNode readSnapshot(JobEntity job, CommandContext commandContext) {
        String snapshotJson = job.getCustomValues();
        if (StringUtils.isEmpty(snapshotJson)) {
            return null;
        }
        ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getObjectMapper();
        try {
            return objectMapper.readTree(snapshotJson);
        } catch (JacksonException e) {
            throw new FlowableException("Could not read async send-event snapshot for " + job, e);
        }
    }

}
