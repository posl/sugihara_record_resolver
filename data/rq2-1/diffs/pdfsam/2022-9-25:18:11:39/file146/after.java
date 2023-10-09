/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 02/mag/2014
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
package org.pdfsam.ui.workarea;

import static org.pdfsam.eventstudio.StaticStudio.eventStudio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.pdfsam.eventstudio.annotation.EventListener;
import org.pdfsam.tool.Tool;
import org.pdfsam.ui.commons.SetActiveModuleRequest;
import org.pdfsam.ui.event.SetTitleEvent;
import org.pdfsam.ui.module.RunButtonTriggerRequest;
import org.pdfsam.ui.quickbar.QuickbarPane;
import org.pdfsam.ui.support.Style;

import javafx.animation.FadeTransition;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

/**
 * Main workarea. It contains a quickbar to quickly access modules and a main area where the module pane is shown.
 * 
 * @author Andrea Vacondio
 *
 */
public class WorkArea extends BorderPane {

    private Map<String, Tool> modules = new HashMap<>();
    private Optional<Tool> current = Optional.empty();
    private ScrollPane center = new ScrollPane();
    private FadeTransition fade = new FadeTransition(new Duration(300), center);

    @Inject
    public WorkArea(List<Tool> tools, QuickbarModuleButtonsPane modulesButtons) {
        getStyleClass().addAll(Style.CONTAINER.css());
        setId("work-area");
        for (Tool tool : tools) {
            this.modules.put(tool.id(), tool);
        }
        fade.setFromValue(0);
        fade.setToValue(1);
        center.setHbarPolicy(ScrollBarPolicy.NEVER);
        center.setFitToWidth(true);
        center.setFitToHeight(true);
        setCenter(center);
        setLeft(new QuickbarPane(modulesButtons));
        eventStudio().addAnnotatedListeners(this);
    }

    @EventListener
    public void onSetActiveModule(SetActiveModuleRequest request) {
        request.getActiveModuleId().ifPresent(id -> {
            Tool requested = modules.get(id);
            if (requested != null) {
                current = Optional.of(requested);
                center.setContent(requested.modulePanel());
                fade.play();
            }
        });
        eventStudio().broadcast(new SetTitleEvent(current.map(m -> m.descriptor().getName()).orElse("")));
    }

    @EventListener
    public void onRunButtonAccelerator(RunButtonTriggerRequest request) {
        if (this.isVisible()) {
            current.ifPresent(m -> eventStudio().broadcast(request, m.id()));
        }
    }
}