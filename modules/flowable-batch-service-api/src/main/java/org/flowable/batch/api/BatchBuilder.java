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
package org.flowable.batch.api;

public interface BatchBuilder {

    BatchBuilder batchType(String batchType);
    
    BatchBuilder searchKey(String searchKey);
    
    BatchBuilder searchKey2(String searchKey2);
    
    BatchBuilder status(String status);
    
    BatchBuilder batchDocumentJson(String batchDocumentJson);
    
    BatchBuilder tenantId(String tenantId);
    
    Batch create();
    
    String getBatchType();
    
    String getSearchKey();
    
    String getSearchKey2();
    
    String getStatus();
    
    String getBatchDocumentJson();
    
    String getTenantId();
}
