/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 25/ott/2013
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
package org.pdfsam.ui.io;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import org.pdfsam.i18n.DefaultI18nContext;
import org.pdfsam.ui.commons.ValidableTextField;
import org.pdfsam.ui.support.FXValidationSupport.ValidationState;
import org.pdfsam.ui.support.Style;
import org.pdfsam.ui.workspace.RestorableView;

import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * {@link ValidableTextField} with attached a browse button to let the user select a file
 * 
 * @author Andrea Vacondio
 * 
 */
abstract class BrowsableField extends HBox implements RestorableView {
    private static final PseudoClass SELECTED_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("selected");

    private Button browseButton;
    private ValidableTextField textField = new ValidableTextField() {

        @Override
        public void paste() {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.hasString()) {
                String text = clipboard.getString();
                if (text.length() > 2 && text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"') {
                    replaceSelection(text.substring(1, text.length() - 1));
                } else {
                    super.paste();
                }
            }
        }

    };
    private HBox validableContainer;
    private String browseWindowTitle = DefaultI18nContext.getInstance().i18n("Select");

    public BrowsableField() {
        this(new Button(DefaultI18nContext.getInstance().i18n("Browse")));
        browseButton.getStyleClass().addAll(Style.BROWSE_BUTTON.css());
        browseButton.prefHeightProperty().bind(validableContainer.heightProperty());
        browseButton.setMaxHeight(USE_PREF_SIZE);
        browseButton.setMinHeight(USE_PREF_SIZE);
        getChildren().add(browseButton);
    }

    public BrowsableField(Button browseButton) {
        this.browseButton = browseButton;
        HBox.setHgrow(textField, Priority.ALWAYS);
        this.getStyleClass().add("browsable-field");
        validableContainer = new HBox(textField);
        validableContainer.getStyleClass().add("validable-container");
        textField.getStyleClass().add("validable-container-field");
        HBox.setHgrow(validableContainer, Priority.ALWAYS);
        textField.validProperty().addListener((o, oldValue, newValue) -> {
            if (newValue == ValidationState.INVALID) {
                validableContainer.getStyleClass().addAll(Style.INVALID.css());
            } else {
                validableContainer.getStyleClass().removeAll(Style.INVALID.css());
            }
        });
        textField.focusedProperty().addListener(
                (o, oldVal, newVal) -> validableContainer.pseudoClassStateChanged(SELECTED_PSEUDOCLASS_STATE, newVal));
        getChildren().add(validableContainer);
    }

    /**
     * @return the internal {@link ValidableTextField}
     */
    public ValidableTextField getTextField() {
        return textField;
    }

    @Override
    public void saveStateTo(Map<String, String> data) {
        data.put(defaultString(getId()) + "browsableField", defaultString(textField.getText()));
    }

    @Override
    public void restoreStateFrom(Map<String, String> data) {
        textField.setText(Optional.ofNullable(data.get(defaultString(getId()) + "browsableField")).orElse(EMPTY));
    }

    public final void setGraphic(Node value) {
        validableContainer.getChildren().clear();
        if (value != null) {
            validableContainer.getChildren().add(value);
        }
        validableContainer.getChildren().add(textField);
    }

    Button getBrowseButton() {
        return browseButton;
    }

    String getBrowseWindowTitle() {
        return browseWindowTitle;
    }

    public void setBrowseWindowTitle(String browseWindowTitle) {
        this.browseWindowTitle = browseWindowTitle;
    }

    /**
     * Set the text from the given input file
     * 
     * @param inputFile
     */
    abstract void setTextFromFile(File inputFile);
}
