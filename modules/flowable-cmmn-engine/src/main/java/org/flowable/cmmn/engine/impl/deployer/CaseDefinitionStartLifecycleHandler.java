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
package org.flowable.cmmn.engine.impl.deployer;

/**
 * Implemented by an opt-in handler attached to a {@code Case} that owns a deploy-time start trigger
 * (event-registry subscription, timer cron, custom subscription type, etc.). The
 * {@link CmmnDeployer} iterates the handlers attached to each case via
 * {@code Case.getStartLifecycleHandlers()} and calls {@link #deploy} on the freshly-deployed case
 * definition's handlers / {@link #undeploy} on the previous (now-superseded) case definition's
 * handlers.
 * <p>
 * Restoration after deployment-deletion goes through {@link #deploy} too — the deploy context's
 * {@code isRestoringPreviousVersion()} flag distinguishes it from a fresh deploy. Both methods must
 * be implemented; a handler that opts into the deploy-time lifecycle owns both halves.
 * <p>
 * Custom integrations install additional handlers via a custom {@code CmmnParseHandler} (registered
 * in {@code customCmmnParseHandlers}) that calls {@code case.addStartLifecycleHandler(...)} during
 * parsing. Multiple handlers may co-exist on a single case.
 */
public interface CaseDefinitionStartLifecycleHandler {

    /**
     * Register the deploy-time artifact (event subscription, timer job, etc.) for this case
     * definition's start trigger when it is freshly deployed. Also called via the
     * deployment-deletion restoration path when the previous version's start triggers are
     * restored — distinguished by {@link CaseDefinitionStartDeployContext#isRestoringPreviousVersion()}.
     */
    void deploy(CaseDefinitionStartDeployContext context);

    /**
     * Remove or update the deploy-time artifact for this case definition's start trigger when
     * its case definition is superseded by a new version. Called on the previous (now-superseded)
     * case definition's handlers.
     */
    void undeploy(CaseDefinitionStartUndeployContext context);
}
