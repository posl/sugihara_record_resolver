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
package org.pdfsam.ui.selection.multiple;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import java.util.Comparator;

import org.pdfsam.i18n.DefaultI18nContext;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Definition of the page ranges column of the selection table
 * 
 * @author Andrea Vacondio
 * 
 */
public class PageRangesColumn implements SelectionTableColumn<String> {
    private String tooltipMessage = DefaultI18nContext.getInstance()
            .i18n("Double click to set selected pages (ex: 2 or 5-23 or 2,5-7,12-)");

    public PageRangesColumn() {
        // nothing
    }

    public PageRangesColumn(String tooltipMessage) {
        if (isNoneBlank(tooltipMessage)) {
            this.tooltipMessage = tooltipMessage;
        }
    }

    @Override
    public String getColumnTitle() {
        return DefaultI18nContext.getInstance().i18n("Page ranges");
    }

    @Override
    public ObservableValue<String> getObservableValue(SelectionTableRowData data) {
        return data.pageSelection;
    }

    @Override
    public String getTextValue(String item) {
        return defaultString(item, EMPTY);
    }

    @Override
    public Comparator<String> comparator() {
        return Comparator.naturalOrder();
    }

    @Override
    public TableColumn<SelectionTableRowData, String> getTableColumn() {
        TableColumn<SelectionTableRowData, String> tableColumn = SelectionTableColumn.super.getTableColumn();
        tableColumn.setEditable(true);
        tableColumn.setOnEditCommit(
                t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).pageSelection.set(t.getNewValue()));
        return tableColumn;
    }

    @Override
    public Callback<TableColumn<SelectionTableRowData, String>, TableCell<SelectionTableRowData, String>> cellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<SelectionTableRowData, String> call(TableColumn<SelectionTableRowData, String> param) {
                return new TooltippedTextFieldTableCell(tooltipMessage);
            }
        };
    }
}
