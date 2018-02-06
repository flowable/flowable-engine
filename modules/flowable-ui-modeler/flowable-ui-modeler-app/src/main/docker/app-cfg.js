'use strict';

var FLOWABLE = FLOWABLE || {};

FLOWABLE.CONFIG = {
    'onPremise' : true,
    'contextRoot' : '/designer',
    'webContextRoot' : '/designer',
    'datesLocalization' : false,
    'formType': 9,
    'deployUrls': [{
        "name": "gasp-public",
        "url": "http://localhost:9998/gasp-public/workflow/deploy"
    }],
    'appTitle': 'RDS System Builder'
};
