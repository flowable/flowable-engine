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

package org.flowable.engine.impl;

import org.flowable.common.engine.api.query.QueryProperty;

/**
 * @author Daniel Meyer
 */
public class EventSubscriptionQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    // properties used in event subscription queries:

    public static final EventSubscriptionQueryProperty ID = new EventSubscriptionQueryProperty("RES.ID_");
    public static final EventSubscriptionQueryProperty EXECUTION_ID = new EventSubscriptionQueryProperty("RES.EXECUTION_ID_");
    public static final EventSubscriptionQueryProperty PROCESS_INSTANCE_ID = new EventSubscriptionQueryProperty("RES.PROC_INST_ID_");
    public static final EventSubscriptionQueryProperty PROCESS_DEFINITION_ID = new EventSubscriptionQueryProperty("RES.PROC_DEF_ID_");
    public static final EventSubscriptionQueryProperty CREATED = new EventSubscriptionQueryProperty("RES.CREATED_");
    public static final EventSubscriptionQueryProperty TENANT_ID = new EventSubscriptionQueryProperty("RES.TENANT_ID_");

    // ///////////////////////////////////////////////

    private final String propertyName;

    public EventSubscriptionQueryProperty(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public String getName() {
        return propertyName;
    }

}
