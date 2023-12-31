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

package com.cburch.logisim.comp;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import javax.swing.Icon;

public abstract class AbstractComponentFactory implements ComponentFactory {
  private static final Icon toolIcon = Icons.getIcon("subcirc.gif");

  private AttributeSet defaultSet;
  protected HDLGeneratorFactory MyHDLGenerator;

  protected AbstractComponentFactory() {
    defaultSet = null;
    MyHDLGenerator = null;
  }

  @Override
  public boolean ActiveOnHigh(AttributeSet attrs) {
    return true;
  }

  @Override
  public AttributeSet createAttributeSet() {
    return AttributeSets.EMPTY;
  }

  @Override
  public void removeComponent(Circuit circ, Component c, CircuitState state) {
    // dummy factory
  }

  @Override
  public abstract Component createComponent(Location loc, AttributeSet attrs);

  //
  // user interface methods
  //
  @Override
  public void drawGhost(ComponentDrawContext context, Color color, int x, int y, AttributeSet attrs) {
    final var g = context.getGraphics();
    final var bds = getOffsetBounds(attrs);
    g.setColor(color);
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRect(x + bds.getX(), y + bds.getY(), bds.getWidth(), bds.getHeight());
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    var dfltSet = defaultSet;
    if (dfltSet == null) {
      dfltSet = (AttributeSet) createAttributeSet().clone();
      defaultSet = dfltSet;
    }
    return dfltSet.getValue(attr);
  }

  @Override
  public StringGetter getDisplayGetter() {
    return StringUtil.constantGetter(getName());
  }

  @Override
  public String getDisplayName() {
    return getDisplayGetter().toString();
  }

  @Override
  public Object getFeature(Object key, AttributeSet attrs) {
    return null;
  }

  @Override
  public HDLGeneratorFactory getHDLGenerator(AttributeSet attrs) {
    if (HDLSupportedComponent(attrs)) return MyHDLGenerator;
    else return null;
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    return CorrectLabel.getCorrectLabel(this.getName());
  }

  @Override
  public String getHDLTopName(AttributeSet attrs) {
    return getHDLName(attrs);
  }

  @Override
  public boolean CheckForGatedClocks(NetlistComponent comp) {
    return false;
  }

  @Override
  public int[] ClockPinIndex(NetlistComponent comp) {
    return new int[] {0};
  }

  @Override
  public abstract String getName();

  @Override
  public abstract Bounds getOffsetBounds(AttributeSet attrs);

  @Override
  public boolean HasThreeStateDrivers(AttributeSet attrs) {
    return false;
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    return false;
  }

  @Override
  public boolean isAllDefaultValues(AttributeSet attrs, LogisimVersion ver) {
    return false;
  }

  @Override
  public void paintIcon(ComponentDrawContext context, int x, int y, AttributeSet attrs) {
    final var g = context.getGraphics();
    if (toolIcon != null) {
      toolIcon.paintIcon(context.getDestination(), g, x + 2, y + 2);
    } else {
      g.setColor(Color.black);
      g.drawRect(x + 5, y + 2, 11, 17);
      Value[] v = {Value.TRUE, Value.FALSE};
      for (var i = 0; i < 3; i++) {
        g.setColor(v[i % 2].getColor());
        g.fillOval(x + 5 - 1, y + 5 + 5 * i - 1, 3, 3);
        g.setColor(v[(i + 1) % 2].getColor());
        g.fillOval(x + 16 - 1, y + 5 + 5 * i - 1, 3, 3);
      }
    }
  }

  @Override
  public boolean RequiresGlobalClock() {
    return false;
  }

  @Override
  public boolean isSocComponent() {
    return false;
  }

  /* HDL Methods */
  @Override
  public boolean RequiresNonZeroLabel() {
    return false;
  }

  @Override
  public String toString() {
    return getName();
  }
}
