/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 21/ott/2013
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
package org.pdfsam.ui.commons;

import static java.util.Optional.ofNullable;

import org.pdfsam.i18n.DefaultI18nContext;
import org.pdfsam.ui.support.Style;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;

/**
 * Panel with a "Close" button to be used as bottom of closeable Stage.
 * 
 * @author Andrea Vacondio
 * 
 */
public class ClosePane extends HBox {

    private final EventHandler<ActionEvent> defaultHandler = e -> this.getScene().getWindow().hide();

    public ClosePane() {
        this(null);
    }

    public ClosePane(EventHandler<ActionEvent> handler) {
        setAlignment(Pos.CENTER_RIGHT);
        getStyleClass().addAll(Style.CONTAINER.css());
        Button closeButton = new Button(DefaultI18nContext.getInstance().i18n("Close"));
        closeButton.getStyleClass().addAll(Style.BUTTON.css());
        closeButton.setTextAlignment(TextAlignment.CENTER);
        closeButton.setOnAction(ofNullable(handler).orElse(defaultHandler));
        getChildren().add(closeButton);
    }
}
