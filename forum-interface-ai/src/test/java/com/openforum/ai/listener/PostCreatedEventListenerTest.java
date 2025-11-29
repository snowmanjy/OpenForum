package com.openforum.ai.listener;

import com.openforum.ai.client.ChatClientFactory;
import com.openforum.ai.config.TenantAiConfig;
import com.openforum.ai.service.AiMemberService;
import com.openforum.ai.service.TenantAiConfigService;
import com.openforum.application.service.PostService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.aggregate.ThreadStatus;
import com.openforum.domain.events.PostCreatedEvent;
import com.openforum.domain.repository.PostRepository;
import com.openforum.domain.repository.ThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostCreatedEventListenerTest {

        @Mock
        private ChatClientFactory chatClientFactory;
        @Mock
        private PostRepository postRepository;
        @Mock
        private ThreadRepository threadRepository;
        @Mock
        private TenantAiConfigService tenantAiConfigService;
        @Mock
        private PostService postService;
        @Mock
        private AiMemberService aiMemberService;
        @Mock
        private TextEncryptor textEncryptor;
        @Mock
        private ChatClient chatClient;
        @Mock
        private ChatClientRequestSpec requestSpec;
        @Mock
        private ChatClientRequestSpec systemSpec;
        @Mock
        private CallResponseSpec callSpec;

        private PostCreatedEventListener listener;

        @BeforeEach
        void setUp() {
                listener = new PostCreatedEventListener(
                                chatClientFactory,
                                postRepository,
                                threadRepository,
                                tenantAiConfigService,
                                postService,
                                aiMemberService,
                                textEncryptor);
        }

        @Test
        void shouldIgnoreBotPosts() {
                // Given
                PostCreatedEvent event = new PostCreatedEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "Bot content",
                                Instant.now(),
                                true, // isBot = true
                                List.of());

                // When
                listener.onPostCreated(event);

                // Then - no interactions with any dependencies
                verifyNoInteractions(chatClientFactory, postRepository, threadRepository,
                                tenantAiConfigService, postService, aiMemberService, textEncryptor);
        }

        @Test
        void shouldExitWhenAiNotEnabled() {
                // Given
                UUID threadId = UUID.randomUUID();
                PostCreatedEvent event = new PostCreatedEvent(
                                UUID.randomUUID(),
                                threadId,
                                UUID.randomUUID(),
                                "User content",
                                Instant.now(),
                                false,
                                List.of());

                Thread thread = Thread.builder()
                                .id(threadId)
                                .tenantId("tenant123")
                                .authorId(UUID.randomUUID())
                                .title("Test Thread")
                                .status(ThreadStatus.OPEN)
                                .build();

                TenantAiConfig config = new TenantAiConfig(false, "prompt", "context", "key");

                when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
                when(tenantAiConfigService.getConfig("tenant123")).thenReturn(Optional.of(config));

                // When
                listener.onPostCreated(event);

                // Then
                verify(threadRepository).findById(threadId);
                verify(tenantAiConfigService).getConfig("tenant123");
                verifyNoInteractions(chatClientFactory, postService);
        }

        @Test
        void shouldExitWhenApiKeyIsBlank() {
                // Given
                UUID threadId = UUID.randomUUID();
                PostCreatedEvent event = new PostCreatedEvent(
                                UUID.randomUUID(),
                                threadId,
                                UUID.randomUUID(),
                                "User content",
                                Instant.now(),
                                false,
                                List.of());

                Thread thread = Thread.builder()
                                .id(threadId)
                                .tenantId("tenant123")
                                .authorId(UUID.randomUUID())
                                .title("Test Thread")
                                .status(ThreadStatus.OPEN)
                                .build();

                TenantAiConfig config = new TenantAiConfig(true, "prompt", "context", "");

                when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
                when(tenantAiConfigService.getConfig("tenant123")).thenReturn(Optional.of(config));

                // When
                listener.onPostCreated(event);

                // Then
                verifyNoInteractions(chatClientFactory, postService);
        }

        @Test
        void shouldGenerateAiReplyForValidPost() {
                // Given
                UUID threadId = UUID.randomUUID();
                UUID postId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();

                PostCreatedEvent event = new PostCreatedEvent(
                                postId,
                                threadId,
                                authorId,
                                "User question",
                                Instant.now(),
                                false,
                                List.of());

                Thread thread = Thread.builder()
                                .id(threadId)
                                .tenantId("tenant123")
                                .authorId(UUID.randomUUID())
                                .title("Test Thread")
                                .status(ThreadStatus.OPEN)
                                .build();

                TenantAiConfig config = new TenantAiConfig(
                                true,
                                "You are helpful",
                                "Product docs",
                                "encrypted_key");

                Post recentPost = Post.builder()
                                .id(UUID.randomUUID())
                                .threadId(threadId)
                                .authorId(authorId)
                                .content("Previous message")
                                .version(1L)
                                .build();

                Member aiMember = Member.create("ai-assistant", "ai@forum.local", "AI Assistant", true);

                // Setup mocks
                when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
                when(tenantAiConfigService.getConfig("tenant123")).thenReturn(Optional.of(config));
                when(textEncryptor.decrypt("encrypted_key")).thenReturn("decrypted_api_key");
                when(postRepository.findByThreadId(threadId, 10)).thenReturn(List.of(recentPost));
                when(aiMemberService.getOrCreateAiMember("tenant123")).thenReturn(aiMember);

                // Mock ChatClient chain
                when(chatClientFactory.createClient("decrypted_api_key", "gpt-4")).thenReturn(chatClient);
                when(chatClient.prompt()).thenReturn(requestSpec);
                when(requestSpec.system(anyString())).thenReturn(systemSpec);
                when(systemSpec.user(anyString())).thenReturn(requestSpec);
                when(requestSpec.call()).thenReturn(callSpec);
                when(callSpec.content()).thenReturn("AI generated response");

                // When
                listener.onPostCreated(event);

                // Then
                verify(chatClientFactory).createClient("decrypted_api_key", "gpt-4");
                verify(postService).createPost(
                                eq(threadId),
                                eq(aiMember.getId()),
                                eq("AI generated response"),
                                eq(postId),
                                eq(Map.of()),
                                eq(List.of()));
        }

        @Test
        void shouldHandleDecryptionFailureGracefully() {
                // Given
                UUID threadId = UUID.randomUUID();
                PostCreatedEvent event = new PostCreatedEvent(
                                UUID.randomUUID(),
                                threadId,
                                UUID.randomUUID(),
                                "User question",
                                Instant.now(),
                                false,
                                List.of());

                Thread thread = Thread.builder()
                                .id(threadId)
                                .tenantId("tenant123")
                                .authorId(UUID.randomUUID())
                                .title("Test Thread")
                                .status(ThreadStatus.OPEN)
                                .build();

                TenantAiConfig config = new TenantAiConfig(true, "prompt", "context", "encrypted_key");

                when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
                when(tenantAiConfigService.getConfig("tenant123")).thenReturn(Optional.of(config));
                when(textEncryptor.decrypt("encrypted_key")).thenThrow(new IllegalStateException("Decryption failed"));

                // When
                listener.onPostCreated(event);

                // Then - should not throw, just log error and exit
                verify(textEncryptor).decrypt("encrypted_key");
                verifyNoInteractions(chatClientFactory, postService); // Should not proceed after decryption failure
        }

        @Test
        void shouldHandleChatClientCreationFailureGracefully() {
                // Given
                UUID threadId = UUID.randomUUID();
                PostCreatedEvent event = new PostCreatedEvent(
                                UUID.randomUUID(),
                                threadId,
                                UUID.randomUUID(),
                                "User question",
                                Instant.now(),
                                false,
                                List.of());

                Thread thread = Thread.builder()
                                .id(threadId)
                                .tenantId("tenant123")
                                .authorId(UUID.randomUUID())
                                .title("Test Thread")
                                .status(ThreadStatus.OPEN)
                                .build();

                TenantAiConfig config = new TenantAiConfig(true, "prompt", "context", "encrypted_key");

                when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
                when(tenantAiConfigService.getConfig("tenant123")).thenReturn(Optional.of(config));
                when(textEncryptor.decrypt("encrypted_key")).thenReturn("decrypted_key");
                when(postRepository.findByThreadId(threadId, 10)).thenReturn(List.of());
                when(chatClientFactory.createClient("decrypted_key", "gpt-4"))
                                .thenThrow(new RuntimeException("ChatClient creation failed"));

                // When
                listener.onPostCreated(event);

                // Then - should catch exception and not block
                verify(chatClientFactory).createClient("decrypted_key", "gpt-4");
                verifyNoInteractions(postService);
        }

        @Test
        void shouldHandleLlmCallFailureGracefully() {
                // Given
                UUID threadId = UUID.randomUUID();
                PostCreatedEvent event = new PostCreatedEvent(
                                UUID.randomUUID(),
                                threadId,
                                UUID.randomUUID(),
                                "User question",
                                Instant.now(),
                                false,
                                List.of());

                Thread thread = Thread.builder()
                                .id(threadId)
                                .tenantId("tenant123")
                                .authorId(UUID.randomUUID())
                                .title("Test Thread")
                                .status(ThreadStatus.OPEN)
                                .build();

                TenantAiConfig config = new TenantAiConfig(true, "prompt", "context", "encrypted_key");

                when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
                when(tenantAiConfigService.getConfig("tenant123")).thenReturn(Optional.of(config));
                when(textEncryptor.decrypt("encrypted_key")).thenReturn("decrypted_key");
                when(postRepository.findByThreadId(threadId, 10)).thenReturn(List.of());
                when(chatClientFactory.createClient("decrypted_key", "gpt-4")).thenReturn(chatClient);
                when(chatClient.prompt()).thenReturn(requestSpec);
                when(requestSpec.system(anyString())).thenReturn(systemSpec);
                when(systemSpec.user(anyString())).thenReturn(requestSpec);
                when(requestSpec.call()).thenThrow(new RuntimeException("LLM API timeout"));

                // When
                listener.onPostCreated(event);

                // Then - should catch exception, log error, and not block
                verify(requestSpec).call();
                verifyNoInteractions(postService); // Should not attempt to post a reply
        }

        @Test
        void shouldHandleThreadNotFoundGracefully() {
                // Given
                UUID threadId = UUID.randomUUID();
                PostCreatedEvent event = new PostCreatedEvent(
                                UUID.randomUUID(),
                                threadId,
                                UUID.randomUUID(),
                                "User question",
                                Instant.now(),
                                false,
                                List.of());

                when(threadRepository.findById(threadId)).thenReturn(Optional.empty());

                // When
                listener.onPostCreated(event);

                // Then - pipeline short-circuits, no further processing
                verify(threadRepository, times(1)).findById(threadId); // Called once for tenantId, not twice.
                verifyNoInteractions(chatClientFactory, postService);
        }

        @Test
        void shouldHandleEmptyLlmResponseGracefully() {
                // Given
                UUID threadId = UUID.randomUUID();
                PostCreatedEvent event = new PostCreatedEvent(
                                UUID.randomUUID(),
                                threadId,
                                UUID.randomUUID(),
                                "User question",
                                Instant.now(),
                                false,
                                List.of());

                Thread thread = Thread.builder()
                                .id(threadId)
                                .tenantId("tenant123")
                                .authorId(UUID.randomUUID())
                                .title("Test Thread")
                                .status(ThreadStatus.OPEN)
                                .build();

                TenantAiConfig config = new TenantAiConfig(true, "prompt", "context", "encrypted_key");
                Member aiMember = Member.create("ai-assistant", "ai@forum.local", "AI Assistant", true);

                when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
                when(tenantAiConfigService.getConfig("tenant123")).thenReturn(Optional.of(config));
                when(textEncryptor.decrypt("encrypted_key")).thenReturn("decrypted_key");
                when(postRepository.findByThreadId(threadId, 10)).thenReturn(List.of());
                when(chatClientFactory.createClient("decrypted_key", "gpt-4")).thenReturn(chatClient);
                when(chatClient.prompt()).thenReturn(requestSpec);
                when(requestSpec.system(anyString())).thenReturn(systemSpec);
                when(systemSpec.user(anyString())).thenReturn(requestSpec);
                when(requestSpec.call()).thenReturn(callSpec);
                when(callSpec.content()).thenReturn(""); // Empty response
                when(aiMemberService.getOrCreateAiMember("tenant123")).thenReturn(aiMember);

                // When
                listener.onPostCreated(event);

                // Then - should still post even if response is empty
                verify(postService).createPost(
                                eq(threadId),
                                eq(aiMember.getId()),
                                eq(""), // Empty content
                                any(),
                                eq(Map.of()),
                                eq(List.of()));
        }

        @Test
        void shouldHandleNullLlmResponseGracefully() {
                // Given
                UUID threadId = UUID.randomUUID();
                PostCreatedEvent event = new PostCreatedEvent(
                                UUID.randomUUID(),
                                threadId,
                                UUID.randomUUID(),
                                "User question",
                                Instant.now(),
                                false,
                                List.of());

                Thread thread = Thread.builder()
                                .id(threadId)
                                .tenantId("tenant123")
                                .authorId(UUID.randomUUID())
                                .title("Test Thread")
                                .status(ThreadStatus.OPEN)
                                .build();

                TenantAiConfig config = new TenantAiConfig(true, "prompt", "context", "encrypted_key");
                Member aiMember = Member.create("ai-assistant", "ai@forum.local", "AI Assistant", true);

                when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));
                when(tenantAiConfigService.getConfig("tenant123")).thenReturn(Optional.of(config));
                when(textEncryptor.decrypt("encrypted_key")).thenReturn("decrypted_key");
                when(postRepository.findByThreadId(threadId, 10)).thenReturn(List.of());
                when(chatClientFactory.createClient("decrypted_key", "gpt-4")).thenReturn(chatClient);
                when(chatClient.prompt()).thenReturn(requestSpec);
                when(requestSpec.system(anyString())).thenReturn(systemSpec);
                when(systemSpec.user(anyString())).thenReturn(requestSpec);
                when(requestSpec.call()).thenReturn(callSpec);
                when(callSpec.content()).thenReturn(null); // Null response
                when(aiMemberService.getOrCreateAiMember("tenant123")).thenReturn(aiMember);

                // When
                listener.onPostCreated(event);

                // Then - should handle null gracefully, not crash
                verify(requestSpec).call();
                // The functional pipeline will handle null appropriately
        }
}
