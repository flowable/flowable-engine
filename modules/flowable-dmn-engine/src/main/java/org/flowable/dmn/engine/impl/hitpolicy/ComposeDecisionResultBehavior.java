/**
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.flowable.dmn.engine.impl.hitpolicy;

import org.flowable.dmn.engine.impl.mvel.MvelExecutionContext;
import org.flowable.dmn.model.RuleOutputClauseContainer;

import java.util.List;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public interface ComposeDecisionResultBehavior {

    void composeDecisionResults(MvelExecutionContext executionContext);
}
