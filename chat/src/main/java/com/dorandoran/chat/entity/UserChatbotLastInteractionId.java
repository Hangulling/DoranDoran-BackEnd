package com.dorandoran.chat.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * UserChatbotLastInteraction의 Composite Primary Key
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserChatbotLastInteractionId implements Serializable {
    private UUID userId;
    private UUID chatbotId;
}

