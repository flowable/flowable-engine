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

var DECISION_TABLE_TOOLBAR_CONFIG = {
    "items" : [
        {
            "type" : "button",
            "title" : "TOOLBAR.ACTION.SAVE",
            "cssClass" : "editor-icon editor-icon-save",
            "action" : "DECISION_TABLE_TOOLBAR.ACTIONS.saveModel",
            "disableOnReadonly": true
        }
    ],
    
    "secondaryItems" : [
		{
		    "type" : "button",
		    "title" : "TOOLBAR.ACTION.CLOSE",
		    "cssClass" : "glyphicon glyphicon-remove",
		    "action" : "DECISION_TABLE_TOOLBAR.ACTIONS.closeEditor"
		}
    ]
};