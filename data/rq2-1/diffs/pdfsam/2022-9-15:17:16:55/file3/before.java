/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 28/nov/2012
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
package org.pdfsam.module;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Metadata to describe a module.
 * 
 * @author Andrea Vacondio
 * 
 */
public final class ModuleDescriptor {

    private String name;
    private String description;
    private int priority = ModulePriority.DEFAULT.getPriority();
    private String supportURL;
    private final List<ModuleInputOutputType> inputTypes;
    public final ModuleCategory category;

    ModuleDescriptor(ModuleCategory category, String name, String description, int priority, String supportURL,
            ModuleInputOutputType... inputTypes) {
        this.category = ofNullable(category).orElse(ModuleCategory.OTHER);
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.supportURL = supportURL;
        this.inputTypes = ofNullable(inputTypes).filter(t -> t.length > 0).map(Arrays::asList)
                .orElseGet(() -> Arrays.asList(ModuleInputOutputType.OTHER));
    }

    /**
     * @return a human readable, internationalized name for the module
     */
    public String getName() {
        return name;
    }

    /**
     * @return a human readable, internationalized description for the module
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return module priority. It is a rough indicator of the popularity of the module. It can be used to present modules to the users in an order that has more chances of being
     *         of interest for them. The idea is to use this value to present most commonly used modules on first.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @return an optional URL pointing at the location where a support resource can be found. This is intended to be a webpage where support material for the module can be found.
     */
    public Optional<String> getSupportURL() {
        return ofNullable(defaultIfBlank(supportURL, null));
    }

    /**
     * @param type
     * @return true if this module has the given input type
     */
    public boolean hasInputType(ModuleInputOutputType type) {
        return inputTypes.contains(type);
    }
}
