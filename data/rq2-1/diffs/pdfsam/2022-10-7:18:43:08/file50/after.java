/*
 * This file is part of the PDF Split And Merge source code
 * Created on 21/lug/2014
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

import jakarta.inject.Named;
import org.pdfsam.core.context.StringPersistentProperty;
import org.pdfsam.core.support.validation.Validators;
import org.pdfsam.gui.components.log.MaxLogRowsChangedEvent;
import org.pdfsam.injector.Provides;
import org.pdfsam.model.io.FileType;
import org.pdfsam.model.io.OpenType;
import org.pdfsam.model.ui.ComboItem;
import org.pdfsam.ui.components.support.FXValidationSupport;
import org.pdfsam.ui.components.support.Style;

import static java.util.Comparator.comparing;
import static org.pdfsam.core.context.ApplicationContext.app;
import static org.pdfsam.core.context.BooleanPersistentProperty.CHECK_FOR_NEWS;
import static org.pdfsam.core.context.BooleanPersistentProperty.CHECK_UPDATES;
import static org.pdfsam.core.context.BooleanPersistentProperty.CLEAR_CONFIRMATION;
import static org.pdfsam.core.context.BooleanPersistentProperty.DONATION_NOTIFICATION;
import static org.pdfsam.core.context.BooleanPersistentProperty.OVERWRITE_OUTPUT;
import static org.pdfsam.core.context.BooleanPersistentProperty.PDF_COMPRESSION_ENABLED;
import static org.pdfsam.core.context.BooleanPersistentProperty.PLAY_SOUNDS;
import static org.pdfsam.core.context.BooleanPersistentProperty.PREMIUM_MODULES;
import static org.pdfsam.core.context.BooleanPersistentProperty.SAVE_PWD_IN_WORKSPACE;
import static org.pdfsam.core.context.BooleanPersistentProperty.SAVE_WORKSPACE_ON_EXIT;
import static org.pdfsam.core.context.BooleanPersistentProperty.SMART_OUTPUT;
import static org.pdfsam.core.context.IntegerPersistentProperty.LOGVIEW_ROWS_NUMBER;
import static org.pdfsam.core.context.StringPersistentProperty.STARTUP_MODULE;
import static org.pdfsam.core.context.StringPersistentProperty.WORKING_PATH;
import static org.pdfsam.core.context.StringPersistentProperty.WORKSPACE_PATH;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;
import static org.pdfsam.i18n.I18nContext.i18n;
import static org.pdfsam.model.ui.ComboItem.keyWithEmptyValue;
import static org.pdfsam.ui.components.help.HelpUtils.helpIcon;

/**
 * Configuration for the PDFsam preferences components
 *
 * @author Andrea Vacondio
 */
public class PreferenceConfig {

    @Provides
    @Named("localeCombo")
    public PreferenceComboBox<ComboItem<String>> localeCombo() {
        return new PreferenceComboBox<>(StringPersistentProperty.LOCALE);
    }

    @Provides
    @Named("startupToolCombo")
    public PreferenceComboBox<ComboItem<String>> startupModuleCombo() {
        PreferenceComboBox<ComboItem<String>> startupToolCombo = new PreferenceComboBox<>(STARTUP_MODULE);
        startupToolCombo.setId("startupModuleCombo");
        startupToolCombo.getItems().add(new ComboItem<>("", i18n().tr("Dashboard")));
        app().runtimeState().tools().values().stream().map(tool -> new ComboItem<>(tool.id(), tool.descriptor().name()))
                .sorted(comparing(ComboItem::description)).forEach(startupToolCombo.getItems()::add);
        startupToolCombo.setValue(keyWithEmptyValue(app().persistentSettings().get(STARTUP_MODULE).orElse("")));
        return startupToolCombo;
    }

    @Provides
    @Named("checkForUpdates")
    public PreferenceCheckBox checkForUpdates() {
        PreferenceCheckBox checkForUpdates = new PreferenceCheckBox(CHECK_UPDATES,
                i18n().tr("Check for updates at startup"), app().persistentSettings().get(CHECK_UPDATES));
        checkForUpdates.setId("checkForUpdates");
        checkForUpdates.setGraphic(helpIcon(
                i18n().tr("Set whether new version availability should be checked on startup (restart needed)")));
        checkForUpdates.getStyleClass().addAll(Style.WITH_HELP.css());
        checkForUpdates.getStyleClass().add("spaced-vitem");
        return checkForUpdates;
    }

    @Provides
    @Named("checkForNews")
    public PreferenceCheckBox checkForNews() {
        PreferenceCheckBox checkForNews = new PreferenceCheckBox(CHECK_FOR_NEWS, i18n().tr("Check for news at startup"),
                app().persistentSettings().get(CHECK_FOR_NEWS));
        checkForNews.setId("checkForNews");
        checkForNews.setGraphic(helpIcon(i18n().tr(
                "Set whether the application should check for news availability on startup (restart needed)")));
        checkForNews.getStyleClass().addAll(Style.WITH_HELP.css());
        checkForNews.getStyleClass().add("spaced-vitem");
        return checkForNews;
    }

    @Provides
    @Named("compressionEnabled")
    public PreferenceCheckBox compressionEnabled() {
        PreferenceCheckBox compressionEnabled = new PreferenceCheckBox(PDF_COMPRESSION_ENABLED,
                i18n().tr("Enabled PDF compression"), app().persistentSettings().get(PDF_COMPRESSION_ENABLED));
        compressionEnabled.setId("compressionEnabled");
        compressionEnabled.setGraphic(
                helpIcon(i18n().tr("Set whether \"Compress output file\" should be enabled by default")));
        compressionEnabled.getStyleClass().addAll(Style.WITH_HELP.css());
        compressionEnabled.getStyleClass().add("spaced-vitem");
        return compressionEnabled;
    }

    @Provides
    @Named("overwriteOutput")
    public PreferenceCheckBox overwriteOutput() {
        PreferenceCheckBox overwriteOutput = new PreferenceCheckBox(OVERWRITE_OUTPUT, i18n().tr("Overwrite files"),
                app().persistentSettings().get(OVERWRITE_OUTPUT));
        overwriteOutput.setId("overwriteOutput");
        overwriteOutput.setGraphic(
                helpIcon(i18n().tr("Set whether \"Overwrite if already exists\" should be enabled by default")));
        overwriteOutput.getStyleClass().addAll(Style.WITH_HELP.css());
        overwriteOutput.getStyleClass().add("spaced-vitem");
        return overwriteOutput;
    }

    @Provides
    @Named("playSounds")
    public PreferenceCheckBox playSounds() {
        PreferenceCheckBox playSounds = new PreferenceCheckBox(PLAY_SOUNDS, i18n().tr("Play alert sounds"),
                app().persistentSettings().get(PLAY_SOUNDS));
        playSounds.setId("playSounds");
        playSounds.setGraphic(helpIcon(i18n().tr("Turn on or off alert sounds")));
        playSounds.getStyleClass().addAll(Style.WITH_HELP.css());
        playSounds.getStyleClass().add("spaced-vitem");
        return playSounds;
    }

    @Provides
    @Named("savePwdInWorkspace")
    public PreferenceCheckBox savePwdInWorkspace() {
        PreferenceCheckBox savePwdInWorkspace = new PreferenceCheckBox(SAVE_PWD_IN_WORKSPACE,
                i18n().tr("Store passwords when saving a workspace file"),
                app().persistentSettings().get(SAVE_PWD_IN_WORKSPACE));
        savePwdInWorkspace.setId("savePwdInWorkspace");
        savePwdInWorkspace.setGraphic(helpIcon(i18n().tr(
                "If an encrypted PDF document has been opened with a password, save the password in the workspace file")));
        savePwdInWorkspace.getStyleClass().addAll(Style.WITH_HELP.css());
        savePwdInWorkspace.getStyleClass().add("spaced-vitem");
        return savePwdInWorkspace;
    }

    @Provides
    @Named("donationNotification")
    public PreferenceCheckBox donationNotification() {
        PreferenceCheckBox donationNotification = new PreferenceCheckBox(DONATION_NOTIFICATION,
                i18n().tr("Show donation window"), app().persistentSettings().get(DONATION_NOTIFICATION));
        donationNotification.setId("donationNotification");
        donationNotification.setGraphic(helpIcon(i18n().tr(
                "Turn on or off the notification appearing once in a while and asking the user to support PDFsam with a donation")));
        donationNotification.getStyleClass().addAll(Style.WITH_HELP.css());
        donationNotification.getStyleClass().add("spaced-vitem");
        return donationNotification;
    }

    @Provides
    @Named("fetchPremiumModules")
    public PreferenceCheckBox fetchPremiumModules() {
        PreferenceCheckBox fetchPremiumModules = new PreferenceCheckBox(PREMIUM_MODULES,
                i18n().tr("Show premium features"), app().persistentSettings().get(PREMIUM_MODULES));
        fetchPremiumModules.setId("fetchPremiumModules");
        fetchPremiumModules.setGraphic(helpIcon(i18n().tr(
                "Set whether the application should fetch and show premium features description in the modules dashboard")));
        fetchPremiumModules.getStyleClass().addAll(Style.WITH_HELP.css());
        fetchPremiumModules.getStyleClass().add("spaced-vitem");
        return fetchPremiumModules;
    }

    @Provides
    @Named("clearConfirmation")
    public PreferenceCheckBox clearConfirmation() {
        PreferenceCheckBox clearConfirmation = new PreferenceCheckBox(CLEAR_CONFIRMATION,
                i18n().tr("Ask for a confirmation when clearing the selection table"),
                app().persistentSettings().get(CLEAR_CONFIRMATION));
        clearConfirmation.setId("clearConfirmation");
        clearConfirmation.setGraphic(helpIcon(i18n().tr(
                "Set whether the application should ask for a confirmation when clearing the selection table")));
        clearConfirmation.getStyleClass().addAll(Style.WITH_HELP.css());
        clearConfirmation.getStyleClass().add("spaced-vitem");
        return clearConfirmation;
    }

    @Provides
    @Named("smartRadio")
    public PreferenceRadioButton smartRadio() {
        PreferenceRadioButton smartRadio = new PreferenceRadioButton(SMART_OUTPUT,
                i18n().tr("Use the selected PDF document directory as output directory"),
                app().persistentSettings().get(SMART_OUTPUT));
        smartRadio.setId("smartRadio");
        return smartRadio;
    }

    @Provides
    @Named("workingDirectory")
    public PreferenceBrowsableDirectoryField workingDirectory() {
        PreferenceBrowsableDirectoryField workingDirectory = new PreferenceBrowsableDirectoryField(WORKING_PATH);
        workingDirectory.getTextField().setText(app().persistentSettings().get(WORKING_PATH).orElse(""));
        workingDirectory.setId("workingDirectory");
        workingDirectory.getStyleClass().add("spaced-vitem");
        return workingDirectory;
    }

    @Provides
    @Named("workspace")
    public PreferenceBrowsableFileField workspace() {
        PreferenceBrowsableFileField workspace = new PreferenceBrowsableFileField(WORKSPACE_PATH, FileType.JSON,
                OpenType.OPEN);
        workspace.getTextField().setText(app().persistentSettings().get(WORKSPACE_PATH).orElse(""));
        workspace.setId("workspace");
        workspace.getStyleClass().add("spaced-vitem");
        return workspace;
    }

    @Provides
    @Named("saveWorkspaceOnExit")
    public PreferenceCheckBox saveWorkspaceOnExit() {
        PreferenceCheckBox saveWorkspaceOnExit = new PreferenceCheckBox(SAVE_WORKSPACE_ON_EXIT,
                i18n().tr("Save default workspace on exit"), app().persistentSettings().get(SAVE_WORKSPACE_ON_EXIT));
        saveWorkspaceOnExit.setId("saveWorkspaceOnExit");
        saveWorkspaceOnExit.setGraphic(
                helpIcon(i18n().tr("If a default workspace is set, save it on application exit")));
        saveWorkspaceOnExit.getStyleClass().addAll(Style.WITH_HELP.css());
        saveWorkspaceOnExit.getStyleClass().add("spaced-vitem");
        return saveWorkspaceOnExit;
    }

    @Provides
    @Named("logViewRowsNumber")
    public PreferenceIntTextField logViewRowsNumber() {
        PreferenceIntTextField logRowsNumber = new PreferenceIntTextField(LOGVIEW_ROWS_NUMBER,
                Validators.positiveInteger());
        logRowsNumber.setText(Integer.toString(app().persistentSettings().get(LOGVIEW_ROWS_NUMBER)));
        logRowsNumber.setErrorMessage(i18n().tr("Maximum number of rows mast be a positive number"));
        logRowsNumber.setId("logViewRowsNumber");
        logRowsNumber.validProperty().addListener((o, oldVal, newVal) -> {
            if (newVal == FXValidationSupport.ValidationState.VALID) {
                eventStudio().broadcast(new MaxLogRowsChangedEvent());
            }
        });
        return logRowsNumber;
    }

}
