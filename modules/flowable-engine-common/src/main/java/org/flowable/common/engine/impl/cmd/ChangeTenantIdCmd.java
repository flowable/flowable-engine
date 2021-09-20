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

package org.flowable.common.engine.impl.cmd;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.tenant.ChangeTenantIdRequest;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.tenant.DefaultChangeTenantIdResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeTenantIdCmd implements Command<ChangeTenantIdResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeTenantIdCmd.class);

    private final ChangeTenantIdRequest changeTenantIdRequest;
    private final Map<String, Function<ChangeTenantIdRequest, Long>> mapOfEntitiesAndFunctions;

    public ChangeTenantIdCmd(ChangeTenantIdRequest changeTenantIdRequest,
            Map<String, Function<ChangeTenantIdRequest, Long>> mapOfEntitiesAndFunctions) {
        this.changeTenantIdRequest = changeTenantIdRequest;
        this.mapOfEntitiesAndFunctions = mapOfEntitiesAndFunctions;
    }

    @Override
    public ChangeTenantIdResult execute(CommandContext commandContext) {
        if (changeTenantIdRequest.getSourceTenantId().equals(changeTenantIdRequest.getTargetTenantId())) {
            throw new FlowableException("The source and the target tenant ids must be different.");
        }

        String operation = changeTenantIdRequest.isDryRun() ? "Simulating" : "Executing";
        String option = changeTenantIdRequest.getOnlyInstancesFromDefaultTenantDefinitions()
                ? " but only for instances from the default tenant definitions"
                : "";
        LOGGER.debug("{} case instance migration from '{}' to '{}'{}.", operation,
                changeTenantIdRequest.getSourceTenantId(), changeTenantIdRequest.getTargetTenantId(), option);

        Map<String, Long> resultMap = mapOfEntitiesAndFunctions.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> {
                    LOGGER.debug("Processing {}.", e.getKey());
                    return e.getValue().apply(changeTenantIdRequest);
                }));
        return new DefaultChangeTenantIdResult(resultMap);
    }

}