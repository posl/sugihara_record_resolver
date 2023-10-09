// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2022
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.bitwig.framework.daw;

import de.mossgrabers.bitwig.framework.daw.data.ParameterImpl;
import de.mossgrabers.bitwig.framework.daw.data.Util;
import de.mossgrabers.framework.controller.valuechanger.IValueChanger;
import de.mossgrabers.framework.daw.IProject;
import de.mossgrabers.framework.daw.data.IParameter;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Project;


/**
 * Encapsulates the Project instance.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class ProjectImpl implements IProject
{
    private final Project     project;
    private final Application application;
    private final IParameter  cueVolumeParameter;
    private final IParameter  cueMixParameter;


    /**
     * Constructor.
     *
     * @param valueChanger The value changer
     * @param project The project
     * @param application The application
     */
    public ProjectImpl (final IValueChanger valueChanger, final Project project, final Application application)
    {
        this.project = project;
        this.application = application;

        this.application.projectName ().markInterested ();

        this.project.hasSoloedTracks ().markInterested ();
        this.project.hasMutedTracks ().markInterested ();

        this.cueVolumeParameter = new ParameterImpl (valueChanger, this.project.cueVolume (), 0);
        this.cueMixParameter = new ParameterImpl (valueChanger, this.project.cueMix (), 0);
    }


    /** {@inheritDoc} */
    @Override
    public void enableObservers (final boolean enable)
    {
        Util.setIsSubscribed (this.application.projectName (), enable);

        Util.setIsSubscribed (this.project.hasSoloedTracks (), enable);
        Util.setIsSubscribed (this.project.hasMutedTracks (), enable);

        this.cueVolumeParameter.enableObservers (enable);
        this.cueMixParameter.enableObservers (enable);
    }


    /** {@inheritDoc} */
    @Override
    public String getName ()
    {
        return this.application.projectName ().get ();
    }


    /** {@inheritDoc} */
    @Override
    public void previous ()
    {
        this.application.previousProject ();
    }


    /** {@inheritDoc} */
    @Override
    public void next ()
    {
        this.application.nextProject ();
    }


    /** {@inheritDoc} */
    @Override
    public void createScene ()
    {
        this.project.createScene ();
    }


    /** {@inheritDoc} */
    @Override
    public void createSceneFromPlayingLauncherClips ()
    {
        this.project.createSceneFromPlayingLauncherClips ();
    }


    /** {@inheritDoc} */
    @Override
    public void save ()
    {
        final Action action = this.application.getAction ("Save");
        if (action != null)
            action.invoke ();
    }


    /** {@inheritDoc} */
    @Override
    public void load ()
    {
        final Action action = this.application.getAction ("Open");
        if (action != null)
            action.invoke ();
    }


    /** {@inheritDoc} */
    @Override
    public IParameter getCueVolumeParameter ()
    {
        return this.cueVolumeParameter;
    }


    /** {@inheritDoc} */
    @Override
    public String getCueVolumeStr ()
    {
        return this.cueVolumeParameter.getDisplayedValue ();
    }


    /** {@inheritDoc} */
    @Override
    public String getCueVolumeStr (final int limit)
    {
        return this.cueVolumeParameter.getDisplayedValue (limit);
    }


    /** {@inheritDoc} */
    @Override
    public int getCueVolume ()
    {
        return this.cueVolumeParameter.getValue ();
    }


    /** {@inheritDoc} */
    @Override
    public void changeCueVolume (final int control)
    {
        this.cueVolumeParameter.changeValue (control);
    }


    /** {@inheritDoc} */
    @Override
    public void setCueVolume (final int value)
    {
        this.cueVolumeParameter.setValue (value);
    }


    /** {@inheritDoc} */
    @Override
    public void resetCueVolume ()
    {
        this.cueVolumeParameter.resetValue ();
    }


    /** {@inheritDoc} */
    @Override
    public void touchCueVolume (final boolean isBeingTouched)
    {
        this.cueVolumeParameter.touchValue (isBeingTouched);
    }


    /** {@inheritDoc} */
    @Override
    public IParameter getCueMixParameter ()
    {
        return this.cueMixParameter;
    }


    /** {@inheritDoc} */
    @Override
    public String getCueMixStr ()
    {
        return this.cueMixParameter.getDisplayedValue ();
    }


    /** {@inheritDoc} */
    @Override
    public String getCueMixStr (final int limit)
    {
        return this.cueMixParameter.getDisplayedValue (limit);
    }


    /** {@inheritDoc} */
    @Override
    public int getCueMix ()
    {
        return this.cueMixParameter.getValue ();
    }


    /** {@inheritDoc} */
    @Override
    public void changeCueMix (final int control)
    {
        this.cueMixParameter.changeValue (control);
    }


    /** {@inheritDoc} */
    @Override
    public void setCueMix (final int value)
    {
        this.cueMixParameter.setValue (value);
    }


    /** {@inheritDoc} */
    @Override
    public void resetCueMix ()
    {
        this.cueMixParameter.resetValue ();
    }


    /** {@inheritDoc} */
    @Override
    public void touchCueMix (final boolean isBeingTouched)
    {
        this.cueMixParameter.touchValue (isBeingTouched);
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasSolo ()
    {
        return this.project.hasSoloedTracks ().get ();
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasMute ()
    {
        return this.project.hasMutedTracks ().get ();
    }


    /** {@inheritDoc} */
    @Override
    public void clearSolo ()
    {
        this.project.unsoloAll ();
    }


    /** {@inheritDoc} */
    @Override
    public void clearMute ()
    {
        this.project.unmuteAll ();
    }
}
