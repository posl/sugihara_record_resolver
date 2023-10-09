// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2022
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.ni.maschine.mk3.command.trigger;

import de.mossgrabers.controller.ni.maschine.mk3.MaschineConfiguration;
import de.mossgrabers.controller.ni.maschine.mk3.controller.MaschineControlSurface;
import de.mossgrabers.controller.ni.maschine.mk3.mode.EditNoteMode;
import de.mossgrabers.framework.command.trigger.mode.ModeMultiSelectCommand;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.featuregroup.ModeManager;
import de.mossgrabers.framework.mode.Modes;
import de.mossgrabers.framework.utils.ButtonEvent;
import de.mossgrabers.framework.view.Views;


/**
 * Command to edit the play position. Switch between Panorama and Duration in note edit mode.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class SwingCommand extends ModeMultiSelectCommand<MaschineControlSurface, MaschineConfiguration>
{
    /**
     * Constructor.
     *
     * @param model The model
     * @param surface The surface
     */
    public SwingCommand (final IModel model, final MaschineControlSurface surface)
    {
        super (model, surface, Modes.POSITION, Modes.LOOP_START, Modes.LOOP_LENGTH);
    }


    /** {@inheritDoc} */
    @Override
    public void execute (final ButtonEvent event, final int velocity)
    {
        final ModeManager modeManager = this.surface.getModeManager ();

        // Switch Gain/Velocity note edit parameters
        if (modeManager.isActive (Modes.NOTE) && this.surface.getViewManager ().isActive (Views.DRUM, Views.PLAY))
        {
            if (event == ButtonEvent.DOWN)
            {
                final EditNoteMode mode = (EditNoteMode) modeManager.get (Modes.NOTE);
                final boolean isPanorama = mode.getSelectedItem () == EditNoteMode.PANORAMA;
                mode.selectItem (isPanorama ? EditNoteMode.DURATION : EditNoteMode.PANORAMA);
                this.surface.getDisplay ().notify (isPanorama ? "Duration" : "Panorama");
            }
            return;
        }

        super.execute (event, velocity);
    }
}
