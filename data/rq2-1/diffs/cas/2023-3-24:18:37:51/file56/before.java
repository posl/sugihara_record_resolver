package org.apereo.cas.couchdb.consent;

import org.apereo.cas.consent.ConsentDecision;
import org.apereo.cas.consent.ConsentReminderOptions;
import org.apereo.cas.util.RandomUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * This is {@link CouchDbConsentDecision}.
 *
 * @author Timur Duehr
 * @since 6.0.0
 * @deprecated Since 7
 */
@Getter
@Setter
@Deprecated(since = "7.0.0")
public class CouchDbConsentDecision extends ConsentDecision {
    @Serial
    private static final long serialVersionUID = -685282558102325489L;

    @JsonProperty("_rev")
    private String rev;

    @JsonProperty("_id")
    private String cid;

    @JsonCreator
    public CouchDbConsentDecision(@JsonProperty("_id") final String cid,
                                  @JsonProperty("_rev") final String rev,
                                  @JsonProperty("id") final long id,
                                  @JsonProperty("principal") final String principal,
                                  @JsonProperty("service") final String service,
                                  @JsonProperty("createdDate") final LocalDateTime createdDate,
                                  @JsonProperty("options") final ConsentReminderOptions options,
                                  @JsonProperty("reminder") final long reminder,
                                  @JsonProperty("reminderTimeUnit") final ChronoUnit reminderTimeUnit,
                                  @JsonProperty("attributes") final String attributes) {
        this.cid = cid;
        this.rev = rev;
        setId(id);
        setPrincipal(principal);
        setService(service);
        setCreatedDate(createdDate);
        setOptions(options);
        setReminder(reminder);
        setReminderTimeUnit(reminderTimeUnit);
        setAttributes(attributes);
    }

    /**
     * Copy constructor.
     *
     * @param c Consent decision to copy from.
     */
    public CouchDbConsentDecision(final ConsentDecision c) {
        setAttributes(c.getAttributes());
        setPrincipal(c.getPrincipal());
        setCreatedDate(c.getCreatedDate());
        setId(c.getId());
        setOptions(c.getOptions());
        setReminder(c.getReminder());
        setReminderTimeUnit(c.getReminderTimeUnit());
        setService(c.getService());
        if (getId() < 0) {
            setId(RandomUtils.nextLong());
        }
    }

    /**
     * Copy consent details to this instance.
     *
     * @param other decision to copy details from.
     * @return CouchDb capable consent decision.
     */
    @CanIgnoreReturnValue
    public CouchDbConsentDecision copyDetailsFrom(final ConsentDecision other) {
        setAttributes(other.getAttributes());
        setPrincipal(other.getPrincipal());
        setCreatedDate(other.getCreatedDate());
        setId(other.getId());
        setOptions(other.getOptions());
        setReminder(other.getReminder());
        setReminderTimeUnit(other.getReminderTimeUnit());
        setService(other.getService());
        return this;
    }
}
