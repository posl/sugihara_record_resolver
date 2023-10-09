/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package stroom.index.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.index.client.presenter.IndexVolumeEditPresenter.IndexVolumeEditView;
import stroom.index.shared.IndexVolume;
import stroom.index.shared.IndexVolume.VolumeUseState;
import stroom.index.shared.IndexVolumeResource;
import stroom.item.client.ItemListBox;
import stroom.node.client.NodeManager;
import stroom.util.shared.ModelStringUtil;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Focus;
import com.google.gwt.user.client.ui.HasText;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

import java.util.List;
import java.util.function.Consumer;

public class IndexVolumeEditPresenter extends MyPresenterWidget<IndexVolumeEditView> {

    private static final IndexVolumeResource INDEX_VOLUME_RESOURCE = GWT.create(IndexVolumeResource.class);

    private final RestFactory restFactory;
    private final NodeManager nodeManager;

    private IndexVolume volume;

    @Inject
    public IndexVolumeEditPresenter(final EventBus eventBus,
                                    final IndexVolumeEditView view,
                                    final RestFactory restFactory,
                                    final NodeManager nodeManager) {
        super(eventBus, view);
        this.restFactory = restFactory;
        this.nodeManager = nodeManager;
    }

    void show(final IndexVolume volume, final String caption, final Consumer<IndexVolume> consumer) {
        nodeManager.listEnabledNodes(
                nodeNames -> {
                    read(nodeNames, volume);

                    final PopupSize popupSize = PopupSize.resizableX();
                    ShowPopupEvent.builder(this)
                            .popupType(PopupType.OK_CANCEL_DIALOG)
                            .popupSize(popupSize)
                            .caption(caption)
                            .onShow(e -> getView().focus())
                            .onHideRequest(event -> {
                                if (event.isOk()) {
                                    try {
                                        write();
                                        final Rest<IndexVolume> rest = restFactory.create();
                                        if (volume.getId() != null) {
                                            rest
                                                    .onSuccess(consumer)
                                                    .call(INDEX_VOLUME_RESOURCE)
                                                    .update(volume.getId(), volume);
                                        } else {
                                            rest
                                                    .onSuccess(consumer)
                                                    .call(INDEX_VOLUME_RESOURCE)
                                                    .create(volume);
                                        }

                                    } catch (final RuntimeException e) {
                                        AlertEvent.fireError(IndexVolumeEditPresenter.this, e.getMessage(), null);
                                    }
                                } else {
                                    consumer.accept(null);
                                }
                            })
                            .fire();
                },
                throwable -> {
                    AlertEvent.fireError(IndexVolumeEditPresenter.this, throwable.getMessage(), null);
                    consumer.accept(null);
                });
    }

    void hide() {
        HidePopupEvent.builder(this).fire();
    }

    private void read(final List<String> nodeNames, final IndexVolume volume) {
        this.volume = volume;

        getView().setNodeNames(nodeNames);
        getView().getNodeName().setText(volume.getNodeName());
        getView().getPath().setText(volume.getPath());
        getView().getState().addItems(VolumeUseState.values());
        getView().getState().setSelectedItem(volume.getState());

        if (volume.getBytesLimit() != null) {
            getView().getByteLimit().setText(ModelStringUtil.formatIECByteSizeString(volume.getBytesLimit()));
        } else {
            getView().getByteLimit().setText("");
        }
    }

    private void write() {
        volume.setNodeName(getView().getNodeName().getText());
        volume.setPath(getView().getPath().getText());
        volume.setState(getView().getState().getSelectedItem());

        Long bytesLimit = null;
        final String limit = getView().getByteLimit().getText().trim();
        if (limit.length() > 0) {
            bytesLimit = ModelStringUtil.parseIECByteSizeString(limit);
        }
        volume.setBytesLimit(bytesLimit);
    }

    public interface IndexVolumeEditView extends View, Focus {

        void setNodeNames(List<String> nodeNames);

        HasText getNodeName();

        HasText getPath();

        ItemListBox<VolumeUseState> getState();

        HasText getByteLimit();
    }
}
