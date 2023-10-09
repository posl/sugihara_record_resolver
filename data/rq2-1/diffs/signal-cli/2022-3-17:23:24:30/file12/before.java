package org.asamk.signal.manager.helper;

import org.asamk.signal.manager.SignalDependencies;
import org.asamk.signal.manager.config.ServiceConfig;
import org.asamk.signal.manager.storage.SignalAccount;
import org.asamk.signal.manager.storage.recipients.Profile;
import org.asamk.signal.manager.storage.recipients.RecipientAddress;
import org.asamk.signal.manager.storage.recipients.RecipientId;
import org.asamk.signal.manager.util.IOUtils;
import org.asamk.signal.manager.util.ProfileUtils;
import org.asamk.signal.manager.util.Utils;
import org.signal.zkgroup.profiles.ProfileKey;
import org.signal.zkgroup.profiles.ProfileKeyCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.crypto.UnidentifiedAccess;
import org.whispersystems.signalservice.api.profiles.ProfileAndCredential;
import org.whispersystems.signalservice.api.profiles.SignalServiceProfile;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.NotFoundException;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.api.services.ProfileService;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public final class ProfileHelper {

    private final static Logger logger = LoggerFactory.getLogger(ProfileHelper.class);

    private final SignalAccount account;
    private final SignalDependencies dependencies;
    private final Context context;

    public ProfileHelper(final Context context) {
        this.account = context.getAccount();
        this.dependencies = context.getDependencies();
        this.context = context;
    }

    public Profile getRecipientProfile(RecipientId recipientId) {
        return getRecipientProfile(recipientId, false);
    }

    public void refreshRecipientProfile(RecipientId recipientId) {
        getRecipientProfile(recipientId, true);
    }

    public List<ProfileKeyCredential> getRecipientProfileKeyCredential(List<RecipientId> recipientIds) {
        try {
            account.getRecipientStore().setBulkUpdating(true);
            final var profileFetches = Flowable.fromIterable(recipientIds)
                    .filter(recipientId -> account.getProfileStore().getProfileKeyCredential(recipientId) == null)
                    .map(recipientId -> retrieveProfile(recipientId,
                            SignalServiceProfile.RequestType.PROFILE_AND_CREDENTIAL).onErrorComplete());
            Maybe.merge(profileFetches, 10).blockingSubscribe();
        } finally {
            account.getRecipientStore().setBulkUpdating(false);
        }

        return recipientIds.stream().map(r -> account.getProfileStore().getProfileKeyCredential(r)).toList();
    }

    public ProfileKeyCredential getRecipientProfileKeyCredential(RecipientId recipientId) {
        var profileKeyCredential = account.getProfileStore().getProfileKeyCredential(recipientId);
        if (profileKeyCredential != null) {
            return profileKeyCredential;
        }

        try {
            blockingGetProfile(retrieveProfile(recipientId, SignalServiceProfile.RequestType.PROFILE_AND_CREDENTIAL));
        } catch (IOException e) {
            logger.warn("Failed to retrieve profile key credential, ignoring: {}", e.getMessage());
            return null;
        }

        return account.getProfileStore().getProfileKeyCredential(recipientId);
    }

    /**
     * @param givenName  if null, the previous givenName will be kept
     * @param familyName if null, the previous familyName will be kept
     * @param about      if null, the previous about text will be kept
     * @param aboutEmoji if null, the previous about emoji will be kept
     * @param avatar     if avatar is null the image from the local avatar store is used (if present),
     */
    public void setProfile(
            String givenName, final String familyName, String about, String aboutEmoji, Optional<File> avatar
    ) throws IOException {
        setProfile(true, givenName, familyName, about, aboutEmoji, avatar);
    }

    public void setProfile(
            boolean uploadProfile,
            String givenName,
            final String familyName,
            String about,
            String aboutEmoji,
            Optional<File> avatar
    ) throws IOException {
        var profile = getRecipientProfile(account.getSelfRecipientId());
        var builder = profile == null ? Profile.newBuilder() : Profile.newBuilder(profile);
        if (givenName != null) {
            builder.withGivenName(givenName);
        }
        if (familyName != null) {
            builder.withFamilyName(familyName);
        }
        if (about != null) {
            builder.withAbout(about);
        }
        if (aboutEmoji != null) {
            builder.withAboutEmoji(aboutEmoji);
        }
        var newProfile = builder.build();

        if (uploadProfile) {
            try (final var streamDetails = avatar == null
                    ? context.getAvatarStore()
                    .retrieveProfileAvatar(account.getSelfRecipientAddress())
                    : avatar.isPresent() ? Utils.createStreamDetailsFromFile(avatar.get()) : null) {
                final var avatarPath = dependencies.getAccountManager()
                        .setVersionedProfile(account.getAci(),
                                account.getProfileKey(),
                                newProfile.getInternalServiceName(),
                                newProfile.getAbout() == null ? "" : newProfile.getAbout(),
                                newProfile.getAboutEmoji() == null ? "" : newProfile.getAboutEmoji(),
                                Optional.absent(),
                                streamDetails,
                                List.of(/* TODO */));
                builder.withAvatarUrlPath(avatarPath.orNull());
                newProfile = builder.build();
            }
        }

        if (avatar != null) {
            if (avatar.isPresent()) {
                context.getAvatarStore()
                        .storeProfileAvatar(account.getSelfRecipientAddress(),
                                outputStream -> IOUtils.copyFileToStream(avatar.get(), outputStream));
            } else {
                context.getAvatarStore().deleteProfileAvatar(account.getSelfRecipientAddress());
            }
        }
        account.getProfileStore().storeProfile(account.getSelfRecipientId(), newProfile);
    }

    public List<Profile> getRecipientProfile(List<RecipientId> recipientIds) {
        try {
            account.getRecipientStore().setBulkUpdating(true);
            final var profileFetches = Flowable.fromIterable(recipientIds)
                    .filter(recipientId -> isProfileRefreshRequired(account.getProfileStore().getProfile(recipientId)))
                    .map(recipientId -> retrieveProfile(recipientId,
                            SignalServiceProfile.RequestType.PROFILE).onErrorComplete());
            Maybe.merge(profileFetches, 10).blockingSubscribe();
        } finally {
            account.getRecipientStore().setBulkUpdating(false);
        }

        return recipientIds.stream().map(r -> account.getProfileStore().getProfile(r)).toList();
    }

    private Profile getRecipientProfile(RecipientId recipientId, boolean force) {
        var profile = account.getProfileStore().getProfile(recipientId);

        if (!force && !isProfileRefreshRequired(profile)) {
            return profile;
        }

        try {
            blockingGetProfile(retrieveProfile(recipientId, SignalServiceProfile.RequestType.PROFILE));
        } catch (IOException e) {
            logger.warn("Failed to retrieve profile, ignoring: {}", e.getMessage());
        }

        return account.getProfileStore().getProfile(recipientId);
    }

    private boolean isProfileRefreshRequired(final Profile profile) {
        if (profile == null) {
            return true;
        }
        // Profiles are cached for 6h before retrieving them again, unless forced
        final var now = System.currentTimeMillis();
        return now - profile.getLastUpdateTimestamp() >= 6 * 60 * 60 * 1000;
    }

    private SignalServiceProfile retrieveProfileSync(String username) throws IOException {
        final var locale = Utils.getDefaultLocale(Locale.US);
        return dependencies.getMessageReceiver().retrieveProfileByUsername(username, Optional.absent(), locale);
    }

    private Profile decryptProfileAndDownloadAvatar(
            final RecipientId recipientId, final ProfileKey profileKey, final SignalServiceProfile encryptedProfile
    ) {
        final var avatarPath = encryptedProfile.getAvatar();
        downloadProfileAvatar(recipientId, avatarPath, profileKey);

        return ProfileUtils.decryptProfile(profileKey, encryptedProfile);
    }

    public void downloadProfileAvatar(
            final RecipientId recipientId, final String avatarPath, final ProfileKey profileKey
    ) {
        var profile = account.getProfileStore().getProfile(recipientId);
        if (profile == null || !Objects.equals(avatarPath, profile.getAvatarUrlPath())) {
            logger.trace("Downloading profile avatar for {}", recipientId);
            downloadProfileAvatar(account.getRecipientStore().resolveRecipientAddress(recipientId),
                    avatarPath,
                    profileKey);
            var builder = profile == null ? Profile.newBuilder() : Profile.newBuilder(profile);
            account.getProfileStore().storeProfile(recipientId, builder.withAvatarUrlPath(avatarPath).build());
        }
    }

    private ProfileAndCredential blockingGetProfile(Single<ProfileAndCredential> profile) throws IOException {
        try {
            return profile.blockingGet();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof PushNetworkException) {
                throw (PushNetworkException) e.getCause();
            } else if (e.getCause() instanceof NotFoundException) {
                throw (NotFoundException) e.getCause();
            } else {
                throw new IOException(e);
            }
        }
    }

    private Single<ProfileAndCredential> retrieveProfile(
            RecipientId recipientId, SignalServiceProfile.RequestType requestType
    ) {
        var unidentifiedAccess = getUnidentifiedAccess(recipientId);
        var profileKey = Optional.fromNullable(account.getProfileStore().getProfileKey(recipientId));

        logger.trace("Retrieving profile for {} {}",
                recipientId,
                profileKey.isPresent() ? "with profile key" : "without profile key");
        final var address = context.getRecipientHelper().resolveSignalServiceAddress(recipientId);
        return retrieveProfile(address, profileKey, unidentifiedAccess, requestType).doOnSuccess(p -> {
            logger.trace("Got new profile for {}", recipientId);
            final var encryptedProfile = p.getProfile();

            if (requestType == SignalServiceProfile.RequestType.PROFILE_AND_CREDENTIAL
                    || account.getProfileStore().getProfileKeyCredential(recipientId) == null) {
                logger.trace("Storing profile credential");
                final var profileKeyCredential = p.getProfileKeyCredential().orNull();
                account.getProfileStore().storeProfileKeyCredential(recipientId, profileKeyCredential);
            }

            final var profile = account.getProfileStore().getProfile(recipientId);

            Profile newProfile = null;
            if (profileKey.isPresent()) {
                logger.trace("Decrypting profile");
                newProfile = decryptProfileAndDownloadAvatar(recipientId, profileKey.get(), encryptedProfile);
            }

            if (newProfile == null) {
                newProfile = (
                        profile == null ? Profile.newBuilder() : Profile.newBuilder(profile)
                ).withLastUpdateTimestamp(System.currentTimeMillis())
                        .withUnidentifiedAccessMode(ProfileUtils.getUnidentifiedAccessMode(encryptedProfile, null))
                        .withCapabilities(ProfileUtils.getCapabilities(encryptedProfile))
                        .build();
            }

            try {
                logger.trace("Storing identity");
                final var identityKey = new IdentityKey(Base64.getDecoder().decode(encryptedProfile.getIdentityKey()));
                account.getIdentityKeyStore().saveIdentity(recipientId, identityKey, new Date());
            } catch (InvalidKeyException ignored) {
                logger.warn("Got invalid identity key in profile for {}",
                        context.getRecipientHelper().resolveSignalServiceAddress(recipientId).getIdentifier());
            }

            logger.trace("Storing profile");
            account.getProfileStore().storeProfile(recipientId, newProfile);

            logger.trace("Done handling retrieved profile");
        }).doOnError(e -> {
            logger.warn("Failed to retrieve profile, ignoring: {}", e.getMessage());
            final var profile = account.getProfileStore().getProfile(recipientId);
            final var newProfile = (
                    profile == null ? Profile.newBuilder() : Profile.newBuilder(profile)
            ).withLastUpdateTimestamp(System.currentTimeMillis())
                    .withUnidentifiedAccessMode(Profile.UnidentifiedAccessMode.UNKNOWN)
                    .withCapabilities(Set.of())
                    .build();

            account.getProfileStore().storeProfile(recipientId, newProfile);
        });
    }

    private Single<ProfileAndCredential> retrieveProfile(
            SignalServiceAddress address,
            Optional<ProfileKey> profileKey,
            Optional<UnidentifiedAccess> unidentifiedAccess,
            SignalServiceProfile.RequestType requestType
    ) {
        final var profileService = dependencies.getProfileService();
        final var locale = Utils.getDefaultLocale(Locale.US);

        return profileService.getProfile(address, profileKey, unidentifiedAccess, requestType, locale).map(pair -> {
            var processor = new ProfileService.ProfileResponseProcessor(pair);
            if (processor.hasResult()) {
                return processor.getResult();
            } else if (processor.notFound()) {
                throw new NotFoundException("Profile not found");
            } else {
                throw pair.getExecutionError()
                        .or(pair.getApplicationError())
                        .or(new IOException("Unknown error while retrieving profile"));
            }
        });
    }

    private void downloadProfileAvatar(
            RecipientAddress address, String avatarPath, ProfileKey profileKey
    ) {
        if (avatarPath == null) {
            try {
                context.getAvatarStore().deleteProfileAvatar(address);
            } catch (IOException e) {
                logger.warn("Failed to delete local profile avatar, ignoring: {}", e.getMessage());
            }
            return;
        }

        try {
            context.getAvatarStore()
                    .storeProfileAvatar(address,
                            outputStream -> retrieveProfileAvatar(avatarPath, profileKey, outputStream));
        } catch (Throwable e) {
            logger.warn("Failed to download profile avatar, ignoring: {}", e.getMessage());
        }
    }

    private void retrieveProfileAvatar(
            String avatarPath, ProfileKey profileKey, OutputStream outputStream
    ) throws IOException {
        var tmpFile = IOUtils.createTempFile();
        try (var input = dependencies.getMessageReceiver()
                .retrieveProfileAvatar(avatarPath,
                        tmpFile,
                        profileKey,
                        ServiceConfig.AVATAR_DOWNLOAD_FAILSAFE_MAX_SIZE)) {
            // Use larger buffer size to prevent AssertionError: Need: 12272 but only have: 8192 ...
            IOUtils.copyStream(input, outputStream, (int) ServiceConfig.AVATAR_DOWNLOAD_FAILSAFE_MAX_SIZE);
        } finally {
            try {
                Files.delete(tmpFile.toPath());
            } catch (IOException e) {
                logger.warn("Failed to delete received profile avatar temp file “{}”, ignoring: {}",
                        tmpFile,
                        e.getMessage());
            }
        }
    }

    private Optional<UnidentifiedAccess> getUnidentifiedAccess(RecipientId recipientId) {
        var unidentifiedAccess = context.getUnidentifiedAccessHelper().getAccessFor(recipientId, true);

        if (unidentifiedAccess.isPresent()) {
            return unidentifiedAccess.get().getTargetUnidentifiedAccess();
        }

        return Optional.absent();
    }
}
