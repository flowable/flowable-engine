/*
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
'use strict';

/* Services */

activitiAdminApp.factory('Account', ['$resource',
    function ($resource) {
        return $resource('app/rest/account', {}, {
        });
    }]);

activitiAdminApp.factory('Sessions', ['$resource',
    function ($resource) {
        return $resource('app/rest/account/sessions/:series', {}, {
            'get': { method: 'GET', isArray: true}
        });
    }]);

activitiAdminApp.factory('Metrics', ['$resource',
    function ($resource) {
        return $resource('/metrics/metrics', {}, {
            'get': { method: 'GET'}
        });
    }]);

activitiAdminApp.factory('LogsService', ['$resource',
    function ($resource) {
        return $resource('app/rest/logs', {}, {
            'findAll': { method: 'GET', isArray: true},
            'changeLevel':  { method: 'PUT'}
        });
    }]);
