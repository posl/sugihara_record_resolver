/*
 * This file is generated by jOOQ.
 */
package stroom.security.identity.db.jooq;


import stroom.security.identity.db.jooq.tables.Account;
import stroom.security.identity.db.jooq.tables.JsonWebKey;
import stroom.security.identity.db.jooq.tables.OauthClient;
import stroom.security.identity.db.jooq.tables.Token;
import stroom.security.identity.db.jooq.tables.TokenType;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Stroom extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom</code>
     */
    public static final Stroom STROOM = new Stroom();

    /**
     * The table <code>stroom.account</code>.
     */
    public final Account ACCOUNT = Account.ACCOUNT;

    /**
     * The table <code>stroom.json_web_key</code>.
     */
    public final JsonWebKey JSON_WEB_KEY = JsonWebKey.JSON_WEB_KEY;

    /**
     * The table <code>stroom.oauth_client</code>.
     */
    public final OauthClient OAUTH_CLIENT = OauthClient.OAUTH_CLIENT;

    /**
     * The table <code>stroom.token</code>.
     */
    public final Token TOKEN = Token.TOKEN;

    /**
     * The table <code>stroom.token_type</code>.
     */
    public final TokenType TOKEN_TYPE = TokenType.TOKEN_TYPE;

    /**
     * No further instances allowed
     */
    private Stroom() {
        super("stroom", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            Account.ACCOUNT,
            JsonWebKey.JSON_WEB_KEY,
            OauthClient.OAUTH_CLIENT,
            Token.TOKEN,
            TokenType.TOKEN_TYPE
        );
    }
}