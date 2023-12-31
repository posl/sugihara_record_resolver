package it.auties.whatsapp4j.utils;

import com.google.protobuf.ByteString;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappLocationCoordinates;
import it.auties.whatsapp4j.model.WhatsappMediaMessageType;
import it.auties.whatsapp4j.model.WhatsappProtobuf;
import it.auties.whatsapp4j.model.WhatsappUserMessage;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * A utility class used to build raw protobuf objects easily.
 * This class really shouldn't be this complicated.
 */
@UtilityClass
public class ProtobufUtils {
    private final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();

    public @NotNull WhatsappProtobuf.WebMessageInfo createMessageInfo(@NotNull WhatsappProtobuf.Message message, @NotNull String recipientJid){
        return WhatsappProtobuf.WebMessageInfo.newBuilder()
                .setMessage(message)
                .setKey(WhatsappProtobuf.MessageKey.newBuilder()
                        .setFromMe(true)
                        .setRemoteJid(recipientJid)
                        .setId(WhatsappUtils.randomId())
                        .build())
                .setMessageTimestamp(Instant.now().getEpochSecond())
                .setStatus(WhatsappProtobuf.WebMessageInfo.WebMessageInfoStatus.PENDING)
                .build();
    }

    public @NotNull WhatsappProtobuf.Message createTextMessage(@NotNull String text, WhatsappUserMessage quotedMessage, boolean forwarded){
        if (!forwarded && quotedMessage == null) {
            return WhatsappProtobuf.Message.newBuilder().setConversation(text).build();
        }

        return WhatsappProtobuf.Message.newBuilder()
                        .setExtendedTextMessage(WhatsappProtobuf.ExtendedTextMessage.newBuilder()
                                .setText(text)
                                .setContextInfo(createContextInfo(quotedMessage, forwarded))
                                .build())
                        .build();

    }

    public @NotNull WhatsappProtobuf.Message createImageMessage(byte @NotNull [] media, String rawMimeType, String caption, WhatsappUserMessage quotedMessage, boolean forwarded){
        var upload = CypherUtils.mediaEncrypt(MANAGER.mediaConnection(), media, WhatsappMediaMessageType.IMAGE);
        var message = WhatsappProtobuf.Message.newBuilder();
        var mimeType = Optional.ofNullable(rawMimeType).orElse(WhatsappMediaMessageType.IMAGE.defaultMimeType());

        var image = WhatsappProtobuf.ImageMessage.newBuilder()
                .setFileSha256(ByteString.copyFrom(upload.fileSha256()))
                .setFileEncSha256(ByteString.copyFrom(upload.fileEncSha256()))
                .setMediaKey(ByteString.copyFrom(upload.mediaKey().data()))
                .setMediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
                .setUrl(upload.url())
                .setDirectPath(upload.directPath())
                .setFileLength(media.length)
                .setContextInfo(createContextInfo(quotedMessage, forwarded))
                .setMimetype(mimeType)
                .setScansSidecar(ByteString.copyFrom(upload.sidecar()))
                .setJpegThumbnail(ByteString.copyFrom(media));

        if(caption != null) image.setCaption(caption);

        return message.setImageMessage(image).build();
    }

    public @NotNull WhatsappProtobuf.Message createDocumentMessage(byte @NotNull [] media, String rawMimeType, String rawTitle, Integer pages, WhatsappUserMessage quotedMessage, boolean forwarded){
        var upload = CypherUtils.mediaEncrypt(MANAGER.mediaConnection(), media, WhatsappMediaMessageType.DOCUMENT);
        var message = WhatsappProtobuf.Message.newBuilder();
        var mimeType = Optional.ofNullable(rawMimeType).orElse(WhatsappMediaMessageType.DOCUMENT.defaultMimeType());
        var title = Optional.ofNullable(rawTitle).orElse("Document");

        var document = WhatsappProtobuf.DocumentMessage.newBuilder()
                .setFileSha256(ByteString.copyFrom(upload.fileSha256()))
                .setFileEncSha256(ByteString.copyFrom(upload.fileEncSha256()))
                .setMediaKey(ByteString.copyFrom(upload.mediaKey().data()))
                .setMediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
                .setUrl(upload.url())
                .setDirectPath(upload.directPath())
                .setFileLength(media.length)
                .setContextInfo(createContextInfo(quotedMessage, forwarded))
                .setMimetype(mimeType)
                .setTitle(title)
                .setFileName(title);

        if(pages != null) document.setPageCount(pages);

        return message.setDocumentMessage(document).build();
    }

    public @NotNull WhatsappProtobuf.Message createAudioMessage(byte @NotNull [] media, String rawMimeType, boolean voiceMessage, WhatsappUserMessage quotedMessage, boolean forwarded){
        var upload = CypherUtils.mediaEncrypt(MANAGER.mediaConnection(), media, WhatsappMediaMessageType.AUDIO);
        var message = WhatsappProtobuf.Message.newBuilder();
        var mimeType = Optional.ofNullable(rawMimeType).orElse(WhatsappMediaMessageType.AUDIO.defaultMimeType());

        var audio = WhatsappProtobuf.AudioMessage.newBuilder()
                .setFileSha256(ByteString.copyFrom(upload.fileSha256()))
                .setFileEncSha256(ByteString.copyFrom(upload.fileEncSha256()))
                .setMediaKey(ByteString.copyFrom(upload.mediaKey().data()))
                .setMediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
                .setUrl(upload.url())
                .setDirectPath(upload.directPath())
                .setFileLength(media.length)
                .setContextInfo(createContextInfo(quotedMessage, forwarded))
                .setMimetype(mimeType)
                .setStreamingSidecar(ByteString.copyFrom(upload.sidecar()))
                .setPtt(voiceMessage);

        return message.setAudioMessage(audio).build();
    }

    @SneakyThrows
    public @NotNull WhatsappProtobuf.Message createVideoMessage(byte @NotNull [] media, String rawMimeType, WhatsappProtobuf.VideoMessage.VideoMessageAttribution attribution, String caption, WhatsappUserMessage quotedMessage, boolean forwarded){
        var upload = CypherUtils.mediaEncrypt(MANAGER.mediaConnection(), media, WhatsappMediaMessageType.VIDEO);
        var message = WhatsappProtobuf.Message.newBuilder();
        var mimeType = Optional.ofNullable(rawMimeType).orElse(WhatsappMediaMessageType.VIDEO.defaultMimeType());

        var video = WhatsappProtobuf.VideoMessage.newBuilder()
                .setFileSha256(ByteString.copyFrom(upload.fileSha256()))
                .setFileEncSha256(ByteString.copyFrom(upload.fileEncSha256()))
                .setMediaKey(ByteString.copyFrom(upload.mediaKey().data()))
                .setMediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
                .setUrl(upload.url())
                .setDirectPath(upload.directPath())
                .setFileLength(media.length)
                .setContextInfo(createContextInfo(quotedMessage, forwarded))
                .setMimetype(mimeType)
                .setStreamingSidecar(ByteString.copyFrom(upload.sidecar()));

        if(caption != null) video.setCaption(caption);
        if(attribution != null){
            Validate.isTrue(Optional.ofNullable(URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(media))).map(guess -> !guess.equals("image/gif")).orElse(true) && !mimeType.equals("image/gif"), "Cannot create a WhatsappGifMessage with mime type image/gif: gif messages on whatsapp are videos played as gifs");
            video.setGifAttribution(attribution);
            video.setGifPlayback(true);
        }

        return message.setVideoMessage(video).build();
    }

    public @NotNull WhatsappProtobuf.Message createStickerMessage(byte @NotNull [] media, WhatsappUserMessage quotedMessage, boolean forwarded){
        var upload = CypherUtils.mediaEncrypt(MANAGER.mediaConnection(), media, WhatsappMediaMessageType.STICKER);
        var message = WhatsappProtobuf.Message.newBuilder();

        var sticker = WhatsappProtobuf.StickerMessage.newBuilder()
                .setFileSha256(ByteString.copyFrom(upload.fileSha256()))
                .setFileEncSha256(ByteString.copyFrom(upload.fileEncSha256()))
                .setMediaKey(ByteString.copyFrom(upload.mediaKey().data()))
                .setMediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
                .setUrl(upload.url())
                .setDirectPath(upload.directPath())
                .setFileLength(media.length)
                .setContextInfo(createContextInfo(quotedMessage, forwarded))
                .setFirstFrameSidecar(ByteString.copyFrom(upload.sidecar()))
                .setFirstFrameLength(upload.sidecar().length);

        return message.setStickerMessage(sticker).build();
    }

    public @NotNull WhatsappProtobuf.Message createLocationMessage(@NotNull WhatsappLocationCoordinates coordinates, String caption, byte[] thumbnail, Integer accuracy, Float speed, WhatsappUserMessage quotedMessage, boolean forwarded) {
        var location = WhatsappProtobuf.LocationMessage.newBuilder()
                .setContextInfo(createContextInfo(quotedMessage, forwarded))
                .setDegreesLatitude(coordinates.latitude())
                .setComment(caption)
                .setDegreesLongitude(coordinates.longitude());

        if(thumbnail != null) location.setJpegThumbnail(ByteString.copyFrom(thumbnail));
        if(accuracy != null) location.setAccuracyInMeters(accuracy);
        if(speed != null) location.setSpeedInMps(speed);

        return WhatsappProtobuf.Message.newBuilder().setLocationMessage(location).build();
    }

    public @NotNull WhatsappProtobuf.Message createGroupInviteMessage(@NotNull String jid, @NotNull String name, @NotNull String code, ZonedDateTime expiration, String caption, byte[] thumbnail, WhatsappUserMessage quotedMessage, boolean forwarded) {
        var invite = WhatsappProtobuf.GroupInviteMessage.newBuilder()
                .setContextInfo(createContextInfo(quotedMessage, forwarded))
                .setGroupJid(jid)
                .setGroupName(name)
                .setInviteCode(code);

        if(thumbnail != null) invite.setJpegThumbnail(ByteString.copyFrom(thumbnail));
        if(caption != null) invite.setCaption(caption);
        if(expiration != null) invite.setInviteExpiration(expiration.toEpochSecond());

        return WhatsappProtobuf.Message.newBuilder().setGroupInviteMessage(invite).build();
    }

    public @NotNull WhatsappProtobuf.Message createContactMessage(@NotNull List<String> sharedContacts) {
        if(sharedContacts.size() == 1){
            return WhatsappProtobuf.Message.newBuilder()
                    .setContactMessage(toContactMessage(sharedContacts.get(0)))
                    .build();
        }

        var contactsArrayMessageBuilder = WhatsappProtobuf.ContactsArrayMessage.newBuilder();
        for(var x = 0; x < sharedContacts.size(); x++){
            contactsArrayMessageBuilder.setContacts(x, toContactMessage(sharedContacts.get(x)));
        }

        return WhatsappProtobuf.Message.newBuilder()
                .setContactsArrayMessage(contactsArrayMessageBuilder.build())
                .build();
    }

    private @NotNull WhatsappProtobuf.ContactMessage toContactMessage(@NotNull String vcard){
        return WhatsappProtobuf.ContactMessage.newBuilder()
                .setVcard(vcard)
                .build();
    }

    private @NotNull WhatsappProtobuf.ContextInfo createContextInfo(WhatsappUserMessage quotedMessage, boolean forwarded){
        var builder =  WhatsappProtobuf.ContextInfo.newBuilder().setIsForwarded(forwarded);
        if(quotedMessage == null){
            return builder.build();
        }

        return builder.setQuotedMessage(quotedMessage.info().getMessage())
                .setParticipant(quotedMessage.senderJid())
                .setStanzaId(quotedMessage.info().getKey().getId())
                .setRemoteJid(quotedMessage.info().getKey().getRemoteJid())
                .setIsForwarded(forwarded)
                .build();
    }
}
