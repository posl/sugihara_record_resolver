/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 30 ago 2016
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

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.util.Comparator;

import org.pdfsam.i18n.DefaultI18nContext;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Definition of the pace column of the selection table
 * 
 * @author Andrea Vacondio
 *
 */
public class PaceColumn implements SelectionTableColumn<String> {

    public PaceColumn() {
        // nothing
    }

    @Override
    public String getColumnTitle() {
        return DefaultI18nContext.getInstance().i18n("Pace");
    }

    @Override
    public ObservableValue<String> getObservableValue(SelectionTableRowData data) {
        return data.pace;
    }

    @Override
    public String getTextValue(String item) {
        return defaultString(item, "1");
    }

    @Override
    public Comparator<String> comparator() {
        return Comparator.naturalOrder();
    }

    @Override
    public TableColumn<SelectionTableRowData, String> getTableColumn() {
        TableColumn<SelectionTableRowData, String> tableColumn = SelectionTableColumn.super.getTableColumn();
        tableColumn.setEditable(true);
        tableColumn.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).pace
                .set(defaultIfBlank(t.getNewValue(), "1")));
        return tableColumn;
    }

    @Override
    public Callback<TableColumn<SelectionTableRowData, String>, TableCell<SelectionTableRowData, String>> cellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<SelectionTableRowData, String> call(TableColumn<SelectionTableRowData, String> param) {
                return new TooltippedTextFieldTableCell(DefaultI18nContext.getInstance().i18n(
                        "Double click to set the number of pages after which the task will switch to the next file"));
            }
        };
    }

}
