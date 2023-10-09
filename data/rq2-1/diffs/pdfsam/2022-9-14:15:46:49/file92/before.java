/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 01/set/2014
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;

import java.util.Locale;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.pdfsam.context.UserContext;
import org.pdfsam.i18n.DefaultI18nContext;
import org.pdfsam.i18n.SetLocaleEvent;
import org.pdfsam.injector.Injector;
import org.pdfsam.injector.Provides;
import org.pdfsam.module.Module;
import org.pdfsam.support.KeyStringValueItem;
import org.pdfsam.test.ClearEventStudioRule;
import org.pdfsam.test.HighPriorityTestModule;
import org.pdfsam.test.InitializeAndApplyJavaFxThreadRule;

/**
 * @author Andrea Vacondio
 *
 */
public class PreferencePaneTest {

    @ClassRule
    public static ClearEventStudioRule STUDIO_RULE = new ClearEventStudioRule();
    @ClassRule
    public static InitializeAndApplyJavaFxThreadRule INIT_FX = new InitializeAndApplyJavaFxThreadRule();
    private Injector injector;

    @BeforeClass
    public static void setUpClass() {
        eventStudio().broadcast(new SetLocaleEvent(Locale.UK.toLanguageTag()));
    }

    @Before
    public void setUp() {
        injector = Injector.start(new Config(), new PreferenceConfig());
    }

    static class Config {
        @Provides
        public UserContext userContext() {
            UserContext userContext = mock(UserContext.class);
            when(userContext.isCheckForUpdates()).thenReturn(Boolean.TRUE);
            when(userContext.isCheckForNews()).thenReturn(Boolean.TRUE);
            when(userContext.isPlaySounds()).thenReturn(Boolean.FALSE);
            when(userContext.isDonationNotification()).thenReturn(Boolean.TRUE);
            when(userContext.isFetchPremiumModules()).thenReturn(Boolean.TRUE);
            when(userContext.isUseSmartOutput()).thenReturn(Boolean.TRUE);
            when(userContext.getDefaultWorkingPath()).thenReturn("/my/path");
            when(userContext.getDefaultWorkspacePath()).thenReturn("/my/path.xml");
            when(userContext.getStartupModule()).thenReturn("");
            return userContext;
        }

        @Provides
        public Module aModule() {
            return new HighPriorityTestModule();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void configOnStartup() {
        PreferencePane victim = injector.instance(PreferencePane.class);
        PreferenceComboBox<KeyStringValueItem<String>> startupModuleCombo = (PreferenceComboBox<KeyStringValueItem<String>>) victim
                .lookup("#startupModuleCombo");
        assertTrue(((PreferenceCheckBox) victim.lookup("#checkForUpdates")).isSelected());
        assertTrue(((PreferenceCheckBox) victim.lookup("#checkForNews")).isSelected());
        assertFalse(((PreferenceCheckBox) victim.lookup("#playSounds")).isSelected());
        assertTrue(((PreferenceCheckBox) victim.lookup("#donationNotification")).isSelected());
        assertTrue(((PreferenceCheckBox) victim.lookup("#fetchPremiumModules")).isSelected());
        assertTrue(((PreferenceRadioButton) victim.lookup("#smartRadio")).isSelected());
        assertEquals("/my/path.xml",
                ((PreferenceBrowsableFileField) victim.lookup("#workspace")).getTextField().getText());
        assertEquals("/my/path",
                ((PreferenceBrowsableDirectoryField) victim.lookup("#workingDirectory")).getTextField().getText());
        assertEquals(DefaultI18nContext.getInstance().i18n("Dashboard"),
                startupModuleCombo.getSelectionModel().getSelectedItem().getValue());
    }

}
