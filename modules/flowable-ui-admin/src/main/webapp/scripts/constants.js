/* Copyright 2005-2015 Alfresco Software, Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

angular.module('flowableAdminApp')
    .constant('gridConstants', {
        defaultTemplate : '<div><div class="ngCellText" title="{{row.getProperty(col.field)}}">{{row.getProperty(col.field)}}</div></div>',
        dateTemplate : '<div><div class="ngCellText" title="{{row.getProperty(col.field) | dateformat:\'full\'}}">{{row.getProperty(col.field) | dateformat}}</div></div>',
        userTemplate : '<div><div class="ngCellText" title="{{row.getProperty(col.field)}}" username="row.getProperty(col.field)"></div></div>',
        groupTemplate : '<div><div class="ngCellText" title="{{row.getProperty(col.field)}}" groupname="row.getProperty(col.field)"></div></div>',
        userObjectTemplate : '<div><div class="ngCellText" title="{{row.getProperty(col.field)}}" user="row.getProperty(col.field)"></div></div>'
    });


    