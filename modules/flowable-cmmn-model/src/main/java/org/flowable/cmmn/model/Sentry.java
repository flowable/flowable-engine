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
package org.flowable.cmmn.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Joram Barrez
 */
public class Sentry extends CaseElement {

    public static final String TRIGGER_MODE_DEFAULT = "default";
    public static final String TRIGGER_MODE_ON_EVENT = "onEvent";

    protected String triggerMode;
    protected List<SentryOnPart> onParts = new ArrayList<>();
    protected SentryIfPart sentryIfPart;

    public boolean isDefaultTriggerMode() {
        return triggerMode == null || TRIGGER_MODE_DEFAULT.equals(triggerMode);
    }

    public boolean isOnEventTriggerMode() {
        return TRIGGER_MODE_ON_EVENT.equals(triggerMode);
    }

    public String getTriggerMode() {
        return triggerMode;
    }
    public void setTriggerMode(String triggerMode) {
        this.triggerMode = triggerMode;
    }
    public List<SentryOnPart> getOnParts() {
        return onParts;
    }
    public void setOnParts(List<SentryOnPart> onParts) {
        this.onParts = onParts;
    }
    public void addSentryOnPart(SentryOnPart sentryOnPart) {
        onParts.add(sentryOnPart);
    }
    public SentryIfPart getSentryIfPart() {
        return sentryIfPart;
    }
    public void setSentryIfPart(SentryIfPart sentryIfPart) {
        this.sentryIfPart = sentryIfPart;
    }
}
