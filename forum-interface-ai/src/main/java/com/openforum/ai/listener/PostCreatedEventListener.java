package com.openforum.ai.listener;

import com.openforum.ai.client.ChatClientFactory;
import com.openforum.ai.config.TenantAiConfig;
import com.openforum.ai.service.AiMemberService;
import com.openforum.ai.service.TenantAiConfigService;
import com.openforum.application.service.PostService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.events.PostCreatedEvent;
import com.openforum.domain.repository.PostRepository;
import com.openforum.domain.repository.ThreadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PostCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(PostCreatedEventListener.class);

    private final ChatClientFactory chatClientFactory;
    private final PostRepository postRepository;
    private final ThreadRepository threadRepository;
    private final TenantAiConfigService tenantAiConfigService;
    private final PostService postService;
    private final AiMemberService aiMemberService;
    private final TextEncryptor textEncryptor;

    public PostCreatedEventListener(
            ChatClientFactory chatClientFactory,
            PostRepository postRepository,
            ThreadRepository threadRepository,
            TenantAiConfigService tenantAiConfigService,
            PostService postService,
            AiMemberService aiMemberService,
            TextEncryptor textEncryptor) {
        this.chatClientFactory = chatClientFactory;
        this.postRepository = postRepository;
        this.threadRepository = threadRepository;
        this.tenantAiConfigService = tenantAiConfigService;
        this.postService = postService;
        this.aiMemberService = aiMemberService;
        this.textEncryptor = textEncryptor;
    }

    /**
     * Handles post creation events asynchronously to generate AI replies.
     * <p>
     * Uses a functional pipeline approach to avoid if/else branching:
     * <ol>
     * <li>Filter: Skip if post is from a bot (prevents infinite loops)</li>
     * <li>Validate: Check tenant AI config is enabled and valid</li>
     * <li>Build Context: Fetch thread and recent posts for RAG</li>
     * <li>Generate: Call LLM with context to generate reply</li>
     * <li>Post: Create reply post as AI member</li>
     * </ol>
     * Any step returning {@code Optional.empty()} short-circuits the pipeline.
     *
     * @param event the post creation event containing post ID, thread ID, content,
     *              and bot flag
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostCreated(PostCreatedEvent event) {
        // Functional pipeline: validate → fetch context → generate → post reply
        Optional.of(event)
                .filter(e -> !e.isBot()) // Bot prevention
                .flatMap(this::validateAndGetConfig)
                .flatMap(ctx -> buildAiContext(ctx, event))
                .flatMap(this::generateAiReply)
                .ifPresent(this::postAiReply);
    }

    /**
     * Validates AI configuration for the thread's tenant.
     * <p>
     * Returns {@code Optional.empty()} if:
     * <ul>
     * <li>Tenant not found</li>
     * <li>AI not enabled in tenant config</li>
     * <li>API key is blank</li>
     * </ul>
     *
     * @param event the post creation event
     * @return AI context with config if valid, empty otherwise
     */
    private Optional<AiContext> validateAndGetConfig(PostCreatedEvent event) {
        return getThreadTenantId(event.threadId())
                .flatMap(tenantAiConfigService::getConfig)
                .filter(TenantAiConfig::enabled)
                .filter(config -> !config.apiKeyEncrypted().isBlank())
                .map(config -> new AiContext(event, config));
    }

    /**
     * Builds AI context by fetching thread details and recent conversation history.
     * <p>
     * Implements lightweight RAG by fetching the last 10 posts for context.
     * Returns {@code Optional.empty()} if thread not found.
     *
     * @param ctx   the validated AI context with config
     * @param event the post creation event
     * @return full context with thread and recent posts, or empty if thread not
     *         found
     */
    private Optional<AiContextWithThread> buildAiContext(AiContext ctx, PostCreatedEvent event) {
        return threadRepository.findById(event.threadId())
                .map(thread -> {
                    List<Post> recentPosts = postRepository.findByThreadId(event.threadId(), 10);
                    return new AiContextWithThread(ctx, thread, recentPosts);
                });
    }

    /**
     * Generates AI reply by calling the LLM with constructed prompts.
     * <p>
     * Steps:
     * <ol>
     * <li>Decrypt API key from tenant config</li>
     * <li>Build conversation log from recent posts (functional stream)</li>
     * <li>Construct system message (prompt + static context)</li>
     * <li>Construct user message (thread title + history + new post)</li>
     * <li>Create dynamic ChatClient with decrypted API key</li>
     * <li>Call LLM and extract response</li>
     * </ol>
     * Returns {@code Optional.empty()} on any exception (logged).
     *
     * @param ctx full AI context with thread and conversation history
     * @return reply context with generated AI response, or empty on failure
     */
    private Optional<AiReplyContext> generateAiReply(AiContextWithThread ctx) {
        try {
            String decryptedApiKey = textEncryptor.decrypt(ctx.aiContext().config().apiKeyEncrypted());
            String conversationLog = buildConversationLog(ctx.recentPosts());
            String systemMessage = buildSystemMessage(ctx.aiContext().config());
            String userMessage = buildUserMessage(ctx.thread(), conversationLog, ctx.aiContext().event());

            ChatClient chatClient = chatClientFactory.createClient(decryptedApiKey, "gpt-4");
            String aiReply = chatClient.prompt()
                    .system(systemMessage)
                    .user(userMessage)
                    .call()
                    .content();

            return Optional.of(new AiReplyContext(ctx.aiContext().event(), aiReply));
        } catch (Exception e) {
            log.error("Failed to generate AI reply for post {}", ctx.aiContext().event().postId(), e);
            return Optional.empty();
        }
    }

    /**
     * Posts the AI-generated reply to the forum.
     * <p>
     * Creates a post as the AI member (with {@code isBot=true}) which will emit
     * another {@code PostCreatedEvent}, but the bot filter prevents infinite loops.
     *
     * @param replyCtx context containing the generated AI reply
     */
    private void postAiReply(AiReplyContext replyCtx) {
        // We need tenantId here. It's not directly in AiReplyContext, but we can get it
        // from the thread or config if we had it.
        // Wait, AiReplyContext has AiContext which has TenantAiConfig, but
        // TenantAiConfig doesn't have tenantId.
        // However, we can fetch the thread again or pass tenantId through the context.
        // Better: Add tenantId to AiContext.
        // For now, let's fetch it from the thread repository again as a fallback, or
        // assume we can get it.
        // Actually, we can just look it up from the threadId.
        String tenantId = threadRepository.findById(replyCtx.event().threadId())
                .map(Thread::getTenantId)
                .orElseThrow(() -> new IllegalStateException("Thread not found for AI reply"));

        Member aiMember = aiMemberService.getOrCreateAiMember(tenantId);
        postService.createPost(
                replyCtx.event().threadId(),
                aiMember.getId(),
                replyCtx.aiReply(),
                replyCtx.event().postId(),
                Map.of(),
                List.of());
        log.info("AI replied to post {} in thread {}", replyCtx.event().postId(), replyCtx.event().threadId());
    }

    // ==================== Pure Helper Functions ====================

    /**
     * Builds conversation log from posts using functional stream.
     *
     * @param posts list of recent posts
     * @return formatted conversation history string
     */
    private String buildConversationLog(List<Post> posts) {
        return posts.stream()
                .map(p -> String.format("[Author %s]: %s", p.getAuthorId(), p.getContent()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Constructs the system message for the LLM.
     *
     * @param config tenant AI configuration
     * @return combined system prompt and static context
     */
    private String buildSystemMessage(TenantAiConfig config) {
        return config.systemPrompt() + "\n\nContext: " + config.staticContext();
    }

    /**
     * Constructs the user message for the LLM with full context.
     *
     * @param thread          the thread being discussed
     * @param conversationLog formatted conversation history
     * @param event           the triggering post creation event
     * @return formatted user message with thread title, history, and new post
     */
    private String buildUserMessage(Thread thread, String conversationLog, PostCreatedEvent event) {
        return String.format(
                "Thread Title: %s\n\nConversation History:\n%s\n\nNew Post: %s",
                thread.getTitle(),
                conversationLog,
                event.content());
    }

    /**
     * Retrieves tenant ID for a thread.
     *
     * @param threadId the thread UUID
     * @return tenant ID if thread found, empty otherwise
     */
    private Optional<String> getThreadTenantId(java.util.UUID threadId) {
        return threadRepository.findById(threadId)
                .map(Thread::getTenantId);
    }

    // ==================== Context Records (Immutable Data Carriers)
    // ====================

    /** Initial context after validation, contains event and validated AI config. */
    private record AiContext(PostCreatedEvent event, TenantAiConfig config) {
    }

    /** Extended context with thread details and conversation history for RAG. */
    private record AiContextWithThread(AiContext aiContext, Thread thread, List<Post> recentPosts) {
    }

    /** Final context containing the generated AI reply ready to post. */
    private record AiReplyContext(PostCreatedEvent event, String aiReply) {
    }
}
