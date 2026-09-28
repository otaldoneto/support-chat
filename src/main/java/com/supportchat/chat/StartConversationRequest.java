package com.supportchat.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StartConversationRequest(@NotBlank @Size(max = 100) String customerName) {
}
