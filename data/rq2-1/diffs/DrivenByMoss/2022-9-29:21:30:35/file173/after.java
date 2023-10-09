// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2022
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.framework.daw.data;

import de.mossgrabers.framework.controller.color.ColorEx;
import de.mossgrabers.framework.parameter.IParameter;


/**
 * Interface to a send.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public interface ISend extends IParameter
{
    /**
     * Get the color of the send channel.
     *
     * @return The color in RGB
     */
    ColorEx getColor ();
}