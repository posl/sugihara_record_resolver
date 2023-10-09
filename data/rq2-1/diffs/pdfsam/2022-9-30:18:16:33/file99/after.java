/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 06/nov/2013
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
package org.pdfsam.ui.components.quickbar;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

/**
 * Toggle button to expand/collapse a quickbar
 * 
 * @author Andrea Vacondio
 * 
 */
class ExpandButton extends HBox {
    private ToggleButton toggle = new ToggleButton();
    private SVGPath expand = new SVGPath();
    private SVGPath collapse = new SVGPath();

    public ExpandButton() {
        getStyleClass().add("quickbar-expand-button");
        toggle.getStyleClass().addAll("pdfsam-toolbar-button", "quickbar-expand-toggle");
        expand.setContent("M0,-5L5,0L0,5Z");
        expand.getStyleClass().add("quickbar-button-arrow");
        collapse.setContent("M0,-5L-5,0L0,5Z");
        collapse.getStyleClass().add("quickbar-button-arrow");
        toggle.setGraphic(expand);
        toggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                toggle.setGraphic(collapse);
            } else {
                toggle.setGraphic(expand);
            }
        });
        HBox.setMargin(toggle, new Insets(0, 7, 0, 7));
        getChildren().add(toggle);
    }

    public final BooleanProperty selectedProperty() {
        return toggle.selectedProperty();
    }
}
