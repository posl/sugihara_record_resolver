package com.cburch.contracts;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * Dummy implementation of java.awt.event.MouseMotionListener interface. The main purpose of this
 * interface is to provide default (empty) implementation of interface methods as, unfortunately
 * JDKs interfaces do not come with default implementation even they easily could. Implementing this
 * interface instead of the parent one allows skipping the need of implementing all, even unneeded,
 * methods. That's saves some efforts and reduces overall LOC.
 */
public interface BaseMouseMotionListenerContract extends MouseMotionListener {
  @Override
  default void mouseDragged(MouseEvent mouseEvent) {
    // no-op implementation
  }

  @Override
  default void mouseMoved(MouseEvent mouseEvent) {
    // no-op implementation
  }
}
