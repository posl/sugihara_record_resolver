/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 26/nov/2013
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

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;

import java.util.Comparator;

/**
 * Definition of a column in the selection table
 * 
 * @author Andrea Vacondio
 * @param <T>
 *            type of the column data
 */
public interface SelectionTableColumn<T> extends TableColumnProvider<T> {

    /**
     * @return the title for the column
     */
    String getColumnTitle();

    /**
     * @return the comparator used to sort the elements
     */
    Comparator<T> comparator();

    /**
     * @return the cell factory used to create the {@link TableCell}
     */
    default Callback<TableColumn<SelectionTableRowData, T>, TableCell<SelectionTableRowData, T>> cellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<SelectionTableRowData, T> call(TableColumn<SelectionTableRowData, T> param) {
                return new TableCell<>() {
                    @Override
                    public void updateItem(final T item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("");
                        } else {
                            setText(getTextValue(item));
                        }
                    }
                };
            }
        };
    }

    /**
     * @param item
     * @return the String representation of the given item
     */
    String getTextValue(T item);

    /**
     * @return the cell value factory used to extract data from the data model
     */
    default Callback<CellDataFeatures<SelectionTableRowData, T>, ObservableValue<T>> cellValueFactory() {
        return param -> {
            if (param.getValue() != null) {
                return getObservableValue(param.getValue());
            }
            return null;
        };
    }

    /**
     * @param data
     * @return the data value extracted from the back bean
     */
    ObservableValue<T> getObservableValue(SelectionTableRowData data);

    @Override
    default TableColumn<SelectionTableRowData, T> getTableColumn() {
        TableColumn<SelectionTableRowData, T> tableColumn = new TableColumn<>(getColumnTitle());
        tableColumn.setCellFactory(cellFactory());
        tableColumn.setCellValueFactory(cellValueFactory());
        tableColumn.setComparator(comparator());
        return tableColumn;
    }

}