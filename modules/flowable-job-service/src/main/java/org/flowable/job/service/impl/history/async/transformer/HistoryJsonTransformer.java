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
package org.flowable.job.service.impl.history.async.transformer;

import java.util.List;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface HistoryJsonTransformer {
    
    String FIELD_NAME_TYPE = "type";
    String FIELD_NAME_DATA = "data";

    List<String> getTypes();

    boolean isApplicable(ObjectNode historicalData, CommandContext commandContext);

    void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext);

}
