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
package org.pdfsam.ui.dashboard.preference;

import org.pdfsam.context.BooleanUserPreference;
import org.pdfsam.context.IntUserPreference;
import org.pdfsam.context.StringUserPreference;
import org.pdfsam.context.UserContext;
import org.pdfsam.i18n.I18nContext;
import org.pdfsam.injector.Provides;
import org.pdfsam.module.Tool;
import org.pdfsam.module.ToolIdNamePair;
import org.pdfsam.support.KeyStringValueItem;
import org.pdfsam.support.LocaleKeyValueItem;
import org.pdfsam.support.io.FileType;
import org.pdfsam.support.validation.Validators;
import org.pdfsam.ui.io.RememberingLatestFileChooserWrapper.OpenType;
import org.pdfsam.ui.log.MaxLogRowsChangedEvent;
import org.pdfsam.ui.support.FXValidationSupport.ValidationState;
import org.pdfsam.ui.support.Style;

import javax.inject.Named;
import java.util.Comparator;
import java.util.List;

import static org.pdfsam.eventstudio.StaticStudio.eventStudio;
import static org.pdfsam.support.KeyStringValueItem.keyEmptyValue;
import static org.pdfsam.support.KeyStringValueItem.keyValue;

/**
 * Configuration for the PDFsam preferences components
 * 
 * @author Andrea Vacondio
 *
 */
public class PreferenceConfig {

    @Provides
    @Named("localeCombo")
    public PreferenceComboBox<LocaleKeyValueItem> localeCombo(UserContext userContext) {
        return new PreferenceComboBox<>(StringUserPreference.LOCALE, userContext);
    }

    @Provides
    @Named("startupModuleCombo")
    public PreferenceComboBox<KeyStringValueItem<String>> startupModuleCombo(List<Tool> tools,
            UserContext userContext) {
        PreferenceComboBox<KeyStringValueItem<String>> startupModuleCombo = new PreferenceComboBox<>(
                StringUserPreference.STARTUP_MODULE, userContext);
        startupModuleCombo.setId("startupModuleCombo");
        startupModuleCombo.getItems().add(keyValue("", i18n().tr("Dashboard")));
        tools.stream().map(ToolIdNamePair::new).sorted(Comparator.comparing(ToolIdNamePair::getValue))
                .forEach(startupModuleCombo.getItems()::add);
        startupModuleCombo.setValue(keyEmptyValue(userContext.getStartupModule()));
        return startupModuleCombo;
    }

    @Provides
    @Named("checkForUpdates")
    public PreferenceCheckBox checkForUpdates(UserContext userContext) {
        PreferenceCheckBox checkForUpdates = new PreferenceCheckBox(BooleanUserPreference.CHECK_UPDATES,
                i18n().tr("Check for updates at startup"), userContext.isCheckForUpdates(), userContext);
        checkForUpdates.setId("checkForUpdates");
        checkForUpdates.setGraphic(helpIcon(I18nContext.getInstance()
                .i18n("Set whether new version availability should be checked on startup (restart needed)")));
        checkForUpdates.getStyleClass().addAll(Style.WITH_HELP.css());
        checkForUpdates.getStyleClass().add("spaced-vitem");
        return checkForUpdates;
    }

    @Provides
    @Named("checkForNews")
    public PreferenceCheckBox checkForNews(UserContext userContext) {
        PreferenceCheckBox checkForNews = new PreferenceCheckBox(BooleanUserPreference.CHECK_FOR_NEWS,
                i18n().tr("Check for news at startup"), userContext.isCheckForNews(), userContext);
        checkForNews.setId("checkForNews");
        checkForNews.setGraphic(helpIcon(I18nContext.getInstance()
                .i18n("Set whether the application should check for news availability on startup (restart needed)")));
        checkForNews.getStyleClass().addAll(Style.WITH_HELP.css());
        checkForNews.getStyleClass().add("spaced-vitem");
        return checkForNews;
    }

    @Provides
    @Named("compressionEnabled")
    public PreferenceCheckBox compressionEnabled(UserContext userContext) {
        PreferenceCheckBox compressionEnabled = new PreferenceCheckBox(BooleanUserPreference.PDF_COMPRESSION_ENABLED,
                i18n().tr("Enabled PDF compression"), userContext.isCompressionEnabled(), userContext);
        compressionEnabled.setId("compressionEnabled");
        compressionEnabled.setGraphic(helpIcon(I18nContext.getInstance()
                .i18n("Set whether \"Compress output file\" should be enabled by default")));
        compressionEnabled.getStyleClass().addAll(Style.WITH_HELP.css());
        compressionEnabled.getStyleClass().add("spaced-vitem");
        return compressionEnabled;
    }

    @Provides
    @Named("overwriteOutput")
    public PreferenceCheckBox overwriteOutput(UserContext userContext) {
        PreferenceCheckBox overwriteOutput = new PreferenceCheckBox(BooleanUserPreference.OVERWRITE_OUTPUT,
                i18n().tr("Overwrite files"), userContext.isOverwriteOutput(), userContext);
        overwriteOutput.setId("overwriteOutput");
        overwriteOutput.setGraphic(helpIcon(I18nContext.getInstance()
                .i18n("Set whether \"Overwrite if already exists\" should be enabled by default")));
        overwriteOutput.getStyleClass().addAll(Style.WITH_HELP.css());
        overwriteOutput.getStyleClass().add("spaced-vitem");
        return overwriteOutput;
    }

    @Provides
    @Named("playSounds")
    public PreferenceCheckBox playSounds(UserContext userContext) {
        PreferenceCheckBox playSounds = new PreferenceCheckBox(BooleanUserPreference.PLAY_SOUNDS,
                i18n().tr("Play alert sounds"), userContext.isPlaySounds(), userContext);
        playSounds.setId("playSounds");
        playSounds.setGraphic(helpIcon(i18n().tr("Turn on or off alert sounds")));
        playSounds.getStyleClass().addAll(Style.WITH_HELP.css());
        playSounds.getStyleClass().add("spaced-vitem");
        return playSounds;
    }

    @Provides
    @Named("savePwdInWorkspace")
    public PreferenceCheckBox savePwdInWorkspace(UserContext userContext) {
        PreferenceCheckBox savePwdInWorkspace = new PreferenceCheckBox(BooleanUserPreference.SAVE_PWD_IN_WORKSPACE,
                i18n().tr("Store passwords when saving a workspace file"), userContext.isSavePwdInWorkspaceFile(),
                userContext);
        savePwdInWorkspace.setId("savePwdInWorkspace");
        savePwdInWorkspace.setGraphic(helpIcon(i18n().tr(
                "If an encrypted PDF document has been opened with a password, save the password in the workspace file")));
        savePwdInWorkspace.getStyleClass().addAll(Style.WITH_HELP.css());
        savePwdInWorkspace.getStyleClass().add("spaced-vitem");
        return savePwdInWorkspace;
    }

    @Provides
    @Named("donationNotification")
    public PreferenceCheckBox donationNotification(UserContext userContext) {
        PreferenceCheckBox donationNotification = new PreferenceCheckBox(BooleanUserPreference.DONATION_NOTIFICATION,
                i18n().tr("Show donation window"), userContext.isDonationNotification(), userContext);
        donationNotification.setId("donationNotification");
        donationNotification.setGraphic(helpIcon(i18n().tr(
                "Turn on or off the notification appearing once in a while and asking the user to support PDFsam with a donation")));
        donationNotification.getStyleClass().addAll(Style.WITH_HELP.css());
        donationNotification.getStyleClass().add("spaced-vitem");
        return donationNotification;
    }

    @Provides
    @Named("fetchPremiumModules")
    public PreferenceCheckBox fetchPremiumModules(UserContext userContext) {
        PreferenceCheckBox fetchPremiumModules = new PreferenceCheckBox(BooleanUserPreference.PREMIUM_MODULES,
                i18n().tr("Show premium features"), userContext.isFetchPremiumModules(), userContext);
        fetchPremiumModules.setId("fetchPremiumModules");
        fetchPremiumModules.setGraphic(helpIcon(i18n().tr(
                "Set whether the application should fetch and show premium features description in the modules dashboard")));
        fetchPremiumModules.getStyleClass().addAll(Style.WITH_HELP.css());
        fetchPremiumModules.getStyleClass().add("spaced-vitem");
        return fetchPremiumModules;
    }

    @Provides
    @Named("clearConfirmation")
    public PreferenceCheckBox clearConfirmation(UserContext userContext) {
        PreferenceCheckBox clearConfirmation = new PreferenceCheckBox(BooleanUserPreference.CLEAR_CONFIRMATION,
                i18n().tr("Ask for a confirmation when clearing the selection table"),
                userContext.isAskClearConfirmation(), userContext);
        clearConfirmation.setId("clearConfirmation");
        clearConfirmation.setGraphic(helpIcon(I18nContext.getInstance()
                .i18n("Set whether the application should ask for a confirmation when clearing the selection table")));
        clearConfirmation.getStyleClass().addAll(Style.WITH_HELP.css());
        clearConfirmation.getStyleClass().add("spaced-vitem");
        return clearConfirmation;
    }

    @Provides
    @Named("smartRadio")
    public PreferenceRadioButton smartRadio(UserContext userContext) {
        PreferenceRadioButton smartRadio = new PreferenceRadioButton(BooleanUserPreference.SMART_OUTPUT,
                i18n().tr("Use the selected PDF document directory as output directory"),
                userContext.isUseSmartOutput(), userContext);
        smartRadio.setId("smartRadio");
        return smartRadio;
    }

    @Provides
    @Named("workingDirectory")
    public PreferenceBrowsableDirectoryField workingDirectory(UserContext userContext) {
        PreferenceBrowsableDirectoryField workingDirectory = new PreferenceBrowsableDirectoryField(
                StringUserPreference.WORKING_PATH, userContext);
        workingDirectory.getTextField().setText(userContext.getDefaultWorkingPath());
        workingDirectory.setId("workingDirectory");
        workingDirectory.getStyleClass().add("spaced-vitem");
        return workingDirectory;
    }

    @Provides
    @Named("workspace")
    public PreferenceBrowsableFileField workspace(UserContext userContext) {
        PreferenceBrowsableFileField workspace = new PreferenceBrowsableFileField(StringUserPreference.WORKSPACE_PATH,
                FileType.JSON, OpenType.OPEN, userContext);
        workspace.getTextField().setText(userContext.getDefaultWorkspacePath());
        workspace.setId("workspace");
        workspace.getStyleClass().add("spaced-vitem");
        return workspace;
    }

    @Provides
    @Named("saveWorkspaceOnExit")
    public PreferenceCheckBox saveWorkspaceOnExit(UserContext userContext) {
        PreferenceCheckBox saveWorkspaceOnExit = new PreferenceCheckBox(BooleanUserPreference.SAVE_WORKSPACE_ON_EXIT,
                i18n().tr("Save default workspace on exit"), userContext.isSaveWorkspaceOnExit(), userContext);
        saveWorkspaceOnExit.setId("saveWorkspaceOnExit");
        saveWorkspaceOnExit.setGraphic(
                helpIcon(i18n().tr("If a default workspace is set, save it on application exit")));
        saveWorkspaceOnExit.getStyleClass().addAll(Style.WITH_HELP.css());
        saveWorkspaceOnExit.getStyleClass().add("spaced-vitem");
        return saveWorkspaceOnExit;
    }

    @Provides
    @Named("logViewRowsNumber")
    public PreferenceIntTextField logViewRowsNumber(UserContext userContext) {
        PreferenceIntTextField logRowsNumber = new PreferenceIntTextField(IntUserPreference.LOGVIEW_ROWS_NUMBER,
                userContext, Validators.positiveInteger());
        logRowsNumber.setText(Integer.toString(userContext.getNumberOfLogRows()));
        logRowsNumber.setErrorMessage(i18n().tr("Maximum number of rows mast be a positive number"));
        logRowsNumber.setId("logViewRowsNumber");
        logRowsNumber.validProperty().addListener((o, oldVal, newVal) -> {
            if (newVal == ValidationState.VALID) {
                eventStudio().broadcast(new MaxLogRowsChangedEvent());
            }
        });
        return logRowsNumber;
    }

}
