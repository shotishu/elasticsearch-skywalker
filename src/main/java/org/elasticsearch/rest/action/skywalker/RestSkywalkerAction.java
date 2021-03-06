/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.rest.action.skywalker;

import java.io.IOException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.skywalker.SkywalkerAction;
import org.elasticsearch.action.skywalker.SkywalkerRequest;
import org.elasticsearch.action.skywalker.SkywalkerResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;
import org.elasticsearch.rest.action.support.RestActions;
import static org.elasticsearch.rest.action.support.RestActions.buildBroadcastShardsHeader;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

public class RestSkywalkerAction extends BaseRestHandler {

    @Inject
    public RestSkywalkerAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/_skywalker", this);
        controller.registerHandler(POST, "/{index}/_skywalker", this);
        controller.registerHandler(GET, "/_skywalker", this);
        controller.registerHandler(GET, "/{index}/_skywalker", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        SkywalkerRequest r = new SkywalkerRequest(RestActions.splitIndices(request.param("index")));
        client.execute(SkywalkerAction.INSTANCE, r, new ActionListener<SkywalkerResponse>() {

            @Override
            public void onResponse(SkywalkerResponse response) {
                try {
                    XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
                    builder.startObject();
                    builder.field("ok", true);
                    buildBroadcastShardsHeader(builder, response);
                    builder.field("result", response.getResponse());
                    builder.endObject();
                    channel.sendResponse(new XContentRestResponse(request, OK, builder));
                } catch (Exception e) {
                    onFailure(e);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                try {
                    channel.sendResponse(new XContentThrowableRestResponse(request, e));
                } catch (IOException e1) {
                    logger.error("Failed to send failure response", e1);
                }
            }
        });
    }
}