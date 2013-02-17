/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Class that can collapse and expand the section-rows in a specific osgi-version of the resultmatrix
 */
var Version = new Class({
	/**
	 * Autobind these methods
	 */
	Binds: ['close', 'open'],
	
	/**
	 * Class constructor
	 * 
	 * @param version DOM-node The versions table-row node
	 */
	initialize: function(version) {
		this.id = version.get('id');
		this.handler = version.getElement('.handler');
		this.sectionTable = version.getElement('table');
		
		if (!$chk(this.id) || !$chk(this.sectionTable) || !$chk(this.handler)) {
			return;
		}
		
		this.sections = version.getElements('tr.parent-'+this.id+'-0');
		this.sections.each(function(section) {
			new Section(section);
		}, this);
		
		this.toggleHandler('open');
	},
	
	/**
	 * Event method that closes the sections inside this version
	 */
	close: function() {
		this.toggleHandler('close');
		this.sectionTable.addClass('hidden');
	},
	
	/**
	 * Event method that closes the sections inside this version
	 */
	open: function() {
		this.toggleHandler('open');
		this.sectionTable.removeClass('hidden');
	},
	
	/**
	 * Toggles the event of the handler to open or close
	 * 
	 * @param newState String 'open' or 'close' 
	 */
	toggleHandler: function(newState) {
		if ($chk(this.handler)) {
			if (newState == 'open') {
				this.handler.removeEvent('click', this.open);
				this.handler.addEvent('click', this.close);
				this.handler.set('src', 'resources/images/folder.png');
			}
			else {
				this.handler.removeEvent('click', this.close);
				this.handler.addEvent('click', this.open);
				this.handler.set('src', 'resources/images/folder_closed.png');
			}
		}
	}
});
