/*
 * This file is part of the PDF Split And Merge source code
 * Created on 05/mag/2014
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
package org.pdfsam.gui.components.dashboard.tools;

import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.pdfsam.eventstudio.annotation.EventListener;
import org.pdfsam.model.premium.PremiumTool;
import org.pdfsam.model.premium.PremiumToolsResponse;
import org.pdfsam.model.tool.Tool;

import java.util.Collection;
import java.util.Comparator;

import static org.pdfsam.core.context.ApplicationContext.app;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;
import static org.pdfsam.i18n.I18nContext.i18n;

/**
 * Panel showing tools button to in the dashboard. It's used a dashboard home where the users can select the tools the want to use.
 *
 * @author Andrea Vacondio
 */
public class ToolsDashboardPane extends VBox {

    @Inject
    public ToolsDashboardPane() {
        this(app().runtimeState().tools().values());
    }

    ToolsDashboardPane(Collection<Tool> tools) {
        FlowPane toolsPane = new FlowPane();
        getStyleClass().addAll("dashboard-container");
        toolsPane.getStyleClass().add("dashboard-modules");
        Comparator<Tool> compareByPrio = Comparator.comparingInt(m -> m.descriptor().priority());
        tools.stream().sorted(compareByPrio.thenComparing(m -> m.descriptor().name())).map(ToolsDashboardTile::new)
                .forEach(toolsPane.getChildren()::add);
        this.getChildren().add(toolsPane);
        eventStudio().addAnnotatedListeners(this);
    }

    @EventListener
    public void onPremiumModules(PremiumToolsResponse e) {
        if (!e.premiumTools().isEmpty()) {
            Label premiumTile = new Label(i18n().tr("Premium features"));
            premiumTile.getStyleClass().add("modules-tile-title");
            var permiumToolsPanel = new FlowPane();
            permiumToolsPanel.getStyleClass().add("dashboard-modules");
            e.premiumTools().stream().sorted(Comparator.comparingInt(PremiumTool::id)).map(PremiumToolTile::new)
                    .forEach(permiumToolsPanel.getChildren()::add);
            Platform.runLater(() -> this.getChildren().addAll(premiumTile, permiumToolsPanel));
        }
    }
}
