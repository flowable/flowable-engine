package org.flowable.identitylink.service.impl;

import org.flowable.identitylink.api.IdentityLink;

/**
 * Just swallows {@link IdentityLink} events
 */
public class NoOperationIdentityLinkEventHandler implements org.flowable.identitylink.service.IdentityLinkEventHandler {

    @Override
    public void handleLinkAddition(IdentityLink identityLink) {

    }

    @Override
    public void handleLinkDeletion(IdentityLink identityLink) {

    }
}
