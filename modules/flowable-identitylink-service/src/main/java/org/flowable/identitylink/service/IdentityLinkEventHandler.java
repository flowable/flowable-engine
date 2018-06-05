package org.flowable.identitylink.service;

import org.flowable.identitylink.api.IdentityLink;

/**
 * An interface to handle identity link event
 *
 * @author martin.grofcik
 */
public interface IdentityLinkEventHandler {

    /**
     * Handle an event of the {@link IdentityLink} addition
     *
     * @param identityLink identityLink which was recently added
     */
    void handleIdentityLinkAddition(IdentityLink identityLink);

    /**
     * Handle an event of {@link IdentityLink} deletion
     *
      * @param identityLink identityLink which is going to be deleted
     */
    void handleIdentityLinkDeletion(IdentityLink identityLink);
}
