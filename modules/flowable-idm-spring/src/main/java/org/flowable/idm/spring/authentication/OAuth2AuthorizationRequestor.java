/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.idm.spring.authentication;

import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author ahmedghonim
 */
public interface OAuth2AuthorizationRequestor {

    public String buildAuthorizationURL(HttpServletRequest request, String config);

}
