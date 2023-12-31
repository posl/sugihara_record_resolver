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

package com.cburch.draw.model;

import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class AbstractCanvasObject implements AttributeSet, CanvasObject, Cloneable {
  private static final int OVERLAP_TRIES = 50;
  private static final int GENERATE_RANDOM_TRIES = 20;

  private EventSourceWeakSupport<AttributeListener> listeners;

  public AbstractCanvasObject() {
    listeners = new EventSourceWeakSupport<>();
  }

  @Override
  public void addAttributeListener(AttributeListener l) {
    listeners.add(l);
  }

  @Override
  public Handle canDeleteHandle(Location loc) {
    return null;
  }

  @Override
  public Handle canInsertHandle(Location desired) {
    return null;
  }

  @Override
  public boolean canMoveHandle(Handle handle) {
    return false;
  }

  @Override
  public boolean canRemove() {
    return true;
  }

  @Override
  public CanvasObject clone() {
    try {
      AbstractCanvasObject ret = (AbstractCanvasObject) super.clone();
      ret.listeners = new EventSourceWeakSupport<>();
      return ret;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  @Override
  public abstract boolean contains(Location loc, boolean assumeFilled);

  @Override
  public boolean containsAttribute(Attribute<?> attr) {
    return getAttributes().contains(attr);
  }

  @Override
  public Handle deleteHandle(Handle handle) {
    throw new UnsupportedOperationException("deleteHandle");
  }

  protected void fireAttributeListChanged() {
    AttributeEvent e = new AttributeEvent(this);
    for (AttributeListener listener : listeners) {
      listener.attributeListChanged(e);
    }
  }

  @Override
  public Attribute<?> getAttribute(String name) {
    for (Attribute<?> attr : getAttributes()) {
      if (attr.getName().equals(name)) return attr;
    }
    return null;
  }

  // methods required by AttributeSet interface
  @Override
  public abstract List<Attribute<?>> getAttributes();

  @Override
  public AttributeSet getAttributeSet() {
    return this;
  }

  @Override
  public abstract Bounds getBounds();

  @Override
  public abstract String getDisplayName();

  @Override
  public String getDisplayNameAndLabel() {
    return getDisplayName();
  }

  @Override
  public abstract List<Handle> getHandles(HandleGesture gesture);

  protected Location getRandomPoint(Bounds bds, Random rand) {
    final var x = bds.getX();
    final var y = bds.getY();
    final var w = bds.getWidth();
    final var h = bds.getHeight();
    for (var i = 0; i < GENERATE_RANDOM_TRIES; i++) {
      final var loc = Location.create(x + rand.nextInt(w), y + rand.nextInt(h));
      if (contains(loc, false)) return loc;
    }
    return null;
  }

  @Override
  public abstract <V> V getValue(Attribute<V> attr);

  @Override
  public void insertHandle(Handle desired, Handle previous) {
    throw new UnsupportedOperationException("insertHandle");
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return false;
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr.isToSave();
  }

  @Override
  public abstract boolean matches(CanvasObject other);

  @Override
  public abstract int matchesHashCode();

  @Override
  public Handle moveHandle(HandleGesture gesture) {
    throw new UnsupportedOperationException("moveHandle");
  }

  @Override
  public boolean overlaps(CanvasObject other) {
    final var a = this.getBounds();
    final var b = other.getBounds();
    final var c = a.intersect(b);
    final var rand = new Random();
    if (c.getWidth() == 0 || c.getHeight() == 0) {
      return false;
    } else if (other instanceof AbstractCanvasObject) {
      AbstractCanvasObject that = (AbstractCanvasObject) other;
      for (var i = 0; i < OVERLAP_TRIES; i++) {
        if (i % 2 == 0) {
          final var loc = this.getRandomPoint(c, rand);
          if (loc != null && that.contains(loc, false)) return true;
        } else {
          Location loc = that.getRandomPoint(c, rand);
          if (loc != null && this.contains(loc, false)) return true;
        }
      }
      return false;
    } else {
      for (var i = 0; i < OVERLAP_TRIES; i++) {
        final var loc = this.getRandomPoint(c, rand);
        if (loc != null && other.contains(loc, false)) return true;
      }
      return false;
    }
  }

  @Override
  public abstract void paint(Graphics g, HandleGesture gesture);

  @Override
  public void removeAttributeListener(AttributeListener l) {
    listeners.remove(l);
  }

  protected boolean setForFill(Graphics g) {
    final var attrs = getAttributes();
    if (attrs.contains(DrawAttr.PAINT_TYPE)) {
      Object value = getValue(DrawAttr.PAINT_TYPE);
      if (value == DrawAttr.PAINT_STROKE) return false;
    }

    final var color = getValue(DrawAttr.FILL_COLOR);
    if (color != null && color.getAlpha() == 0) {
      return false;
    } else {
      if (color != null) g.setColor(color);
      return true;
    }
  }

  protected boolean setForStroke(Graphics g) {
    final var attrs = getAttributes();
    if (attrs.contains(DrawAttr.PAINT_TYPE)) {
      Object value = getValue(DrawAttr.PAINT_TYPE);
      if (value == DrawAttr.PAINT_FILL) return false;
    }

    final var width = getValue(DrawAttr.STROKE_WIDTH);
    if (width != null && width > 0) {
      final var color = getValue(DrawAttr.STROKE_COLOR);
      if (color != null && color.getAlpha() == 0) {
        return false;
      } else {
        GraphicsUtil.switchToWidth(g, width);
        if (color != null) g.setColor(color);
        return true;
      }
    } else {
      return false;
    }
  }

  @Override
  public void setReadOnly(Attribute<?> attr, boolean value) {
    throw new UnsupportedOperationException("setReadOnly");
  }

  @Override
  public final <V> void setValue(Attribute<V> attr, V value) {
    Object old = getValue(attr);
    final var same = Objects.equals(old, value);
    if (!same) {
      updateValue(attr, value);
      final var e = new AttributeEvent(this, attr, value, old);
      for (final var listener : listeners) {
        listener.attributeValueChanged(e);
      }
    }
  }

  public abstract Element toSvgElement(Document doc);

  @Override
  public abstract void translate(int dx, int dy);

  protected abstract void updateValue(Attribute<?> attr, Object value);
}
