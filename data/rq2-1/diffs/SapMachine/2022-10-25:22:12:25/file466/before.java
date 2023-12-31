/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */
package com.sun.hotspot.igv.bytecodes;

import com.sun.hotspot.igv.data.ChangedListener;
import com.sun.hotspot.igv.data.Group;
import com.sun.hotspot.igv.data.InputGraph;
import com.sun.hotspot.igv.data.services.InputGraphProvider;
import com.sun.hotspot.igv.util.LookupHistory;
import java.awt.BorderLayout;
import java.io.Serializable;
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.util.*;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * @author Thomas Wuerthinger
 */
final class BytecodeViewTopComponent extends TopComponent implements ExplorerManager.Provider, ChangedListener<InputGraphProvider> {

    private static BytecodeViewTopComponent instance;
    private static final String PREFERRED_ID = "BytecodeViewTopComponent";
    private final ExplorerManager manager;
    private final BeanTreeView treeView;
    private MethodNode rootNode;

    private BytecodeViewTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(BytecodeViewTopComponent.class, "CTL_BytecodeViewTopComponent"));
        setToolTipText(NbBundle.getMessage(BytecodeViewTopComponent.class, "HINT_BytecodeViewTopComponent"));

        manager = new ExplorerManager();
        rootNode = new MethodNode(null, null, "");
        manager.setRootContext(rootNode);

        setLayout(new BorderLayout());

        treeView = new BeanTreeView();
        treeView.setRootVisible(false);
        this.add(BorderLayout.CENTER, treeView);
        associateLookup(ExplorerUtils.createLookup(manager, getActionMap()));
    }

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance()}.
     */
    public static synchronized BytecodeViewTopComponent getDefault() {
        if (instance == null) {
            instance = new BytecodeViewTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the BytecodeViewTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized BytecodeViewTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            ErrorManager.getDefault().log(ErrorManager.WARNING, "Cannot find BytecodeView component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof BytecodeViewTopComponent) {
            return (BytecodeViewTopComponent) win;
        }
        ErrorManager.getDefault().log(ErrorManager.WARNING, "There seem to be multiple components with the '" + PREFERRED_ID + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    public void componentOpened() {
        LookupHistory.addListener(InputGraphProvider.class, this);
    }

    @Override
    public void componentClosed() {
        LookupHistory.removeListener(InputGraphProvider.class, this);
    }

    @Override
    public Object writeReplace() {
        return new ResolvableHelper();
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return manager;
    }

    @Override
    public void requestActive() {
        super.requestActive();
        this.treeView.requestFocus();
    }

    @Override
    public boolean requestFocus(boolean temporary) {
        this.treeView.requestFocus();
        return super.requestFocus(temporary);
    }

    @Override
    protected boolean requestFocusInWindow(boolean temporary) {
        this.treeView.requestFocus();
        return super.requestFocusInWindow(temporary);
    }

    @Override
    public void changed(InputGraphProvider lastProvider) {
        if (lastProvider != null) {
            InputGraph graph = lastProvider.getGraph();
            if (graph != null) {
                Group g = graph.getGroup();
                rootNode.update(graph, g.getMethod());
                return;
            }
        }
        rootNode = new MethodNode(null, null, "");
        manager.setRootContext(rootNode);
    }

    static final class ResolvableHelper implements Serializable {

        private static final long serialVersionUID = 1L;

        public Object readResolve() {
            return BytecodeViewTopComponent.getDefault();
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
