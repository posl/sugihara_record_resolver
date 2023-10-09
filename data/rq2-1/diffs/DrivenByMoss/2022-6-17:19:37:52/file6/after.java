// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2022
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.mackie.mcu.command.trigger;

import de.mossgrabers.controller.mackie.mcu.MCUConfiguration;
import de.mossgrabers.controller.mackie.mcu.controller.MCUControlSurface;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.daw.data.IItem;
import de.mossgrabers.framework.featuregroup.ModeManager;
import de.mossgrabers.framework.mode.Modes;
import de.mossgrabers.framework.utils.ButtonEvent;


/**
 * A select track command which activates the volume mode temporarily.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class FaderTouchCommand extends SelectCommand
{
    private static final boolean [] isTrackTouched = new boolean [8 * 4];


    /**
     * Constructor.
     *
     * @param index The channel index
     * @param model The model
     * @param surface The surface
     */
    public FaderTouchCommand (final int index, final IModel model, final MCUControlSurface surface)
    {
        super (index, model, surface);
    }


    /** {@inheritDoc} */
    @Override
    public void executeNormal (final ButtonEvent event)
    {
        final MCUConfiguration configuration = this.surface.getConfiguration ();
        if (event == ButtonEvent.LONG || configuration.useFadersAsKnobs ())
            return;

        final boolean isTouched = event == ButtonEvent.DOWN;

        // Master Channel
        if (this.index == 8)
        {
            if (isTouched && configuration.isTouchChannel ())
                this.model.getMasterTrack ().select ();
            return;
        }

        final ModeManager modeManager = this.surface.getModeManager ();
        final boolean isLayerMode = Modes.isLayerMode (modeManager.getActiveID ());

        // Select channel or layer
        if (configuration.isTouchChannel () && event == ButtonEvent.DOWN)
        {
            final IItem item = isLayerMode ? this.model.getCursorDevice ().getLayerBank ().getItem (this.channel) : this.getTrackBank ().getItem (this.channel);
            item.select ();
        }

        if (configuration.useFadersAsKnobs ())
        {
            modeManager.getActive ().onKnobTouch (this.index, isTouched);
            return;
        }

        final Modes volumeMode = isLayerMode ? Modes.DEVICE_LAYER_VOLUME : Modes.VOLUME;
        modeManager.get (volumeMode).onKnobTouch (this.index, isTouched);

        final int pos = this.surface.getSurfaceID () * 8 + this.index;

        // Temporarily enable (layer) volume mode
        if (configuration.isTouchChannelVolumeMode ())
        {
            if (isTouched)
            {
                if (!hasTouchedFader ())
                {
                    if (modeManager.isActive (volumeMode))
                        modeManager.setPreviousID (volumeMode);
                    else
                        modeManager.setActive (volumeMode);
                }
                setTouchedFader (pos, true);
            }
            else
            {
                setTouchedFader (pos, false);
                if (!hasTouchedFader ())
                    modeManager.restore ();
            }
        }
    }


    private static boolean hasTouchedFader ()
    {
        synchronized (isTrackTouched)
        {
            for (final boolean element: isTrackTouched)
            {
                if (element)
                    return true;
            }
        }
        return false;
    }


    private static void setTouchedFader (final int pos, final boolean isTouched)
    {
        synchronized (isTrackTouched)
        {
            isTrackTouched[pos] = isTouched;
        }
    }
}
