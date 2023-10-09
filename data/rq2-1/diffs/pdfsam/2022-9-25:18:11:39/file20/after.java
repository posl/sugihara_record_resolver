/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 13/dic/2011
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
package org.pdfsam.core.support;

import org.w3c.dom.Node;

/**
 * Utility class providing xml related methods.
 * 
 * @author Andrea Vacondio
 * 
 */
public final class XmlUtils {
    private XmlUtils() {
        // hide
    }

    /**
     * @param node
     * @param attributeName
     * @return the String value of the given attributeName for the given node. null if the attributeName is not found or the input node is null.
     */
    public static String nullSafeGetStringAttribute(Node node, String attributeName) {
        if (node != null) {
            Node namedItem = node.getAttributes().getNamedItem(attributeName);
            if (namedItem != null) {
                return namedItem.getNodeValue();
            }
        }
        return null;
    }
}
