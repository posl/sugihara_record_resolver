/*
 * This file is part of the PDF Split And Merge source code
 * Created on 29/lug/2014
 * Copyright 2017 by Sober Lemur S.r.l. (info@pdfsam.org).
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
package org.pdfsam.ui.components.tool;

import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.pdfsam.model.ui.ShowLogMessagesRequest;
import org.pdfsam.test.ClearEventStudioExtension;
import org.pdfsam.test.HitTestListener;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;

/**
 * @author Andrea Vacondio
 */
@ExtendWith(ApplicationExtension.class)
public class TaskFailedButtonTest {

    @RegisterExtension
    static ClearEventStudioExtension staticExtension = new ClearEventStudioExtension("LogStage");

    @Start
    public void start(Stage stage) {
        TaskFailedButton button = new TaskFailedButton();
        button.setMaxHeight(30);
        Scene scene = new Scene(new HBox(button));
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void failButtonVisibleOnFailure(FxRobot robot) {
        HitTestListener<ShowLogMessagesRequest> hit = new HitTestListener<>();
        eventStudio().add(ShowLogMessagesRequest.class, hit);
        robot.clickOn(".footer-failed-button");
        assertTrue(hit.isHit());
    }
}
