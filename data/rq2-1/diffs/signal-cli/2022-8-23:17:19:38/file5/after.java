package org.asamk.signal.manager.helper;

import org.asamk.signal.manager.Manager;
import org.asamk.signal.manager.SignalDependencies;
import org.asamk.signal.manager.actions.HandleAction;
import org.asamk.signal.manager.actions.RefreshPreKeysAction;
import org.asamk.signal.manager.actions.RenewSessionAction;
import org.asamk.signal.manager.actions.ResendMessageAction;
import org.asamk.signal.manager.actions.RetrieveProfileAction;
import org.asamk.signal.manager.actions.RetrieveStorageDataAction;
import org.asamk.signal.manager.actions.SendGroupInfoAction;
import org.asamk.signal.manager.actions.SendGroupInfoRequestAction;
import org.asamk.signal.manager.actions.SendPniIdentityKeyAction;
import org.asamk.signal.manager.actions.SendProfileKeyAction;
import org.asamk.signal.manager.actions.SendReceiptAction;
import org.asamk.signal.manager.actions.SendRetryMessageRequestAction;
import org.asamk.signal.manager.actions.SendSyncBlockedListAction;
import org.asamk.signal.manager.actions.SendSyncConfigurationAction;
import org.asamk.signal.manager.actions.SendSyncContactsAction;
import org.asamk.signal.manager.actions.SendSyncGroupsAction;
import org.asamk.signal.manager.actions.SendSyncKeysAction;
import org.asamk.signal.manager.actions.UpdateAccountAttributesAction;
import org.asamk.signal.manager.api.MessageEnvelope;
import org.asamk.signal.manager.api.Pair;
import org.asamk.signal.manager.api.ReceiveConfig;
import org.asamk.signal.manager.api.StickerPackId;
import org.asamk.signal.manager.api.TrustLevel;
import org.asamk.signal.manager.api.UntrustedIdentityException;
import org.asamk.signal.manager.groups.GroupId;
import org.asamk.signal.manager.groups.GroupNotFoundException;
import org.asamk.signal.manager.groups.GroupUtils;
import org.asamk.signal.manager.jobs.RetrieveStickerPackJob;
import org.asamk.signal.manager.storage.SignalAccount;
import org.asamk.signal.manager.storage.groups.GroupInfoV1;
import org.asamk.signal.manager.storage.recipients.Profile;
import org.asamk.signal.manager.storage.recipients.RecipientId;
import org.asamk.signal.manager.storage.stickers.StickerPack;
import org.asamk.signal.manager.util.KeyUtils;
import org.signal.libsignal.metadata.ProtocolInvalidKeyException;
import org.signal.libsignal.metadata.ProtocolInvalidKeyIdException;
import org.signal.libsignal.metadata.ProtocolInvalidMessageException;
import org.signal.libsignal.metadata.ProtocolNoSessionException;
import org.signal.libsignal.metadata.ProtocolUntrustedIdentityException;
import org.signal.libsignal.metadata.SelfSendException;
import org.signal.libsignal.protocol.message.DecryptionErrorMessage;
import org.signal.libsignal.zkgroup.InvalidInputException;
import org.signal.libsignal.zkgroup.profiles.ProfileKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.signalservice.api.messages.SignalServiceContent;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.api.messages.SignalServiceGroupV2;
import org.whispersystems.signalservice.api.messages.SignalServiceReceiptMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceStoryMessage;
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage;
import org.whispersystems.signalservice.api.messages.multidevice.StickerPackOperationMessage;
import org.whispersystems.signalservice.api.push.ServiceId;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class IncomingMessageHandler {

    private final static Logger logger = LoggerFactory.getLogger(IncomingMessageHandler.class);

    private final SignalAccount account;
    private final SignalDependencies dependencies;
    private final Context context;

    public IncomingMessageHandler(final Context context) {
        this.account = context.getAccount();
        this.dependencies = context.getDependencies();
        this.context = context;
    }

    public Pair<List<HandleAction>, Exception> handleRetryEnvelope(
            final SignalServiceEnvelope envelope,
            final ReceiveConfig receiveConfig,
            final Manager.ReceiveMessageHandler handler
    ) {
        final List<HandleAction> actions = new ArrayList<>();
        if (envelope.isPreKeySignalMessage()) {
            actions.add(RefreshPreKeysAction.create());
        }

        SignalServiceContent content = null;
        if (!envelope.isReceipt()) {
            account.getIdentityKeyStore().setRetryingDecryption(true);
            try {
                content = dependencies.getCipher().decrypt(envelope);
            } catch (ProtocolUntrustedIdentityException e) {
                final var recipientId = account.getRecipientResolver().resolveRecipient(e.getSender());
                final var exception = new UntrustedIdentityException(account.getRecipientAddressResolver()
                        .resolveRecipientAddress(recipientId), e.getSenderDevice());
                return new Pair<>(List.of(), exception);
            } catch (Exception e) {
                return new Pair<>(List.of(), e);
            } finally {
                account.getIdentityKeyStore().setRetryingDecryption(false);
            }
        }
        actions.addAll(checkAndHandleMessage(envelope, content, receiveConfig, handler, null));
        return new Pair<>(actions, null);
    }

    public Pair<List<HandleAction>, Exception> handleEnvelope(
            final SignalServiceEnvelope envelope,
            final ReceiveConfig receiveConfig,
            final Manager.ReceiveMessageHandler handler
    ) {
        final var actions = new ArrayList<HandleAction>();
        if (envelope.hasSourceUuid()) {
            // Store uuid if we don't have it already
            // address/uuid in envelope is sent by server
            account.getRecipientTrustedResolver().resolveRecipientTrusted(envelope.getSourceAddress());
        }
        SignalServiceContent content = null;
        Exception exception = null;
        if (!envelope.isReceipt()) {
            try {
                content = dependencies.getCipher().decrypt(envelope);
            } catch (ProtocolUntrustedIdentityException e) {
                final var recipientId = account.getRecipientResolver().resolveRecipient(e.getSender());
                actions.add(new RetrieveProfileAction(recipientId));
                exception = new UntrustedIdentityException(account.getRecipientAddressResolver()
                        .resolveRecipientAddress(recipientId), e.getSenderDevice());
            } catch (ProtocolInvalidKeyIdException | ProtocolInvalidKeyException | ProtocolNoSessionException |
                     ProtocolInvalidMessageException e) {
                logger.debug("Failed to decrypt incoming message", e);
                final var sender = account.getRecipientResolver().resolveRecipient(e.getSender());
                if (context.getContactHelper().isContactBlocked(sender)) {
                    logger.debug("Received invalid message from blocked contact, ignoring.");
                } else {
                    final var senderProfile = context.getProfileHelper().getRecipientProfile(sender);
                    final var selfProfile = context.getProfileHelper().getSelfProfile();
                    final var serviceId = ServiceId.parseOrThrow(e.getSender());
                    if ((!sender.equals(account.getSelfRecipientId()) || e.getSenderDevice() != account.getDeviceId())
                            && senderProfile != null
                            && senderProfile.getCapabilities().contains(Profile.Capability.senderKey)
                            && selfProfile != null
                            && selfProfile.getCapabilities().contains(Profile.Capability.senderKey)) {
                        logger.debug("Received invalid message, requesting message resend.");
                        actions.add(new SendRetryMessageRequestAction(sender, serviceId, e, envelope));
                    } else {
                        logger.debug("Received invalid message, queuing renew session action.");
                        actions.add(new RenewSessionAction(sender, serviceId));
                    }
                }
                exception = e;
            } catch (SelfSendException e) {
                logger.debug("Dropping unidentified message from self.");
                return new Pair<>(List.of(), null);
            } catch (Exception e) {
                logger.debug("Failed to handle incoming message", e);
                exception = e;
            }
        }

        actions.addAll(checkAndHandleMessage(envelope, content, receiveConfig, handler, exception));
        return new Pair<>(actions, exception);
    }

    private List<HandleAction> checkAndHandleMessage(
            final SignalServiceEnvelope envelope,
            final SignalServiceContent content,
            final ReceiveConfig receiveConfig,
            final Manager.ReceiveMessageHandler handler,
            final Exception exception
    ) {
        if (!envelope.hasSourceUuid() && content != null) {
            // Store uuid if we don't have it already
            // address/uuid is validated by unidentified sender certificate
            account.getRecipientTrustedResolver().resolveRecipientTrusted(content.getSender());
        }
        if (envelope.isReceipt()) {
            final var senderDeviceAddress = getSender(envelope, content);
            final var sender = senderDeviceAddress.serviceId();
            final var senderDeviceId = senderDeviceAddress.deviceId();
            account.getMessageSendLogStore().deleteEntryForRecipient(envelope.getTimestamp(), sender, senderDeviceId);
        }

        if (isMessageBlocked(envelope, content)) {
            logger.info("Ignoring a message from blocked user/group: {}", envelope.getTimestamp());
            return List.of();
        } else if (isNotAllowedToSendToGroup(envelope, content)) {
            logger.info("Ignoring a group message from an unauthorized sender (no member or admin): {} {}",
                    (envelope.hasSourceUuid() ? envelope.getSourceAddress() : content.getSender()).getIdentifier(),
                    envelope.getTimestamp());
            return List.of();
        } else {
            List<HandleAction> actions;
            if (content != null) {
                actions = handleMessage(envelope, content, receiveConfig);
            } else {
                actions = List.of();
            }
            handler.handleMessage(MessageEnvelope.from(envelope,
                    content,
                    account.getRecipientResolver(),
                    account.getRecipientAddressResolver(),
                    context.getAttachmentHelper()::getAttachmentFile,
                    exception), exception);
            return actions;
        }
    }

    public List<HandleAction> handleMessage(
            SignalServiceEnvelope envelope, SignalServiceContent content, ReceiveConfig receiveConfig
    ) {
        var actions = new ArrayList<HandleAction>();
        final var senderDeviceAddress = getSender(envelope, content);
        final var sender = senderDeviceAddress.recipientId();
        final var senderServiceId = senderDeviceAddress.serviceId();
        final var senderDeviceId = senderDeviceAddress.deviceId();
        final var destination = getDestination(envelope);

        if (content.getReceiptMessage().isPresent()) {
            final var message = content.getReceiptMessage().get();
            if (message.isDeliveryReceipt()) {
                account.getMessageSendLogStore()
                        .deleteEntriesForRecipient(message.getTimestamps(), senderServiceId, senderDeviceId);
            }
        }

        if (content.getSenderKeyDistributionMessage().isPresent()) {
            final var message = content.getSenderKeyDistributionMessage().get();
            final var protocolAddress = senderServiceId.toProtocolAddress(senderDeviceId);
            logger.debug("Received a sender key distribution message for distributionId {} from {}",
                    message.getDistributionId(),
                    protocolAddress);
            dependencies.getMessageSender().processSenderKeyDistributionMessage(protocolAddress, message);
        }

        if (content.getDecryptionErrorMessage().isPresent()) {
            var message = content.getDecryptionErrorMessage().get();
            logger.debug("Received a decryption error message from {}.{} (resend request for {})",
                    sender,
                    senderDeviceId,
                    message.getTimestamp());
            if (message.getDeviceId() == account.getDeviceId()) {
                handleDecryptionErrorMessage(actions, sender, senderServiceId, senderDeviceId, message);
            } else {
                logger.debug("Request is for another one of our devices");
            }
        }

        if (content.getDataMessage().isPresent()) {
            var message = content.getDataMessage().get();

            if (content.isNeedsReceipt()) {
                actions.add(new SendReceiptAction(sender,
                        SignalServiceReceiptMessage.Type.DELIVERY,
                        message.getTimestamp()));
            } else {
                // Message wasn't sent as unidentified sender message
                final var contact = context.getAccount().getContactStore().getContact(sender);
                if (account.isPrimaryDevice()
                        && contact != null
                        && !contact.isBlocked()
                        && contact.isProfileSharingEnabled()) {
                    actions.add(UpdateAccountAttributesAction.create());
                    actions.add(new SendProfileKeyAction(sender));
                }
            }
            if (receiveConfig.sendReadReceipts()) {
                actions.add(new SendReceiptAction(sender,
                        SignalServiceReceiptMessage.Type.READ,
                        message.getTimestamp()));
            }

            actions.addAll(handleSignalServiceDataMessage(message,
                    false,
                    senderDeviceAddress,
                    destination,
                    receiveConfig.ignoreAttachments()));
        }

        if (content.getStoryMessage().isPresent()) {
            final var message = content.getStoryMessage().get();
            actions.addAll(handleSignalServiceStoryMessage(message, sender, receiveConfig.ignoreAttachments()));
        }

        if (content.getSyncMessage().isPresent()) {
            var syncMessage = content.getSyncMessage().get();
            actions.addAll(handleSyncMessage(syncMessage, senderDeviceAddress, receiveConfig.ignoreAttachments()));
        }

        return actions;
    }

    private void handleDecryptionErrorMessage(
            final List<HandleAction> actions,
            final RecipientId sender,
            final ServiceId senderServiceId,
            final int senderDeviceId,
            final DecryptionErrorMessage message
    ) {
        final var logEntries = account.getMessageSendLogStore()
                .findMessages(senderServiceId,
                        senderDeviceId,
                        message.getTimestamp(),
                        message.getRatchetKey().isEmpty());

        for (final var logEntry : logEntries) {
            actions.add(new ResendMessageAction(sender, message.getTimestamp(), logEntry));
        }

        if (message.getRatchetKey().isPresent()) {
            if (account.getAciSessionStore()
                    .isCurrentRatchetKey(senderServiceId, senderDeviceId, message.getRatchetKey().get())) {
                if (logEntries.isEmpty()) {
                    logger.debug("Renewing the session with sender");
                    actions.add(new RenewSessionAction(sender, senderServiceId));
                } else {
                    logger.trace("Archiving the session with sender, a resend message has already been queued");
                    context.getAccount().getAciSessionStore().archiveSessions(senderServiceId);
                }
            }
            return;
        }

        var found = false;
        for (final var logEntry : logEntries) {
            if (logEntry.groupId().isEmpty()) {
                continue;
            }
            final var group = account.getGroupStore().getGroup(logEntry.groupId().get());
            if (group == null) {
                continue;
            }
            found = true;
            logger.trace("Deleting shared sender key with {} ({}): {}",
                    sender,
                    senderDeviceId,
                    group.getDistributionId());
            account.getSenderKeyStore().deleteSharedWith(senderServiceId, senderDeviceId, group.getDistributionId());
        }
        if (!found) {
            logger.debug("Reset all shared sender keys with this recipient, no related message found in send log");
            account.getSenderKeyStore().deleteSharedWith(senderServiceId);
        }
    }

    private List<HandleAction> handleSyncMessage(
            final SignalServiceSyncMessage syncMessage, final DeviceAddress sender, final boolean ignoreAttachments
    ) {
        var actions = new ArrayList<HandleAction>();
        account.setMultiDevice(true);
        if (syncMessage.getSent().isPresent()) {
            var message = syncMessage.getSent().get();
            final var destination = message.getDestination().orElse(null);
            if (message.getDataMessage().isPresent()) {
                actions.addAll(handleSignalServiceDataMessage(message.getDataMessage().get(),
                        true,
                        sender,
                        destination == null
                                ? null
                                : new DeviceAddress(context.getRecipientHelper().resolveRecipient(destination),
                                        destination.getServiceId(),
                                        0),
                        ignoreAttachments));
            }
            if (message.getStoryMessage().isPresent()) {
                actions.addAll(handleSignalServiceStoryMessage(message.getStoryMessage().get(),
                        sender.recipientId(),
                        ignoreAttachments));
            }
        }
        if (syncMessage.getRequest().isPresent() && account.isPrimaryDevice()) {
            var rm = syncMessage.getRequest().get();
            if (rm.isContactsRequest()) {
                actions.add(SendSyncContactsAction.create());
            }
            if (rm.isGroupsRequest()) {
                actions.add(SendSyncGroupsAction.create());
            }
            if (rm.isBlockedListRequest()) {
                actions.add(SendSyncBlockedListAction.create());
            }
            if (rm.isKeysRequest()) {
                actions.add(SendSyncKeysAction.create());
            }
            if (rm.isConfigurationRequest()) {
                actions.add(SendSyncConfigurationAction.create());
            }
            if (rm.isPniIdentityRequest()) {
                actions.add(SendPniIdentityKeyAction.create());
            }
        }
        if (syncMessage.getGroups().isPresent()) {
            try {
                final var groupsMessage = syncMessage.getGroups().get();
                context.getAttachmentHelper()
                        .retrieveAttachment(groupsMessage, context.getSyncHelper()::handleSyncDeviceGroups);
            } catch (Exception e) {
                logger.warn("Failed to handle received sync groups, ignoring: {}", e.getMessage());
            }
        }
        if (syncMessage.getBlockedList().isPresent()) {
            final var blockedListMessage = syncMessage.getBlockedList().get();
            for (var address : blockedListMessage.getAddresses()) {
                context.getContactHelper()
                        .setContactBlocked(context.getRecipientHelper().resolveRecipient(address), true);
            }
            for (var groupId : blockedListMessage.getGroupIds()
                    .stream()
                    .map(GroupId::unknownVersion)
                    .collect(Collectors.toSet())) {
                try {
                    context.getGroupHelper().setGroupBlocked(groupId, true);
                } catch (GroupNotFoundException e) {
                    logger.warn("BlockedListMessage contained groupID that was not found in GroupStore: {}",
                            groupId.toBase64());
                }
            }
        }
        if (syncMessage.getContacts().isPresent()) {
            try {
                final var contactsMessage = syncMessage.getContacts().get();
                context.getAttachmentHelper()
                        .retrieveAttachment(contactsMessage.getContactsStream(),
                                context.getSyncHelper()::handleSyncDeviceContacts);
            } catch (Exception e) {
                logger.warn("Failed to handle received sync contacts, ignoring: {}", e.getMessage());
            }
        }
        if (syncMessage.getVerified().isPresent()) {
            final var verifiedMessage = syncMessage.getVerified().get();
            account.getIdentityKeyStore()
                    .setIdentityTrustLevel(verifiedMessage.getDestination().getServiceId(),
                            verifiedMessage.getIdentityKey(),
                            TrustLevel.fromVerifiedState(verifiedMessage.getVerified()));
        }
        if (syncMessage.getStickerPackOperations().isPresent()) {
            final var stickerPackOperationMessages = syncMessage.getStickerPackOperations().get();
            for (var m : stickerPackOperationMessages) {
                if (m.getPackId().isEmpty()) {
                    continue;
                }
                final var stickerPackId = StickerPackId.deserialize(m.getPackId().get());
                final var installed = m.getType().isEmpty()
                        || m.getType().get() == StickerPackOperationMessage.Type.INSTALL;

                var sticker = account.getStickerStore().getStickerPack(stickerPackId);
                if (m.getPackKey().isPresent()) {
                    if (sticker == null) {
                        sticker = new StickerPack(-1, stickerPackId, m.getPackKey().get(), installed);
                        account.getStickerStore().addStickerPack(sticker);
                    }
                    if (installed) {
                        context.getJobExecutor()
                                .enqueueJob(new RetrieveStickerPackJob(stickerPackId, m.getPackKey().get()));
                    }
                }

                if (sticker != null && sticker.isInstalled() != installed) {
                    account.getStickerStore().updateStickerPackInstalled(sticker.packId(), installed);
                }
            }
        }
        if (syncMessage.getFetchType().isPresent()) {
            switch (syncMessage.getFetchType().get()) {
                case LOCAL_PROFILE:
                    actions.add(new RetrieveProfileAction(account.getSelfRecipientId()));
                case STORAGE_MANIFEST:
                    actions.add(RetrieveStorageDataAction.create());
            }
        }
        if (syncMessage.getKeys().isPresent()) {
            final var keysMessage = syncMessage.getKeys().get();
            if (keysMessage.getStorageService().isPresent()) {
                final var storageKey = keysMessage.getStorageService().get();
                account.setStorageKey(storageKey);
                actions.add(RetrieveStorageDataAction.create());
            }
        }
        if (syncMessage.getConfiguration().isPresent()) {
            final var configurationMessage = syncMessage.getConfiguration().get();
            final var configurationStore = account.getConfigurationStore();
            if (configurationMessage.getReadReceipts().isPresent()) {
                configurationStore.setReadReceipts(configurationMessage.getReadReceipts().get());
            }
            if (configurationMessage.getLinkPreviews().isPresent()) {
                configurationStore.setLinkPreviews(configurationMessage.getLinkPreviews().get());
            }
            if (configurationMessage.getTypingIndicators().isPresent()) {
                configurationStore.setTypingIndicators(configurationMessage.getTypingIndicators().get());
            }
            if (configurationMessage.getUnidentifiedDeliveryIndicators().isPresent()) {
                configurationStore.setUnidentifiedDeliveryIndicators(configurationMessage.getUnidentifiedDeliveryIndicators()
                        .get());
            }
        }
        if (syncMessage.getPniIdentity().isPresent()) {
            final var pniIdentity = syncMessage.getPniIdentity().get();
            account.setPniIdentityKeyPair(KeyUtils.getIdentityKeyPair(pniIdentity.getPublicKey().toByteArray(),
                    pniIdentity.getPrivateKey().toByteArray()));
            actions.add(RefreshPreKeysAction.create());
        }
        // TODO handle PniChangeNumber
        return actions;
    }

    private boolean isMessageBlocked(SignalServiceEnvelope envelope, SignalServiceContent content) {
        SignalServiceAddress source;
        if (!envelope.isUnidentifiedSender() && envelope.hasSourceUuid()) {
            source = envelope.getSourceAddress();
        } else if (content != null) {
            source = content.getSender();
        } else {
            return false;
        }
        final var recipientId = context.getRecipientHelper().resolveRecipient(source);
        if (context.getContactHelper().isContactBlocked(recipientId)) {
            return true;
        }

        if (content != null && content.getDataMessage().isPresent()) {
            var message = content.getDataMessage().get();
            if (message.getGroupContext().isPresent()) {
                var groupId = GroupUtils.getGroupId(message.getGroupContext().get());
                return context.getGroupHelper().isGroupBlocked(groupId);
            }
        }

        return false;
    }

    private boolean isNotAllowedToSendToGroup(SignalServiceEnvelope envelope, SignalServiceContent content) {
        SignalServiceAddress source;
        if (!envelope.isUnidentifiedSender() && envelope.hasSourceUuid()) {
            source = envelope.getSourceAddress();
        } else if (content != null) {
            source = content.getSender();
        } else {
            return false;
        }

        if (content == null || content.getDataMessage().isEmpty()) {
            return false;
        }

        var message = content.getDataMessage().get();
        if (message.getGroupContext().isEmpty()) {
            return false;
        }

        if (message.getGroupContext().get().getGroupV1().isPresent()) {
            var groupInfo = message.getGroupContext().get().getGroupV1().get();
            if (groupInfo.getType() == SignalServiceGroup.Type.QUIT) {
                return false;
            }
        }

        var groupId = GroupUtils.getGroupId(message.getGroupContext().get());
        var group = context.getGroupHelper().getGroup(groupId);
        if (group == null) {
            return false;
        }

        final var recipientId = context.getRecipientHelper().resolveRecipient(source);
        if (!group.isMember(recipientId) && !(group.isPendingMember(recipientId) && message.isGroupV2Update())) {
            return true;
        }

        if (group.isAnnouncementGroup() && !group.isAdmin(recipientId)) {
            return message.getBody().isPresent()
                    || message.getAttachments().isPresent()
                    || message.getQuote()
                    .isPresent()
                    || message.getPreviews().isPresent()
                    || message.getMentions().isPresent()
                    || message.getSticker().isPresent();
        }
        return false;
    }

    private List<HandleAction> handleSignalServiceDataMessage(
            SignalServiceDataMessage message,
            boolean isSync,
            DeviceAddress source,
            DeviceAddress destination,
            boolean ignoreAttachments
    ) {
        var actions = new ArrayList<HandleAction>();
        if (message.getGroupContext().isPresent()) {
            final var groupContext = message.getGroupContext().get();
            if (groupContext.getGroupV1().isPresent()) {
                var groupInfo = groupContext.getGroupV1().get();
                var groupId = GroupId.v1(groupInfo.getGroupId());
                var group = context.getGroupHelper().getGroup(groupId);
                if (group == null || group instanceof GroupInfoV1) {
                    var groupV1 = (GroupInfoV1) group;
                    switch (groupInfo.getType()) {
                        case UPDATE: {
                            if (groupV1 == null) {
                                groupV1 = new GroupInfoV1(groupId);
                            }

                            if (groupInfo.getAvatar().isPresent()) {
                                var avatar = groupInfo.getAvatar().get();
                                context.getGroupHelper().downloadGroupAvatar(groupV1.getGroupId(), avatar);
                            }

                            if (groupInfo.getName().isPresent()) {
                                groupV1.name = groupInfo.getName().get();
                            }

                            if (groupInfo.getMembers().isPresent()) {
                                groupV1.addMembers(groupInfo.getMembers()
                                        .get()
                                        .stream()
                                        .map(context.getRecipientHelper()::resolveRecipient)
                                        .collect(Collectors.toSet()));
                            }

                            account.getGroupStore().updateGroup(groupV1);
                            break;
                        }
                        case DELIVER:
                            if (groupV1 == null && !isSync) {
                                actions.add(new SendGroupInfoRequestAction(source.recipientId(), groupId));
                            }
                            break;
                        case QUIT: {
                            if (groupV1 != null) {
                                groupV1.removeMember(source.recipientId());
                                account.getGroupStore().updateGroup(groupV1);
                            }
                            break;
                        }
                        case REQUEST_INFO:
                            if (groupV1 != null && !isSync) {
                                actions.add(new SendGroupInfoAction(source.recipientId(), groupV1.getGroupId()));
                            }
                            break;
                    }
                } else {
                    // Received a group v1 message for a v2 group
                }
            }
            if (groupContext.getGroupV2().isPresent()) {
                handleGroupV2Context(groupContext.getGroupV2().get());
            }
        }

        final var conversationPartnerAddress = isSync ? destination : source;
        if (conversationPartnerAddress != null && message.isEndSession()) {
            account.getAciSessionStore().deleteAllSessions(conversationPartnerAddress.serviceId());
        }
        if (message.isExpirationUpdate() || message.getBody().isPresent()) {
            if (message.getGroupContext().isPresent()) {
                final var groupContext = message.getGroupContext().get();
                if (groupContext.getGroupV1().isPresent()) {
                    var groupInfo = groupContext.getGroupV1().get();
                    var group = account.getGroupStore().getOrCreateGroupV1(GroupId.v1(groupInfo.getGroupId()));
                    if (group != null) {
                        if (group.messageExpirationTime != message.getExpiresInSeconds()) {
                            group.messageExpirationTime = message.getExpiresInSeconds();
                            account.getGroupStore().updateGroup(group);
                        }
                    }
                } else if (groupContext.getGroupV2().isPresent()) {
                    // disappearing message timer already stored in the DecryptedGroup
                }
            } else if (conversationPartnerAddress != null) {
                context.getContactHelper()
                        .setExpirationTimer(conversationPartnerAddress.recipientId(), message.getExpiresInSeconds());
            }
        }
        if (!ignoreAttachments) {
            if (message.getAttachments().isPresent()) {
                for (var attachment : message.getAttachments().get()) {
                    context.getAttachmentHelper().downloadAttachment(attachment);
                }
            }
            if (message.getSharedContacts().isPresent()) {
                for (var contact : message.getSharedContacts().get()) {
                    if (contact.getAvatar().isPresent()) {
                        context.getAttachmentHelper().downloadAttachment(contact.getAvatar().get().getAttachment());
                    }
                }
            }
            if (message.getPreviews().isPresent()) {
                final var previews = message.getPreviews().get();
                for (var preview : previews) {
                    if (preview.getImage().isPresent()) {
                        context.getAttachmentHelper().downloadAttachment(preview.getImage().get());
                    }
                }
            }
            if (message.getQuote().isPresent()) {
                final var quote = message.getQuote().get();

                for (var quotedAttachment : quote.getAttachments()) {
                    final var thumbnail = quotedAttachment.getThumbnail();
                    if (thumbnail != null) {
                        context.getAttachmentHelper().downloadAttachment(thumbnail);
                    }
                }
            }
        }
        if (message.getProfileKey().isPresent()) {
            handleIncomingProfileKey(message.getProfileKey().get(), source.recipientId());
        }
        if (message.getSticker().isPresent()) {
            final var messageSticker = message.getSticker().get();
            final var stickerPackId = StickerPackId.deserialize(messageSticker.getPackId());
            var sticker = account.getStickerStore().getStickerPack(stickerPackId);
            if (sticker == null) {
                sticker = new StickerPack(stickerPackId, messageSticker.getPackKey());
                account.getStickerStore().addStickerPack(sticker);
            }
            context.getJobExecutor().enqueueJob(new RetrieveStickerPackJob(stickerPackId, messageSticker.getPackKey()));
        }
        return actions;
    }

    private List<HandleAction> handleSignalServiceStoryMessage(
            SignalServiceStoryMessage message, RecipientId source, boolean ignoreAttachments
    ) {
        var actions = new ArrayList<HandleAction>();
        if (message.getGroupContext().isPresent()) {
            handleGroupV2Context(message.getGroupContext().get());
        }

        if (!ignoreAttachments) {
            if (message.getFileAttachment().isPresent()) {
                context.getAttachmentHelper().downloadAttachment(message.getFileAttachment().get());
            }
            if (message.getTextAttachment().isPresent()) {
                final var textAttachment = message.getTextAttachment().get();
                if (textAttachment.getPreview().isPresent()) {
                    final var preview = textAttachment.getPreview().get();
                    if (preview.getImage().isPresent()) {
                        context.getAttachmentHelper().downloadAttachment(preview.getImage().get());
                    }
                }
            }
        }

        if (message.getProfileKey().isPresent()) {
            handleIncomingProfileKey(message.getProfileKey().get(), source);
        }

        return actions;
    }

    private void handleGroupV2Context(final SignalServiceGroupV2 groupContext) {
        final var groupMasterKey = groupContext.getMasterKey();

        context.getGroupHelper()
                .getOrMigrateGroup(groupMasterKey,
                        groupContext.getRevision(),
                        groupContext.hasSignedGroupChange() ? groupContext.getSignedGroupChange() : null);
    }

    private void handleIncomingProfileKey(final byte[] profileKeyBytes, final RecipientId source) {
        if (profileKeyBytes.length != 32) {
            logger.debug("Received invalid profile key of length {}", profileKeyBytes.length);
            return;
        }
        final ProfileKey profileKey;
        try {
            profileKey = new ProfileKey(profileKeyBytes);
        } catch (InvalidInputException e) {
            throw new AssertionError(e);
        }
        if (account.getSelfRecipientId().equals(source)) {
            this.account.setProfileKey(profileKey);
        }
        this.account.getProfileStore().storeProfileKey(source, profileKey);
    }

    private DeviceAddress getSender(SignalServiceEnvelope envelope, SignalServiceContent content) {
        if (!envelope.isUnidentifiedSender() && envelope.hasSourceUuid()) {
            return new DeviceAddress(context.getRecipientHelper().resolveRecipient(envelope.getSourceAddress()),
                    envelope.getSourceAddress().getServiceId(),
                    envelope.getSourceDevice());
        } else {
            return new DeviceAddress(context.getRecipientHelper().resolveRecipient(content.getSender()),
                    content.getSender().getServiceId(),
                    content.getSenderDevice());
        }
    }

    private DeviceAddress getDestination(SignalServiceEnvelope envelope) {
        if (!envelope.hasDestinationUuid()) {
            return new DeviceAddress(account.getSelfRecipientId(), account.getAci(), account.getDeviceId());
        }
        final var addressOptional = SignalServiceAddress.fromRaw(envelope.getDestinationUuid(), null);
        if (addressOptional.isEmpty()) {
            return new DeviceAddress(account.getSelfRecipientId(), account.getAci(), account.getDeviceId());
        }
        final var address = addressOptional.get();
        return new DeviceAddress(context.getRecipientHelper().resolveRecipient(address), address.getServiceId(), 0);
    }

    private record DeviceAddress(RecipientId recipientId, ServiceId serviceId, int deviceId) {}
}
