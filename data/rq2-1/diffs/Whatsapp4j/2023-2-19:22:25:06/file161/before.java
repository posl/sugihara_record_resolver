package it.auties.whatsapp.model.button;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.UINT32;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that represents a template for a button
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("TemplateButton")
public class ButtonTemplate
    implements ProtobufMessage {

  @ProtobufProperty(index = 4, type = UINT32)
  private int index;

  @ProtobufProperty(index = 1, type = MESSAGE, implementation = QuickReplyButton.class)
  private QuickReplyButton quickReplyButton;

  @ProtobufProperty(index = 2, type = MESSAGE, implementation = URLButton.class)
  private URLButton urlButton;

  @ProtobufProperty(index = 3, type = MESSAGE, implementation = CallButton.class)
  private CallButton callButton;

  /**
   * Constructs a new template from a quick reply
   *
   * @param quickReplyButton the non-null quick reply
   * @return a non-null button template
   */
  public static ButtonTemplate of(@NonNull QuickReplyButton quickReplyButton) {
    return ButtonTemplate.builder()
        .quickReplyButton(quickReplyButton)
        .build();
  }

  /**
   * Constructs a new template from an url button
   *
   * @param urlButton the non-null url button
   * @return a non-null button template
   */
  public static ButtonTemplate of(@NonNull URLButton urlButton) {
    return ButtonTemplate.builder()
        .urlButton(urlButton)
        .build();
  }

  /**
   * Constructs a new template from a call button
   *
   * @param callButton the non-null call button
   * @return a non-null button template
   */
  public static ButtonTemplate of(@NonNull CallButton callButton) {
    return ButtonTemplate.builder()
        .callButton(callButton)
        .build();
  }

  /**
   * Returns the type of button that this message wraps
   *
   * @return a non-null button type
   */
  public ButtonType buttonType() {
    if (quickReplyButton != null) {
      return ButtonType.QUICK_REPLY_BUTTON;
    }
    if (urlButton != null) {
      return ButtonType.URL_BUTTON;
    }
    if (callButton != null) {
      return ButtonType.CALL_BUTTON;
    }
    return ButtonType.NONE;
  }

  /**
   * The constants of this enumerated type describe the various types of buttons that a template can
   * wrap
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum ButtonType
      implements ProtobufMessage {

    /**
     * No button
     */
    NONE(0),
    /**
     * Quick reply button
     */
    QUICK_REPLY_BUTTON(1),
    /**
     * Url button
     */
    URL_BUTTON(2),
    /**
     * Call button
     */
    CALL_BUTTON(3);
    @Getter
    private final int index;

    @JsonCreator
    public static ButtonType of(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(ButtonType.NONE);
    }
  }
}