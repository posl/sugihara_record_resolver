/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 25 ott 2020
 * Copyright 2019 by Sober Lemur S.a.s di Vacondio Andrea (info@pdfsam.org).
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
package org.pdfsam.gui.components.dialog;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.pdfsam.core.context.BooleanPersistentProperty;
import org.pdfsam.eventstudio.annotation.EventListener;
import org.pdfsam.injector.Auto;
import org.pdfsam.model.tool.ClearToolRequest;

import static org.pdfsam.core.context.ApplicationContext.app;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;

/**
 * @author Andrea Vacondio
 *
 */
@Auto
public class ClearToolConfirmationDialogController {

    private final Provider<ClearToolConfirmationDialog> dialog;

    @Inject
    public ClearToolConfirmationDialogController(Provider<ClearToolConfirmationDialog> dialog) {
        this.dialog = dialog;
        eventStudio().addAnnotatedListeners(this);
    }

    @EventListener
    public void request(ClearToolRequest event) {
        if (!app().persistentSettings().get(BooleanPersistentProperty.CLEAR_CONFIRMATION) || !event.askConfirmation()
                || dialog.get().clearEverything(event.clearEverything()).response()) {
            eventStudio().broadcast(event, event.toolBinding());
        }
    }
}
