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
package org.flowable.common.engine.impl.db;

import liquibase.license.LicenseInstallResult;
import liquibase.license.LicenseService;
import liquibase.license.Location;

/**
 * A special Liquibase {@link LicenseService} that always returns {@code false} for the license check.
 * This is there to support only using the Community Features of the Liquibase license in the scope of the Flowable Liquibase usage.
 *
 * @author Filip Hrisafov
 */
public class FlowableLiquibaseLicenseService implements LicenseService {

    @Override
    public int getPriority() {
        return PRIORITY_SPECIALIZED;
    }

    @Override
    public boolean licenseIsInstalled() {
        return false;
    }

    @Override
    public boolean licenseIsValid(String subject) {
        // The license is never valid
        return false;
    }

    @Override
    public String getLicenseInfo() {
        return "Temporary License Service by Flowable that works without jaxb-api. This license service is only value for the community edition of Liquibase";
    }

    @Override
    public LicenseInstallResult installLicense(Location... locations) {
        throw new UnsupportedOperationException("Installing licenses not supported");
    }

    @Override
    public void disable() {

    }

    @Override
    public boolean licenseIsAboutToExpire() {
        return true;
    }

    @Override
    public int daysTilExpiration() {
        // The license is always expired
        return -1;
    }
}
