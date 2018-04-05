package org.flowable.job.service;

import org.flowable.job.api.Job;

/**
 * @author martin.grofcik
 */
@FunctionalInterface
public interface InternalJobParentStateResolver {
    
    boolean isSuspended(Job job);
}
