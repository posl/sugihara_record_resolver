/*-
 * ========================LICENSE_START=================================
 * restheart-mongodb
 * %%
 * Copyright (C) 2014 - 2020 SoftInstigate
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * =========================LICENSE_END==================================
 */
package org.restheart.mongodb.handlers.database;

import com.google.common.annotations.VisibleForTesting;
import io.undertow.server.HttpServerExchange;
import java.util.Optional;

import org.bson.BsonArray;
import org.restheart.exchange.MongoRequest;
import org.restheart.exchange.MongoResponse;
import org.restheart.handlers.PipelinedHandler;
import org.restheart.mongodb.db.Database;
import org.restheart.mongodb.db.DatabaseImpl;
import org.restheart.mongodb.utils.ResponseHelper;
import org.restheart.utils.HttpStatus;

/**
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class GetDBHandler extends PipelinedHandler {
    private Database dbsDAO = new DatabaseImpl();

    /**
     * Creates a new instance of GetDBHandler
     */
    public GetDBHandler() {
        super();
    }

    /**
     *
     * @param next
     */
    public GetDBHandler(PipelinedHandler next) {
        super(next);
    }

    /**
     *
     * @param next
     * @param dbsDAO
     */
    @VisibleForTesting
    public GetDBHandler(PipelinedHandler next, Database dbsDAO) {
        super(next);
        this.dbsDAO = dbsDAO;
    }

    /**
     *
     * @param exchange
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        var request = MongoRequest.of(exchange);
        var response = MongoResponse.of(exchange);

        if (request.isInError()) {
            next(exchange);
            return;
        }

        var colls = dbsDAO.getCollectionNames(Optional.ofNullable(request.getClientSession()), request.getDBName());

        if (request.getPagesize() > 0) {
            var data = dbsDAO.getDatabaseData(
                Optional.ofNullable(request.getClientSession()),
                request.getDBName(),
                colls,
                request.getPage(),
                request.getPagesize(),
                request.isNoCache());
            response.setContent(data);
        } else {
            response.setContent(new BsonArray());
        }

        response.setCount(dbsDAO.getDBSize(colls));

        response.setContentTypeAsJson();
        response.setStatusCode(HttpStatus.SC_OK);

        ResponseHelper.injectEtagHeader(exchange, request.getDbProps());

        next(exchange);
    }
}
