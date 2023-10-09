package org.pdfsam.persistence;
/*
 * This file is part of the PDF Split And Merge source code
 * Created on 13/09/22
 * Copyright 2022 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
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

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * @author Andrea Vacondio
 */
@AnalyzeClasses(packages = "org.pdfsam.persistence", importOptions = { ImportOption.DoNotIncludeTests.class })
public class TestCycles {

    @ArchTest
    public static final ArchRule myRule = SlicesRuleDefinition.slices().matching("org.pdfsam.(*)..").should()
            .beFreeOfCycles();

}
