package de.mossgrabers.controller.novation.launchcontrol.mode.main;

import de.mossgrabers.controller.novation.launchcontrol.controller.LaunchControlXLControlSurface;
import de.mossgrabers.controller.novation.launchcontrol.mode.buttons.XLDrumSequencerMode;
import de.mossgrabers.framework.controller.ContinuousID;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.daw.INoteClip;
import de.mossgrabers.framework.daw.IStepInfo;
import de.mossgrabers.framework.daw.constants.Capability;
import de.mossgrabers.framework.daw.data.IChannel;
import de.mossgrabers.framework.daw.data.IDrumDevice;
import de.mossgrabers.framework.daw.data.ILayer;
import de.mossgrabers.framework.daw.data.bank.IDrumPadBank;
import de.mossgrabers.framework.mode.Modes;
import de.mossgrabers.framework.view.sequencer.AbstractDrumView;

import java.util.List;


/**
 * Drum sequencer mode for the LauchControl XL.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class XLDrumPadEditMode extends XLBaseNoteEditMode
{
    private int note;


    /**
     * Constructor.
     *
     * @param surface The control surface
     * @param model The model
     * @param clipRows The rows of the monitored clip
     * @param clipCols The columns of the monitored clip
     * @param controls The IDs of the knobs or faders to control this mode
     */
    public XLDrumPadEditMode (final LaunchControlXLControlSurface surface, final IModel model, final int clipRows, final int clipCols, final List<ContinuousID> controls)
    {
        super ("Drum Sequencer", surface, model, clipRows, clipCols, controls);

        this.defaultMode = Modes.DRUM_SEQUENCER;
        this.note = this.model.getScales ().getDrumOffset ();
    }


    /** {@inheritDoc} */
    @Override
    protected int getNoteRow (final int channel, final int step)
    {
        return this.note;
    }


    /** {@inheritDoc} */
    @Override
    protected void executeRow0 (final int index)
    {
        this.note = this.model.getScales ().getDrumOffset () + index;
        ((XLDrumSequencerMode) this.surface.getTrackButtonModeManager ().get (Modes.DRUM_SEQUENCER)).setSelectedPad (index);
        final ILayer item = this.model.getDrumDevice ().getDrumPadBank ().getItem (index);
        if (item.doesExist ())
            this.host.showNotification ("Drum Pad: " + item.getName ());
    }


    /** {@inheritDoc} */
    @Override
    protected void handleKnobRow0 (final INoteClip clip, final int channel, final int column, final int noteRow, final double normalizedValue)
    {
        if (this.host.supports (Capability.NOTE_EDIT_CHANCE))
        {
            clip.updateStepChance (channel, column, noteRow, normalizedValue);
            this.surface.getDisplay ().notify (String.format ("Chance: %d%%", Integer.valueOf ((int) Math.round (normalizedValue * 100))));
        }
    }


    /** {@inheritDoc} */
    @Override
    protected int getKnobValueRow0 (final int noteRow, final IStepInfo stepInfo)
    {
        if (this.host.supports (Capability.NOTE_EDIT_CHANCE))
            return (int) (stepInfo.getChance () * 127);
        return 0;
    }


    /** {@inheritDoc} */
    @Override
    protected int getFirstRowColor (final int index)
    {
        final boolean isRecording = this.model.hasRecordingState ();
        final IDrumDevice drumDevice = this.model.getDrumDevice ();
        final String drumPadColor = this.getDrumPadColor (index, drumDevice.getDrumPadBank (), isRecording);
        return this.colorManager.getColorIndex (drumPadColor);
    }


    /** {@inheritDoc} */
    @Override
    public void selectPreviousItem ()
    {
        this.scales.decDrumOctave ();
        this.host.showNotification (this.scales.getDrumRangeText ());
    }


    /** {@inheritDoc} */
    @Override
    public void selectNextItem ()
    {
        this.scales.incDrumOctave ();
        this.host.showNotification (this.scales.getDrumRangeText ());
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasPreviousItem ()
    {
        return this.scales.canScrollDrumOctaveDown ();
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasNextItem ()
    {
        return this.scales.canScrollDrumOctaveUp ();
    }


    private String getDrumPadColor (final int index, final IDrumPadBank drumPadBank, final boolean isRecording)
    {
        final int offsetY = this.scales.getDrumOffset ();

        // Playing note?
        if (this.keyManager.isKeyPressed (offsetY + index))
            return isRecording ? AbstractDrumView.COLOR_PAD_RECORD : AbstractDrumView.COLOR_PAD_PLAY;

        // Selected?
        final int selectedPad = ((XLDrumSequencerMode) this.surface.getTrackButtonModeManager ().get (Modes.DRUM_SEQUENCER)).getSelectedPad ();
        if (selectedPad == index)
            return AbstractDrumView.COLOR_PAD_SELECTED;

        // Exists and active?
        final IChannel drumPad = drumPadBank.getItem (index);
        if (!drumPad.doesExist () || !drumPad.isActivated ())
            return this.surface.getConfiguration ().isTurnOffEmptyDrumPads () ? AbstractDrumView.COLOR_PAD_OFF : AbstractDrumView.COLOR_PAD_NO_CONTENT;

        // Muted or soloed?
        if (drumPad.isMute () || drumPadBank.hasSoloedPads () && !drumPad.isSolo ())
            return AbstractDrumView.COLOR_PAD_MUTED;
        return AbstractDrumView.COLOR_PAD_HAS_CONTENT;
    }


    /** {@inheritDoc} */
    @Override
    protected Modes getSequencerMode ()
    {
        return Modes.DRUM_SEQUENCER;
    }
}
