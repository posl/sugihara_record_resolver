/*
 * This file is part of the PDF Split And Merge source code
 * Created on 27/nov/2013
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
package org.pdfsam.ui.components.selection.multiple;

import javafx.application.Platform;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.unicons.UniconsLine;
import org.pdfsam.core.context.BooleanPersistentProperty;
import org.pdfsam.core.support.EncryptionUtils;
import org.pdfsam.eventstudio.annotation.EventListener;
import org.pdfsam.eventstudio.annotation.EventStation;
import org.pdfsam.model.io.NativeOpenFileRequest;
import org.pdfsam.model.pdf.PdfDocumentDescriptor;
import org.pdfsam.model.pdf.PdfLoadRequest;
import org.pdfsam.model.tool.ClearToolRequest;
import org.pdfsam.model.tool.ToolBound;
import org.pdfsam.model.ui.ShowPdfDescriptorRequest;
import org.pdfsam.model.ui.dnd.FilesDroppedEvent;
import org.pdfsam.model.ui.workspace.RestorableView;
import org.pdfsam.ui.components.selection.PasswordFieldPopup;
import org.pdfsam.ui.components.selection.RemoveSelectedEvent;
import org.pdfsam.ui.components.selection.SetPageRangesRequest;
import org.pdfsam.ui.components.selection.ShowPasswordFieldPopupRequest;
import org.pdfsam.ui.components.selection.multiple.move.MoveSelectedEvent;
import org.pdfsam.ui.components.selection.multiple.move.MoveType;
import org.pdfsam.ui.components.selection.multiple.move.SelectionAndFocus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.pdfsam.core.context.ApplicationContext.app;
import static org.pdfsam.core.support.EncryptionUtils.encrypt;
import static org.pdfsam.core.support.io.ObjectCollectionWriter.writeContent;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;
import static org.pdfsam.i18n.I18nContext.i18n;
import static org.pdfsam.model.ui.SetDestinationRequest.requestDestination;
import static org.pdfsam.model.ui.SetDestinationRequest.requestFallbackDestination;

/**
 * Table displaying selected pdf documents
 *
 * @author Andrea Vacondio
 */
public class SelectionTable extends TableView<SelectionTableRowData> implements ToolBound, RestorableView {

    private static final Logger LOG = LoggerFactory.getLogger(SelectionTable.class);

    private static final DataFormat DND_TABLE_SELECTION_MIME_TYPE = new DataFormat(
            "application/x-java-table-selection-list");

    private String toolBinding = StringUtils.EMPTY;
    private final Label placeHolder = new Label(i18n().tr("Drag and drop PDF files here"));
    private PasswordFieldPopup passwordPopup;
    private Consumer<SelectionChangedEvent> selectionChangedConsumer;

    public SelectionTable(String toolBinding, boolean canDuplicateItems, boolean canMove,
            TableColumnProvider<?>... columns) {
        this.toolBinding = defaultString(toolBinding);
        setEditable(true);
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        getColumns().add(new IndexColumn());
        Arrays.stream(columns).forEach(c -> getColumns().add(c.getTableColumn()));
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        getStyleClass().add("selection-table");
        initDragAndDrop(canMove);
        getSelectionModel().getSelectedIndices().addListener((Change<? extends Integer> c) -> {

            ObservableList<? extends Integer> selected = c.getList();
            if (selected.isEmpty()) {
                eventStudio().broadcast(SelectionChangedEvent.clearSelectionEvent(), toolBinding);
                LOG.trace("Selection cleared for {}", toolBinding);
            } else {
                SelectionChangedEvent newSelectionEvent = SelectionChangedEvent.select(selected).ofTotalRows(getItems().size());
                eventStudio().broadcast(newSelectionEvent, toolBinding);
                LOG.trace("{} for {}", newSelectionEvent, toolBinding);
            }

        });
        placeHolder.getStyleClass().add("drag-drop-placeholder");
        placeHolder.setDisable(true);
        setPlaceholder(placeHolder);
        passwordPopup = new PasswordFieldPopup(this.toolBinding);

        ContextMenu contextMenu = new ContextMenu();
        initTopSectionContextMenu(contextMenu, Arrays.stream(columns).anyMatch(PageRangesColumn.class::isInstance));
        initItemsSectionContextMenu(contextMenu, canDuplicateItems, canMove);
        initBottomSectionContextMenu(contextMenu);
        setContextMenu(contextMenu);
        eventStudio().addAnnotatedListeners(this);
        eventStudio().add(SelectionChangedEvent.class, e -> selectionChangedConsumer.accept(e), toolBinding);
    }

    private void initTopSectionContextMenu(ContextMenu contextMenu, boolean hasRanges) {
        MenuItem setDestinationItem = createMenuItem(i18n().tr("Set destination"), UniconsLine.CROSSHAIR);
        setDestinationItem.setOnAction(e -> eventStudio().broadcast(
                requestDestination(getSelectionModel().getSelectedItem().descriptor().getFile(), toolBinding()),
                toolBinding()));
        setDestinationItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.ALT_DOWN));

        selectionChangedConsumer = e -> setDestinationItem.setDisable(!e.isSingleSelection());
        contextMenu.getItems().add(setDestinationItem);

        if (hasRanges) {
            MenuItem setPageRangesItem = createMenuItem(i18n().tr("Set as range for all"),
                    UniconsLine.RIGHT_INDENT_ALT);
            setPageRangesItem.setOnAction(e -> eventStudio().broadcast(
                    new SetPageRangesRequest(getSelectionModel().getSelectedItem().pageSelection.get()),
                    toolBinding()));
            setPageRangesItem.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
            selectionChangedConsumer = selectionChangedConsumer.andThen(
                    e -> setPageRangesItem.setDisable(!e.isSingleSelection()));
            contextMenu.getItems().add(setPageRangesItem);
        }
        contextMenu.getItems().add(new SeparatorMenuItem());
    }

    private void initItemsSectionContextMenu(ContextMenu contextMenu, boolean canDuplicate, boolean canMove) {

        MenuItem removeSelected = createMenuItem(i18n().tr("Remove"), UniconsLine.MINUS);
        removeSelected.setOnAction(e -> eventStudio().broadcast(new RemoveSelectedEvent(), toolBinding()));
        removeSelected.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        contextMenu.getItems().add(removeSelected);
        selectionChangedConsumer = selectionChangedConsumer.andThen(
                e -> removeSelected.setDisable(e.isClearSelection()));
        if (canMove) {
            MenuItem moveTopSelected = createMenuItem(i18n().tr("Move to Top"), UniconsLine.ANGLE_DOUBLE_UP);
            moveTopSelected.setOnAction(
                    e -> eventStudio().broadcast(new MoveSelectedEvent(MoveType.TOP), toolBinding()));

            MenuItem moveUpSelected = createMenuItem(i18n().tr("Move Up"), UniconsLine.ANGLE_UP);
            moveUpSelected.setOnAction(e -> eventStudio().broadcast(new MoveSelectedEvent(MoveType.UP), toolBinding()));

            MenuItem moveDownSelected = createMenuItem(i18n().tr("Move Down"), UniconsLine.ANGLE_DOWN);
            moveDownSelected.setOnAction(
                    e -> eventStudio().broadcast(new MoveSelectedEvent(MoveType.DOWN), toolBinding()));

            MenuItem moveBottomSelected = createMenuItem(i18n().tr("Move to Bottom"), UniconsLine.ANGLE_DOUBLE_DOWN);
            moveBottomSelected.setOnAction(
                    e -> eventStudio().broadcast(new MoveSelectedEvent(MoveType.BOTTOM), toolBinding()));

            contextMenu.getItems().addAll(moveTopSelected, moveUpSelected, moveDownSelected, moveBottomSelected);

            moveBottomSelected.setAccelerator(new KeyCodeCombination(KeyCode.END, KeyCombination.ALT_DOWN));
            moveDownSelected.setAccelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.ALT_DOWN));
            moveUpSelected.setAccelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.ALT_DOWN));
            moveTopSelected.setAccelerator(new KeyCodeCombination(KeyCode.HOME, KeyCombination.ALT_DOWN));

            selectionChangedConsumer = selectionChangedConsumer.andThen(e -> {
                moveTopSelected.setDisable(!e.canMove(MoveType.TOP));
                moveUpSelected.setDisable(!e.canMove(MoveType.UP));
                moveDownSelected.setDisable(!e.canMove(MoveType.DOWN));
                moveBottomSelected.setDisable(!e.canMove(MoveType.BOTTOM));
            });
        }
        if (canDuplicate) {
            MenuItem duplicateItem = createMenuItem(i18n().tr("Duplicate"), UniconsLine.COPY);
            duplicateItem.setOnAction(e -> eventStudio().broadcast(new DuplicateSelectedEvent(), toolBinding()));
            duplicateItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.ALT_DOWN));

            contextMenu.getItems().add(duplicateItem);

            selectionChangedConsumer = selectionChangedConsumer.andThen(
                    e -> duplicateItem.setDisable(e.isClearSelection()));
        }
    }

    private void initBottomSectionContextMenu(ContextMenu contextMenu) {

        MenuItem copyItem = createMenuItem(i18n().tr("Copy to clipboard"), UniconsLine.COPY_ALT);
        copyItem.setOnAction(e -> copySelectedToClipboard());

        MenuItem infoItem = createMenuItem(i18n().tr("Document properties"), UniconsLine.INFO_CIRCLE);
        infoItem.setOnAction(e -> Platform.runLater(() -> eventStudio().broadcast(
                new ShowPdfDescriptorRequest(getSelectionModel().getSelectedItem().descriptor()))));

        MenuItem openFileItem = createMenuItem(i18n().tr("Open"), UniconsLine.FILE_ALT);
        openFileItem.setOnAction(e -> eventStudio().broadcast(
                new NativeOpenFileRequest(getSelectionModel().getSelectedItem().descriptor().getFile())));

        MenuItem openFolderItem = createMenuItem(i18n().tr("Open Folder"), UniconsLine.FOLDER);
        openFolderItem.setOnAction(e -> eventStudio().broadcast(new NativeOpenFileRequest(
                getSelectionModel().getSelectedItem().descriptor().getFile().getParentFile())));

        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
        infoItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.ALT_DOWN));
        openFileItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        openFolderItem.setAccelerator(
                new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN));

        contextMenu.getItems().addAll(new SeparatorMenuItem(), copyItem, infoItem, openFileItem, openFolderItem);

        selectionChangedConsumer = selectionChangedConsumer.andThen(e -> {
            copyItem.setDisable(e.isClearSelection());
            infoItem.setDisable(!e.isSingleSelection());
            openFileItem.setDisable(!e.isSingleSelection());
            openFolderItem.setDisable(!e.isSingleSelection());
        });
    }

    private MenuItem createMenuItem(String text, Ikon icon) {
        var item = new MenuItem(text);
        //TODO set font size 1.1 em
        item.setGraphic(FontIcon.of(icon));
        item.setDisable(true);
        return item;
    }

    private void initDragAndDrop(boolean canMove) {
        setOnDragOver(e -> dragConsume(e, this.onDragOverConsumer()));
        setOnDragEntered(e -> dragConsume(e, this.onDragEnteredConsumer()));
        setOnDragExited(this::onDragExited);
        setOnDragDropped(e -> dragConsume(e, this.onDragDropped()));
        if (canMove) {
            setRowFactory(tv -> {
                TableRow<SelectionTableRowData> row = new TableRow<>();
                row.setOnDragDetected(e -> {
                    ArrayList<Integer> selection = new ArrayList<>(getSelectionModel().getSelectedIndices());
                    if (!row.isEmpty() && !selection.isEmpty()) {
                        Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                        db.setDragView(row.snapshot(null, null));
                        ClipboardContent cc = new ClipboardContent();
                        cc.put(DND_TABLE_SELECTION_MIME_TYPE, selection);
                        db.setContent(cc);
                        e.consume();

                    }
                });

                row.setOnDragOver(e -> {
                    if (e.getGestureSource() != row && e.getDragboard().hasContent(DND_TABLE_SELECTION_MIME_TYPE)) {
                        if (!((List<Integer>) e.getDragboard().getContent(DND_TABLE_SELECTION_MIME_TYPE)).contains(
                                row.getIndex())) {
                            e.acceptTransferModes(TransferMode.MOVE);
                            e.consume();
                        }
                    }
                });
                row.setOnDragEntered(e -> {
                    if (!row.isEmpty() && e.getDragboard().hasContent(DND_TABLE_SELECTION_MIME_TYPE)) {
                        if (!((List<Integer>) e.getDragboard().getContent(DND_TABLE_SELECTION_MIME_TYPE)).contains(
                                row.getIndex())) {
                            row.setOpacity(0.6);
                        }
                    }
                });
                row.setOnDragExited(e -> {
                    if (!row.isEmpty() && e.getDragboard().hasContent(DND_TABLE_SELECTION_MIME_TYPE)) {
                        if (!((List<Integer>) e.getDragboard().getContent(DND_TABLE_SELECTION_MIME_TYPE)).contains(
                                row.getIndex())) {
                            row.setOpacity(1);
                        }
                    }
                });

                row.setOnDragDropped(e -> {
                    Dragboard db = e.getDragboard();
                    if (db.hasContent(DND_TABLE_SELECTION_MIME_TYPE)) {
                        Optional<SelectionTableRowData> focus = ofNullable(getFocusModel().getFocusedItem());
                        Optional<SelectionTableRowData> toDrop = of(row).filter(r -> !r.isEmpty())
                                .map(TableRow::getIndex).map(getItems()::get);

                        List<Integer> dragged = (List<Integer>) e.getDragboard()
                                .getContent(DND_TABLE_SELECTION_MIME_TYPE);
                        List<SelectionTableRowData> toMove = dragged.stream().map(getItems()::get)
                                .filter(Objects::nonNull).collect(Collectors.toList());
                        getItems().removeAll(toMove);

                        int dropIndex = getItems().size();
                        if (toDrop.isPresent()) {
                            int toDropNewIndex = toDrop.map(getItems()::indexOf).get();
                            if (toDropNewIndex == row.getIndex()) {
                                // we dropped up
                                dropIndex = toDropNewIndex;
                            } else {
                                // we dropped down
                                dropIndex = toDropNewIndex + 1;
                            }
                        }
                        getSortOrder().clear();
                        getItems().addAll(dropIndex, toMove);
                        e.setDropCompleted(true);
                        getSelectionModel().clearSelection();
                        getSelectionModel().selectRange(dropIndex, dropIndex + toMove.size());
                        focus.map(getItems()::indexOf).ifPresent(getFocusModel()::focus);
                        e.consume();
                    }
                });

                return row;
            });
        }
    }

    private void dragConsume(DragEvent e, Consumer<DragEvent> c) {
        if (e.getDragboard().hasFiles()) {
            c.accept(e);
        }
        e.consume();
    }

    private Consumer<DragEvent> onDragOverConsumer() {
        return (DragEvent e) -> e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
    }

    private Consumer<DragEvent> onDragEnteredConsumer() {
        return (DragEvent e) -> placeHolder.setDisable(false);

    }

    private void onDragExited(DragEvent e) {
        placeHolder.setDisable(true);
        e.consume();
    }

    private Consumer<DragEvent> onDragDropped() {
        return (DragEvent e) -> {
            eventStudio().broadcast(new FilesDroppedEvent(toolBinding, true, e.getDragboard().getFiles()));
            e.setDropCompleted(true);
        };
    }

    @Override
    @EventStation
    public String toolBinding() {
        return toolBinding;
    }

    @EventListener(priority = Integer.MIN_VALUE)
    public void onLoadDocumentsRequest(PdfLoadRequest loadEvent) {
        getItems().addAll(loadEvent.getDocuments().stream().map(SelectionTableRowData::new).toList());
        this.sort();
        loadEvent.getDocuments().stream().findFirst().ifPresent(
                f -> eventStudio().broadcast(requestFallbackDestination(f.getFile(), toolBinding()), toolBinding()));
        eventStudio().broadcast(loadEvent);
    }

    @EventListener
    public void onDuplicate(final DuplicateSelectedEvent event) {
        LOG.trace("Duplicating selected items");
        getSelectionModel().getSelectedItems().forEach(i -> getItems().add(i.duplicate()));
        this.sort();
    }

    @EventListener
    public void onClear(final ClearToolRequest event) {
        getItems().forEach(d -> d.descriptor().releaseAll());
        getSelectionModel().clearSelection();
        getItems().clear();
    }

    @EventListener
    public void onRemoveSelected(RemoveSelectedEvent event) {
        SortedSet<Integer> indices = new TreeSet<>(Collections.reverseOrder());
        indices.addAll(getSelectionModel().getSelectedIndices());
        LOG.trace("Removing {} items", indices.size());
        indices.forEach(i -> getItems().remove(i.intValue()).invalidate());
        requestFocus();
    }

    @EventListener
    public void onMoveSelected(final MoveSelectedEvent event) {
        getSortOrder().clear();
        ObservableList<Integer> selectedIndices = getSelectionModel().getSelectedIndices();
        Integer[] selected = selectedIndices.toArray(new Integer[selectedIndices.size()]);
        int focus = getFocusModel().getFocusedIndex();
        getSelectionModel().clearSelection();
        SelectionAndFocus newSelection = event.type().move(selected, getItems(), focus);
        if (!SelectionAndFocus.NULL.equals(newSelection)) {
            LOG.trace("Changing selection to {}", newSelection);
            getSelectionModel().selectIndices(newSelection.getRow(), newSelection.getRows());
            getFocusModel().focus(newSelection.getFocus());
            scrollTo(Math.max(newSelection.getRow() - 1, 0));
        }
    }

    @EventListener
    public void onSetPageRanges(SetPageRangesRequest event) {
        getItems().stream().forEach(i -> i.pageSelection.set(event.range()));
    }

    @EventListener
    public void showPasswordFieldPopup(ShowPasswordFieldPopupRequest request) {
        Scene scene = this.getScene();
        if (scene != null) {
            Window owner = scene.getWindow();
            if (owner != null && owner.isShowing()) {
                Point2D nodeCoord = request.requestingNode().localToScene(request.requestingNode().getWidth() / 2,
                        request.requestingNode().getHeight() / 1.5);
                double anchorX = Math.round(owner.getX() + scene.getX() + nodeCoord.getX() + 2);
                double anchorY = Math.round(owner.getY() + scene.getY() + nodeCoord.getY() + 2);
                passwordPopup.showFor(this, request.pdfDescriptor(), anchorX, anchorY);
            }
        }
    }

    private void copySelectedToClipboard() {
        ClipboardContent content = new ClipboardContent();
        writeContent(getSelectionModel().getSelectedItems().stream().map(item -> {
            return item.descriptor().getFile().getAbsolutePath() + ", " + item.descriptor().getFile().length() + ", "
                    + item.descriptor().pages().getValue();
        }).collect(Collectors.toList())).to(content);
        Clipboard.getSystemClipboard().setContent(content);
    }

    @Override
    public void saveStateTo(Map<String, String> data) {
        data.put(defaultString(getId()) + "input.size", Integer.toString(getItems().size()));
        IntStream.range(0, getItems().size()).forEach(i -> {
            SelectionTableRowData current = getItems().get(i);
            String id = defaultString(getId());
            data.put(id + "input." + i, current.descriptor().getFile().getAbsolutePath());
            if (app().persistentSettings().get(BooleanPersistentProperty.SAVE_PWD_IN_WORKSPACE)) {
                data.put(id + "input.password.enc" + i, encrypt(current.descriptor().getPassword()));
            }
            data.put(id + "input.range." + i, defaultString(current.pageSelection.get()));
            data.put(id + "input.step." + i, defaultString(current.pace.get()));
            data.put(id + "input.reverse." + i, Boolean.toString(current.reverse.get()));
        });
    }

    @Override
    public void restoreStateFrom(Map<String, String> data) {
        onClear(null);
        int size = Optional.ofNullable(data.get(defaultString(getId()) + "input.size")).map(Integer::valueOf).orElse(0);
        if (size > 0) {
            PdfLoadRequest loadEvent = new PdfLoadRequest(toolBinding());
            List<SelectionTableRowData> items = new ArrayList<>();
            IntStream.range(0, size).forEach(i -> {
                String id = defaultString(getId());
                Optional.ofNullable(data.get(id + "input." + i)).ifPresent(f -> {
                    PdfDocumentDescriptor descriptor = PdfDocumentDescriptor.newDescriptor(new File(f),
                            ofNullable(data.get(id + "input.password.enc" + i)).map(EncryptionUtils::decrypt)
                                    .orElseGet(() -> data.get(defaultString(getId()) + "input.password." + i)));
                    loadEvent.add(descriptor);
                    SelectionTableRowData row = new SelectionTableRowData(descriptor);
                    row.pageSelection.set(data.get(id + "input.range." + i));
                    row.pace.set(data.get(id + "input.step." + i));
                    row.reverse.set(Boolean.parseBoolean(data.get(id + "input.reverse." + i)));
                    items.add(row);
                });
            });
            getItems().addAll(items);
            eventStudio().broadcast(loadEvent);
        }

    }
}
