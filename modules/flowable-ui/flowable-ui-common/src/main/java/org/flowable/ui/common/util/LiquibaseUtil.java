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
package org.flowable.ui.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.flowable.ui.common.service.liquibase.FlowableUiHubService;

import liquibase.Scope;
import liquibase.ui.LoggerUIService;

/**
 * @author Filip Hrisafov
 */
public class LiquibaseUtil {

    private static final String LIQUIBASE_HUB_SERVICE_CLASS_NAME = "liquibase.hub.HubService";
    protected static final Map<String, Object> LIQUIBASE_SCOPE_VALUES = new HashMap<>();

    static {
        LIQUIBASE_SCOPE_VALUES.put("liquibase.plugin." + LIQUIBASE_HUB_SERVICE_CLASS_NAME, FlowableUiHubService.class);
        LoggerUIService uiService = new LoggerUIService();
        uiService.setStandardLogLevel(Level.FINE);
        LIQUIBASE_SCOPE_VALUES.put(Scope.Attr.ui.name(), uiService);
    }

    public static <T> T runInFlowableScope(Callable<T> callable) throws Exception {
        return Scope.child(LIQUIBASE_SCOPE_VALUES, callable::call);
    }

}
