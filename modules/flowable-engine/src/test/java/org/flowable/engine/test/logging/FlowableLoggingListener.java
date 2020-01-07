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
package org.flowable.engine.test.logging;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.impl.logging.LoggingListener;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class FlowableLoggingListener implements LoggingListener {
    
    public static List<ObjectNode> TEST_LOGGING_NODES = new ArrayList<>();

    @Override
    public void loggingGenerated(List<ObjectNode> loggingNodes) {
        TEST_LOGGING_NODES.addAll(loggingNodes);
    }

    public static void clear() {
        TEST_LOGGING_NODES.clear();
    }
}
