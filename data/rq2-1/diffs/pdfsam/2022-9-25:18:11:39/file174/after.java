/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 22/ago/2014
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
package org.pdfsam.ui.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.AccessDeniedException;
import java.util.Collections;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.pdfsam.core.BrandableProperty;
import org.pdfsam.core.AppBrand;
import org.pdfsam.core.context.UserContext;
import org.pdfsam.service.tool.UsageService;
import org.pdfsam.news.NewImportantNewsEvent;
import org.pdfsam.news.NewsData;
import org.pdfsam.test.ClearEventStudioRule;
import org.pdfsam.test.InitializeAndApplyJavaFxThreadRule;
import org.pdfsam.update.NoUpdateAvailable;
import org.pdfsam.update.UpdateAvailableEvent;
import org.sejda.model.exception.InvalidTaskParametersException;
import org.sejda.model.exception.TaskIOException;
import org.sejda.model.notification.event.TaskExecutionCompletedEvent;
import org.sejda.model.notification.event.TaskExecutionFailedEvent;

/**
 * @author Andrea Vacondio
 *
 */
public class NotificationsControllerTest {

    @ClassRule
    public static ClearEventStudioRule STUDIO_RULE = new ClearEventStudioRule();
    @Rule
    public InitializeAndApplyJavaFxThreadRule javaFxThread = new InitializeAndApplyJavaFxThreadRule();
    private UsageService service;
    private NotificationsContainer container;
    private NotificationsController victim;
    private UserContext context;

    @Before
    public void setUp() {
        service = mock(UsageService.class);
        container = mock(NotificationsContainer.class);
        AppBrand appBrand = mock(AppBrand.class);
        context = mock(UserContext.class);
        when(appBrand.property(BrandableProperty.DOWNLOAD_URL)).thenReturn("http://www.pdfsam.org");
        when(appBrand.property(BrandableProperty.DONATE_URL)).thenReturn("http://www.pdfsam.org");
        when(appBrand.property(BrandableProperty.TWEETER_SHARE_URL)).thenReturn("http://www.pdfsam.org");
        when(appBrand.property(BrandableProperty.FACEBOOK_SHARE_URL)).thenReturn("http://www.pdfsam.org");
        when(context.isDonationNotification()).thenReturn(true);
        victim = new NotificationsController(container, service, appBrand, context);
    }

    @Test
    public void onAddRequest() {
        AddNotificationRequestEvent event = new AddNotificationRequestEvent(NotificationType.INFO, "msg", "title");
        victim.onAddRequest(event);
        verify(container).addNotification(eq("title"), any());
    }

    @Test
    public void onRemoveRequest() {
        RemoveNotificationRequestEvent event = new RemoveNotificationRequestEvent("id");
        victim.onRemoveRequest(event);
        verify(container).removeNotification("id");
    }

    @Test
    public void onUpdateAvailable() {
        UpdateAvailableEvent event = new UpdateAvailableEvent("new version");
        victim.onUpdateAvailable(event);
        verify(container).addStickyNotification(anyString(), any());
    }

    @Test
    public void onNoUpdateAvailable() {
        NoUpdateAvailable event = new NoUpdateAvailable();
        victim.onNoUpdateAvailable(event);
        verify(container).addNotification(anyString(), any());
    }

    @Test
    public void onTaskFailed() {
        TaskExecutionFailedEvent event = new TaskExecutionFailedEvent(new Exception("some exception"), null);
        victim.onTaskFailed(event);
        verify(container, never()).addNotification(anyString(), any());
    }

    @Test
    public void onInvalidParameters() {
        TaskExecutionFailedEvent event = new TaskExecutionFailedEvent(
                new InvalidTaskParametersException("", Collections.emptyList()), null);
        victim.onTaskFailed(event);
        verify(container).addNotification(anyString(), any());
    }

    @Test
    public void onAccessDenied() {
        TaskExecutionFailedEvent event = new TaskExecutionFailedEvent(
                new TaskIOException(new AccessDeniedException("the file")), null);
        victim.onTaskFailed(event);
        verify(container).addNotification(anyString(), any());
    }

    @Test
    public void onTaskCompleteAndNoProDisplay() {
        when(service.getTotalUsage()).thenReturn(1L);
        victim.onTaskCompleted(new TaskExecutionCompletedEvent(1, null));
        when(service.getTotalUsage()).thenReturn(6L);
        victim.onTaskCompleted(new TaskExecutionCompletedEvent(1, null));
        verify(container, never()).addStickyNotification(anyString(), any());
    }

    @Test
    public void onTaskCompleteAndProDisplay() {
        when(service.getTotalUsage()).thenReturn(5L);
        victim.onTaskCompleted(new TaskExecutionCompletedEvent(1, null));
        verify(container).addStickyNotification(anyString(), any());
    }

    @Test
    public void onTaskCompleteDontDisplaySettingIsOn() {
        when(context.isDonationNotification()).thenReturn(false);
        when(service.getTotalUsage()).thenReturn(5L);
        victim.onTaskCompleted(new TaskExecutionCompletedEvent(1, null));
        verify(container, never()).addStickyNotification(anyString(), any());
    }

    @Test
    public void onNewImportantNews() {
        NewsData data = new NewsData();
        data.setTitle("title");
        data.setContent("content");
        data.setLink("link");
        NewImportantNewsEvent event = new NewImportantNewsEvent(data);
        victim.onNewImportantNews(event);
        verify(container).addStickyNotification(eq("title"), any());
    }
}
