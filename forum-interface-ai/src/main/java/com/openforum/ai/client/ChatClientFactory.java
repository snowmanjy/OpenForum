package com.openforum.ai.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
public class ChatClientFactory {

    public ChatClient createClient(String decryptedApiKey, String modelName) {
        OpenAiApi openAiApi = new OpenAiApi(decryptedApiKey);

        OpenAiChatModel chatModel = new OpenAiChatModel(
                openAiApi,
                OpenAiChatOptions.builder()
                        .withModel(modelName != null ? modelName : "gpt-4")
                        .build());

        return ChatClient.builder(chatModel).build();
    }
}
