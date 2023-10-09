/*
 * This file is part of the PDF Split And Merge source code
 * Created on 14/ago/2014
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
package org.pdfsam.service.update;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pdfsam.core.AppBrand;
import org.pdfsam.core.BrandableProperty;
import org.pdfsam.eventstudio.Listener;
import org.pdfsam.model.update.NoUpdateAvailable;
import org.pdfsam.model.update.UpdateAvailableEvent;
import org.pdfsam.model.update.UpdateCheckRequest;
import org.pdfsam.test.ClearEventStudioExtension;
import org.pdfsam.test.JavaFxThreadExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;

/**
 * @author Andrea Vacondio
 */
@ExtendWith({ ClearEventStudioExtension.class, JavaFxThreadExtension.class })
public class UpdatesControllerTest {

    private UpdatesController victim;
    private UpdateService service;
    private Listener<UpdateAvailableEvent> listener;
    private Listener<NoUpdateAvailable> noUpdatesListener;

    @BeforeEach
    public void setUp() {
        service = mock(UpdateService.class);
        listener = mock(Listener.class);
        noUpdatesListener = mock(Listener.class);
        AppBrand appBrand = mock(AppBrand.class);
        when(appBrand.property(BrandableProperty.VERSION)).thenReturn("3.0.0.M1");
        victim = new UpdatesController(service, appBrand);
    }

    @Test
    public void pasitiveCheckForUpdates() {
        when(service.getLatestVersion()).thenReturn("3.0.0");
        eventStudio().add(UpdateAvailableEvent.class, listener);
        victim.checkForUpdates(new UpdateCheckRequest(false));
        verify(service, timeout(1000).times(1)).getLatestVersion();
        verify(listener, timeout(1000).times(1)).onEvent(any(UpdateAvailableEvent.class));
    }

    @Test
    public void pasitiveCheckForUpdatesNotifyNoUpdates() {
        when(service.getLatestVersion()).thenReturn("3.0.0");
        eventStudio().add(UpdateAvailableEvent.class, listener);
        eventStudio().add(NoUpdateAvailable.class, noUpdatesListener);
        victim.checkForUpdates(new UpdateCheckRequest(true));
        verify(service, timeout(1000).times(1)).getLatestVersion();
        verify(listener, timeout(1000).times(1)).onEvent(any(UpdateAvailableEvent.class));
        verify(noUpdatesListener, after(1000).never()).onEvent(any(NoUpdateAvailable.class));
    }

    @Test
    public void negativeCheckForUpdates() {
        when(service.getLatestVersion()).thenReturn("3.0.0.M1");
        eventStudio().add(UpdateAvailableEvent.class, listener);
        victim.checkForUpdates(new UpdateCheckRequest(false));
        verify(service, timeout(1000).times(1)).getLatestVersion();
        verify(listener, after(1000).never()).onEvent(any(UpdateAvailableEvent.class));
    }

    @Test
    public void negativeCheckForUpdatesNotifyNoUpdates() {
        when(service.getLatestVersion()).thenReturn("3.0.0.M1");
        eventStudio().add(UpdateAvailableEvent.class, listener);
        eventStudio().add(NoUpdateAvailable.class, noUpdatesListener);
        victim.checkForUpdates(new UpdateCheckRequest(true));
        verify(service, timeout(1000).times(1)).getLatestVersion();
        verify(listener, after(1000).never()).onEvent(any(UpdateAvailableEvent.class));
        verify(noUpdatesListener, timeout(1000).times(1)).onEvent(any(NoUpdateAvailable.class));
    }

    @Test
    public void exceptionalCheckForUpdates() {
        when(service.getLatestVersion()).thenThrow(new RuntimeException("Mock"));
        eventStudio().add(UpdateAvailableEvent.class, listener);
        victim.checkForUpdates(new UpdateCheckRequest(false));
        verify(service, timeout(1000).times(1)).getLatestVersion();
        verify(listener, after(1000).never()).onEvent(any(UpdateAvailableEvent.class));
    }
}
