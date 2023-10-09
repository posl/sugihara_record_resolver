/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 08/ago/2014
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.pdfsam.model.pdf.PdfDocumentDescriptor;
import org.pdfsam.test.ClearEventStudioExtension;
import org.sejda.conversion.exception.ConversionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Andrea Vacondio
 *
 */
@ExtendWith(ClearEventStudioExtension.class)
public class SelectionTableRowDataTest {

    @Test
    public void empty() throws ConversionException {
        PdfDocumentDescriptor descriptor = mock(PdfDocumentDescriptor.class);
        SelectionTableRowData victim = new SelectionTableRowData(descriptor);
        assertTrue(victim.toPageRangeSet().isEmpty());
    }

    @Test
    public void invalidate() {
        PdfDocumentDescriptor descriptor = mock(PdfDocumentDescriptor.class);
        SelectionTableRowData victim = new SelectionTableRowData(descriptor);
        victim.invalidate();
        verify(descriptor).release();
    }

    @Test
    public void duplicate() {
        PdfDocumentDescriptor descriptor = mock(PdfDocumentDescriptor.class);
        SelectionTableRowData victim = new SelectionTableRowData(descriptor);
        assertFalse(victim.reverse.get());
        assertEquals("1", victim.pace.get());
        assertThat(victim.pageSelection.get()).isEmpty();
        victim.reverse.set(true);
        victim.pace.set("3");
        victim.pageSelection.set("4");
        SelectionTableRowData dupe = victim.duplicate();
        verify(descriptor).retain();
        assertTrue(dupe.reverse.get());
        assertEquals("3", dupe.pace.get());
        assertEquals("4", dupe.pageSelection.get());

    }
}
