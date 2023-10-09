// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2023
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.bitwig.framework.daw.data;

import de.mossgrabers.bitwig.framework.daw.ApplicationImpl;
import de.mossgrabers.bitwig.framework.daw.HostImpl;
import de.mossgrabers.bitwig.framework.daw.ModelImpl;
import de.mossgrabers.framework.controller.valuechanger.IValueChanger;
import de.mossgrabers.framework.daw.IHost;
import de.mossgrabers.framework.daw.data.ICursorTrack;
import de.mossgrabers.framework.daw.data.bank.ITrackBank;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;


/**
 * The cursor track.
 *
 * @author Jürgen Moßgraber
 */
public class CursorTrackImpl extends TrackImpl implements ICursorTrack
{
    private static final int           NUM_TRACKS_LARGE_BANK = 400;

    private final ModelImpl            model;
    private final SettableBooleanValue isPinnedAttr;
    private final TrackBank            largeTrackBank;


    /**
     * Constructor.
     *
     * @param model The model to retrieve the current track bank
     * @param host The DAW host
     * @param valueChanger The valueChanger
     * @param application The application
     * @param cursorTrack The cursor track
     * @param rootGroup The root track
     * @param numSends The number of sends of a bank
     * @param numScenes The number of scenes of a bank
     */
    public CursorTrackImpl (final ModelImpl model, final IHost host, final IValueChanger valueChanger, final CursorTrack cursorTrack, final Track rootGroup, final ApplicationImpl application, final int numSends, final int numScenes)
    {
        super (host, valueChanger, application, cursorTrack, rootGroup, cursorTrack, -1, numSends, numScenes);

        this.model = model;

        this.largeTrackBank = ((HostImpl) host).getControllerHost ().createTrackBank (NUM_TRACKS_LARGE_BANK, 0, 0, true);

        this.isPinnedAttr = cursorTrack.isPinned ();
        this.isPinnedAttr.markInterested ();
    }


    /** {@inheritDoc} */
    @Override
    public void enableObservers (final boolean enable)
    {
        super.enableObservers (enable);

        Util.setIsSubscribed (this.cursorTrack.hasPrevious (), enable);
        Util.setIsSubscribed (this.cursorTrack.hasNext (), enable);
        Util.setIsSubscribed (this.isPinnedAttr, enable);
    }


    /** {@inheritDoc} */
    @Override
    public int getIndex ()
    {
        return this.getPosition () % this.model.getCurrentTrackBank ().getPageSize ();
    }


    /** {@inheritDoc} */
    @Override
    public void selectPrevious ()
    {
        this.cursorTrack.selectPrevious ();
    }


    /** {@inheritDoc} */
    @Override
    public boolean canSelectPrevious ()
    {
        return this.cursorTrack.hasPrevious ().get ();
    }


    /** {@inheritDoc} */
    @Override
    public boolean canSelectNext ()
    {
        return this.cursorTrack.hasNext ().get ();
    }


    /** {@inheritDoc} */
    @Override
    public void selectNext ()
    {
        this.cursorTrack.selectNext ();
    }


    /** {@inheritDoc} */
    @Override
    public void swapWithPrevious ()
    {
        final ITrackBank tb = this.model.getCurrentTrackBank ();
        if (tb == null)
            return;
        final int position = this.getPosition ();
        if (position == 0)
            return;
        final Track prevTrack = this.largeTrackBank.getItemAt (position - 1);
        this.track.afterTrackInsertionPoint ().moveTracks (prevTrack);
        prevTrack.selectInEditor ();
    }


    /** {@inheritDoc} */
    @Override
    public void swapWithNext ()
    {
        final ITrackBank tb = this.model.getCurrentTrackBank ();
        if (tb == null)
            return;
        final int position = this.getPosition ();
        if (position >= NUM_TRACKS_LARGE_BANK - 1)
            return;
        final Track nextTrack = this.largeTrackBank.getItemAt (position + 1);
        this.track.beforeTrackInsertionPoint ().moveTracks (nextTrack);
        nextTrack.selectInEditor ();
    }


    /** {@inheritDoc} */
    @Override
    public boolean isPinned ()
    {
        return this.isPinnedAttr.get ();
    }


    /** {@inheritDoc} */
    @Override
    public void togglePinned ()
    {
        this.isPinnedAttr.toggle ();
    }


    /** {@inheritDoc} */
    @Override
    public void setPinned (final boolean isPinned)
    {
        this.isPinnedAttr.set (isPinned);
    }
}