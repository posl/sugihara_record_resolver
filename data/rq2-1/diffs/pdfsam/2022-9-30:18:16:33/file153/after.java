/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 22/ago/2014
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
package org.pdfsam.ui.components.log;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.qos.logback.classic.Level;

/**
 * @author Andrea Vacondio
 *
 */
public class LogLevelTest {

    @Test
    public void toLogLevel() {
        assertEquals(LogLevel.ERROR, LogLevel.toLogLevel(Level.ERROR_INT));
        assertEquals(LogLevel.WARN, LogLevel.toLogLevel(Level.WARN_INT));
        assertEquals(LogLevel.INFO, LogLevel.toLogLevel(Level.INFO_INT));
        assertEquals(LogLevel.INFO, LogLevel.toLogLevel(Level.DEBUG_INT));
        assertEquals(LogLevel.INFO, LogLevel.toLogLevel(Level.TRACE_INT));
    }
}
