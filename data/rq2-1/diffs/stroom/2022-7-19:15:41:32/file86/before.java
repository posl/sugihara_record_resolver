/*
 * This file is generated by jOOQ.
 */
package stroom.config.impl.db.jooq;


import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import stroom.config.impl.db.jooq.tables.Config;
import stroom.config.impl.db.jooq.tables.Preferences;


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
     * The table <code>stroom.config</code>.
     */
    public final Config CONFIG = Config.CONFIG;

    /**
     * The table <code>stroom.preferences</code>.
     */
    public final Preferences PREFERENCES = Preferences.PREFERENCES;

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
        return Arrays.<Table<?>>asList(
            Config.CONFIG,
            Preferences.PREFERENCES);
    }
}