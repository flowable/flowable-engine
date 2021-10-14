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

import java.util.Date;

public interface BatchPart {

    /**
     * The technical id of the batch part
     */
    String getId();

    /**
     * The type of the batch part
     */
    String getType();

    /**
     * The type of the batch that this batch part is linked to
     */
    String getBatchType();

    /**
     * The id of the batch that this batch part is linked to
     */
    String getBatchId();

    /**
     * The time the batch part was created
     */
    Date getCreateTime();

    /**
     * The time the batch part was completed
     */
    Date getCompleteTime();

    /**
     * Whether the batch part is completed
     */
    boolean isCompleted();

    /**
     * The search key of the batch part
     */
    String getSearchKey();

    /**
     * The second search key of the batch part
     */
    String getSearchKey2();

    /**
     * The search key of the batch that this batch part is linked to
     */
    String getBatchSearchKey();

    /**
     * The second search key of the batch that this batch part is linked to
     */
    String getBatchSearchKey2();
    
    /**
     * The status of the batch part
     */
    String getStatus();

    /**
     * The scope id of the batch part
     */
    String getScopeId();
    
    /**
     * The scope type of the batch part
     */
    String getScopeType();
    
    /**
     * The sub scope id of the batch part
     */
    String getSubScopeId();

    /**
     * The result document of the batch part
     */
    String getResultDocumentJson(String engineType);
    
    /**
     * The tenant id of the batch part
     */
    String getTenantId();
}