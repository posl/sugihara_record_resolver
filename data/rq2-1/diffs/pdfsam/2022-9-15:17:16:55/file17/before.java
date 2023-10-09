/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 29/nov/2012
 * Copyright 2017 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.update;

/**
 * Request to check if an updated version is available.
 * 
 * @author Andrea Vacondio
 * 
 */
public class UpdateCheckRequest {
    public static final UpdateCheckRequest INSTANCE = new UpdateCheckRequest();

    /**
     * tells if response to this request should notify about No updates available
     */
    public final boolean nofityNoUpdates;

    public UpdateCheckRequest() {
        this.nofityNoUpdates = false;
    }

    public UpdateCheckRequest(boolean nofityNoUpdates) {
        this.nofityNoUpdates = nofityNoUpdates;
    }
}
