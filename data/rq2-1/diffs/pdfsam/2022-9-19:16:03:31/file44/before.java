/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 09/giu/2014
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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class RegexValidatorTest {
    private Validator<String> victim = Validators.regexMatching("^([0-9]+,?)+$");

    @Test
    public void testNegative() {
        Assert.assertFalse(victim.isValid("dsdsa"));
    }

    @Test
    public void testPositive() {
        Assert.assertTrue(victim.isValid("2"));
        Assert.assertTrue(victim.isValid("2,3,4"));
    }

    @Test
    public void testAllowBlank() {
        Assert.assertFalse(victim.isValid(""));
        Assert.assertTrue(Validators.validEmpty(victim).isValid(""));
    }
}
