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
package org.flowable.engine.migration;

public abstract class EnableActivityMapping {

    public abstract String getActivityId();

    public static EnableActivityMapping.EnableMapping enable(String activityId) {
        return new EnableMapping(activityId);
    }

    public static class EnableMapping extends EnableActivityMapping {

        public String activityId;

        public EnableMapping(String activityId) {
            this.activityId = activityId;
        }

        @Override
        public String getActivityId() {
            return activityId;
        }

        @Override
        public String toString() {
            return "EnableMapping{" + "activityId='" + activityId + "\'}";
        }
    }
}