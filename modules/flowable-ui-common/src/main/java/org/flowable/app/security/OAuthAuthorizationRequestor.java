/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.app.security;

import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author ahmedghonim
 */
public interface OAuthAuthorizationRequestor {

    public String getName();

    public boolean canAuthorize(String id);

    public String buildAuthorizationURL(HttpServletRequest request);

}
