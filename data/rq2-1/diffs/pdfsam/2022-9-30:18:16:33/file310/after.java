/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 31 ago 2016
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

import org.junit.jupiter.api.Test;
import org.pdfsam.model.pdf.PdfDocumentDescriptor;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Andrea Vacondio
 *
 */
public class PaceColumnTest {
    @Test
    public void getObservableValue() {
        File file = mock(File.class);
        SelectionTableRowData data = new SelectionTableRowData(PdfDocumentDescriptor.newDescriptorNoPassword(file));
        data.pace.set("2");
        assertEquals("2", new PaceColumn().getObservableValue(data).getValue());
    }

    @Test
    public void getNullTextValue() {
        assertEquals("1", new PaceColumn().getTextValue(null));
    }

    @Test
    public void comparator() {
        assertEquals(-1, new PaceColumn().comparator().compare("1", "2"));
    }
}
