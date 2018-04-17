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
package org.flowable.common.engine.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;

/**
 * @author Joram Barrez
 */
public class FlowableVersions {
    
    public static final String CURRENT_VERSION = "6.3.1.0"; // Note the extra .x at the end. To cater for snapshot releases with different database changes
    
    public static final List<FlowableVersion> FLOWABLE_VERSIONS = new ArrayList<>();
    
    public static final String LAST_V5_VERSION = "5.99.0.0";
    
    public static final String LAST_V6_VERSION_BEFORE_SERVICES = "6.1.2.0";
    
    static {

        /* Previous */

        FLOWABLE_VERSIONS.add(new FlowableVersion("5.7"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.8"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.9"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.10"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.11"));

        // 5.12.1 was a bugfix release on 5.12 and did NOT change the version in ACT_GE_PROPERTY
        // On top of that, DB2 create script for 5.12.1 was shipped with a 'T' suffix ...
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.12", Arrays.asList("5.12.1", "5.12T")));

        FLOWABLE_VERSIONS.add(new FlowableVersion("5.13"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.14"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.15"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.15.1"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.16"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.16.1"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.16.2-SNAPSHOT"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.16.2"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.16.3.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.16.4.0"));

        FLOWABLE_VERSIONS.add(new FlowableVersion("5.17.0.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.17.0.1"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.17.0.2"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.18.0.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.18.0.1"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.20.0.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.20.0.1"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.20.0.2"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.21.0.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.22.0.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.23.0.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("5.24.0.0"));

        /*
         * Version 5.18.0.1 is the latest v5 version in the list here, although if you would look at the v5 code, you'll see there are a few other releases afterwards.
         * 
         * The reasoning is as follows: after 5.18.0.1, no database changes were done anymore. And if there would be database changes, they would have been part of both 5.x _and_ 6.x upgrade scripts.
         * The logic below will assume it's one of these releases in case it isn't found in the list here and do the upgrade from the 'virtual' release 5.99.0.0 to make sure th v6 changes are applied.
         */

        // This is the latest version of the 5 branch. It's a 'virtual' version cause it doesn't exist, but it is
        // there to make sure all previous version can upgrade to the 6 version correctly.
        FLOWABLE_VERSIONS.add(new FlowableVersion(LAST_V5_VERSION));

        // Version 6
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.0.0.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.0.0.1"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.0.0.2"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.0.0.3"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.0.0.4"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.0.0.5"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.0.1.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.1.0.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.1.1.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion(LAST_V6_VERSION_BEFORE_SERVICES));
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.2.0.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.2.1.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.3.0.0"));
        FLOWABLE_VERSIONS.add(new FlowableVersion("6.3.0.1"));

        /* Current */
        FLOWABLE_VERSIONS.add(new FlowableVersion(CURRENT_VERSION));
    }
    
    /**
     * Returns the index in the list of {@link #FLOWABLE_VERSIONS} matching the provided string version. Returns -1 if no match can be found.
     */
    public static int findMatchingVersionIndex(FlowableVersion flowableVersion) {
        return findMatchingVersionIndex(flowableVersion.mainVersion);
    }
    
    public static int findMatchingVersionIndex(String dbVersion) {
        int index = 0;
        int matchingVersionIndex = -1;
        while (matchingVersionIndex < 0 && index < FlowableVersions.FLOWABLE_VERSIONS.size()) {
            if (FlowableVersions.FLOWABLE_VERSIONS.get(index).matches(dbVersion)) {
                matchingVersionIndex = index;
            } else {
                index++;
            }
        }
        return matchingVersionIndex;
    }
    
    public static FlowableVersion getPreviousVersion(String version) {
        int currentVersion = findMatchingVersionIndex(version);
        if (currentVersion > 0) {
            return FLOWABLE_VERSIONS.get(currentVersion - 1);
        }
        return null;
    }
    
    public static int getFlowableVersionIndexForDbVersion(String dbVersion) {
        int matchingVersionIndex;
        // Determine index in the sequence of Flowable releases
        matchingVersionIndex = findMatchingVersionIndex(dbVersion);

        // If no match has been found, but the version starts with '5.x',
        // we assume it's the last version (see comment in the VERSIONS list)
        if (matchingVersionIndex < 0 && dbVersion != null && dbVersion.startsWith("5.")) {
            matchingVersionIndex = findMatchingVersionIndex(FlowableVersions.LAST_V5_VERSION);
        }

        // Exception when no match was found: unknown/unsupported version
        if (matchingVersionIndex < 0) {
            throw new FlowableException("Could not update Flowable database schema: unknown version from database: '" + dbVersion + "'");
        }
        
        return matchingVersionIndex;
    }

}
