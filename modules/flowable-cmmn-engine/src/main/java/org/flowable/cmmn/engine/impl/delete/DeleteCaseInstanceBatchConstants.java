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
package org.flowable.cmmn.engine.impl.delete;

/**
 * @author Filip Hrisafov
 */
public interface DeleteCaseInstanceBatchConstants {

    String STATUS_IN_PROGRESS = "inProgress";
    String STATUS_WAITING = "waiting";
    String STATUS_COMPLETED = "completed";
    String STATUS_FAILED = "failed";

    String BATCH_PART_COMPUTE_IDS_TYPE = "computeDeleteCaseInstanceIds";
    String BATCH_PART_DELETE_CASE_INSTANCES_TYPE = "deleteCase";
}
