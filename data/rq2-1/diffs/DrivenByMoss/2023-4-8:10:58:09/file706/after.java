// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2023
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.novation.launchpad.controller;

import de.mossgrabers.framework.controller.valuechanger.IValueChanger;
import de.mossgrabers.framework.scale.Scales;


/**
 * Changes matrices to different grid note mapping of the Launchpad.
 *
 * @author Jürgen Moßgraber
 */
public class LaunchpadScales extends Scales
{
    /**
     * Constructor.
     *
     * @param valueChanger A value changer
     * @param startNote The first MIDI note of the pad grid
     * @param endNote The last MIDI note of the pad grid
     * @param numColumns The number of columns of the pad grid
     * @param numRows The number of rows of the pad grid
     */
    public LaunchpadScales (final IValueChanger valueChanger, final int startNote, final int endNote, final int numColumns, final int numRows)
    {
        super (valueChanger, startNote, endNote, numColumns, numRows);
    }


    /** {@inheritDoc} */
    @Override
    public int [] translateMatrixToGrid (final int [] matrix)
    {
        final int [] gridMatrix = Scales.getEmptyMatrix ();
        for (int i = 36; i < 100; i++)
            gridMatrix[LaunchpadPadGrid.TRANSLATE_MATRIX[i - 36]] = matrix[i];
        return gridMatrix;
    }
}