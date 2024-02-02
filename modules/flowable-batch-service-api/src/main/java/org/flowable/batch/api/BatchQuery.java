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

import java.util.Collection;
import java.util.Date;

import org.flowable.common.engine.api.query.DeleteQuery;
import org.flowable.common.engine.api.query.Query;

public interface BatchQuery extends Query<BatchQuery, Batch>, DeleteQuery<BatchQuery, Batch> {

    /** Only select batches with the given id */
    BatchQuery batchId(String batchId);
    
    /** Only select batches which exist for the given type. **/
    BatchQuery batchType(String batchType);
    
    /** Only select batches which exist for the given types. **/
    BatchQuery batchTypes(Collection<String> batchTypes);
    
    /** Only select batches which exist for the given search key. **/
    BatchQuery searchKey(String searchKey);
    
    /** Only select batches which exist for the given search key. **/
    BatchQuery searchKey2(String searchKey2);
    
    /** Only select batches where the create time is lower than the given date. */
    BatchQuery createTimeLowerThan(Date date);

    /** Only select batches where the create time is higher then the given date. */
    BatchQuery createTimeHigherThan(Date date);
    
    /** Only select batches where the complete time is lower than the given date. */
    BatchQuery completeTimeLowerThan(Date date);

    /** Only select batches where the complete time is higher then the given date. */
    BatchQuery completeTimeHigherThan(Date date);
    
    /** Only select batches which exist for the given status. **/
    BatchQuery status(String status);
    
    /** Only select batches which exist for the given tenant id. **/
    BatchQuery tenantId(String tenantId);
    
    /** Only select batches with a tenant id like the given one. **/
    BatchQuery tenantIdLike(String tenantIdLike);

    /** Only select batches that do not have a tenant id. **/
    BatchQuery withoutTenantId();

    // sorting //////////////////////////////////////////

    /**
     * Order by batch id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    BatchQuery orderByBatchId();

    /**
     * Order by batch create time (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    BatchQuery orderByBatchCreateTime();
    
    /**
     * Order by batch create time (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    BatchQuery orderByBatchTenantId();

}
