/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 04/set/2014
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
package org.pdfsam.gui.components.quickbar;

import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Panel showing quickbar buttons and having an expand/collapse feature
 * 
 * @author Andrea Vacondio
 *
 */
public class QuickbarPane extends VBox {

    public QuickbarPane(BaseQuickbarButtonsPane items) {
        ExpandButton expandButton = new ExpandButton();
        getStyleClass().add("quickbar");
        items.displayTextProperty().bind(expandButton.selectedProperty());
        VBox buttonContainer = new VBox(expandButton, items);
        buttonContainer.getStyleClass().add("quickbar-buttons");
        setVgrow(buttonContainer, Priority.ALWAYS);
        getChildren().addAll(buttonContainer);
    }
}
