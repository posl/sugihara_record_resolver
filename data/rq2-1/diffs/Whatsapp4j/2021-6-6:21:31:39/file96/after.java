package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HSMCurrency {
  @JsonProperty(value = "2")
  private long amount1000;

  @JsonProperty(value = "1")
  private String currencyCode;
}
