/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 09/ott/2014
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
package org.pdfsam.ui.components.dialog;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Provider;

import org.pdfsam.eventstudio.annotation.EventListener;
import org.pdfsam.eventstudio.exception.BroadcastInterruptionException;
import org.pdfsam.i18n.I18nContext;
import org.pdfsam.injector.Auto;
import org.pdfsam.tool.TaskExecutionRequestEvent;
import org.sejda.model.exception.TaskOutputVisitException;
import org.sejda.model.output.DirectoryTaskOutput;
import org.sejda.model.output.ExistingOutputPolicy;
import org.sejda.model.output.FileOrDirectoryTaskOutput;
import org.sejda.model.output.FileTaskOutput;
import org.sejda.model.output.TaskOutputDispatcher;
import org.sejda.model.parameter.base.AbstractParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller receiving task execution requests and displaying the dialog if necessary
 * 
 * @author Andrea Vacondio
 *
 */
@Auto
public class OverwriteDialogController {
    private static final Logger LOG = LoggerFactory.getLogger(OverwriteDialogController.class);

    private Provider<OverwriteConfirmationDialog> dialog;

    @Inject
    public OverwriteDialogController(Provider<OverwriteConfirmationDialog> dialog) {
        this.dialog = dialog;
        eventStudio().addAnnotatedListeners(this);
    }

    @EventListener(priority = Integer.MIN_VALUE)
    public void request(TaskExecutionRequestEvent event) {
        AbstractParameters params = event.getParameters();
        try {
            if (params.getExistingOutputPolicy() != ExistingOutputPolicy.OVERWRITE) {
                event.getParameters().getOutput().accept(new TaskOutputDispatcher() {

                    @Override
                    public void dispatch(FileOrDirectoryTaskOutput output) {
                        onDirectory(params, output.getDestination());
                    }

                    @Override
                    public void dispatch(DirectoryTaskOutput output) {
                        onDirectory(params, output.getDestination());
                    }

                    @Override
                    public void dispatch(FileTaskOutput output) {
                        onFile(params, output.getDestination());
                    }

                });
            }
        } catch (TaskOutputVisitException e) {
            // it should never happen
            LOG.warn("Unable to show overwrite confirmation dialog", e);
        }
    }

    private void onDirectory(AbstractParameters params, File dir) {
        if (isNotEmpty(dir.listFiles())) {
            I18nContext i18n = I18nContext.getInstance();
            OverwriteConfirmationDialog dlg = dialog.get().init();
            ExistingOutputPolicy response = dlg.title(i18n.i18n(
                    "Directory not empty"))
                    .messageTitle(i18n.i18n("The selected directory is not empty"))
                    .messageContent(i18n.i18n("What would you like to do in case of files with the same name?"))
                    .buttons(dlg.defaultButton(i18n.i18n("Overwrite"), ExistingOutputPolicy.OVERWRITE),
                            dlg.button(i18n.i18n("Rename"), ExistingOutputPolicy.RENAME),
                            dlg.button(i18n.i18n("Skip"), ExistingOutputPolicy.SKIP),
                            dlg.cancelButton(i18n.i18n("Cancel")))
                    .response()
                    .orElseThrow(() -> new BroadcastInterruptionException(i18n.i18n("Don't overwrite existing file")));

            LOG.trace("Setting existing output policy to {}", response);
            params.setExistingOutputPolicy(response);
        }
    }

    private void onFile(AbstractParameters params, File file) {
        if (file.exists()) {
            I18nContext i18n = I18nContext.getInstance();
            OverwriteConfirmationDialog dlg = dialog.get().init();
            ExistingOutputPolicy response = dlg.title(i18n.i18n(
                    "Overwrite confirmation"))
                    .messageTitle(i18n.i18n("A file with the given name already exists"))
                    .messageContent(i18n.i18n("What would you like to do?"))
                    .buttons(dlg.defaultButton(i18n.i18n("Overwrite"), ExistingOutputPolicy.OVERWRITE),
                            dlg.button(i18n.i18n("Rename"), ExistingOutputPolicy.RENAME), dlg.cancelButton(i18n.i18n(
                                    "Cancel")))
                    .response()
                    .orElseThrow(() -> new BroadcastInterruptionException(i18n.i18n("Don't overwrite existing file")));
            LOG.trace("Setting existing output policy to {}", response);
            params.setExistingOutputPolicy(response);
        }
    }
}
