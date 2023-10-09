package stroom.processor.impl.db;

import stroom.db.util.ExpressionMapper;
import stroom.db.util.ExpressionMapperFactory;
import stroom.db.util.GenericDao;
import stroom.db.util.JooqUtil;
import stroom.entity.shared.ExpressionCriteria;
import stroom.processor.impl.ProcessorDao;
import stroom.processor.impl.db.jooq.tables.records.ProcessorRecord;
import stroom.processor.shared.Processor;
import stroom.processor.shared.ProcessorFields;
import stroom.util.shared.ResultPage;

import org.jooq.Condition;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

import static stroom.processor.impl.db.jooq.tables.Processor.PROCESSOR;
import static stroom.processor.impl.db.jooq.tables.ProcessorFilter.PROCESSOR_FILTER;

class ProcessorDaoImpl implements ProcessorDao {

    private final ProcessorDbConnProvider processorDbConnProvider;
    private final GenericDao<ProcessorRecord, Processor, Integer> genericDao;
    private final ExpressionMapper expressionMapper;

    @Inject
    public ProcessorDaoImpl(final ProcessorDbConnProvider processorDbConnProvider,
                            final ExpressionMapperFactory expressionMapperFactory) {
        this.processorDbConnProvider = processorDbConnProvider;
        this.genericDao = new GenericDao<>(processorDbConnProvider, PROCESSOR, PROCESSOR.ID, Processor.class);

        expressionMapper = expressionMapperFactory.create();
        expressionMapper.map(ProcessorFields.ID, PROCESSOR.ID, Integer::valueOf);
        expressionMapper.map(ProcessorFields.CREATE_USER, PROCESSOR_FILTER.CREATE_USER, value -> value);
        expressionMapper.map(ProcessorFields.PIPELINE, PROCESSOR.PIPELINE_UUID, value -> value);
        expressionMapper.map(ProcessorFields.ENABLED, PROCESSOR.ENABLED, Boolean::valueOf);
        expressionMapper.map(ProcessorFields.DELETED, PROCESSOR.DELETED, Boolean::valueOf);
        expressionMapper.map(ProcessorFields.UUID, PROCESSOR.UUID, value -> value);
    }

    @Override
    public Processor create(final Processor processor) {
        // We don't use the delegate DAO here as we want to handle potential duplicates carefully so this
        // behaves as a getOrCreate method.
        return JooqUtil.contextResult(processorDbConnProvider, context -> {
            final Optional<ProcessorRecord> optional = context
                    .insertInto(PROCESSOR,
                            PROCESSOR.CREATE_TIME_MS,
                            PROCESSOR.CREATE_USER,
                            PROCESSOR.UPDATE_TIME_MS,
                            PROCESSOR.UPDATE_USER,
                            PROCESSOR.UUID,
                            PROCESSOR.TASK_TYPE,
                            PROCESSOR.PIPELINE_UUID,
                            PROCESSOR.ENABLED,
                            PROCESSOR.DELETED)
                    .values(processor.getCreateTimeMs(),
                            processor.getCreateUser(),
                            processor.getUpdateTimeMs(),
                            processor.getUpdateUser(),
                            processor.getUuid(),
                            processor.getTaskType(),
                            processor.getPipelineUuid(),
                            processor.isEnabled(),
                            processor.isDeleted())
                    .onDuplicateKeyIgnore()
                    .returning(PROCESSOR.ID)
                    .fetchOptional();

            if (optional.isPresent()) {
                final Integer id = optional.get().getId();
                return context
                        .select()
                        .from(PROCESSOR)
                        .where(PROCESSOR.ID.eq(id))
                        .fetchOneInto(Processor.class);
            }

            return context
                    .select()
                    .from(PROCESSOR)
                    .where(PROCESSOR.PIPELINE_UUID.eq(processor.getPipelineUuid()))
                    .fetchOneInto(Processor.class);
        });
    }

    @Override
    public Processor update(final Processor processor) {
        return genericDao.update(processor);
    }

    @Override
    public boolean delete(final int id) {
        return genericDao.delete(id);
    }

    @Override
    public boolean logicalDelete(final int id) {
        return JooqUtil.contextResult(processorDbConnProvider, context -> context
                .update(PROCESSOR)
                .set(PROCESSOR.DELETED, true)
                .where(PROCESSOR.ID.eq(id))
                .execute()) > 0;
    }

    @Override
    public Optional<Processor> fetch(final int id) {
        return genericDao.fetch(id);
    }

    @Override
    public Optional<Processor> fetchByUuid(final String uuid) {
        return JooqUtil.contextResult(processorDbConnProvider, context -> context
                        .select()
                        .from(PROCESSOR)
                        .where(PROCESSOR.UUID.eq(uuid))
                        .fetchOptional())
                .map(record -> record.into(Processor.class));
    }

    @Override
    public ResultPage<Processor> find(final ExpressionCriteria criteria) {
        final Condition condition = expressionMapper.apply(criteria.getExpression());
        final int offset = JooqUtil.getOffset(criteria.getPageRequest());
        final int limit = JooqUtil.getLimit(criteria.getPageRequest(), true);
        final List<Processor> list = JooqUtil.contextResult(processorDbConnProvider, context -> context
                        .select()
                        .from(PROCESSOR)
                        .where(condition)
                        .limit(offset, limit)
                        .fetch())
                .into(Processor.class);
        return ResultPage.createCriterialBasedList(list, criteria);
    }
}
