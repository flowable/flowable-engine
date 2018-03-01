/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.app.rest.oauth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.flowable.app.model.common.OAuthRequestorRepresentation;
import org.flowable.app.model.common.ResultListDataRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.flowable.app.security.OAuthAuthorizationRequestor;

/**
 *
 * @author ahmedghonim
 */
@RestController
public class OAuthRequestorResource {

    @Autowired(required = false)
    protected Collection<OAuthAuthorizationRequestor> oAuth2Requestors;

    @RequestMapping(value = "/rest/requestor", method = RequestMethod.GET)
    public ResultListDataRepresentation getRequestors(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer start,
            @RequestParam(required = false) String groupId) {

        int startValue = start != null ? start.intValue() : 0;

        List<OAuthAuthorizationRequestor> users = oAuth2Requestors == null ? new ArrayList<>() : new ArrayList<>(oAuth2Requestors);
        ResultListDataRepresentation result = new ResultListDataRepresentation();
        result.setTotal(new Long(users.size()));
        result.setStart(startValue);
        result.setSize(users.size());
        result.setData(convertToUserRepresentations(users));
        return result;
    }

    protected List<OAuthRequestorRepresentation> convertToUserRepresentations(List<OAuthAuthorizationRequestor> requestors) {
        List<OAuthRequestorRepresentation> result = new ArrayList<>(requestors.size());
        for (OAuthAuthorizationRequestor requestor : requestors) {
            result.add(new OAuthRequestorRepresentation(requestor));
        }
        return result;
    }
}
