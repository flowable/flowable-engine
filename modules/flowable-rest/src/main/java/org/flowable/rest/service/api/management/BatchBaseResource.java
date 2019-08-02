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

package org.flowable.rest.service.api.management;

import org.flowable.batch.api.Batch;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.ManagementService;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

public class BatchBaseResource {

    @Autowired
    protected ManagementService managementService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    protected Batch getBatchById(String batchId) {
        Batch batch = managementService.createBatchQuery().batchId(batchId).singleResult();
        validateBatch(batch, batchId);
        return batch;
    }
    
    protected void validateBatch(Batch batch, String batchId) {
        if (batch == null) {
            throw new FlowableObjectNotFoundException("Could not find a batch with id '" + batchId + "'.", Batch.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessBatchInfoById(batch);
        }
    }
}
