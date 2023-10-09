package stroom.docrefinfo.api;

import stroom.docref.DocRef;
import stroom.docref.DocRefInfo;

import java.util.List;
import java.util.Optional;

public interface DocRefInfoService {

    /**
     * @return A list of all known and readable docRefs for the given type.
     */
    List<DocRef> findByType(final String type);

    Optional<DocRefInfo> info(DocRef docRef);

    Optional<String> name(DocRef docRef);

    /**
     * Find by case-sensitive match on the name.
     * If allowWildCards is true '*' can be used to denote a 0-many char wild card.
     * Names may not be unique for a given type, so a non-wild carded nameFilter may return
     * more than one {@link DocRef}.
     * @param type The {@link DocRef} type. Mandatory.
     * @param nameFilter The name of the {@link DocRef}s to filter by. If allowWildCards is true
     *             find all matching else find those with an exact case-sensitive name match.
     */
    List<DocRef> findByName(final String type,
                            final String nameFilter,
                            final boolean allowWildCards);

    /**
     * Find by case-sensitive match on the name.
     * If allowWildCards is true '*' can be used to denote a 0-many char wild card.
     * Names may not be unique for a given type, so a non-wild carded nameFilter may return
     * more than one {@link DocRef}. Applies all nameFilters using an OR, i.e. returns all docRefs
     * associated with any of the passed nameFilters.
     * @param type The {@link DocRef} type. Mandatory.
     * @param nameFilters The names of the {@link DocRef}s to filter by. If allowWildCards is true
     *             find all matching else find those with an exact case-sensitive name match.
     */
    List<DocRef> findByNames(final String type,
                             final List<String> nameFilters,
                             final boolean allowWildCards);
}
