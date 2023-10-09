/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 16/nov/2012
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
package org.pdfsam.support.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 * 
 */
public class IntRangeStringValidatorTest {
    private Validator<String> victim = Validators.positiveIntRange(1, 5);

    @Test
    public void negative() {
        assertFalse(victim.isValid("-123"));
    }

    @Test
    public void invalid() {
        assertFalse(victim.isValid("dsdsa"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidLower() {
        Validators.positiveIntRange(0, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidUpper() {
        Validators.positiveIntRange(1, 0);
    }

    @Test
    public void valid() {
        assertTrue(victim.isValid("1"));
        assertTrue(victim.isValid("3"));
        assertTrue(victim.isValid("5"));
    }

    @Test
    public void allowBlank() {
        assertFalse(victim.isValid(""));
        assertTrue(Validators.validEmpty(victim).isValid(""));
    }
}
