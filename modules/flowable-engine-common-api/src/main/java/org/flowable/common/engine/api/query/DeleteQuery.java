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
package org.flowable.common.engine.api.query;

/**
 * Describes methods for Queries that can perform delete operations.
 *
 * @author Filip Hrisafov
 */
public interface DeleteQuery<T extends DeleteQuery<T, U>, U> {

    /**
     * Delete all instances that match the query.
     */
    void delete();

    /**
     * Delete all instances and their related data that match the query in bulk.
     *
     * Note that only the historical data directly related to the instance will be deleted.
     *
     * This means that for example for historic case/process instances, it will not delete
     * any associated historic case/process instance (these should be deleted through
     * the respective deletion for the particular model) which is owned by another engine (e.g. a process for the cmmn engine).
     * Use the specific deletion methods on the respective history services otherwise: they delete with cascading to all
     * other engines, but are not as performant as the bulk delete here.
     */
    void deleteWithRelatedData();

}
