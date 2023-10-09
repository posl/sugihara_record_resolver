package org.pdfsam.test;
/*
 * This file is part of the PDF Split And Merge source code
 * Created on 24/09/22
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

import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.extension.Extension;

import javax.swing.SwingUtilities;

/**
 * @author Andrea Vacondio
 */
public class JavaFxThreadInitializeExtension implements Extension {

    static {
        System.out.println("Initializing JavaFX thread");
        SwingUtilities.invokeLater(JFXPanel::new);
        System.out.println("JavaFX initialized");
    }
}
