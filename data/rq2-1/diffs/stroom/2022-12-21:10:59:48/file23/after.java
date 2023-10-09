/*
 * Copyright 2016 Crown Copyright
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
 */

package stroom.importexport.client.event;

import stroom.importexport.shared.ImportConfigResponse;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

public class ImportConfigConfirmEvent extends GwtEvent<ImportConfigConfirmEvent.Handler> {

    private static Type<Handler> TYPE;
    private final ImportConfigResponse response;

    private ImportConfigConfirmEvent(final ImportConfigResponse response) {
        this.response = response;
    }

    public static void fire(final HasHandlers source,
                            final ImportConfigResponse response) {
        source.fireEvent(new ImportConfigConfirmEvent(response));
    }

    public static Type<Handler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return getType();
    }

    @Override
    protected void dispatch(final Handler handler) {
        handler.onConfirmImport(this);
    }

    public ImportConfigResponse getResponse() {
        return response;
    }

    public interface Handler extends EventHandler {

        void onConfirmImport(ImportConfigConfirmEvent event);
    }
}
