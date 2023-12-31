package org.opendatadiscovery.oddplatform.dto.alert;

import java.util.Collection;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.opendatadiscovery.oddplatform.model.tables.pojos.AlertChunkPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.AlertPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DataEntityPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.OwnerPojo;

@Data
@RequiredArgsConstructor
public class AlertDto {
    private final AlertPojo alert;
    private final Collection<AlertChunkPojo> chunks;
    private final DataEntityPojo dataEntity;
    private final OwnerPojo updatedByOwner;
}
