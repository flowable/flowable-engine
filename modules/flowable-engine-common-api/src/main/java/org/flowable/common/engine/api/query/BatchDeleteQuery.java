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
 * @author Filip Hrisafov
 */
public interface BatchDeleteQuery<Q extends BatchDeleteQuery<Q>> {

    /**
     * Perform the deletion in parallel using the given batch size.
     * Doing the deletion in parallel means that multiple batches can be deleted in the same time
     *
     * @param batchSize the size of each batch deletion
     * @param batchName the name of the batch
     * @return the id the batch that is doing the deletion
     */
    String deleteInParallelUsingBatch(int batchSize, String batchName);

    /**
     * Perform the deletion sequentially using the given batch size.
     * Doing the deletion sequentially means that the deletion will be done one batch at a time.
     *
     * @param batchSize the size of each batch deletion
     * @param batchName the name of the batch
     * @return the id the batch that is doing the deletion
     */
    String deleteSequentiallyUsingBatch(int batchSize, String batchName);

}
