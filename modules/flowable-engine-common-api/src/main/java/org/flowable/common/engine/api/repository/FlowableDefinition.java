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
package org.flowable.common.engine.api.repository;

/**
 * Common interface for all engine definitions (process, case, decision, app, form, event and channel definitions).
 *
 * <p>A definition is a versioned, deployable artifact contained in an {@link EngineDeployment}. This interface exposes
 * only the properties that are common to and mandatory for every definition type, so that consumers can handle any
 * definition uniformly regardless of the owning engine. Engine-specific properties (such as category or description)
 * remain on the concrete definition interfaces.
 */
public interface FlowableDefinition {

    /** unique identifier */
    String getId();

    /** unique key, stable across all versions of this definition */
    String getKey();

    /** label used for display purposes */
    String getName();

    /** version of this definition */
    int getVersion();

    /** name of the resource of this definition within its deployment */
    String getResourceName();

    /** the deployment in which this definition is contained */
    String getDeploymentId();

    /** the tenant identifier of this definition */
    String getTenantId();

}
