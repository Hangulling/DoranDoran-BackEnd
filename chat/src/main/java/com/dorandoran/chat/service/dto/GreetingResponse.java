package com.dorandoran.chat.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GreetingResponse {
    private String botMessage;
    private String guideMessage;
}