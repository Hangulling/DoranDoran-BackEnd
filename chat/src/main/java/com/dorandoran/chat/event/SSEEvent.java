package com.dorandoran.chat.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SSEEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID chatroomId;
  private String eventType;
  private Object data;
  private Long timestamp;
}