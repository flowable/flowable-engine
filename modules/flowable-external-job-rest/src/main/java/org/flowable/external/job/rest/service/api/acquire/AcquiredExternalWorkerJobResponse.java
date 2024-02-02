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
package org.flowable.external.job.rest.service.api.acquire;

import java.util.List;

import org.flowable.common.rest.variable.EngineRestVariable;
import org.flowable.external.job.rest.service.api.query.ExternalWorkerJobResponse;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Filip Hrisafov
 */
public class AcquiredExternalWorkerJobResponse extends ExternalWorkerJobResponse {

    @ApiModelProperty(value = "The variables from the scope of the job")
    protected List<EngineRestVariable> variables;

    public List<EngineRestVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<EngineRestVariable> variables) {
        this.variables = variables;
    }
}
