/*
  Copyright (C) 2015-2021 AsamK and contributors

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.asamk.signal.manager;

import org.asamk.signal.manager.actions.HandleAction;
import org.asamk.signal.manager.api.Configuration;
import org.asamk.signal.manager.api.Device;
import org.asamk.signal.manager.api.Group;
import org.asamk.signal.manager.api.Identity;
import org.asamk.signal.manager.api.InactiveGroupLinkException;
import org.asamk.signal.manager.api.InvalidDeviceLinkException;
import org.asamk.signal.manager.api.Message;
import org.asamk.signal.manager.api.Pair;
import org.asamk.signal.manager.api.RecipientIdentifier;
import org.asamk.signal.manager.api.SendGroupMessageResults;
import org.asamk.signal.manager.api.SendMessageResult;
import org.asamk.signal.manager.api.SendMessageResults;
import org.asamk.signal.manager.api.TypingAction;
import org.asamk.signal.manager.api.UpdateGroup;
import org.asamk.signal.manager.config.ServiceConfig;
import org.asamk.signal.manager.config.ServiceEnvironmentConfig;
import org.asamk.signal.manager.groups.GroupId;
import org.asamk.signal.manager.groups.GroupInviteLinkUrl;
import org.asamk.signal.manager.groups.GroupNotFoundException;
import org.asamk.signal.manager.groups.GroupSendingNotAllowedException;
import org.asamk.signal.manager.groups.LastGroupAdminException;
import org.asamk.signal.manager.groups.NotAGroupMemberException;
import org.asamk.signal.manager.helper.AttachmentHelper;
import org.asamk.signal.manager.helper.ContactHelper;
import org.asamk.signal.manager.helper.GroupHelper;
import org.asamk.signal.manager.helper.GroupV2Helper;
import org.asamk.signal.manager.helper.IdentityHelper;
import org.asamk.signal.manager.helper.IncomingMessageHandler;
import org.asamk.signal.manager.helper.PinHelper;
import org.asamk.signal.manager.helper.PreKeyHelper;
import org.asamk.signal.manager.helper.ProfileHelper;
import org.asamk.signal.manager.helper.SendHelper;
import org.asamk.signal.manager.helper.StorageHelper;
import org.asamk.signal.manager.helper.SyncHelper;
import org.asamk.signal.manager.helper.UnidentifiedAccessHelper;
import org.asamk.signal.manager.jobs.Context;
import org.asamk.signal.manager.storage.SignalAccount;
import org.asamk.signal.manager.storage.groups.GroupInfo;
import org.asamk.signal.manager.storage.identities.IdentityInfo;
import org.asamk.signal.manager.storage.messageCache.CachedMessage;
import org.asamk.signal.manager.storage.recipients.Contact;
import org.asamk.signal.manager.storage.recipients.Profile;
import org.asamk.signal.manager.storage.recipients.RecipientAddress;
import org.asamk.signal.manager.storage.recipients.RecipientId;
import org.asamk.signal.manager.storage.stickers.Sticker;
import org.asamk.signal.manager.storage.stickers.StickerPackId;
import org.asamk.signal.manager.util.KeyUtils;
import org.asamk.signal.manager.util.StickerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalSessionLock;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.messages.SignalServiceReceiptMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceTypingMessage;
import org.whispersystems.signalservice.api.push.ACI;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.AuthorizationFailedException;
import org.whispersystems.signalservice.api.util.DeviceNameUtil;
import org.whispersystems.signalservice.api.util.InvalidNumberException;
import org.whispersystems.signalservice.api.util.PhoneNumberFormatter;
import org.whispersystems.signalservice.api.websocket.WebSocketConnectionState;
import org.whispersystems.signalservice.api.websocket.WebSocketUnavailableException;
import org.whispersystems.signalservice.internal.contacts.crypto.Quote;
import org.whispersystems.signalservice.internal.contacts.crypto.UnauthenticatedQuoteException;
import org.whispersystems.signalservice.internal.contacts.crypto.UnauthenticatedResponseException;
import org.whispersystems.signalservice.internal.util.DynamicCredentialsProvider;
import org.whispersystems.signalservice.internal.util.Hex;
import org.whispersystems.signalservice.internal.util.Util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.asamk.signal.manager.config.ServiceConfig.capabilities;

public class ManagerImpl implements Manager {

    private final static Logger logger = LoggerFactory.getLogger(ManagerImpl.class);

    private final ServiceEnvironmentConfig serviceEnvironmentConfig;
    private final SignalDependencies dependencies;

    private SignalAccount account;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final ProfileHelper profileHelper;
    private final PinHelper pinHelper;
    private final StorageHelper storageHelper;
    private final SendHelper sendHelper;
    private final SyncHelper syncHelper;
    private final AttachmentHelper attachmentHelper;
    private final GroupHelper groupHelper;
    private final ContactHelper contactHelper;
    private final IncomingMessageHandler incomingMessageHandler;
    private final PreKeyHelper preKeyHelper;
    private final IdentityHelper identityHelper;

    private final Context context;
    private boolean hasCaughtUpWithOldMessages = false;
    private boolean ignoreAttachments = false;

    private Thread receiveThread;
    private final Set<ReceiveMessageHandler> weakHandlers = new HashSet<>();
    private final Set<ReceiveMessageHandler> messageHandlers = new HashSet<>();
    private final List<Runnable> closedListeners = new ArrayList<>();
    private boolean isReceivingSynchronous;

    ManagerImpl(
            SignalAccount account,
            PathConfig pathConfig,
            ServiceEnvironmentConfig serviceEnvironmentConfig,
            String userAgent
    ) {
        this.account = account;
        this.serviceEnvironmentConfig = serviceEnvironmentConfig;

        final var credentialsProvider = new DynamicCredentialsProvider(account.getAci(),
                account.getAccount(),
                account.getPassword(),
                account.getDeviceId());
        final var sessionLock = new SignalSessionLock() {
            private final ReentrantLock LEGACY_LOCK = new ReentrantLock();

            @Override
            public Lock acquire() {
                LEGACY_LOCK.lock();
                return LEGACY_LOCK::unlock;
            }
        };
        this.dependencies = new SignalDependencies(serviceEnvironmentConfig,
                userAgent,
                credentialsProvider,
                account.getSignalProtocolStore(),
                executor,
                sessionLock);
        final var avatarStore = new AvatarStore(pathConfig.avatarsPath());
        final var attachmentStore = new AttachmentStore(pathConfig.attachmentsPath());
        final var stickerPackStore = new StickerPackStore(pathConfig.stickerPacksPath());

        this.attachmentHelper = new AttachmentHelper(dependencies, attachmentStore);
        this.pinHelper = new PinHelper(dependencies.getKeyBackupService());
        final var unidentifiedAccessHelper = new UnidentifiedAccessHelper(account,
                dependencies,
                account::getProfileKey,
                this::getRecipientProfile);
        this.profileHelper = new ProfileHelper(account,
                dependencies,
                avatarStore,
                unidentifiedAccessHelper::getAccessFor,
                this::resolveSignalServiceAddress);
        final GroupV2Helper groupV2Helper = new GroupV2Helper(profileHelper::getRecipientProfileKeyCredential,
                this::getRecipientProfile,
                account::getSelfRecipientId,
                dependencies.getGroupsV2Operations(),
                dependencies.getGroupsV2Api(),
                this::resolveSignalServiceAddress);
        this.sendHelper = new SendHelper(account,
                dependencies,
                unidentifiedAccessHelper,
                this::resolveSignalServiceAddress,
                account.getRecipientStore(),
                this::handleIdentityFailure,
                this::getGroupInfo,
                this::refreshRegisteredUser);
        this.groupHelper = new GroupHelper(account,
                dependencies,
                attachmentHelper,
                sendHelper,
                groupV2Helper,
                avatarStore,
                this::resolveSignalServiceAddress,
                account.getRecipientStore());
        this.storageHelper = new StorageHelper(account, dependencies, groupHelper, profileHelper);
        this.contactHelper = new ContactHelper(account);
        this.syncHelper = new SyncHelper(account,
                attachmentHelper,
                sendHelper,
                groupHelper,
                avatarStore,
                this::resolveSignalServiceAddress);
        preKeyHelper = new PreKeyHelper(account, dependencies);

        this.context = new Context(account,
                dependencies,
                stickerPackStore,
                sendHelper,
                groupHelper,
                syncHelper,
                profileHelper,
                storageHelper,
                preKeyHelper);
        var jobExecutor = new JobExecutor(context);

        this.incomingMessageHandler = new IncomingMessageHandler(account,
                dependencies,
                account.getRecipientStore(),
                this::resolveSignalServiceAddress,
                groupHelper,
                contactHelper,
                attachmentHelper,
                syncHelper,
                this::getRecipientProfile,
                jobExecutor);
        this.identityHelper = new IdentityHelper(account,
                dependencies,
                this::resolveSignalServiceAddress,
                syncHelper,
                profileHelper);
    }

    @Override
    public String getSelfNumber() {
        return account.getAccount();
    }

    @Override
    public void checkAccountState() throws IOException {
        if (account.getLastReceiveTimestamp() == 0) {
            logger.info("The Signal protocol expects that incoming messages are regularly received.");
        } else {
            var diffInMilliseconds = System.currentTimeMillis() - account.getLastReceiveTimestamp();
            long days = TimeUnit.DAYS.convert(diffInMilliseconds, TimeUnit.MILLISECONDS);
            if (days > 7) {
                logger.warn(
                        "Messages have been last received {} days ago. The Signal protocol expects that incoming messages are regularly received.",
                        days);
            }
        }
        try {
            preKeyHelper.refreshPreKeysIfNecessary();
            if (account.getAci() == null) {
                account.setAci(dependencies.getAccountManager().getOwnAci());
            }
            updateAccountAttributes(null);
        } catch (AuthorizationFailedException e) {
            account.setRegistered(false);
            throw e;
        }
    }

    /**
     * This is used for checking a set of phone numbers for registration on Signal
     *
     * @param numbers The set of phone number in question
     * @return A map of numbers to canonicalized number and uuid. If a number is not registered the uuid is null.
     * @throws IOException if it's unable to get the contacts to check if they're registered
     */
    @Override
    public Map<String, Pair<String, UUID>> areUsersRegistered(Set<String> numbers) throws IOException {
        Map<String, String> canonicalizedNumbers = numbers.stream().collect(Collectors.toMap(n -> n, n -> {
            try {
                final var canonicalizedNumber = PhoneNumberFormatter.formatNumber(n, account.getAccount());
                if (!canonicalizedNumber.equals(n)) {
                    logger.debug("Normalized number {} to {}.", n, canonicalizedNumber);
                }
                return canonicalizedNumber;
            } catch (InvalidNumberException e) {
                return "";
            }
        }));

        // Note "registeredUsers" has no optionals. It only gives us info on users who are registered
        var registeredUsers = getRegisteredUsers(canonicalizedNumbers.values()
                .stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet()));

        return numbers.stream().collect(Collectors.toMap(n -> n, n -> {
            final var number = canonicalizedNumbers.get(n);
            final var aci = registeredUsers.get(number);
            return new Pair<>(number.isEmpty() ? null : number, aci == null ? null : aci.uuid());
        }));
    }

    @Override
    public void updateAccountAttributes(String deviceName) throws IOException {
        final String encryptedDeviceName;
        if (deviceName == null) {
            encryptedDeviceName = account.getEncryptedDeviceName();
        } else {
            final var privateKey = account.getIdentityKeyPair().getPrivateKey();
            encryptedDeviceName = DeviceNameUtil.encryptDeviceName(deviceName, privateKey);
            account.setEncryptedDeviceName(encryptedDeviceName);
        }
        dependencies.getAccountManager()
                .setAccountAttributes(encryptedDeviceName,
                        null,
                        account.getLocalRegistrationId(),
                        true,
                        null,
                        account.getPinMasterKey() == null ? null : account.getPinMasterKey().deriveRegistrationLock(),
                        account.getSelfUnidentifiedAccessKey(),
                        account.isUnrestrictedUnidentifiedAccess(),
                        capabilities,
                        account.isDiscoverableByPhoneNumber());
    }

    @Override
    public Configuration getConfiguration() {
        final var configurationStore = account.getConfigurationStore();
        return new Configuration(java.util.Optional.ofNullable(configurationStore.getReadReceipts()),
                java.util.Optional.ofNullable(configurationStore.getUnidentifiedDeliveryIndicators()),
                java.util.Optional.ofNullable(configurationStore.getTypingIndicators()),
                java.util.Optional.ofNullable(configurationStore.getLinkPreviews()));
    }

    @Override
    public void updateConfiguration(
            Configuration configuration
    ) throws IOException, NotMasterDeviceException {
        if (!account.isMasterDevice()) {
            throw new NotMasterDeviceException();
        }

        final var configurationStore = account.getConfigurationStore();
        if (configuration.readReceipts().isPresent()) {
            configurationStore.setReadReceipts(configuration.readReceipts().get());
        }
        if (configuration.unidentifiedDeliveryIndicators().isPresent()) {
            configurationStore.setUnidentifiedDeliveryIndicators(configuration.unidentifiedDeliveryIndicators().get());
        }
        if (configuration.typingIndicators().isPresent()) {
            configurationStore.setTypingIndicators(configuration.typingIndicators().get());
        }
        if (configuration.linkPreviews().isPresent()) {
            configurationStore.setLinkPreviews(configuration.linkPreviews().get());
        }
        syncHelper.sendConfigurationMessage();
    }

    /**
     * @param givenName  if null, the previous givenName will be kept
     * @param familyName if null, the previous familyName will be kept
     * @param about      if null, the previous about text will be kept
     * @param aboutEmoji if null, the previous about emoji will be kept
     * @param avatar     if avatar is null the image from the local avatar store is used (if present),
     */
    @Override
    public void setProfile(
            String givenName, final String familyName, String about, String aboutEmoji, java.util.Optional<File> avatar
    ) throws IOException {
        profileHelper.setProfile(givenName,
                familyName,
                about,
                aboutEmoji,
                avatar == null ? null : Optional.fromNullable(avatar.orElse(null)));
        syncHelper.sendSyncFetchProfileMessage();
    }

    @Override
    public void unregister() throws IOException {
        // When setting an empty GCM id, the Signal-Server also sets the fetchesMessages property to false.
        // If this is the master device, other users can't send messages to this number anymore.
        // If this is a linked device, other users can still send messages, but this device doesn't receive them anymore.
        dependencies.getAccountManager().setGcmId(Optional.absent());

        account.setRegistered(false);
        close();
    }

    @Override
    public void deleteAccount() throws IOException {
        try {
            pinHelper.removeRegistrationLockPin();
        } catch (IOException e) {
            logger.warn("Failed to remove registration lock pin");
        }
        account.setRegistrationLockPin(null, null);

        dependencies.getAccountManager().deleteAccount();

        account.setRegistered(false);
        close();
    }

    @Override
    public void submitRateLimitRecaptchaChallenge(String challenge, String captcha) throws IOException {
        captcha = captcha == null ? null : captcha.replace("signalcaptcha://", "");

        dependencies.getAccountManager().submitRateLimitRecaptchaChallenge(challenge, captcha);
    }

    @Override
    public List<Device> getLinkedDevices() throws IOException {
        var devices = dependencies.getAccountManager().getDevices();
        account.setMultiDevice(devices.size() > 1);
        var identityKey = account.getIdentityKeyPair().getPrivateKey();
        return devices.stream().map(d -> {
            String deviceName = d.getName();
            if (deviceName != null) {
                try {
                    deviceName = DeviceNameUtil.decryptDeviceName(deviceName, identityKey);
                } catch (IOException e) {
                    logger.debug("Failed to decrypt device name, maybe plain text?", e);
                }
            }
            return new Device(d.getId(),
                    deviceName,
                    d.getCreated(),
                    d.getLastSeen(),
                    d.getId() == account.getDeviceId());
        }).collect(Collectors.toList());
    }

    @Override
    public void removeLinkedDevices(long deviceId) throws IOException {
        dependencies.getAccountManager().removeDevice(deviceId);
        var devices = dependencies.getAccountManager().getDevices();
        account.setMultiDevice(devices.size() > 1);
    }

    @Override
    public void addDeviceLink(URI linkUri) throws IOException, InvalidDeviceLinkException {
        var info = DeviceLinkInfo.parseDeviceLinkUri(linkUri);

        addDevice(info.deviceIdentifier(), info.deviceKey());
    }

    private void addDevice(
            String deviceIdentifier, ECPublicKey deviceKey
    ) throws IOException, InvalidDeviceLinkException {
        var identityKeyPair = account.getIdentityKeyPair();
        var verificationCode = dependencies.getAccountManager().getNewDeviceVerificationCode();

        try {
            dependencies.getAccountManager()
                    .addDevice(deviceIdentifier,
                            deviceKey,
                            identityKeyPair,
                            Optional.of(account.getProfileKey().serialize()),
                            verificationCode);
        } catch (InvalidKeyException e) {
            throw new InvalidDeviceLinkException("Invalid device link", e);
        }
        account.setMultiDevice(true);
    }

    @Override
    public void setRegistrationLockPin(java.util.Optional<String> pin) throws IOException {
        if (!account.isMasterDevice()) {
            throw new RuntimeException("Only master device can set a PIN");
        }
        if (pin.isPresent()) {
            final var masterKey = account.getPinMasterKey() != null
                    ? account.getPinMasterKey()
                    : KeyUtils.createMasterKey();

            pinHelper.setRegistrationLockPin(pin.get(), masterKey);

            account.setRegistrationLockPin(pin.get(), masterKey);
        } else {
            // Remove KBS Pin
            pinHelper.removeRegistrationLockPin();

            account.setRegistrationLockPin(null, null);
        }
    }

    void refreshPreKeys() throws IOException {
        preKeyHelper.refreshPreKeys();
    }

    @Override
    public Profile getRecipientProfile(RecipientIdentifier.Single recipient) throws IOException {
        return profileHelper.getRecipientProfile(resolveRecipient(recipient));
    }

    private Profile getRecipientProfile(RecipientId recipientId) {
        return profileHelper.getRecipientProfile(recipientId);
    }

    @Override
    public List<Group> getGroups() {
        return account.getGroupStore().getGroups().stream().map(this::toGroup).collect(Collectors.toList());
    }

    private Group toGroup(final GroupInfo groupInfo) {
        if (groupInfo == null) {
            return null;
        }

        return new Group(groupInfo.getGroupId(),
                groupInfo.getTitle(),
                groupInfo.getDescription(),
                groupInfo.getGroupInviteLink(),
                groupInfo.getMembers()
                        .stream()
                        .map(account.getRecipientStore()::resolveRecipientAddress)
                        .collect(Collectors.toSet()),
                groupInfo.getPendingMembers()
                        .stream()
                        .map(account.getRecipientStore()::resolveRecipientAddress)
                        .collect(Collectors.toSet()),
                groupInfo.getRequestingMembers()
                        .stream()
                        .map(account.getRecipientStore()::resolveRecipientAddress)
                        .collect(Collectors.toSet()),
                groupInfo.getAdminMembers()
                        .stream()
                        .map(account.getRecipientStore()::resolveRecipientAddress)
                        .collect(Collectors.toSet()),
                groupInfo.isBlocked(),
                groupInfo.getMessageExpirationTimer(),
                groupInfo.getPermissionAddMember(),
                groupInfo.getPermissionEditDetails(),
                groupInfo.getPermissionSendMessage(),
                groupInfo.isMember(account.getSelfRecipientId()),
                groupInfo.isAdmin(account.getSelfRecipientId()));
    }

    @Override
    public SendGroupMessageResults quitGroup(
            GroupId groupId, Set<RecipientIdentifier.Single> groupAdmins
    ) throws GroupNotFoundException, IOException, NotAGroupMemberException, LastGroupAdminException {
        final var newAdmins = resolveRecipients(groupAdmins);
        return groupHelper.quitGroup(groupId, newAdmins);
    }

    @Override
    public void deleteGroup(GroupId groupId) throws IOException {
        groupHelper.deleteGroup(groupId);
    }

    @Override
    public Pair<GroupId, SendGroupMessageResults> createGroup(
            String name, Set<RecipientIdentifier.Single> members, File avatarFile
    ) throws IOException, AttachmentInvalidException {
        return groupHelper.createGroup(name, members == null ? null : resolveRecipients(members), avatarFile);
    }

    @Override
    public SendGroupMessageResults updateGroup(
            final GroupId groupId, final UpdateGroup updateGroup
    ) throws IOException, GroupNotFoundException, AttachmentInvalidException, NotAGroupMemberException, GroupSendingNotAllowedException {
        return groupHelper.updateGroup(groupId,
                updateGroup.getName(),
                updateGroup.getDescription(),
                updateGroup.getMembers() == null ? null : resolveRecipients(updateGroup.getMembers()),
                updateGroup.getRemoveMembers() == null ? null : resolveRecipients(updateGroup.getRemoveMembers()),
                updateGroup.getAdmins() == null ? null : resolveRecipients(updateGroup.getAdmins()),
                updateGroup.getRemoveAdmins() == null ? null : resolveRecipients(updateGroup.getRemoveAdmins()),
                updateGroup.isResetGroupLink(),
                updateGroup.getGroupLinkState(),
                updateGroup.getAddMemberPermission(),
                updateGroup.getEditDetailsPermission(),
                updateGroup.getAvatarFile(),
                updateGroup.getExpirationTimer(),
                updateGroup.getIsAnnouncementGroup());
    }

    @Override
    public Pair<GroupId, SendGroupMessageResults> joinGroup(
            GroupInviteLinkUrl inviteLinkUrl
    ) throws IOException, InactiveGroupLinkException {
        return groupHelper.joinGroup(inviteLinkUrl);
    }

    private SendMessageResults sendMessage(
            SignalServiceDataMessage.Builder messageBuilder, Set<RecipientIdentifier> recipients
    ) throws IOException, NotAGroupMemberException, GroupNotFoundException, GroupSendingNotAllowedException {
        var results = new HashMap<RecipientIdentifier, List<SendMessageResult>>();
        long timestamp = System.currentTimeMillis();
        messageBuilder.withTimestamp(timestamp);
        for (final var recipient : recipients) {
            if (recipient instanceof RecipientIdentifier.Single single) {
                final var recipientId = resolveRecipient(single);
                final var result = sendHelper.sendMessage(messageBuilder, recipientId);
                results.put(recipient,
                        List.of(SendMessageResult.from(result,
                                account.getRecipientStore(),
                                account.getRecipientStore()::resolveRecipientAddress)));
            } else if (recipient instanceof RecipientIdentifier.NoteToSelf) {
                final var result = sendHelper.sendSelfMessage(messageBuilder);
                results.put(recipient,
                        List.of(SendMessageResult.from(result,
                                account.getRecipientStore(),
                                account.getRecipientStore()::resolveRecipientAddress)));
            } else if (recipient instanceof RecipientIdentifier.Group group) {
                final var result = sendHelper.sendAsGroupMessage(messageBuilder, group.groupId());
                results.put(recipient,
                        result.stream()
                                .map(sendMessageResult -> SendMessageResult.from(sendMessageResult,
                                        account.getRecipientStore(),
                                        account.getRecipientStore()::resolveRecipientAddress))
                                .collect(Collectors.toList()));
            }
        }
        return new SendMessageResults(timestamp, results);
    }

    private SendMessageResults sendTypingMessage(
            SignalServiceTypingMessage.Action action, Set<RecipientIdentifier> recipients
    ) throws IOException, NotAGroupMemberException, GroupNotFoundException, GroupSendingNotAllowedException {
        var results = new HashMap<RecipientIdentifier, List<SendMessageResult>>();
        final var timestamp = System.currentTimeMillis();
        for (var recipient : recipients) {
            if (recipient instanceof RecipientIdentifier.Single) {
                final var message = new SignalServiceTypingMessage(action, timestamp, Optional.absent());
                final var recipientId = resolveRecipient((RecipientIdentifier.Single) recipient);
                final var result = sendHelper.sendTypingMessage(message, recipientId);
                results.put(recipient,
                        List.of(SendMessageResult.from(result,
                                account.getRecipientStore(),
                                account.getRecipientStore()::resolveRecipientAddress)));
            } else if (recipient instanceof RecipientIdentifier.Group) {
                final var groupId = ((RecipientIdentifier.Group) recipient).groupId();
                final var message = new SignalServiceTypingMessage(action, timestamp, Optional.of(groupId.serialize()));
                final var result = sendHelper.sendGroupTypingMessage(message, groupId);
                results.put(recipient,
                        result.stream()
                                .map(r -> SendMessageResult.from(r,
                                        account.getRecipientStore(),
                                        account.getRecipientStore()::resolveRecipientAddress))
                                .collect(Collectors.toList()));
            }
        }
        return new SendMessageResults(timestamp, results);
    }

    @Override
    public SendMessageResults sendTypingMessage(
            TypingAction action, Set<RecipientIdentifier> recipients
    ) throws IOException, NotAGroupMemberException, GroupNotFoundException, GroupSendingNotAllowedException {
        return sendTypingMessage(action.toSignalService(), recipients);
    }

    @Override
    public SendMessageResults sendReadReceipt(
            RecipientIdentifier.Single sender, List<Long> messageIds
    ) throws IOException {
        final var timestamp = System.currentTimeMillis();
        var receiptMessage = new SignalServiceReceiptMessage(SignalServiceReceiptMessage.Type.READ,
                messageIds,
                timestamp);

        final var result = sendHelper.sendReceiptMessage(receiptMessage, resolveRecipient(sender));
        return new SendMessageResults(timestamp,
                Map.of(sender,
                        List.of(SendMessageResult.from(result,
                                account.getRecipientStore(),
                                account.getRecipientStore()::resolveRecipientAddress))));
    }

    @Override
    public SendMessageResults sendViewedReceipt(
            RecipientIdentifier.Single sender, List<Long> messageIds
    ) throws IOException {
        final var timestamp = System.currentTimeMillis();
        var receiptMessage = new SignalServiceReceiptMessage(SignalServiceReceiptMessage.Type.VIEWED,
                messageIds,
                timestamp);

        final var result = sendHelper.sendReceiptMessage(receiptMessage, resolveRecipient(sender));
        return new SendMessageResults(timestamp,
                Map.of(sender,
                        List.of(SendMessageResult.from(result,
                                account.getRecipientStore(),
                                account.getRecipientStore()::resolveRecipientAddress))));
    }

    @Override
    public SendMessageResults sendMessage(
            Message message, Set<RecipientIdentifier> recipients
    ) throws IOException, AttachmentInvalidException, NotAGroupMemberException, GroupNotFoundException, GroupSendingNotAllowedException {
        final var messageBuilder = SignalServiceDataMessage.newBuilder();
        applyMessage(messageBuilder, message);
        return sendMessage(messageBuilder, recipients);
    }

    private void applyMessage(
            final SignalServiceDataMessage.Builder messageBuilder, final Message message
    ) throws AttachmentInvalidException, IOException {
        messageBuilder.withBody(message.messageText());
        final var attachments = message.attachments();
        if (attachments != null) {
            messageBuilder.withAttachments(attachmentHelper.uploadAttachments(attachments));
        }
        if (message.mentions().size() > 0) {
            messageBuilder.withMentions(resolveMentions(message.mentions()));
        }
        if (message.quote().isPresent()) {
            final var quote = message.quote().get();
            messageBuilder.withQuote(new SignalServiceDataMessage.Quote(quote.timestamp(),
                    resolveSignalServiceAddress(resolveRecipient(quote.author())),
                    quote.message(),
                    List.of(),
                    resolveMentions(quote.mentions())));
        }
    }

    private ArrayList<SignalServiceDataMessage.Mention> resolveMentions(final List<Message.Mention> mentionList) throws IOException {
        final var mentions = new ArrayList<SignalServiceDataMessage.Mention>();
        for (final var m : mentionList) {
            final var recipientId = resolveRecipient(m.recipient());
            mentions.add(new SignalServiceDataMessage.Mention(resolveSignalServiceAddress(recipientId).getAci(),
                    m.start(),
                    m.length()));
        }
        return mentions;
    }

    @Override
    public SendMessageResults sendRemoteDeleteMessage(
            long targetSentTimestamp, Set<RecipientIdentifier> recipients
    ) throws IOException, NotAGroupMemberException, GroupNotFoundException, GroupSendingNotAllowedException {
        var delete = new SignalServiceDataMessage.RemoteDelete(targetSentTimestamp);
        final var messageBuilder = SignalServiceDataMessage.newBuilder().withRemoteDelete(delete);
        return sendMessage(messageBuilder, recipients);
    }

    @Override
    public SendMessageResults sendMessageReaction(
            String emoji,
            boolean remove,
            RecipientIdentifier.Single targetAuthor,
            long targetSentTimestamp,
            Set<RecipientIdentifier> recipients
    ) throws IOException, NotAGroupMemberException, GroupNotFoundException, GroupSendingNotAllowedException {
        var targetAuthorRecipientId = resolveRecipient(targetAuthor);
        var reaction = new SignalServiceDataMessage.Reaction(emoji,
                remove,
                resolveSignalServiceAddress(targetAuthorRecipientId),
                targetSentTimestamp);
        final var messageBuilder = SignalServiceDataMessage.newBuilder().withReaction(reaction);
        return sendMessage(messageBuilder, recipients);
    }

    @Override
    public SendMessageResults sendEndSessionMessage(Set<RecipientIdentifier.Single> recipients) throws IOException {
        var messageBuilder = SignalServiceDataMessage.newBuilder().asEndSessionMessage();

        try {
            return sendMessage(messageBuilder,
                    recipients.stream().map(RecipientIdentifier.class::cast).collect(Collectors.toSet()));
        } catch (GroupNotFoundException | NotAGroupMemberException | GroupSendingNotAllowedException e) {
            throw new AssertionError(e);
        } finally {
            for (var recipient : recipients) {
                final var recipientId = resolveRecipient(recipient);
                account.getSessionStore().deleteAllSessions(recipientId);
            }
        }
    }

    @Override
    public void deleteRecipient(final RecipientIdentifier.Single recipient) throws IOException {
        account.removeRecipient(resolveRecipient(recipient));
    }

    @Override
    public void deleteContact(final RecipientIdentifier.Single recipient) throws IOException {
        account.getContactStore().deleteContact(resolveRecipient(recipient));
    }

    @Override
    public void setContactName(
            RecipientIdentifier.Single recipient, String name
    ) throws NotMasterDeviceException, IOException {
        if (!account.isMasterDevice()) {
            throw new NotMasterDeviceException();
        }
        contactHelper.setContactName(resolveRecipient(recipient), name);
    }

    @Override
    public void setContactBlocked(
            RecipientIdentifier.Single recipient, boolean blocked
    ) throws NotMasterDeviceException, IOException {
        if (!account.isMasterDevice()) {
            throw new NotMasterDeviceException();
        }
        contactHelper.setContactBlocked(resolveRecipient(recipient), blocked);
        // TODO cycle our profile key
        syncHelper.sendBlockedList();
    }

    @Override
    public void setGroupBlocked(
            final GroupId groupId, final boolean blocked
    ) throws GroupNotFoundException, IOException, NotMasterDeviceException {
        if (!account.isMasterDevice()) {
            throw new NotMasterDeviceException();
        }
        groupHelper.setGroupBlocked(groupId, blocked);
        // TODO cycle our profile key
        syncHelper.sendBlockedList();
    }

    /**
     * Change the expiration timer for a contact
     */
    @Override
    public void setExpirationTimer(
            RecipientIdentifier.Single recipient, int messageExpirationTimer
    ) throws IOException {
        var recipientId = resolveRecipient(recipient);
        contactHelper.setExpirationTimer(recipientId, messageExpirationTimer);
        final var messageBuilder = SignalServiceDataMessage.newBuilder().asExpirationUpdate();
        try {
            sendMessage(messageBuilder, Set.of(recipient));
        } catch (NotAGroupMemberException | GroupNotFoundException | GroupSendingNotAllowedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Upload the sticker pack from path.
     *
     * @param path Path can be a path to a manifest.json file or to a zip file that contains a manifest.json file
     * @return if successful, returns the URL to install the sticker pack in the signal app
     */
    @Override
    public URI uploadStickerPack(File path) throws IOException, StickerPackInvalidException {
        var manifest = StickerUtils.getSignalServiceStickerManifestUpload(path);

        var messageSender = dependencies.getMessageSender();

        var packKey = KeyUtils.createStickerUploadKey();
        var packIdString = messageSender.uploadStickerManifest(manifest, packKey);
        var packId = StickerPackId.deserialize(Hex.fromStringCondensed(packIdString));

        var sticker = new Sticker(packId, packKey);
        account.getStickerStore().updateSticker(sticker);

        try {
            return new URI("https",
                    "signal.art",
                    "/addstickers/",
                    "pack_id="
                            + URLEncoder.encode(Hex.toStringCondensed(packId.serialize()), StandardCharsets.UTF_8)
                            + "&pack_key="
                            + URLEncoder.encode(Hex.toStringCondensed(packKey), StandardCharsets.UTF_8));
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void requestAllSyncData() throws IOException {
        syncHelper.requestAllSyncData();
        retrieveRemoteStorage();
    }

    void retrieveRemoteStorage() throws IOException {
        if (account.getStorageKey() != null) {
            storageHelper.readDataFromStorage();
        }
    }

    private RecipientId refreshRegisteredUser(RecipientId recipientId) throws IOException {
        final var address = resolveSignalServiceAddress(recipientId);
        if (!address.getNumber().isPresent()) {
            return recipientId;
        }
        final var number = address.getNumber().get();
        final var uuid = getRegisteredUser(number);
        return resolveRecipientTrusted(new SignalServiceAddress(uuid, number));
    }

    private ACI getRegisteredUser(final String number) throws IOException {
        final Map<String, ACI> aciMap;
        try {
            aciMap = getRegisteredUsers(Set.of(number));
        } catch (NumberFormatException e) {
            throw new IOException(number, e);
        }
        final var uuid = aciMap.get(number);
        if (uuid == null) {
            throw new IOException(number, null);
        }
        return uuid;
    }

    private Map<String, ACI> getRegisteredUsers(final Set<String> numbers) throws IOException {
        final Map<String, ACI> registeredUsers;
        try {
            registeredUsers = dependencies.getAccountManager()
                    .getRegisteredUsers(ServiceConfig.getIasKeyStore(),
                            numbers,
                            serviceEnvironmentConfig.getCdsMrenclave());
        } catch (Quote.InvalidQuoteFormatException | UnauthenticatedQuoteException | SignatureException | UnauthenticatedResponseException | InvalidKeyException e) {
            throw new IOException(e);
        }

        // Store numbers as recipients, so we have the number/uuid association
        registeredUsers.forEach((number, aci) -> resolveRecipientTrusted(new SignalServiceAddress(aci, number)));

        return registeredUsers;
    }

    private void retryFailedReceivedMessages(ReceiveMessageHandler handler) {
        Set<HandleAction> queuedActions = new HashSet<>();
        for (var cachedMessage : account.getMessageCache().getCachedMessages()) {
            var actions = retryFailedReceivedMessage(handler, cachedMessage);
            if (actions != null) {
                queuedActions.addAll(actions);
            }
        }
        handleQueuedActions(queuedActions);
    }

    private List<HandleAction> retryFailedReceivedMessage(
            final ReceiveMessageHandler handler, final CachedMessage cachedMessage
    ) {
        var envelope = cachedMessage.loadEnvelope();
        if (envelope == null) {
            cachedMessage.delete();
            return null;
        }

        final var result = incomingMessageHandler.handleRetryEnvelope(envelope, ignoreAttachments, handler);
        final var actions = result.first();
        final var exception = result.second();

        if (exception instanceof UntrustedIdentityException) {
            if (System.currentTimeMillis() - envelope.getServerDeliveredTimestamp() > 1000L * 60 * 60 * 24 * 30) {
                // Envelope is more than a month old, cleaning up.
                cachedMessage.delete();
                return null;
            }
            if (!envelope.hasSourceUuid()) {
                final var identifier = ((UntrustedIdentityException) exception).getSender();
                final var recipientId = account.getRecipientStore().resolveRecipient(identifier);
                try {
                    account.getMessageCache().replaceSender(cachedMessage, recipientId);
                } catch (IOException ioException) {
                    logger.warn("Failed to move cached message to recipient folder: {}", ioException.getMessage());
                }
            }
            return null;
        }

        // If successful and for all other errors that are not recoverable, delete the cached message
        cachedMessage.delete();
        return actions;
    }

    @Override
    public void addReceiveHandler(final ReceiveMessageHandler handler, final boolean isWeakListener) {
        if (isReceivingSynchronous) {
            throw new IllegalStateException("Already receiving message synchronously.");
        }
        synchronized (messageHandlers) {
            if (isWeakListener) {
                weakHandlers.add(handler);
            } else {
                messageHandlers.add(handler);
                startReceiveThreadIfRequired();
            }
        }
    }

    private void startReceiveThreadIfRequired() {
        if (receiveThread != null) {
            return;
        }
        receiveThread = new Thread(() -> {
            logger.debug("Starting receiving messages");
            while (!Thread.interrupted()) {
                try {
                    receiveMessagesInternal(1L, TimeUnit.HOURS, false, (envelope, e) -> {
                        synchronized (messageHandlers) {
                            Stream.concat(messageHandlers.stream(), weakHandlers.stream()).forEach(h -> {
                                try {
                                    h.handleMessage(envelope, e);
                                } catch (Exception ex) {
                                    logger.warn("Message handler failed, ignoring", ex);
                                }
                            });
                        }
                    });
                    break;
                } catch (IOException e) {
                    logger.warn("Receiving messages failed, retrying", e);
                }
            }
            logger.debug("Finished receiving messages");
            hasCaughtUpWithOldMessages = false;
            synchronized (messageHandlers) {
                receiveThread = null;

                // Check if in the meantime another handler has been registered
                if (!messageHandlers.isEmpty()) {
                    logger.debug("Another handler has been registered, starting receive thread again");
                    startReceiveThreadIfRequired();
                }
            }
        });

        receiveThread.start();
    }

    @Override
    public void removeReceiveHandler(final ReceiveMessageHandler handler) {
        final Thread thread;
        synchronized (messageHandlers) {
            weakHandlers.remove(handler);
            messageHandlers.remove(handler);
            if (!messageHandlers.isEmpty() || receiveThread == null || isReceivingSynchronous) {
                return;
            }
            thread = receiveThread;
            receiveThread = null;
        }

        stopReceiveThread(thread);
    }

    private void stopReceiveThread(final Thread thread) {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public boolean isReceiving() {
        if (isReceivingSynchronous) {
            return true;
        }
        synchronized (messageHandlers) {
            return messageHandlers.size() > 0;
        }
    }

    @Override
    public void receiveMessages(long timeout, TimeUnit unit, ReceiveMessageHandler handler) throws IOException {
        receiveMessages(timeout, unit, true, handler);
    }

    @Override
    public void receiveMessages(ReceiveMessageHandler handler) throws IOException {
        receiveMessages(1L, TimeUnit.HOURS, false, handler);
    }

    private void receiveMessages(
            long timeout, TimeUnit unit, boolean returnOnTimeout, ReceiveMessageHandler handler
    ) throws IOException {
        if (isReceiving()) {
            throw new IllegalStateException("Already receiving message.");
        }
        isReceivingSynchronous = true;
        receiveThread = Thread.currentThread();
        try {
            receiveMessagesInternal(timeout, unit, returnOnTimeout, handler);
        } finally {
            receiveThread = null;
            hasCaughtUpWithOldMessages = false;
            isReceivingSynchronous = false;
        }
    }

    private void receiveMessagesInternal(
            long timeout, TimeUnit unit, boolean returnOnTimeout, ReceiveMessageHandler handler
    ) throws IOException {
        retryFailedReceivedMessages(handler);

        // Use a Map here because java Set doesn't have a get method ...
        Map<HandleAction, HandleAction> queuedActions = new HashMap<>();

        final var signalWebSocket = dependencies.getSignalWebSocket();
        final var webSocketStateDisposable = Observable.merge(signalWebSocket.getUnidentifiedWebSocketState(),
                        signalWebSocket.getWebSocketState())
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .distinctUntilChanged()
                .subscribe(this::onWebSocketStateChange);
        signalWebSocket.connect();

        hasCaughtUpWithOldMessages = false;
        var backOffCounter = 0;
        final var MAX_BACKOFF_COUNTER = 9;

        while (!Thread.interrupted()) {
            SignalServiceEnvelope envelope;
            final CachedMessage[] cachedMessage = {null};
            final var nowMillis = System.currentTimeMillis();
            if (nowMillis - account.getLastReceiveTimestamp() > 60000) {
                account.setLastReceiveTimestamp(nowMillis);
            }
            logger.debug("Checking for new message from server");
            try {
                var result = signalWebSocket.readOrEmpty(unit.toMillis(timeout), envelope1 -> {
                    final var recipientId = envelope1.hasSourceUuid()
                            ? resolveRecipient(envelope1.getSourceAddress())
                            : null;
                    // store message on disk, before acknowledging receipt to the server
                    cachedMessage[0] = account.getMessageCache().cacheMessage(envelope1, recipientId);
                });
                backOffCounter = 0;

                if (result.isPresent()) {
                    envelope = result.get();
                    logger.debug("New message received from server");
                } else {
                    logger.debug("Received indicator that server queue is empty");
                    handleQueuedActions(queuedActions.keySet());
                    queuedActions.clear();

                    hasCaughtUpWithOldMessages = true;
                    synchronized (this) {
                        this.notifyAll();
                    }

                    // Continue to wait another timeout for new messages
                    continue;
                }
            } catch (AssertionError e) {
                if (e.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                } else {
                    throw e;
                }
            } catch (IOException e) {
                logger.debug("Pipe unexpectedly unavailable: {}", e.getMessage());
                if (e instanceof WebSocketUnavailableException || "Connection closed!".equals(e.getMessage())) {
                    final var sleepMilliseconds = 100 * (long) Math.pow(2, backOffCounter);
                    backOffCounter = Math.min(backOffCounter + 1, MAX_BACKOFF_COUNTER);
                    logger.warn("Connection closed unexpectedly, reconnecting in {} ms", sleepMilliseconds);
                    try {
                        Thread.sleep(sleepMilliseconds);
                    } catch (InterruptedException interruptedException) {
                        return;
                    }
                    hasCaughtUpWithOldMessages = false;
                    signalWebSocket.connect();
                    continue;
                }
                throw e;
            } catch (TimeoutException e) {
                backOffCounter = 0;
                if (returnOnTimeout) return;
                continue;
            }

            final var result = incomingMessageHandler.handleEnvelope(envelope, ignoreAttachments, handler);
            for (final var h : result.first()) {
                final var existingAction = queuedActions.get(h);
                if (existingAction == null) {
                    queuedActions.put(h, h);
                } else {
                    existingAction.mergeOther(h);
                }
            }
            final var exception = result.second();

            if (hasCaughtUpWithOldMessages) {
                handleQueuedActions(queuedActions.keySet());
                queuedActions.clear();
            }
            if (cachedMessage[0] != null) {
                if (exception instanceof UntrustedIdentityException) {
                    logger.debug("Keeping message with untrusted identity in message cache");
                    final var address = ((UntrustedIdentityException) exception).getSender();
                    final var recipientId = resolveRecipient(address);
                    if (!envelope.hasSourceUuid()) {
                        try {
                            cachedMessage[0] = account.getMessageCache().replaceSender(cachedMessage[0], recipientId);
                        } catch (IOException ioException) {
                            logger.warn("Failed to move cached message to recipient folder: {}",
                                    ioException.getMessage());
                        }
                    }
                } else {
                    cachedMessage[0].delete();
                }
            }
        }
        handleQueuedActions(queuedActions.keySet());
        queuedActions.clear();
        dependencies.getSignalWebSocket().disconnect();
        webSocketStateDisposable.dispose();
    }

    private void onWebSocketStateChange(final WebSocketConnectionState s) {
        if (s.equals(WebSocketConnectionState.AUTHENTICATION_FAILED)) {
            account.setRegistered(false);
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setIgnoreAttachments(final boolean ignoreAttachments) {
        this.ignoreAttachments = ignoreAttachments;
    }

    @Override
    public boolean hasCaughtUpWithOldMessages() {
        return hasCaughtUpWithOldMessages;
    }

    private void handleQueuedActions(final Collection<HandleAction> queuedActions) {
        logger.debug("Handling message actions");
        var interrupted = false;
        for (var action : queuedActions) {
            try {
                action.execute(context);
            } catch (Throwable e) {
                if ((e instanceof AssertionError || e instanceof RuntimeException)
                        && e.getCause() instanceof InterruptedException) {
                    interrupted = true;
                    continue;
                }
                logger.warn("Message action failed.", e);
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isContactBlocked(final RecipientIdentifier.Single recipient) {
        final RecipientId recipientId;
        try {
            recipientId = resolveRecipient(recipient);
        } catch (IOException e) {
            return false;
        }
        return contactHelper.isContactBlocked(recipientId);
    }

    @Override
    public void sendContacts() throws IOException {
        syncHelper.sendContacts();
    }

    @Override
    public List<Pair<RecipientAddress, Contact>> getContacts() {
        return account.getContactStore()
                .getContacts()
                .stream()
                .map(p -> new Pair<>(account.getRecipientStore().resolveRecipientAddress(p.first()), p.second()))
                .collect(Collectors.toList());
    }

    @Override
    public String getContactOrProfileName(RecipientIdentifier.Single recipient) {
        final RecipientId recipientId;
        try {
            recipientId = resolveRecipient(recipient);
        } catch (IOException e) {
            return null;
        }

        final var contact = account.getContactStore().getContact(recipientId);
        if (contact != null && !Util.isEmpty(contact.getName())) {
            return contact.getName();
        }

        final var profile = getRecipientProfile(recipientId);
        if (profile != null) {
            return profile.getDisplayName();
        }

        return null;
    }

    @Override
    public Group getGroup(GroupId groupId) {
        return toGroup(groupHelper.getGroup(groupId));
    }

    private GroupInfo getGroupInfo(GroupId groupId) {
        return groupHelper.getGroup(groupId);
    }

    @Override
    public List<Identity> getIdentities() {
        return account.getIdentityKeyStore()
                .getIdentities()
                .stream()
                .map(this::toIdentity)
                .collect(Collectors.toList());
    }

    private Identity toIdentity(final IdentityInfo identityInfo) {
        if (identityInfo == null) {
            return null;
        }

        final var address = account.getRecipientStore().resolveRecipientAddress(identityInfo.getRecipientId());
        final var scannableFingerprint = identityHelper.computeSafetyNumberForScanning(identityInfo.getRecipientId(),
                identityInfo.getIdentityKey());
        return new Identity(address,
                identityInfo.getIdentityKey(),
                identityHelper.computeSafetyNumber(identityInfo.getRecipientId(), identityInfo.getIdentityKey()),
                scannableFingerprint == null ? null : scannableFingerprint.getSerialized(),
                identityInfo.getTrustLevel(),
                identityInfo.getDateAdded());
    }

    @Override
    public List<Identity> getIdentities(RecipientIdentifier.Single recipient) {
        IdentityInfo identity;
        try {
            identity = account.getIdentityKeyStore().getIdentity(resolveRecipient(recipient));
        } catch (IOException e) {
            identity = null;
        }
        return identity == null ? List.of() : List.of(toIdentity(identity));
    }

    /**
     * Trust this the identity with this fingerprint
     *
     * @param recipient   account of the identity
     * @param fingerprint Fingerprint
     */
    @Override
    public boolean trustIdentityVerified(RecipientIdentifier.Single recipient, byte[] fingerprint) {
        RecipientId recipientId;
        try {
            recipientId = resolveRecipient(recipient);
        } catch (IOException e) {
            return false;
        }
        return identityHelper.trustIdentityVerified(recipientId, fingerprint);
    }

    /**
     * Trust this the identity with this safety number
     *
     * @param recipient    account of the identity
     * @param safetyNumber Safety number
     */
    @Override
    public boolean trustIdentityVerifiedSafetyNumber(RecipientIdentifier.Single recipient, String safetyNumber) {
        RecipientId recipientId;
        try {
            recipientId = resolveRecipient(recipient);
        } catch (IOException e) {
            return false;
        }
        return identityHelper.trustIdentityVerifiedSafetyNumber(recipientId, safetyNumber);
    }

    /**
     * Trust this the identity with this scannable safety number
     *
     * @param recipient    account of the identity
     * @param safetyNumber Scannable safety number
     */
    @Override
    public boolean trustIdentityVerifiedSafetyNumber(RecipientIdentifier.Single recipient, byte[] safetyNumber) {
        RecipientId recipientId;
        try {
            recipientId = resolveRecipient(recipient);
        } catch (IOException e) {
            return false;
        }
        return identityHelper.trustIdentityVerifiedSafetyNumber(recipientId, safetyNumber);
    }

    /**
     * Trust all keys of this identity without verification
     *
     * @param recipient account of the identity
     */
    @Override
    public boolean trustIdentityAllKeys(RecipientIdentifier.Single recipient) {
        RecipientId recipientId;
        try {
            recipientId = resolveRecipient(recipient);
        } catch (IOException e) {
            return false;
        }
        return identityHelper.trustIdentityAllKeys(recipientId);
    }

    @Override
    public void addClosedListener(final Runnable listener) {
        synchronized (closedListeners) {
            closedListeners.add(listener);
        }
    }

    private void handleIdentityFailure(
            final RecipientId recipientId,
            final org.whispersystems.signalservice.api.messages.SendMessageResult.IdentityFailure identityFailure
    ) {
        this.identityHelper.handleIdentityFailure(recipientId, identityFailure);
    }

    private SignalServiceAddress resolveSignalServiceAddress(RecipientId recipientId) {
        final var address = account.getRecipientStore().resolveRecipientAddress(recipientId);
        if (address.uuid().isPresent()) {
            return address.toSignalServiceAddress();
        }

        // Address in recipient store doesn't have a uuid, this shouldn't happen
        // Try to retrieve the uuid from the server
        final var number = address.number().get();
        final ACI aci;
        try {
            aci = getRegisteredUser(number);
        } catch (IOException e) {
            logger.warn("Failed to get uuid for e164 number: {}", number, e);
            // Return SignalServiceAddress with unknown UUID
            return address.toSignalServiceAddress();
        }
        return resolveSignalServiceAddress(account.getRecipientStore().resolveRecipient(aci));
    }

    private Set<RecipientId> resolveRecipients(Collection<RecipientIdentifier.Single> recipients) throws IOException {
        final var recipientIds = new HashSet<RecipientId>(recipients.size());
        for (var number : recipients) {
            final var recipientId = resolveRecipient(number);
            recipientIds.add(recipientId);
        }
        return recipientIds;
    }

    private RecipientId resolveRecipient(final RecipientIdentifier.Single recipient) throws IOException {
        if (recipient instanceof RecipientIdentifier.Uuid uuidRecipient) {
            return account.getRecipientStore().resolveRecipient(ACI.from(uuidRecipient.uuid()));
        } else {
            final var number = ((RecipientIdentifier.Number) recipient).number();
            return account.getRecipientStore().resolveRecipient(number, () -> {
                try {
                    return getRegisteredUser(number);
                } catch (IOException e) {
                    return null;
                }
            });
        }
    }

    private RecipientId resolveRecipient(RecipientAddress address) {
        return account.getRecipientStore().resolveRecipient(address);
    }

    private RecipientId resolveRecipient(SignalServiceAddress address) {
        return account.getRecipientStore().resolveRecipient(address);
    }

    private RecipientId resolveRecipientTrusted(SignalServiceAddress address) {
        return account.getRecipientStore().resolveRecipientTrusted(address);
    }

    @Override
    public void close() throws IOException {
        Thread thread;
        synchronized (messageHandlers) {
            weakHandlers.clear();
            messageHandlers.clear();
            thread = receiveThread;
            receiveThread = null;
        }
        if (thread != null) {
            stopReceiveThread(thread);
        }
        executor.shutdown();

        dependencies.getSignalWebSocket().disconnect();

        synchronized (closedListeners) {
            closedListeners.forEach(Runnable::run);
            closedListeners.clear();
        }

        if (account != null) {
            account.close();
        }
        account = null;
    }
}
