package stroom.pipeline.refdata;

import stroom.pipeline.refdata.store.RefDataStoreModule.RefDataPurge;
import stroom.util.cache.CacheConfig;
import stroom.util.config.annotations.RequiresRestart;
import stroom.util.config.annotations.RequiresRestart.RestartScope;
import stroom.util.shared.AbstractConfig;
import stroom.util.shared.IsStroomConfig;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.constraints.Min;

@JsonPropertyOrder(alphabetic = true)
public class ReferenceDataConfig extends AbstractConfig implements IsStroomConfig {

    private final int maxPutsBeforeCommit;
    private final int maxPurgeDeletesBeforeCommit;
    private final StroomDuration purgeAge;
    private final int loadingLockStripes;
    private final ReferenceDataLmdbConfig lmdbConfig;
    private final CacheConfig effectiveStreamCache;

    public ReferenceDataConfig() {
        maxPutsBeforeCommit = 200_000;
        maxPurgeDeletesBeforeCommit = 200_000;
        purgeAge = StroomDuration.ofDays(30);
        loadingLockStripes = 2048;
        lmdbConfig = new ReferenceDataLmdbConfig();

        effectiveStreamCache = CacheConfig.builder()
                .maximumSize(1000L)
                .expireAfterWrite(StroomDuration.ofMinutes(10))
                .build();
    }

    @JsonCreator
    public ReferenceDataConfig(@JsonProperty("maxPutsBeforeCommit") final int maxPutsBeforeCommit,
                               @JsonProperty("maxPurgeDeletesBeforeCommit") final int maxPurgeDeletesBeforeCommit,
                               @JsonProperty("purgeAge") final StroomDuration purgeAge,
                               @JsonProperty("loadingLockStripes") final int loadingLockStripes,
                               @JsonProperty("lmdb") final ReferenceDataLmdbConfig lmdbConfig,
                               @JsonProperty("effectiveStreamCache") final CacheConfig effectiveStreamCache) {
        this.maxPutsBeforeCommit = maxPutsBeforeCommit;
        this.maxPurgeDeletesBeforeCommit = maxPurgeDeletesBeforeCommit;
        this.purgeAge = purgeAge;
        this.loadingLockStripes = loadingLockStripes;
        this.lmdbConfig = lmdbConfig;
        this.effectiveStreamCache = effectiveStreamCache;
    }

    @Min(0)
    @JsonPropertyDescription("The maximum number of puts into the store (in a single load) before the " +
            "transaction is committed. There is only one write transaction available at a time so reducing " +
            "this value allows multiple loads to potentially each load a chunk at a time. However, load times " +
            "increase rapidly with values below around 2,000. For maximum performance of a single load set this " +
            "value to 0 to only commit at the very end of the load.")
    public int getMaxPutsBeforeCommit() {
        return maxPutsBeforeCommit;
    }

    @Min(0)
    @JsonPropertyDescription("The maximum number of entries in one reference stream to purge before the " +
            "transaction is committed. A value high enough to purge all entries in one transaction is " +
            "preferable but for large reference streams this may result in errors due to the transaction " +
            "being too large.")
    public int getMaxPurgeDeletesBeforeCommit() {
        return maxPurgeDeletesBeforeCommit;
    }

    @JsonPropertyDescription("The time to retain reference data for in the off heap store. The time is taken " +
            "from the time that the reference stream was last accessed, e.g. a lookup was made against it. " +
            "In ISO-8601 duration format, e.g. 'P1DT12H'. Used by job '" + RefDataPurge.JOB_NAME + "'.")
    public StroomDuration getPurgeAge() {
        return purgeAge;
    }

    @Min(2)
    @RequiresRestart(RestartScope.SYSTEM)
    @JsonPropertyDescription("The number of lock stripes used for preventing multiple pipeline processes " +
            "from loading the same reference stream at the same time. Values should be a power of 2. " +
            "Lower values will mean it is more likely for two different streams from blocking one another.")
    public int getLoadingLockStripes() {
        return loadingLockStripes;
    }

    @JsonProperty("lmdb")
    public ReferenceDataLmdbConfig getLmdbConfig() {
        return lmdbConfig;
    }

    public CacheConfig getEffectiveStreamCache() {
        return effectiveStreamCache;
    }

    public ReferenceDataConfig withLmdbConfig(final ReferenceDataLmdbConfig lmdbConfig) {
        return new ReferenceDataConfig(
                maxPutsBeforeCommit,
                maxPurgeDeletesBeforeCommit,
                purgeAge,
                loadingLockStripes,
                lmdbConfig,
                effectiveStreamCache);
    }

    public ReferenceDataConfig withPurgeAge(final StroomDuration purgeAge) {
        return new ReferenceDataConfig(
                maxPutsBeforeCommit,
                maxPurgeDeletesBeforeCommit,
                purgeAge,
                loadingLockStripes,
                lmdbConfig,
                effectiveStreamCache);
    }

    public ReferenceDataConfig withMaxPutsBeforeCommit(final int maxPutsBeforeCommit) {
        return new ReferenceDataConfig(
                maxPutsBeforeCommit,
                maxPurgeDeletesBeforeCommit,
                purgeAge,
                loadingLockStripes,
                lmdbConfig,
                effectiveStreamCache);
    }

    public ReferenceDataConfig withMaxPurgeDeletesBeforeCommit(final int maxPurgeDeletesBeforeCommit) {
        return new ReferenceDataConfig(
                maxPutsBeforeCommit,
                maxPurgeDeletesBeforeCommit,
                purgeAge,
                loadingLockStripes,
                lmdbConfig,
                effectiveStreamCache);
    }

    public ReferenceDataConfig withEffectiveStreamCache(final CacheConfig effectiveStreamCache) {
        return new ReferenceDataConfig(
                maxPutsBeforeCommit,
                maxPurgeDeletesBeforeCommit,
                purgeAge,
                loadingLockStripes,
                lmdbConfig,
                effectiveStreamCache);
    }

    @Override
    public String toString() {
        return "ReferenceDataConfig{" +
                "maxPutsBeforeCommit=" + maxPutsBeforeCommit +
                ", maxPurgeDeletesBeforeCommit=" + maxPurgeDeletesBeforeCommit +
                ", purgeAge=" + purgeAge +
                ", loadingLockStripes=" + loadingLockStripes +
                ", lmdbConfig=" + lmdbConfig +
                ", effectiveStreamCache=" + effectiveStreamCache +
                '}';
    }
}
