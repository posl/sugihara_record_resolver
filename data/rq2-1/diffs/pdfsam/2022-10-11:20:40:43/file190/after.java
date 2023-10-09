/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 19/feb/2014
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
package org.pdfsam.ui.components.selection.multiple.move;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Information about a multiple selection and a single focus
 * 
 * @author Andrea Vacondio
 *
 */
class MultipleSelectionAndFocus implements SelectionAndFocus {

    private int focus = -1;
    private int originalFocus = -1;
    private int row = -1;
    private final Set<Integer> rows = new HashSet<>();

    MultipleSelectionAndFocus(int originalFocus) {
        this.originalFocus = originalFocus;
    }

    void moveTo(int row, int newRow) {
        if (focus == -1 && originalFocus == row) {
            focus = newRow;
        }
        if (this.row == -1) {
            this.row = newRow;
        } else {
            rows.add(newRow);
        }
    }

    void moveUp(int row) {
        int newRow = row - 1;
        moveTo(row, newRow);
    }

    void moveDown(int row) {
        int newRow = row + 1;
        moveTo(row, newRow);
    }

    @Override
    public int getFocus() {
        return focus;
    }

    @Override
    public int row() {
        return row;
    }

    @Override
    public int[] getRows() {
        // TODO this sucks
        return ArrayUtils.toPrimitive(rows.toArray(new Integer[0]));
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
