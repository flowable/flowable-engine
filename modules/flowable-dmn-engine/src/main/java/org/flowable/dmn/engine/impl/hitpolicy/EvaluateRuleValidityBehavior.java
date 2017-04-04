/**
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.flowable.dmn.engine.impl.hitpolicy;

import org.flowable.dmn.engine.impl.mvel.MvelExecutionContext;

/**
 * @author Yvo Swillens
 */
public interface EvaluateRuleValidityBehavior {

    void evaluateRuleValidity(int ruleNumber, MvelExecutionContext executionContext);
}
