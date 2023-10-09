/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 26/ago/2014
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
package org.pdfsam.ui.workarea;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.pdfsam.tool.Tool;

/**
 * @author Andrea Vacondio
 *
 */
class QuickbarModuleButtonsProvider {

    private List<Tool> tools;

    @Inject
    QuickbarModuleButtonsProvider(List<Tool> tools) {
        this.tools = new ArrayList<>(tools);
        Comparator<Tool> compareByPrio = Comparator.comparingInt(m-> m.descriptor().getPriority());
        this.tools.sort(compareByPrio.thenComparing(m-> m.descriptor().getName()));
    }

    public List<ModuleButton> buttons() {
        return this.tools.stream().map(ModuleButton::new).collect(Collectors.toList());
    }
}
