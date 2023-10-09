package it.auties.whatsapp.util;

import static it.auties.whatsapp.util.Specification.Signal.CURRENT_VERSION;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageContainer;
import java.io.ByteArrayOutputStream;
import java.util.zip.Inflater;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BytesHelper
    implements JacksonProvider {

  public byte versionToBytes(int version) {
    return (byte) (version << 4 | CURRENT_VERSION);
  }

  public int bytesToVersion(byte version) {
    return Byte.toUnsignedInt(version) >> 4;
  }

  @SneakyThrows
  public byte[] deflate(byte[] compressed) {
    var decompressor = new Inflater();
    decompressor.setInput(compressed);
    var result = new ByteArrayOutputStream();
    var buffer = new byte[1024];
    while (!decompressor.finished()) {
      var count = decompressor.inflate(buffer);
      result.write(buffer, 0, count);
    }
    return result.toByteArray();
  }

  public byte[] messageToBytes(Message message) {
    return messageToBytes(MessageContainer.of(message));
  }

  @SneakyThrows
  public byte[] messageToBytes(MessageContainer container) {
    var padRandomByte = KeyHelper.header();
    var padding = Bytes.newBuffer(padRandomByte)
        .fill((byte) padRandomByte)
        .toByteArray();
    return Bytes.of(PROTOBUF.writeValueAsBytes(container))
        .append(padding)
        .toByteArray();
  }

  @SneakyThrows
  public MessageContainer bytesToMessage(byte[] bytes) {
    var message = Bytes.of(bytes)
        .cut(-bytes[bytes.length - 1])
        .toByteArray();
    return PROTOBUF.readMessage(message, MessageContainer.class);
  }

  public byte[] longToBytes(long number) {
    return Bytes.newBuffer()
        .appendUnsignedInt((int) number)
        .toByteArray();
  }

  public byte[] intToBytes(int input, int length) {
    var result = new byte[length];
    for (var i = length - 1; i >= 0; i--) {
      result[i] = (byte) (255 & input);
      input >>>= 8;
    }
    return result;
  }

  public int bytesToInt(byte[] bytes, int length) {
    var result = 0;
    for (var i = 0; i < length; i++) {
      result = 256 * result + Byte.toUnsignedInt(bytes[i]);
    }
    return result;
  }
}