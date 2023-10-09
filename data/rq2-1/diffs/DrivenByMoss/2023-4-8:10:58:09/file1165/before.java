// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2023
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.framework.mode.track;

import de.mossgrabers.framework.configuration.Configuration;
import de.mossgrabers.framework.controller.IControlSurface;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.daw.data.ITrack;
import de.mossgrabers.framework.parameter.IParameter;


/**
 * The track crossfade A mode.
 *
 * @param <S> The type of the control surface
 * @param <C> The type of the configuration
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class TrackCrossfadeAMode<S extends IControlSurface<C>, C extends Configuration> extends DefaultTrackMode<S, C>
{
    /**
     * Constructor.
     *
     * @param surface The surface
     * @param model The model
     */
    public TrackCrossfadeAMode (final S surface, final IModel model)
    {
        super ("Track Crossfade A", surface, model, true);
    }


    /** {@inheritDoc} */
    @Override
    protected void executeMethod (final ITrack track)
    {
        final IParameter crossfadeParameter = track.getCrossfadeParameter ();
        final double v = this.model.getValueChanger ().toNormalizedValue (crossfadeParameter.getValue ());
        crossfadeParameter.setNormalizedValue (v < 0.1 ? 0.5 : 0);
    }
}
