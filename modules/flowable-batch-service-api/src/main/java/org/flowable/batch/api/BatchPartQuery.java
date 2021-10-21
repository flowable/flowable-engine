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

import org.flowable.common.engine.api.query.Query;

public interface BatchPartQuery extends Query<BatchPartQuery, BatchPart> {

    /**
     * Only select batch parts with the given id
     */
    BatchPartQuery id(String id);

    /**
     * Only select batch parts with the given batch id
     */
    BatchPartQuery batchId(String batchId);

    /**
     * Only select batch parts with the given type.
     **/
    BatchPartQuery type(String type);

    /**
     * Only select batch parts with the given search key.
     **/
    BatchPartQuery searchKey(String searchKey);

    /**
     * Only select batch parts with the given search key.
     **/
    BatchPartQuery searchKey2(String searchKey2);

    /**
     * Only select batch parts with the given batch type.
     **/
    BatchPartQuery batchType(String batchType);

    /**
     * Only select batch parts with the given batch search key.
     **/
    BatchPartQuery batchSearchKey(String searchKey);

    /**
     * Only select batch parts with the batch given search key.
     **/
    BatchPartQuery batchSearchKey2(String searchKey2);

    /**
     * Only select batch parts with the given status.
     **/
    BatchPartQuery status(String status);

    /**
     * Only select batch parts with the given scope id.
     **/
    BatchPartQuery scopeId(String scopeId);

    /**
     * Only select batch parts with the given sub scope id.
     **/
    BatchPartQuery subScopeId(String subScopeId);

    /**
     * Only select batch parts with the given scope type.
     **/
    BatchPartQuery scopeType(String scopeType);

    /**
     * Only select batch parts with the given tenant id.
     **/
    BatchPartQuery tenantId(String tenantId);

    /**
     * Only select batch parts with a tenant id like the given one.
     **/
    BatchPartQuery tenantIdLike(String tenantIdLike);

    /**
     * Only select batch parts that do not have a tenant id.
     **/
    BatchPartQuery withoutTenantId();

    /**
     * Only select batch parts that have been completed.
     **/
    BatchPartQuery completed();

    // sorting //////////////////////////////////////////

    /**
     * Order by batch id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    BatchPartQuery orderByBatchId();

    /**
     * Order by batch part create time (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    BatchPartQuery orderByCreateTime();
}
