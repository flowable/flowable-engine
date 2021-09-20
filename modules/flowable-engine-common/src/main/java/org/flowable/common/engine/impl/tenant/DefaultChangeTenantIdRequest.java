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
package org.flowable.common.engine.impl.tenant;

import org.flowable.common.engine.api.tenant.ChangeTenantIdRequest;

public class DefaultChangeTenantIdRequest implements ChangeTenantIdRequest {

    private String sourceTenantId;
    private String targetTenantId;
    private String defaultTenantId;
    private boolean onlyInstancesFromDefaultTenantDefinitions;
    private String scope;
    private boolean dryRun;

    private DefaultChangeTenantIdRequest(Builder builder) {
        this.sourceTenantId = builder.sourceTenantId;
        this.targetTenantId = builder.targetTenantId;
        this.defaultTenantId = builder.defaultTenantId;
        this.onlyInstancesFromDefaultTenantDefinitions = builder.onlyInstancesFromDefaultTenantDefinitions;
        this.scope = builder.scope;
        this.dryRun = builder.dryRun;
    }

    @Override
    public String getSourceTenantId() {
        return this.sourceTenantId;
    }

    @Override
    public String getTargetTenantId() {
        return this.targetTenantId;
    }

    @Override
    public String getDefaultTenantId() {
        return this.defaultTenantId;
    }

    @Override
    public boolean getOnlyInstancesFromDefaultTenantDefinitions() {
        return this.onlyInstancesFromDefaultTenantDefinitions;
    }

    @Override
    public String getScope() {
        return this.scope;
    }

    @Override
    public boolean isDryRun() {
        return this.dryRun;
    }

    public static Builder builder(String sourceTenantId, String targetTenantId) {
        return new Builder(sourceTenantId, targetTenantId);
    }

    public static Builder builder(ChangeTenantIdRequest changeTenantIdRequest) {
        return new Builder(changeTenantIdRequest);
    }

    public static class Builder {

        private String sourceTenantId;
        private String targetTenantId;
        private String defaultTenantId;
        private boolean onlyInstancesFromDefaultTenantDefinitions;
        private String scope;
        private boolean dryRun = true; //by default, we want to always build dry run requests. Actual requests must be explicit.

        private Builder(String sourceTenantId, String targetTenantId) {
            this.sourceTenantId = sourceTenantId;
            this.targetTenantId = targetTenantId;
        }

        private Builder(ChangeTenantIdRequest changeTenantIdRequest) {
            this.sourceTenantId = changeTenantIdRequest.getSourceTenantId();
            this.targetTenantId = changeTenantIdRequest.getTargetTenantId();
            this.defaultTenantId = changeTenantIdRequest.getDefaultTenantId();
            this.onlyInstancesFromDefaultTenantDefinitions = changeTenantIdRequest.getOnlyInstancesFromDefaultTenantDefinitions();
            this.scope = changeTenantIdRequest.getScope();
            this.dryRun = changeTenantIdRequest.isDryRun();
        }

        public Builder defaultTenantId(String defaultTenantId) {
            this.defaultTenantId = defaultTenantId;
            return this;
        }

        public Builder onlyInstancesFromDefaultTenantDefinitions(boolean onlyInstancesFromDefaultTenantDefinitions) {
            this.onlyInstancesFromDefaultTenantDefinitions = onlyInstancesFromDefaultTenantDefinitions;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public String getSourceTenantId() {
            return this.sourceTenantId;
        }

        public String getTargetTenantId() {
            return this.targetTenantId;
        }

        public String getDefaultTenantId() {
            return this.defaultTenantId;
        }

        public boolean getOnlyInstancesFromDefaultTenantDefinitions() {
            return this.onlyInstancesFromDefaultTenantDefinitions;
        }

        public String getScope() {
            return this.scope;
        }

        public boolean isDryRun() {
            return this.dryRun;
        }

        public ChangeTenantIdRequest build() {
            return new DefaultChangeTenantIdRequest(this);
        }

    }

}