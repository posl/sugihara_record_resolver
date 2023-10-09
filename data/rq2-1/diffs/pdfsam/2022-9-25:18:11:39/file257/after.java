/*
 * This file is part of the PDF Split And Merge source code
 * Created on 26 ott 2015
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
package org.pdfsam.service.news;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.pdfsam.eventstudio.Listener;
import org.pdfsam.model.news.FetchLatestNewsRequest;
import org.pdfsam.model.news.LatestNewsResponse;
import org.pdfsam.model.news.NewImportantNewsEvent;
import org.pdfsam.model.news.NewsData;
import org.pdfsam.model.news.ShowNewsPanelRequest;
import org.pdfsam.test.ClearEventStudioExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;

/**
 * @author Andrea Vacondio
 */
@ExtendWith(ClearEventStudioExtension.class)
public class LatestNewsControllerTest {

    private NewsService service;
    private LatestNewsController victim;

    @BeforeEach
    public void setUp() {
        service = mock(NewsService.class);
        victim = new LatestNewsController(service);
    }

    @Test
    public void noCurrentLastShowNewsPanel() {
        victim.onShowNewsPanel(ShowNewsPanelRequest.INSTANCE);
        verify(service, never()).setLatestNewsSeen(anyInt());
    }

    @Test
    public void fetchLatestNews() {
        NewsData data = new NewsData(5, "title", "content", "20221010", "link", false);
        when(service.getLatestNews()).thenReturn(List.of(data));
        when(service.getLatestNewsSeen()).thenReturn(5);

        Listener<LatestNewsResponse> listener = mock(Listener.class);
        Listener<NewImportantNewsEvent> listenerImportantNews = mock(Listener.class);
        eventStudio().add(LatestNewsResponse.class, listener);
        eventStudio().add(NewImportantNewsEvent.class, listenerImportantNews);
        victim.fetchLatestNews(FetchLatestNewsRequest.INSTANCE);
        verify(service, timeout(1000).times(1)).getLatestNews();
        ArgumentCaptor<LatestNewsResponse> captor = ArgumentCaptor.forClass(LatestNewsResponse.class);
        verify(listener, timeout(1000).times(1)).onEvent(captor.capture());
        verify(listenerImportantNews, never()).onEvent(any());
        assertTrue(captor.getValue().isUpToDate());
        assertEquals(1, captor.getValue().latestNews().size());
        assertEquals(data, captor.getValue().latestNews().get(0));
    }

    @Test
    public void fetchLatestNewsWithImportant() {
        NewsData data = new NewsData(5, "title", "content", "20221010", "link", true);
        when(service.getLatestNews()).thenReturn(List.of(data));
        when(service.getLatestNewsSeen()).thenReturn(5);
        when(service.getLatestImportantNewsSeen()).thenReturn(3);

        Listener<LatestNewsResponse> listener = mock(Listener.class);
        Listener<NewImportantNewsEvent> listenerImportantNews = mock(Listener.class);
        eventStudio().add(LatestNewsResponse.class, listener);
        eventStudio().add(NewImportantNewsEvent.class, listenerImportantNews);
        victim.fetchLatestNews(FetchLatestNewsRequest.INSTANCE);
        verify(service, timeout(1000).times(1)).getLatestNews();
        ArgumentCaptor<NewImportantNewsEvent> captor = ArgumentCaptor.forClass(NewImportantNewsEvent.class);
        verify(listener, timeout(1000).times(1)).onEvent(any());
        verify(listenerImportantNews, timeout(1000).times(1)).onEvent(captor.capture());
        assertEquals(data, captor.getValue().news());
    }

    @Test
    public void fetchLatestNewsWithImportantButSeen() {
        NewsData data = new NewsData(5, "title", "content", "20221010", "link", true);
        when(service.getLatestNews()).thenReturn(List.of(data));
        when(service.getLatestNewsSeen()).thenReturn(5);
        when(service.getLatestImportantNewsSeen()).thenReturn(5);

        Listener<LatestNewsResponse> listener = mock(Listener.class);
        Listener<NewImportantNewsEvent> listenerImportantNews = mock(Listener.class);
        eventStudio().add(LatestNewsResponse.class, listener);
        eventStudio().add(NewImportantNewsEvent.class, listenerImportantNews);
        victim.fetchLatestNews(FetchLatestNewsRequest.INSTANCE);
        verify(service, timeout(1000).times(1)).getLatestNews();
        ArgumentCaptor<NewImportantNewsEvent> captor = ArgumentCaptor.forClass(NewImportantNewsEvent.class);
        verify(listener, timeout(1000).times(1)).onEvent(any());
        verify(listenerImportantNews, never()).onEvent(captor.capture());
    }

    @Test
    public void emptyFetchNews() {
        when(service.getLatestNews()).thenReturn(Collections.emptyList());
        Listener<LatestNewsResponse> listener = mock(Listener.class);
        eventStudio().add(LatestNewsResponse.class, listener);
        victim.fetchLatestNews(FetchLatestNewsRequest.INSTANCE);
        verify(service, timeout(1000).times(1)).getLatestNews();
        verify(listener, after(1000).never()).onEvent(any());
    }

    @Test
    public void nullFetchNews() {
        when(service.getLatestNews()).thenReturn(null);
        Listener<LatestNewsResponse> listener = mock(Listener.class);
        eventStudio().add(LatestNewsResponse.class, listener);
        victim.fetchLatestNews(FetchLatestNewsRequest.INSTANCE);
        verify(service, timeout(1000).times(1)).getLatestNews();
        verify(listener, after(1000).never()).onEvent(any());
    }

    @Test
    public void failingFetchNews() {
        when(service.getLatestNews()).thenThrow(new RuntimeException());
        Listener<LatestNewsResponse> listener = mock(Listener.class);
        eventStudio().add(LatestNewsResponse.class, listener);
        victim.fetchLatestNews(FetchLatestNewsRequest.INSTANCE);
        verify(service, timeout(1000).times(1)).getLatestNews();
        verify(listener, after(1000).never()).onEvent(any());
    }

    @Test
    public void onShowNewsPanel() {
        NewsData data = new NewsData(5, "title", "content", "20221010", "link", false);
        when(service.getLatestNews()).thenReturn(List.of(data));
        when(service.getLatestNewsSeen()).thenReturn(4);
        Listener<LatestNewsResponse> listener = mock(Listener.class);
        eventStudio().add(LatestNewsResponse.class, listener);
        victim.fetchLatestNews(FetchLatestNewsRequest.INSTANCE);
        verify(listener, timeout(1000).times(1)).onEvent(any());
        victim.onShowNewsPanel(ShowNewsPanelRequest.INSTANCE);
        verify(service).setLatestNewsSeen(5);
    }
}
