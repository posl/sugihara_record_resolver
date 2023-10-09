package org.asamk.signal.manager.helper;

import org.asamk.signal.manager.SignalDependencies;
import org.asamk.signal.manager.api.RecipientIdentifier;
import org.asamk.signal.manager.api.UnregisteredRecipientException;
import org.asamk.signal.manager.config.ServiceConfig;
import org.asamk.signal.manager.config.ServiceEnvironmentConfig;
import org.asamk.signal.manager.storage.SignalAccount;
import org.asamk.signal.manager.storage.recipients.RecipientId;
import org.signal.libsignal.protocol.InvalidKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.signalservice.api.push.ACI;
import org.whispersystems.signalservice.api.push.ServiceId;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.services.CdsiV2Service;
import org.whispersystems.signalservice.internal.contacts.crypto.Quote;
import org.whispersystems.signalservice.internal.contacts.crypto.UnauthenticatedQuoteException;
import org.whispersystems.signalservice.internal.contacts.crypto.UnauthenticatedResponseException;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RecipientHelper {

    private final static Logger logger = LoggerFactory.getLogger(RecipientHelper.class);

    private final SignalAccount account;
    private final SignalDependencies dependencies;
    private final ServiceEnvironmentConfig serviceEnvironmentConfig;

    public RecipientHelper(final Context context) {
        this.account = context.getAccount();
        this.dependencies = context.getDependencies();
        this.serviceEnvironmentConfig = dependencies.getServiceEnvironmentConfig();
    }

    public SignalServiceAddress resolveSignalServiceAddress(RecipientId recipientId) {
        final var address = account.getRecipientAddressResolver().resolveRecipientAddress(recipientId);
        if (address.number().isEmpty() || address.serviceId().isPresent()) {
            return address.toSignalServiceAddress();
        }

        // Address in recipient store doesn't have a uuid, this shouldn't happen
        // Try to retrieve the uuid from the server
        final var number = address.number().get();
        final ACI aci;
        try {
            aci = getRegisteredUser(number);
        } catch (UnregisteredRecipientException e) {
            logger.warn("Failed to get uuid for e164 number: {}", number);
            // Return SignalServiceAddress with unknown UUID
            return address.toSignalServiceAddress();
        } catch (IOException e) {
            logger.warn("Failed to get uuid for e164 number: {}", number, e);
            // Return SignalServiceAddress with unknown UUID
            return address.toSignalServiceAddress();
        }
        return account.getRecipientAddressResolver()
                .resolveRecipientAddress(account.getRecipientResolver().resolveRecipient(aci))
                .toSignalServiceAddress();
    }

    public RecipientId resolveRecipient(final SignalServiceAddress address) {
        return account.getRecipientResolver().resolveRecipient(address);
    }

    public Set<RecipientId> resolveRecipients(Collection<RecipientIdentifier.Single> recipients) throws UnregisteredRecipientException {
        final var recipientIds = new HashSet<RecipientId>(recipients.size());
        for (var number : recipients) {
            final var recipientId = resolveRecipient(number);
            recipientIds.add(recipientId);
        }
        return recipientIds;
    }

    public RecipientId resolveRecipient(final RecipientIdentifier.Single recipient) throws UnregisteredRecipientException {
        if (recipient instanceof RecipientIdentifier.Uuid uuidRecipient) {
            return account.getRecipientResolver().resolveRecipient(ServiceId.from(uuidRecipient.uuid()));
        } else {
            final var number = ((RecipientIdentifier.Number) recipient).number();
            return account.getRecipientStore().resolveRecipient(number, () -> {
                try {
                    return getRegisteredUser(number);
                } catch (Exception e) {
                    return null;
                }
            });
        }
    }

    public RecipientId refreshRegisteredUser(RecipientId recipientId) throws IOException, UnregisteredRecipientException {
        final var address = resolveSignalServiceAddress(recipientId);
        if (address.getNumber().isEmpty()) {
            return recipientId;
        }
        final var number = address.getNumber().get();
        final var uuid = getRegisteredUser(number);
        return account.getRecipientTrustedResolver().resolveRecipientTrusted(new SignalServiceAddress(uuid, number));
    }

    public Map<String, ACI> getRegisteredUsers(final Set<String> numbers) throws IOException {
        Map<String, ACI> registeredUsers;
        try {
            registeredUsers = getRegisteredUsersV2(numbers, true);
        } catch (IOException e) {
            logger.warn("CDSI request failed, trying fallback to CDS", e);
            registeredUsers = getRegisteredUsersV1(numbers);
        }

        // Store numbers as recipients, so we have the number/uuid association
        registeredUsers.forEach((number, aci) -> account.getRecipientTrustedResolver()
                .resolveRecipientTrusted(new SignalServiceAddress(aci, number)));

        return registeredUsers;
    }

    private ACI getRegisteredUser(final String number) throws IOException, UnregisteredRecipientException {
        final Map<String, ACI> aciMap;
        try {
            aciMap = getRegisteredUsers(Set.of(number));
        } catch (NumberFormatException e) {
            throw new UnregisteredRecipientException(new org.asamk.signal.manager.api.RecipientAddress(null, number));
        }
        final var uuid = aciMap.get(number);
        if (uuid == null) {
            throw new UnregisteredRecipientException(new org.asamk.signal.manager.api.RecipientAddress(null, number));
        }
        return uuid;
    }

    private Map<String, ACI> getRegisteredUsersV1(final Set<String> numbers) throws IOException {
        final Map<String, ACI> registeredUsers;
        try {
            registeredUsers = dependencies.getAccountManager()
                    .getRegisteredUsers(ServiceConfig.getIasKeyStore(),
                            numbers,
                            serviceEnvironmentConfig.getCdsMrenclave());
        } catch (Quote.InvalidQuoteFormatException | UnauthenticatedQuoteException | SignatureException |
                 UnauthenticatedResponseException | InvalidKeyException | NumberFormatException e) {
            throw new IOException(e);
        }
        return registeredUsers;
    }

    private Map<String, ACI> getRegisteredUsersV2(final Set<String> numbers, boolean useCompat) throws IOException {
        // Only partial refresh is implemented here
        final CdsiV2Service.Response response;
        try {
            response = dependencies.getAccountManager()
                    .getRegisteredUsersWithCdsi(Set.of(),
                            numbers,
                            account.getRecipientStore().getServiceIdToProfileKeyMap(),
                            useCompat,
                            Optional.empty(),
                            serviceEnvironmentConfig.getCdsiMrenclave(),
                            token -> {
                                // Not storing for partial refresh
                            });
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
        logger.debug("CDSI request successful, quota used by this request: {}", response.getQuotaUsedDebugOnly());

        final var registeredUsers = new HashMap<String, ACI>();
        response.getResults().forEach((key, value) -> {
            if (value.getAci().isPresent()) {
                registeredUsers.put(key, value.getAci().get());
            }
        });
        return registeredUsers;
    }

    private ACI getRegisteredUserByUsername(String username) throws IOException {
        return dependencies.getAccountManager().getAciByUsername(username);
    }
}
