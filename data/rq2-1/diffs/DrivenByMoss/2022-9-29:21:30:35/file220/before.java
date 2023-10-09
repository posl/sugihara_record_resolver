// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2022
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.framework.view.sequencer;

import de.mossgrabers.framework.configuration.Configuration;
import de.mossgrabers.framework.controller.ButtonID;
import de.mossgrabers.framework.controller.IControlSurface;
import de.mossgrabers.framework.controller.color.ColorEx;
import de.mossgrabers.framework.controller.grid.IPadGrid;
import de.mossgrabers.framework.controller.hardware.IHwButton;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.daw.INoteClip;
import de.mossgrabers.framework.daw.IStepInfo;
import de.mossgrabers.framework.daw.StepState;
import de.mossgrabers.framework.daw.constants.Resolution;
import de.mossgrabers.framework.daw.data.GridStep;
import de.mossgrabers.framework.daw.data.IDrumDevice;
import de.mossgrabers.framework.utils.ButtonEvent;

import java.util.List;
import java.util.Optional;


/**
 * Abstract implementation for a drum sequencer with N lanes (sounds). The grid is split into rows /
 * N areas: Each area contains N steps of the acitve page and resolution.
 *
 * @param <S> The type of the control surface
 * @param <C> The type of the configuration
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public abstract class AbstractDrumLaneView<S extends IControlSurface<C>, C extends Configuration> extends AbstractDrumView<S, C>
{
    protected final IDrumDevice primary;
    protected final int         lanes;


    /**
     * Constructor for an 8x8 grid.
     *
     * @param name The name of the view
     * @param surface The surface
     * @param model The model
     * @param lanes The number of lanes to display
     * @param clipCols The columns of the clip (of the clip page)
     * @param useDawColors True to use the drum machine pad colors for coloring the octaves
     */
    protected AbstractDrumLaneView (final String name, final S surface, final IModel model, final int lanes, final int clipCols, final boolean useDawColors)
    {
        this (name, surface, model, lanes, 8, GRID_COLUMNS, clipCols, true, useDawColors);
    }


    /**
     * Constructor.
     *
     * @param name The name of the view
     * @param surface The surface
     * @param model The model
     * @param lanes The number of lanes to display
     * @param numRows The number of available rows on the grid
     * @param numColumns The number of available columns on the grid
     * @param clipCols The columns of the clip (of the clip page)
     * @param followSelection Follow the drum pad selection if true
     * @param useDawColors True to use the drum machine pad colors for coloring the octaves
     */
    protected AbstractDrumLaneView (final String name, final S surface, final IModel model, final int lanes, final int numRows, final int numColumns, final int clipCols, final boolean followSelection, final boolean useDawColors)
    {
        // numSequencerLines is set to 1 since it is only used as an indicator to trigger the clip
        // creation
        super (name, surface, model, 1, 0, numColumns, 128, clipCols, followSelection, useDawColors);

        this.lanes = lanes;
        this.allRows = numRows;

        this.primary = this.model.getDrumDevice ();
    }


    /** {@inheritDoc} */
    @Override
    public void onGridNote (final int note, final int velocity)
    {
        if (!this.isActive ())
            return;

        final int index = note - DRUM_START_KEY;
        final int x = index % this.numColumns;
        final int y = index / this.numColumns;

        final int sound = y % this.lanes + this.scales.getDrumOffset ();
        final int laneOffset = (this.allRows - 1 - y) / this.lanes * this.numColumns;
        final int col = laneOffset + x;

        final int channel = this.configuration.getMidiEditChannel ();
        final int vel = this.configuration.isAccentActive () ? this.configuration.getFixedAccentValue () : this.surface.getButton (ButtonID.get (ButtonID.PAD1, index)).getPressedVelocity ();
        final INoteClip clip = this.getClip ();

        if (this.handleNoteAreaButtonCombinations (clip, channel, col, y, sound, velocity, vel))
            return;

        if (velocity == 0)
            clip.toggleStep (channel, col, sound, vel);
    }


    /**
     * Handle button combinations on the note area of the sequencer.
     *
     * @param clip The sequenced MIDI clip
     * @param channel The MIDI channel of the note
     * @param row The row in the current page in the clip
     * @param note The note in the current page of the pad in the clip
     * @param step The step in the current page in the clip
     * @param velocity The velocity
     * @param accentVelocity The velocity or accent value
     * @return True if handled
     */
    protected boolean handleNoteAreaButtonCombinations (final INoteClip clip, final int channel, final int step, final int row, final int note, final int velocity, final int accentVelocity)
    {
        // Handle note duplicate function
        if (this.isButtonCombination (ButtonID.DUPLICATE))
        {
            if (velocity == 0)
            {
                final IStepInfo noteStep = clip.getStep (channel, step, note);
                if (noteStep.getState () == StepState.START)
                    this.copyNote = noteStep;
                else if (this.copyNote != null)
                    clip.setStep (channel, step, note, this.copyNote);
            }
            return true;
        }

        if (this.isButtonCombination (ButtonID.MUTE))
        {
            if (velocity == 0)
            {
                final IStepInfo stepInfo = clip.getStep (channel, step, note);
                final StepState isSet = stepInfo.getState ();
                if (isSet == StepState.START)
                    this.getClip ().updateStepMuteState (channel, step, note, !stepInfo.isMuted ());
            }
            return true;
        }

        // Change length of a note or create a new one with a length
        final int laneOffset = (this.allRows - row - 1) / this.lanes * this.numColumns;
        final int offset = row * this.numColumns;
        for (int s = 0; s < step; s++)
        {
            final IHwButton button = this.surface.getButton (ButtonID.get (ButtonID.PAD1, offset + s));
            if (button.isLongPressed ())
            {
                final int start = s + laneOffset;
                button.setConsumed ();
                final int length = step - start + 1;
                final double duration = length * Resolution.getValueAt (this.getResolutionIndex ());
                final StepState state = note < 0 ? StepState.OFF : clip.getStep (channel, start, note).getState ();
                if (state == StepState.START)
                    clip.updateStepDuration (channel, start, note, duration);
                else
                    clip.setStep (channel, start, note, accentVelocity, duration);
                return true;
            }
        }

        return false;
    }


    /** {@inheritDoc} */
    @Override
    public void drawGrid ()
    {
        final IPadGrid padGrid = this.surface.getPadGrid ();
        if (!this.isActive ())
        {
            padGrid.turnOff ();
            return;
        }

        // Clip length/loop area
        final INoteClip clip = this.getClip ();
        final int step = clip.getCurrentStep ();

        // Paint the sequencer steps
        final int hiStep = this.isInXRange (step) ? step % this.clipCols : -1;
        final int offsetY = this.scales.getDrumOffset ();
        final int editMidiChannel = this.configuration.getMidiEditChannel ();
        final List<GridStep> editNotes = this.getEditNotes ();
        for (int sound = 0; sound < this.lanes; sound++)
        {
            final int noteRow = offsetY + sound;
            final Optional<ColorEx> drumPadColor = this.getPadColor (this.primary, sound);
            for (int col = 0; col < this.clipCols; col++)
            {
                final IStepInfo stepInfo = clip.getStep (editMidiChannel, col, noteRow);
                final boolean hilite = col == hiStep;
                final int x = col % this.numColumns;
                int y = this.lanes - 1 - sound;
                if (col >= this.numColumns)
                    y += this.lanes;
                padGrid.lightEx (x, y, this.getStepColor (stepInfo, hilite, drumPadColor, editMidiChannel, col, noteRow, editNotes));
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    public void updateNoteMapping ()
    {
        this.surface.setKeyTranslationTable (this.scales.translateMatrixToGrid (EMPTY_TABLE));
    }


    /** {@inheritDoc} */
    @Override
    public void onOctaveDown (final ButtonEvent event)
    {
        this.changeOctave (event, false, this.lanes, true, true);
    }


    /** {@inheritDoc} */
    @Override
    public void onOctaveUp (final ButtonEvent event)
    {
        this.changeOctave (event, true, this.lanes, true, true);
    }
}
