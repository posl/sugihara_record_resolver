/*
 * This file is part of the PDF Split And Merge source code
 * Created on 03/mag/2014
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
package org.pdfsam.gui.components.banner;

import javafx.animation.Animation.Status;
import javafx.animation.Timeline;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.unicons.UniconsLine;
import org.pdfsam.eventstudio.annotation.EventListener;
import org.pdfsam.gui.components.log.ErrorLoggedEvent;
import org.pdfsam.gui.components.log.LogAreaVisiblityChangedEvent;
import org.pdfsam.model.ui.HideStageRequest;
import org.pdfsam.model.ui.ShowStageRequest;
import org.pdfsam.ui.components.commons.Animations;

import static org.pdfsam.eventstudio.StaticStudio.eventStudio;
import static org.pdfsam.i18n.I18nContext.i18n;

/**
 * Button requesting to open the log window
 *
 * @author Andrea Vacondio
 */
class LogButton extends BannerButton {

    static final String HAS_ERRORS_CSS_CLASS = "log-has-errors";
    private Timeline anim;
    private Object action = ShowStageRequest.INSTANCE;

    LogButton() {
        super(UniconsLine.COMMENT_EXCLAMATION);
        setOnAction(e -> eventStudio().broadcast(action, "LogStage"));
        setTooltip(new Tooltip(i18n().tr("Application messages")));
        anim = Animations.shake(this);
        eventStudio().addAnnotatedListeners(this);
    }

    @EventListener
    public void onLogMessage(ErrorLoggedEvent event) {
        hasUnseenErrors(true);
    }

    @EventListener
    public void onViewedLogArea(LogAreaVisiblityChangedEvent event) {
        hasUnseenErrors(false);
    }

    @EventListener(station = "LogStage")
    public void onHideStage(HideStageRequest event) {
        action = ShowStageRequest.INSTANCE;
    }

    @EventListener(station = "LogStage")
    public void onShowStage(ShowStageRequest event) {
        hasUnseenErrors(false);
        action = HideStageRequest.INSTANCE;
    }

    void hasUnseenErrors(boolean value) {
        if (value) {
            if (!(anim.getStatus() == Status.RUNNING)) {
                anim.play();
            }
            if (!getStyleClass().contains(HAS_ERRORS_CSS_CLASS)) {
                getStyleClass().add(HAS_ERRORS_CSS_CLASS);
            }
        } else {
            getStyleClass().remove(HAS_ERRORS_CSS_CLASS);
            anim.stop();
            setRotate(0);
            setScaleY(1);
        }
    }
}
