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
package org.restheart.mongodb.interceptors;

import com.mongodb.MongoClient;
import org.bson.BsonDocument;
import org.bson.BsonString;
import static org.restheart.exchange.ExchangeKeys.FS_FILES_SUFFIX;
import static org.restheart.exchange.ExchangeKeys._SCHEMAS;

import java.util.Optional;

import org.restheart.exchange.MongoRequest;
import org.restheart.exchange.MongoResponse;
import org.restheart.mongodb.db.DatabaseImpl;
import org.restheart.plugins.InjectMongoClient;
import org.restheart.plugins.InterceptPoint;
import org.restheart.plugins.MongoInterceptor;
import org.restheart.plugins.RegisterPlugin;
import org.restheart.utils.HttpStatus;

/**
 * Injects the collection properties into the Request
 *
 * It is also responsible of sending NOT_FOUND in case of requests involving not
 * existing collections (that are not PUT)
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
@RegisterPlugin(name = "collectionPropsInjector",
        description = "Injects the collection properties into the BsonRequest",
        interceptPoint = InterceptPoint.REQUEST_BEFORE_AUTH,
        priority = Integer.MIN_VALUE + 1)
public class CollectionPropsInjector implements MongoInterceptor {
    private DatabaseImpl dbsDAO = null;

    private static final String RESOURCE_DOES_NOT_EXIST = "Resource does not exist";
    private static final String COLLECTION_DOES_NOT_EXIST = "Collection '%s' does not exist";
    private static final String FILE_BUCKET_DOES_NOT_EXIST = "File Bucket '%s' does not exist";
    private static final String SCHEMA_STORE_DOES_NOT_EXIST = "Schema Store does not exist";

    /**
     * Makes sure that dbsDAO is instantiated after MongoClient initialization
     *
     * @param mclient
     */
    @InjectMongoClient
    public void init(MongoClient mclient) {
        this.dbsDAO = new DatabaseImpl();
    }

    /**
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @Override
    public void handle(MongoRequest request, MongoResponse response) throws Exception {
        var dbName = request.getDBName();
        var collName = request.getCollectionName();

        if (dbName != null && collName != null && !request.isDbMeta()) {
            BsonDocument collProps;

            if (!MetadataCachesSingleton.isEnabled() || request.isNoCache()) {
                collProps = dbsDAO.getCollectionProperties(Optional.ofNullable(request.getClientSession()), dbName, collName);
            } else {
                collProps = MetadataCachesSingleton.getInstance().getCollectionProperties(dbName, collName);
            }

            // if collProps is null, the collection does not exist
            if (collProps == null && checkCollection(request)) {
                doesNotExists(request, response);
                return;
            }

            if (collProps == null && request.isGet()) {
                collProps = new BsonDocument("_id", new BsonString(collName));
            }

            request.setCollectionProps(collProps);
        }
    }

    @Override
    public boolean resolve(MongoRequest request, MongoResponse response) {
        return dbsDAO != null
                && request.isHandledBy("mongo")
                && !(request.isInError()
                || request.isMetrics()
                || request.isDbSize()
                || request.isTxn()
                || request.isTxns());
    }

    /**
     *
     * @param request
     * @param response
     * @throws Exception
     */
    protected void doesNotExists(MongoRequest request, MongoResponse response)
            throws Exception {
        final String errMsg;
        final String resourceName = request.getCollectionName();

        if (resourceName == null) {
            errMsg = RESOURCE_DOES_NOT_EXIST;
        } else if (resourceName.endsWith(FS_FILES_SUFFIX)) {
            errMsg = String.format(FILE_BUCKET_DOES_NOT_EXIST, request.getCollectionName());
        } else if (_SCHEMAS.equals(resourceName)) {
            errMsg = SCHEMA_STORE_DOES_NOT_EXIST;
        } else {
            errMsg = String.format(COLLECTION_DOES_NOT_EXIST, request.getCollectionName());
        }

        response.setInError(HttpStatus.SC_NOT_FOUND, errMsg);
    }

    /**
     *
     * @param request
     * @return
     */
    public static boolean checkCollection(MongoRequest request) {
        return !(request.isCollection() && request.isPut())
                && !(request.isFilesBucket() && request.isPut())
                && !(request.isSchemaStore() && request.isPut())
                && !request.isRoot()
                && !request.isDb()
                && !request.isDbSize();
    }
}
