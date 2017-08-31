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
package org.flowable.cmmn.engine.impl.delegate;

import org.flowable.cmmn.engine.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.PlanItemJavaDelegateActivityBehavior;
import org.flowable.cmmn.engine.runtime.DelegatePlanItemInstance;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.util.ReflectUtil;

/**
 * @author Joram Barrez
 */
public class CmmnClassDelegate implements CmmnActivityBehavior {
    
    protected String className;
    protected CmmnActivityBehavior activityBehaviorInstance;
    
    public CmmnClassDelegate(String className) {
        this.className = className;
    }

    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        if (activityBehaviorInstance == null) {
            activityBehaviorInstance = getCmmnActivityBehavior(className);
        }
        activityBehaviorInstance.execute(planItemInstance);
    }
    
    protected CmmnActivityBehavior getCmmnActivityBehavior(String className) {
        Object instance = instantiate(className);
        
        if (instance instanceof CmmnActivityBehavior) {
            return (CmmnActivityBehavior) instance;
            
        } else if (instance instanceof PlanItemJavaDelegate) {
            return new PlanItemJavaDelegateActivityBehavior((PlanItemJavaDelegate) instance);
            
        } else if (instance instanceof PlanItemJavaDelegateActivityBehavior) {
            return (PlanItemJavaDelegateActivityBehavior) instance;
            
        } else {
            throw new FlowableIllegalArgumentException(className + " does not implement the " 
                    + CmmnActivityBehavior.class + " nor the " + PlanItemJavaDelegate.class + " interface");
            
        }
    }
    
    protected Object instantiate(String className) {
        return ReflectUtil.instantiate(className);
    }

}
