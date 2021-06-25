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
package org.flowable.cmmn.converter;

import javax.xml.stream.XMLStreamReader;

import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.PlanFragment;

/**
 * @author Joram Barrez
 */
public class PlanFragmentXmlConverter extends TaskXmlConverter {
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_PLAN_FRAGMENT;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {

        /*
         * A plan fragment does NOT have a runtime state, even though it has an associated plan item.
         *
         * From the CMMN spec: "Unlike other PlanItemDefinitions, a PlanFragment does not have a representation in run-time,
         * i.e., there is no notion of lifecycle tracking of a PlanFragment (not being a Stage) in the context of a Case instance.
         * Just the PlanItems that are contained in it are instantiated and have their lifecyles that are tracked.
         *
         * Do note that a Stage is a subclass of a PlanFragment (but this is only for plan item / sentry containment).
         */

        PlanFragment planFragment = new PlanFragment();
        planFragment.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));

        planFragment.setCase(conversionHelper.getCurrentCase());
        planFragment.setParent(conversionHelper.getCurrentPlanFragment());

        conversionHelper.setCurrentPlanFragment(planFragment);
        conversionHelper.addPlanFragment(planFragment);

        return planFragment;
    }

    @Override
    protected void elementEnd(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        super.elementEnd(xtr, conversionHelper);
        conversionHelper.removeCurrentPlanFragment();
    }
    
}
