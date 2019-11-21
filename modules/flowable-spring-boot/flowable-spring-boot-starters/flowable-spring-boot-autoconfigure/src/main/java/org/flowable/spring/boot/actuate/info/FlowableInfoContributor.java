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
package org.flowable.spring.boot.actuate.info;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.FlowableVersions;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

/**
 * @author Filip Hrisafov
 */
public class FlowableInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("flowable", version());
    }

    protected Map<String, Object> version() {
        Map<String, Object> info = new HashMap<>();
        info.put("dbVersion", FlowableVersions.CURRENT_VERSION);
        info.put("version", getVersion());
        return info;
    }

    /**
     * Return the full version string of the present Flowable codebase, or {@code null}
     * if it cannot be determined.
     *
     * @return the version of Flowable or {@code null}
     * @see Package#getImplementationVersion()
     */
    private static String getVersion() {
        Package pkg = FlowableInfoContributor.class.getPackage();
        return (pkg != null) ? pkg.getImplementationVersion() : null;
    }
}
