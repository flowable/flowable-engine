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

import org.flowable.batch.api.BatchPart;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.ManagementService;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

public class BatchPartBaseResource {

    @Autowired
    protected ManagementService managementService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    protected BatchPart getBatchPartById(String batchPartId) {
        BatchPart batchPart = managementService.getBatchPart(batchPartId);
        validateBatchPart(batchPart, batchPartId);
        return batchPart;
    }
    
    protected void validateBatchPart(BatchPart batchPart, String batchPartId) {
        if (batchPart == null) {
            throw new FlowableObjectNotFoundException("Could not find a batch part with id '" + batchPartId + "'.", BatchPart.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessBatchPartInfoById(batchPart);
        }
    }
}
