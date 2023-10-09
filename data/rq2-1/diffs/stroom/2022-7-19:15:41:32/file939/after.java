package stroom.security.impl.db;

import stroom.db.util.GenericDao;
import stroom.db.util.JooqUtil;
import stroom.security.api.ProcessingUserIdentityProvider;
import stroom.security.impl.UserDao;
import stroom.security.impl.db.jooq.tables.StroomUser;
import stroom.security.impl.db.jooq.tables.records.StroomUserRecord;
import stroom.security.shared.User;
import stroom.util.filter.QuickFilterPredicateFactory;

import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Record1;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

import static stroom.security.impl.db.jooq.Tables.STROOM_USER;
import static stroom.security.impl.db.jooq.Tables.STROOM_USER_GROUP;

public class UserDaoImpl implements UserDao {

    private static final Function<Record, User> RECORD_TO_USER_MAPPER = record -> {
        final User user = new User();
        user.setId(record.get(STROOM_USER.ID));
        user.setVersion(record.get(STROOM_USER.VERSION));
        user.setCreateTimeMs(record.get(STROOM_USER.CREATE_TIME_MS));
        user.setCreateUser(record.get(STROOM_USER.CREATE_USER));
        user.setUpdateTimeMs(record.get(STROOM_USER.UPDATE_TIME_MS));
        user.setUpdateUser(record.get(STROOM_USER.UPDATE_USER));
        user.setName(record.get(STROOM_USER.NAME));
        user.setUuid(record.get(STROOM_USER.UUID));
        user.setGroup(record.get(STROOM_USER.IS_GROUP));
        return user;
    };

    private static final BiFunction<User, StroomUserRecord, StroomUserRecord> USER_TO_RECORD_MAPPER =
            (user, record) -> {
                record.from(user);
                record.set(STROOM_USER.ID, user.getId());
                record.set(STROOM_USER.VERSION, user.getVersion());
                record.set(STROOM_USER.CREATE_TIME_MS, user.getCreateTimeMs());
                record.set(STROOM_USER.CREATE_USER, user.getCreateUser());
                record.set(STROOM_USER.UPDATE_TIME_MS, user.getUpdateTimeMs());
                record.set(STROOM_USER.UPDATE_USER, user.getUpdateUser());
                record.set(STROOM_USER.NAME, user.getName());
                record.set(STROOM_USER.UUID, user.getUuid());
                record.set(STROOM_USER.IS_GROUP, user.isGroup());
                record.set(STROOM_USER.ENABLED, true);
                return record;
            };


    private final ProcessingUserIdentityProvider processingUserIdentityProvider;
    private final SecurityDbConnProvider securityDbConnProvider;
    private final GenericDao<StroomUserRecord, User, Integer> genericDao;

    @Inject
    public UserDaoImpl(final ProcessingUserIdentityProvider processingUserIdentityProvider,
                       final SecurityDbConnProvider securityDbConnProvider) {
        this.processingUserIdentityProvider = processingUserIdentityProvider;
        this.securityDbConnProvider = securityDbConnProvider;

        genericDao = new GenericDao<>(
                securityDbConnProvider,
                STROOM_USER,
                STROOM_USER.ID,
                USER_TO_RECORD_MAPPER,
                RECORD_TO_USER_MAPPER);
    }

    @Override
    public User create(final User user) {
        return genericDao.create(user);
    }

    @Override
    public User tryCreate(final User user, final Consumer<User> onUserCreateAction) {
        return genericDao.tryCreate(user, STROOM_USER.NAME, STROOM_USER.IS_GROUP, onUserCreateAction);
    }

    @Override
    public Optional<User> getById(final int id) {
        return genericDao.fetch(id);
    }

    @Override
    public Optional<User> getByUuid(final String uuid) {
        return JooqUtil.contextResult(securityDbConnProvider, context -> context
                .select()
                .from(STROOM_USER)
                .where(STROOM_USER.UUID.eq(uuid))
                .fetchOptional())
                .map(RECORD_TO_USER_MAPPER);
    }

    @Override
    public Optional<User> getByName(final String name) {
        return JooqUtil.contextResult(securityDbConnProvider, context -> context
                .select()
                .from(STROOM_USER)
                .where(STROOM_USER.NAME.eq(name))
                .fetchOptional())
                .map(RECORD_TO_USER_MAPPER);
    }

    @Override
    public Optional<User> getByName(final String name,
                                    final boolean isGroup) {
        return JooqUtil.contextResult(securityDbConnProvider, context -> context
                .select()
                .from(STROOM_USER)
                .where(STROOM_USER.NAME.eq(name))
                .and(STROOM_USER.IS_GROUP.eq(isGroup))
                .fetchOptional())
                .map(RECORD_TO_USER_MAPPER);
    }

    @Override
    public User update(final User user) {
        return genericDao.update(user);
    }

    @Override
    public void delete(final String uuid) {
        JooqUtil.context(securityDbConnProvider, context -> context
                .deleteFrom(STROOM_USER)
                .where(STROOM_USER.UUID.eq(uuid))
                .execute()
        );
    }

    @Override
    public List<User> find(final String quickFilterInput,
                           final boolean isGroup) {
        final Condition condition = STROOM_USER.IS_GROUP.eq(isGroup);

        return QuickFilterPredicateFactory.filterStream(
                quickFilterInput,
                FILTER_FIELD_MAPPERS,
                JooqUtil.contextResult(securityDbConnProvider, context -> context
                        .select().from(STROOM_USER)
                        .where(condition)
                        .and(getExcludedUsersCondition())
                        .orderBy(STROOM_USER.NAME)
                        .fetch())
                        .stream()
                        .map(RECORD_TO_USER_MAPPER)
        ).collect(Collectors.toList());
    }

    @Override
    public List<User> findUsersInGroup(final String groupUuid, final String quickFilterInput) {
        return QuickFilterPredicateFactory.filterStream(
                quickFilterInput,
                FILTER_FIELD_MAPPERS,
                JooqUtil.contextResult(securityDbConnProvider, context -> context
                        .select()
                        .from(STROOM_USER)
                        .join(STROOM_USER_GROUP)
                        .on(STROOM_USER.UUID.eq(STROOM_USER_GROUP.USER_UUID))
                        .where(STROOM_USER_GROUP.GROUP_UUID.eq(groupUuid))
                        .and(getExcludedUsersCondition())
                        .fetch())
                        .stream()
                        .map(RECORD_TO_USER_MAPPER)
        ).collect(Collectors.toList());
    }


    @Override
    public List<User> findGroupsForUser(final String userUuid, final String quickFilterInput) {
        return QuickFilterPredicateFactory.filterStream(
                quickFilterInput,
                FILTER_FIELD_MAPPERS,
                JooqUtil.contextResult(securityDbConnProvider, context -> context
                        .select()
                        .from(STROOM_USER)
                        .join(STROOM_USER_GROUP)
                        .on(STROOM_USER.UUID.eq(STROOM_USER_GROUP.GROUP_UUID))
                        .where(STROOM_USER_GROUP.USER_UUID.eq(userUuid))
                        .fetch())
                        .stream()
                        .map(RECORD_TO_USER_MAPPER)
        ).collect(Collectors.toList());
    }

    @Override
    public Set<String> findGroupUuidsForUser(final String userUuid) {
        return JooqUtil.contextResult(securityDbConnProvider, context ->
                context.select(STROOM_USER_GROUP.GROUP_UUID)
                        .from(STROOM_USER_GROUP)
                        .where(STROOM_USER_GROUP.USER_UUID.eq(userUuid))
                        .fetch())
                .stream()
                .map(Record1::value1)
                .collect(Collectors.toSet());
    }

    @Override
    public List<User> findGroupsForUserName(final String userName) {
        StroomUser userUser = STROOM_USER.as("userUser");
        StroomUser groupUser = STROOM_USER.as("groupUser");
        return JooqUtil.contextResult(securityDbConnProvider, context ->
                context.select()
                        .from(groupUser)
                        // group users -> groups
                        .join(STROOM_USER_GROUP)
                        .on(groupUser.UUID.eq(STROOM_USER_GROUP.GROUP_UUID))
                        // users -> groups
                        .join(userUser)
                        .on(userUser.UUID.eq(STROOM_USER_GROUP.USER_UUID))
                        .where(userUser.NAME.eq(userName))
                        .orderBy(groupUser.NAME)
                        .fetch())
                .map(RECORD_TO_USER_MAPPER::apply);
    }

    @Override
    public void addUserToGroup(final String userUuid,
                               final String groupUuid) {
        JooqUtil.context(securityDbConnProvider, context ->
                context.insertInto(STROOM_USER_GROUP)
                        .columns(STROOM_USER_GROUP.USER_UUID, STROOM_USER_GROUP.GROUP_UUID)
                        .values(userUuid, groupUuid)
                        .onDuplicateKeyIgnore()
                        .execute());
    }

    @Override
    public void removeUserFromGroup(final String userUuid,
                                    final String groupUuid) {
        JooqUtil.context(securityDbConnProvider, context ->
                context.deleteFrom(STROOM_USER_GROUP)
                        .where(STROOM_USER_GROUP.USER_UUID.eq(userUuid))
                        .and(STROOM_USER_GROUP.GROUP_UUID.eq(groupUuid))
                        .execute());
    }

    private Condition getExcludedUsersCondition() {
        final String procUserId = processingUserIdentityProvider.get().getId();
        return STROOM_USER.NAME.notEqual(procUserId);
    }
}
