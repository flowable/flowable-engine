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

package org.flowable.cdi.impl.util;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.DynamicCmmnService;
import org.flowable.cmmn.engine.CmmnEngine;

/**
 * Makes the managed cmmn engine and the provided services available for injection
 *
 * @author Andy Verberne
 * @since 6.6.1
 */
@ApplicationScoped
public class FlowableCmmnServices {

    private CmmnEngine cmmnEngine;

    public void setCmmnEngine(CmmnEngine cmmnEngine) {
        this.cmmnEngine = cmmnEngine;
    }

    @Produces
    @Named
    @ApplicationScoped
    public CmmnEngine cmmnEngine() {
        return cmmnEngine;
    }

    @Produces
    @Named
    @ApplicationScoped
    public CmmnRuntimeService cmmnRuntimeService() {
        return cmmnEngine().getCmmnRuntimeService();
    }

    @Produces
    @Named
    @ApplicationScoped
    public CmmnTaskService cmmnTaskService() {
        return cmmnEngine().getCmmnTaskService();
    }

    @Produces
    @Named
    @ApplicationScoped
    public CmmnRepositoryService cmmnRepositoryService() {
        return cmmnEngine().getCmmnRepositoryService();
    }

    @Produces
    @Named
    @ApplicationScoped
    public DynamicCmmnService dynamicCmmnService() {
        return cmmnEngine().getDynamicCmmnService();
    }

    @Produces
    @Named
    @ApplicationScoped
    public CmmnHistoryService cmmnHistoryService() {
        return cmmnEngine().getCmmnHistoryService();
    }

    @Produces
    @Named
    @ApplicationScoped
    public CmmnManagementService cmmnManagementService() {
        return cmmnEngine().getCmmnManagementService();
    }
}
