package stroom.pipeline.refdata;

import stroom.data.shared.StreamTypeNames;
import stroom.docref.DocRef;
import stroom.pipeline.refdata.RefDataStoreHolder.MapAvailability;
import stroom.pipeline.refdata.store.ProcessingState;
import stroom.pipeline.refdata.store.RefDataStore;
import stroom.pipeline.refdata.store.RefDataStoreFactory;
import stroom.pipeline.refdata.store.RefStreamDefinition;
import stroom.pipeline.shared.data.PipelineReference;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class TestRefDataStoreHolder {

    @Mock
    private RefDataStoreFactory mockRefDataStoreFactory;
    @Mock
    private RefDataStore mockRefDataStore;

    private PipelineReference pipelineReference;

    @BeforeEach
    void setUp() {
        pipelineReference = new PipelineReference(
                null,
                DocRef.builder()
                        .name("MY_FEED")
                        .build(),
                StreamTypeNames.REFERENCE);
    }

    @Test
    void testIsLookupNeeded_validMap() {

        Mockito.when(mockRefDataStore.getMapNames(Mockito.any()))
                .thenReturn(Set.of("foo", "bar"));
        Mockito.when(mockRefDataStore.getLoadState(Mockito.any()))
                .thenReturn(Optional.of(ProcessingState.COMPLETE));


        final RefDataStoreHolder refDataStoreHolder = new RefDataStoreHolder(mockRefDataStoreFactory);
        final RefStreamDefinition refStreamDefinition = new RefStreamDefinition(
                "pipeUUID", "pipeVer", 1L);

        final String mapName = "foo";
        MapAvailability mapAvailability = refDataStoreHolder.getMapAvailabilityInStream(
                pipelineReference, refStreamDefinition, mapName);

        // Always true at this point as it doesn't know either way
        Assertions.assertThat(mapAvailability)
                .isEqualTo(MapAvailability.UNKNOWN);
        Assertions.assertThat(mapAvailability.isLookupRequired())
                .isTrue();

        refDataStoreHolder.addKnownMapNames(mockRefDataStore, refStreamDefinition);

        mapAvailability = refDataStoreHolder.getMapAvailabilityInStream(
                pipelineReference, refStreamDefinition, mapName);

        // The map is there so true
        Assertions.assertThat(mapAvailability)
                .isEqualTo(MapAvailability.PRESENT);
        Assertions.assertThat(mapAvailability.isLookupRequired())
                .isTrue();
    }

    @Test
    void testIsLookupNeeded_invalidMap() {

        Mockito.when(mockRefDataStore.getMapNames(Mockito.any()))
                .thenReturn(Set.of("foo", "bar"));
        Mockito.when(mockRefDataStore.getLoadState(Mockito.any()))
                .thenReturn(Optional.of(ProcessingState.COMPLETE));

        final RefDataStoreHolder refDataStoreHolder = new RefDataStoreHolder(mockRefDataStoreFactory);
        final RefStreamDefinition refStreamDefinition = new RefStreamDefinition(
                "pipeUUID", "pipeVer", 1L);

        final String mapName = "UNKNOWN_MAP";

        MapAvailability mapAvailability = refDataStoreHolder.getMapAvailabilityInStream(
                pipelineReference, refStreamDefinition, mapName);

        // Always true at this point as it doesn't know either way
        Assertions.assertThat(mapAvailability)
                .isEqualTo(MapAvailability.UNKNOWN);
        Assertions.assertThat(mapAvailability.isLookupRequired())
                        .isTrue();

        refDataStoreHolder.addKnownMapNames(mockRefDataStore, refStreamDefinition);

        mapAvailability = refDataStoreHolder.getMapAvailabilityInStream(
                pipelineReference, refStreamDefinition, mapName);

        // The map is there so true
        Assertions.assertThat(mapAvailability)
                .isEqualTo(MapAvailability.NOT_PRESENT);
        Assertions.assertThat(mapAvailability.isLookupRequired())
                .isFalse();
    }

    @Test
    void testIsLookupNeeded_multiple() {

        final RefStreamDefinition refStreamDefinition1 = new RefStreamDefinition(
                "pipeUUID1", "pipeVer1", 1L);
        final RefStreamDefinition refStreamDefinition2 = new RefStreamDefinition(
                "pipeUUID2", "pipeVer2", 2L);

        Mockito.when(mockRefDataStore.getMapNames(Mockito.eq(refStreamDefinition1)))
                .thenReturn(Set.of("foo", "bar"));
        Mockito.when(mockRefDataStore.getMapNames(Mockito.eq(refStreamDefinition2)))
                .thenReturn(Set.of("bar"));
        Mockito.when(mockRefDataStore.getLoadState(Mockito.any()))
                .thenReturn(Optional.of(ProcessingState.COMPLETE));

        final RefDataStoreHolder refDataStoreHolder = new RefDataStoreHolder(mockRefDataStoreFactory);

        final String mapName = "foo";

        // Always true at this point as it doesn't know either way
        Assertions.assertThat(refDataStoreHolder.getMapAvailabilityInStream(
                pipelineReference, refStreamDefinition1, mapName))
                .isEqualTo(MapAvailability.UNKNOWN);
        Assertions.assertThat(refDataStoreHolder.getMapAvailabilityInStream(
                pipelineReference, refStreamDefinition2, mapName))
                .isEqualTo(MapAvailability.UNKNOWN);

        refDataStoreHolder.addKnownMapNames(mockRefDataStore, refStreamDefinition1);
        refDataStoreHolder.addKnownMapNames(mockRefDataStore, refStreamDefinition2);

        // The map is there
        Assertions.assertThat(refDataStoreHolder.getMapAvailabilityInStream(
                pipelineReference, refStreamDefinition1, mapName))
                .isEqualTo(MapAvailability.PRESENT);
        // The map is not there
        Assertions.assertThat(refDataStoreHolder.getMapAvailabilityInStream(
                pipelineReference, refStreamDefinition2, mapName))
                .isEqualTo(MapAvailability.NOT_PRESENT);
    }
}
