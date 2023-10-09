/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 28/nov/2013
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
package org.pdfsam.ui.selection.multiple;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.pdfsam.eventstudio.annotation.EventListener;
import org.pdfsam.eventstudio.annotation.EventStation;
import org.pdfsam.tool.ToolBound;
import org.pdfsam.pdf.PdfDocumentDescriptor;
import org.pdfsam.pdf.PdfFilesListLoadRequest;
import org.pdfsam.pdf.PdfLoadRequestEvent;
import org.pdfsam.core.support.io.FileType;
import org.pdfsam.ui.commons.ClearModuleEvent;
import org.pdfsam.ui.commons.RemoveSelectedEvent;
import org.pdfsam.ui.io.FileChoosers;
import org.pdfsam.ui.io.RememberingLatestFileChooserWrapper;
import org.pdfsam.ui.io.RememberingLatestFileChooserWrapper.OpenType;
import org.pdfsam.ui.selection.ToolbarButton;
import org.pdfsam.ui.selection.multiple.move.MoveSelectedEvent;
import org.pdfsam.ui.selection.multiple.move.MoveType;
import org.pdfsam.ui.support.Style;

import javafx.event.ActionEvent;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;

/**
 * Toolbar for the selection table
 *
 * @author Andrea Vacondio
 */
class SelectionTableToolbar extends ToolBar implements ToolBound {

    private String ownerModule = StringUtils.EMPTY;

    public SelectionTableToolbar(String ownerModule, boolean canMove) {
        this.ownerModule = defaultString(ownerModule);
        getItems().addAll(new AddButton(ownerModule), new ClearButton(ownerModule), new RemoveButton(ownerModule));
        if (canMove) {
            getItems().addAll(new MoveUpButton(ownerModule), new MoveDownButton(ownerModule));
        }
        getStyleClass().add("selection-tool-bar");
    }

    @Override
    public String toolBinding() {
        return ownerModule;
    }

    /**
     * Button to request the load of the pdf documents selected using a {@link FileChooser}
     *
     * @author Andrea Vacondio
     *
     */
    static class AddButton extends SplitMenuButton implements ToolBound {

        private String ownerModule = StringUtils.EMPTY;

        public AddButton(String ownerModule) {
            this.ownerModule = defaultString(ownerModule);
            getStyleClass().addAll(Style.BUTTON.css());
            getStyleClass().addAll("pdfsam-split-button", "toolbar-splitbutton");
            setTooltip(new Tooltip(i18n().tr("Add documents to the table")));
            setText(i18n().tr("_Add"));
            setOnAction(this::loadDocuments);

            MenuItem fromList = new MenuItem();
            fromList.setText(i18n().tr("PDF list from _text/csv file"));
            fromList.setOnAction(this::loadList);
            getItems().add(fromList);
        }

        public void loadDocuments(ActionEvent event) {
            RememberingLatestFileChooserWrapper fileChooser = FileChoosers.getFileChooser(
                    i18n().tr("Select pdf documents to load"), FileType.PDF);
            List<File> chosenFiles = fileChooser.showOpenMultipleDialog(this.getScene().getWindow());
            if (chosenFiles != null && !chosenFiles.isEmpty()) {
                PdfLoadRequestEvent loadEvent = new PdfLoadRequestEvent(toolBinding());
                chosenFiles.stream().map(PdfDocumentDescriptor::newDescriptorNoPassword).forEach(loadEvent::add);
                eventStudio().broadcast(loadEvent, toolBinding());
            }
        }

        public void loadList(ActionEvent event) {
            RememberingLatestFileChooserWrapper fileChooser = FileChoosers.getFileChooser(
                    i18n().tr("Select a text or CSV file to load"), FileType.CSV, FileType.TXT);
            File chosenFile = fileChooser.showDialog(this.getScene().getWindow(), OpenType.OPEN);
            if (nonNull(chosenFile)) {
                eventStudio().broadcast(new PdfFilesListLoadRequest(toolBinding(), chosenFile.toPath()));
            }
        }

        @Override
        @EventStation
        public String toolBinding() {
            return ownerModule;
        }
    }

    /**
     * Button to request that the selected rows are removed
     * 
     * @author Andrea Vacondio
     * 
     */
    static class RemoveButton extends ToolbarButton {

        public RemoveButton(String ownerModule) {
            super(ownerModule);
            setTooltip(new Tooltip(i18n().tr("Removes selected documents")));
            setText(i18n().tr("_Remove"));
            setOnAction(this::removeSelected);
            setDisable(true);
            eventStudio().addAnnotatedListeners(this);
        }

        public void removeSelected(ActionEvent event) {
            eventStudio().broadcast(new RemoveSelectedEvent(), toolBinding());
        }

        @EventListener
        public void disableIfNoSelection(final SelectionChangedEvent event) {
            setDisable(event.isClearSelection());
        }
    }

    /**
     * Button to request the selection table to clear its data
     *
     * @author Andrea Vacondio
     *
     */
    static class ClearButton extends SplitMenuButton implements ToolBound {

        private String ownerModule = StringUtils.EMPTY;

        public ClearButton(String ownerModule) {
            setId("clear-button");
            this.ownerModule = defaultString(ownerModule);
            getStyleClass().addAll(Style.BUTTON.css());
            getStyleClass().addAll("pdfsam-split-button", "toolbar-splitbutton");
            setTooltip(new Tooltip(i18n().tr("Removes every document")));
            setText(i18n().tr("_Clear"));
            setOnAction(this::clear);

            MenuItem clearAllSettings = new MenuItem();
            clearAllSettings.setText(i18n().tr("C_lear all settings"));
            clearAllSettings.setOnAction(this::clearAll);
            getItems().add(clearAllSettings);
        }

        public void clear(ActionEvent event) {
            eventStudio().broadcast(new ClearModuleEvent(toolBinding(), false, true));
        }

        public void clearAll(ActionEvent event) {
            eventStudio().broadcast(new ClearModuleEvent(toolBinding(), true, true));
        }

        @Override
        @EventStation
        public String toolBinding() {
            return ownerModule;
        }
    }

    /**
     * Base button for move selected rows actions
     * 
     * @author Andrea Vacondio
     * 
     */
    private static class BaseMoveSelectedButton extends ToolbarButton {

        private MoveType type;

        public BaseMoveSelectedButton(String ownerModule, MoveType type) {
            super(ownerModule);
            this.type = type;
            setDisable(true);
            setOnAction(this::moveOnClick);
            eventStudio().addAnnotatedListeners(this);
        }

        public void moveOnClick(ActionEvent event) {
            eventStudio().broadcast(new MoveSelectedEvent(type), toolBinding());
        }

        @EventListener
        public void disableIfCannotMoveDown(final SelectionChangedEvent event) {
            setDisable(!event.canMove(type));
        }
    }

    static class MoveUpButton extends BaseMoveSelectedButton {

        public MoveUpButton(String ownerModule) {
            super(ownerModule, MoveType.UP);
            setTooltip(new Tooltip(i18n().tr("Moves up selected documents")));
            setText(i18n().tr("Move _Up"));
        }
    }

    static class MoveDownButton extends BaseMoveSelectedButton {

        public MoveDownButton(String ownerModule) {
            super(ownerModule, MoveType.DOWN);
            setTooltip(new Tooltip(i18n().tr("Moves down selected documents")));
            setText(i18n().tr("Move _Down"));
        }
    }
}