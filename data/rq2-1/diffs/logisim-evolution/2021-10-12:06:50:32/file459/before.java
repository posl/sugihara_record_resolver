/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

public class ShowStateIcon extends BaseIcon {

  private final boolean pressed;

  public ShowStateIcon(boolean pressed) {
    this.pressed = pressed;
  }

  @Override
  protected void paintIcon(Graphics2D gfx) {
    gfx.setStroke(new BasicStroke(AppPreferences.getScaled(1)));
    if (pressed) {
      gfx.setColor(Color.MAGENTA.brighter().brighter().brighter());
      gfx.fillRect(0, 0, getIconWidth(), getIconHeight());
    }
    gfx.setColor(Color.BLACK);
    gfx.drawRect(0, 0, getIconWidth(), getIconHeight() / 2);
    final var font = gfx.getFont().deriveFont((float) getIconWidth() / (float) 2);
    final var textLayout = new TextLayout("101", font, gfx.getFontRenderContext());
    textLayout.draw(
        gfx,
        (float) ((double) getIconWidth() / 2.0 - textLayout.getBounds().getCenterX()),
        (float) ((double) getIconHeight() / 4.0 - textLayout.getBounds().getCenterY()));
    final var iconBorder = AppPreferences.ICON_BORDER;
    final var wh = AppPreferences.getScaled(AppPreferences.IconSize / 2 - iconBorder);
    final var offset = AppPreferences.getScaled(iconBorder);
    gfx.setColor(Color.RED);
    gfx.fillOval(offset, offset + getIconHeight() / 2, wh, wh);
    gfx.setColor(Color.GREEN);
    gfx.fillOval(offset + getIconWidth() / 2, offset + getIconHeight() / 2, wh, wh);
    gfx.setColor(Color.BLACK);
    gfx.drawOval(offset, offset + getIconHeight() / 2, wh, wh);
    gfx.drawOval(offset + getIconWidth() / 2, offset + getIconHeight() / 2, wh, wh);
  }
}
