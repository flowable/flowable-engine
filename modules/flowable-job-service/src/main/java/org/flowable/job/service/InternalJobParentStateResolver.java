package org.flowable.job.service;

import org.flowable.job.api.Job;

/**
 * @author martin.grofcik
 */
public interface InternalJobParentStateResolver {
    
    boolean isSuspended(Job job);
}
