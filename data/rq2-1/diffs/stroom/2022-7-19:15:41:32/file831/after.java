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
 */

package stroom.receive.common;

import stroom.proxy.feed.remote.GetFeedStatusRequest;
import stroom.proxy.feed.remote.GetFeedStatusResponse;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Tag(name = "Feed Status")
@Path(FeedStatusResource.BASE_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface FeedStatusResource extends RestResource {

    String BASE_RESOURCE_PATH = "/feedStatus" + ResourcePaths.V1;
    String GET_FEED_STATUS_PATH_PART = "/getFeedStatus";

    @POST
    @Path(GET_FEED_STATUS_PATH_PART)
    @Operation(
            summary = "Submit a request to get the status of a feed",
            operationId = "getFeedStatus")
        // TODO This should really be a GET with the feedName and senderDn as params
    GetFeedStatusResponse getFeedStatus(
            @Parameter(description = "GetFeedStatusRequest", required = true) GetFeedStatusRequest request);
}
