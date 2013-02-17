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
 * Class that can collapse and expand the sub-section-rows in a specific section-row of the resultmatrix
 */
var Section = new Class({
	/**
	 * Autobinds these methods
	 */
	Binds: ['open', 'close'],
	
	/**
	 * Class constructor
	 * 
	 * @param section DOM-node The root-section table-row node 
	 */
	initialize: function(section) {
		this.id = section.get('id');
		if (!$chk(this.id)) {
			return
		}
		this.handler = section;
		
		this.subsection = section.getNext();
		if ($chk(this.subsection) && this.subsection.hasClass('subsections')) {
			this.subsection.getElements('tr.parent-'+this.id).each(function(subsection) {
				new Section(subsection);
			}, this);
			
			this.handler.addEvent('click', this.close);
		}
	},
	
	/**
	 * Event method that closes the subsections
	 */
	close: function() {
		this.subsection.addClass('hidden');
		this.toggleHandler('close');
	},
	
	/**
	 * Event method that opens the subsections
	 */
	open: function() {
		this.subsection.removeClass('hidden');
		this.toggleHandler('open');
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
