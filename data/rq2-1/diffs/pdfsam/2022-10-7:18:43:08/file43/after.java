/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 12/dic/2014
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
package org.pdfsam.gui.components.dashboard.preference;

import javafx.scene.control.Button;
import org.pdfsam.injector.Prototype;
import org.pdfsam.model.update.UpdateCheckRequest;
import org.pdfsam.ui.components.support.Style;

import static org.pdfsam.eventstudio.StaticStudio.eventStudio;
import static org.pdfsam.i18n.I18nContext.i18n;

/**
 * Button requesting a check for update
 * 
 * @author Andrea Vacondio
 *
 */
@Prototype
class CheckForUpdatesButton extends Button {

    CheckForUpdatesButton() {
        super(i18n().tr("Check for updates now"));
        getStyleClass().addAll(Style.BUTTON.css());
        setOnAction(e -> eventStudio().broadcast(new UpdateCheckRequest(true)));
    }
}
