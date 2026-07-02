package com.creatorspace.module.community;

import com.creatorspace.security.JwtService;
import com.creatorspace.security.LoginUser;
import com.creatorspace.testsupport.PostgresIntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CommunityApiIntegrationTests extends PostgresIntegrationTestSupport {

    @Container
    private static final PostgreSQLContainer POSTGRES = createPostgres("creatorspace_community_test");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, POSTGRES);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("app.ai.enabled", () -> "true");
        registry.add("app.ai.provider", () -> "local");
        registry.add("app.ai.zhipu-model", () -> "local-rule-assistant");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void usersCanFollowFriendCommentReactAndManageVisibility() throws Exception {
        String suffix = uniqueSuffix();
        TestUser alice = createUser("community-alice-" + suffix, "Alice " + suffix, "USER");
        TestUser bob = createUser("community-bob-" + suffix, "Bob " + suffix, "USER");
        String aliceToken = token(alice);
        String bobToken = token(bob);
        String adminToken = adminToken();

        long publicArticleId = createArticle(alice.id(), "community-public-" + suffix, "PUBLIC");
        long friendsArticleId = createArticle(alice.id(), "community-friends-" + suffix, "FRIENDS");
        long selectedArticleId = createArticle(alice.id(), "community-selected-" + suffix, "SELECTED_FRIENDS");
        long projectId = createVisibleProject(alice.id(), "community-project-" + suffix);
        long inspirationId = createInspiration(alice.id(), "Community inspiration " + suffix);

        long friendshipId = sendAndAcceptFriendRequest(aliceToken, bobToken, alice.id());
        createMutualFollow(aliceToken, bobToken, alice.id(), bob.id());

        mockMvc.perform(get("/api/me/friends")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));

        mockMvc.perform(get("/api/me/friends/status")
                        .header("Authorization", bearer(bobToken))
                        .param("userId", alice.id().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACCEPTED")));

        mockMvc.perform(get("/api/users/{userId}", alice.id())
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username", is(alice.username())));

        mockMvc.perform(get("/api/users/{userId}/articles", alice.id())
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/users/{userId}/followers", alice.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));
        mockMvc.perform(get("/api/users/{userId}/following", alice.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));
        mockMvc.perform(get("/api/users/{userId}/friends", alice.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));

        exerciseVisibilityRules(aliceToken, bob.id(), selectedArticleId);

        long guestbookId = createAndApproveGuestbookEntry(bobToken, adminToken);
        long commentId = createAndExerciseComment(bobToken, adminToken, publicArticleId);

        exerciseInteractions(bobToken, publicArticleId, projectId, inspirationId, guestbookId, commentId);

        mockMvc.perform(delete("/api/me/friends/{id}", friendshipId)
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk());
    }

    @Test
    void communityEndpointsRejectInvalidOrUnauthorizedOperations() throws Exception {
        String suffix = uniqueSuffix();
        TestUser alice = createUser("community-invalid-alice-" + suffix, "Alice invalid", "USER");
        TestUser bob = createUser("community-invalid-bob-" + suffix, "Bob invalid", "USER");
        String aliceToken = token(alice);
        String bobToken = token(bob);
        long aliceArticleId = createArticle(alice.id(), "community-invalid-article-" + suffix, "PUBLIC");

        mockMvc.perform(post("/api/me/friend-requests")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("addresseeId", alice.id()))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/me/follow/{userId}", alice.id())
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/creator/articles/{articleId}/visibility-users", aliceArticleId)
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userId", bob.id(), "ruleType", "ALLOW"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/me/likes")
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("targetType", "UNKNOWN", "targetId", aliceArticleId))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/comments")
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", 99999999L,
                                "content", "missing target"
                        ))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/admin/guestbook")
                        .header("Authorization", bearer(token(alice)))
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanRunAiAssistantWorkflowsAndModerationUtilities() throws Exception {
        String suffix = uniqueSuffix();
        TestUser author = createUser("community-ai-author-" + suffix, "AI author", "USER");
        String adminToken = adminToken();
        long articleId = createArticle(author.id(), "community-ai-article-" + suffix, "PUBLIC");
        long projectId = createVisibleProject(author.id(), "community-ai-project-" + suffix);
        long commentId = insertApprovedComment(articleId, author.id());

        String taskResponse = mockMvc.perform(post("/api/admin/ai/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "taskType", "summary",
                                "targetType", "article",
                                "targetId", articleId,
                                "prompt", "summarize this public article"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUCCEEDED")))
                .andExpect(jsonPath("$.data.suggestions", not(empty())))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long taskId = dataId(taskResponse);

        mockMvc.perform(post("/api/admin/ai/tasks/{id}/messages", taskId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("prompt", "continue with a shorter version"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", not(empty())));

        mockMvc.perform(post("/api/admin/ai/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "taskType", "tags",
                                "targetType", "project",
                                "targetId", projectId,
                                "prompt", "suggest tags"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestions", not(empty())));

        mockMvc.perform(post("/api/admin/ai/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "taskType", "review",
                                "targetType", "comment",
                                "targetId", commentId,
                                "prompt", "review comment risk"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestions", not(empty())));

        mockMvc.perform(post("/api/admin/ai/workflows")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("workflowType", "OPERATION_REPORT", "days", 14))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUCCEEDED")));

        mockMvc.perform(post("/api/admin/ai/workflows")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("workflowType", "TOPIC_IDEAS", "days", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUCCEEDED")));
        mockMvc.perform(post("/api/admin/ai/workflows")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("workflowType", "HOMEPAGE_RECOMMENDATIONS", "days", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUCCEEDED")));
        mockMvc.perform(post("/api/admin/ai/workflows")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("workflowType", "COMMENT_RISK_QUEUE", "days", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUCCEEDED")));

        long privateArticleId = createArticle(author.id(), "community-ai-private-" + suffix, "SELF");
        mockMvc.perform(post("/api/admin/ai/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "taskType", "summary",
                                "targetType", "article",
                                "targetId", privateArticleId,
                                "prompt", "try private article"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("FAILED")));

        mockMvc.perform(post("/api/admin/ai/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "taskType", "general",
                                "prompt", "give a general operations suggestion"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUCCEEDED")));

        mockMvc.perform(post("/api/admin/ai/workflows")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("workflowType", "UNKNOWN_WORKFLOW", "days", 7))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/ai/suggestions")
                        .header("Authorization", bearer(adminToken))
                        .param("status", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));

        List<Long> suggestionIds = jdbcTemplate.query("""
                        select id from ai_suggestions
                        where status = 'PENDING'
                        order by id
                        limit 2
                        """,
                (rs, rowNum) -> rs.getLong("id"));
        mockMvc.perform(put("/api/admin/ai/suggestions/{id}/adopt", suggestionIds.get(0))
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ADOPTED")));
        mockMvc.perform(put("/api/admin/ai/suggestions/{id}/ignore", suggestionIds.get(1))
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("REJECTED")));
        mockMvc.perform(put("/api/admin/ai/suggestions/{id}/adopt", suggestionIds.get(0))
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict());

        exerciseSensitiveWordAdministration(adminToken, suffix);
        exerciseOperationLogQuery(adminToken, author.id());
    }

    private long sendAndAcceptFriendRequest(String aliceToken, String bobToken, Long aliceId) throws Exception {
        String response = mockMvc.perform(post("/api/me/friend-requests")
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("addresseeId", aliceId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long friendshipId = dataId(response);

        mockMvc.perform(get("/api/me/friend-requests")
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));

        mockMvc.perform(get("/api/me/friend-requests/sent")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));

        mockMvc.perform(put("/api/me/friend-requests/{id}/accept", friendshipId)
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACCEPTED")));
        return friendshipId;
    }

    private void createMutualFollow(String aliceToken, String bobToken, Long aliceId, Long bobId) throws Exception {
        mockMvc.perform(post("/api/me/follow/{userId}", aliceId)
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/me/follow/{userId}", bobId)
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/me/follow/status/{userId}", aliceId)
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.following", is(true)))
                .andExpect(jsonPath("$.data.friend", is(true)));
        mockMvc.perform(get("/api/me/follow/followers")
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));
        mockMvc.perform(get("/api/me/follow/following")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));
    }

    private void exerciseVisibilityRules(String aliceToken, Long bobId, long articleId) throws Exception {
        mockMvc.perform(post("/api/creator/articles/{articleId}/visibility-users", articleId)
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userId", bobId, "ruleType", " allow "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleType", is("ALLOW")));

        mockMvc.perform(get("/api/creator/articles/{articleId}/visibility-users", articleId)
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", not(empty())));

        mockMvc.perform(delete("/api/creator/articles/{articleId}/visibility-users/{userId}", articleId, bobId)
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk());
    }

    private long createAndApproveGuestbookEntry(String bobToken, String adminToken) throws Exception {
        String response = mockMvc.perform(post("/api/guestbook")
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "hello from community integration"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long guestbookId = dataId(response);

        mockMvc.perform(get("/api/admin/guestbook")
                        .header("Authorization", bearer(adminToken))
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));

        mockMvc.perform(put("/api/admin/guestbook/{id}/approve", guestbookId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        mockMvc.perform(get("/api/guestbook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));

        String secondResponse = mockMvc.perform(post("/api/guestbook")
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "another community note"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long secondId = dataId(secondResponse);
        mockMvc.perform(put("/api/admin/guestbook/batch-approve")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ids", List.of(secondId, 99999999L)))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/guestbook/{id}", secondId)
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "edited community note"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING")));
        mockMvc.perform(put("/api/admin/guestbook/batch-reject")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ids", List.of(secondId)))))
                .andExpect(status().isOk());

        return guestbookId;
    }

    private long createAndExerciseComment(String bobToken, String adminToken, long articleId) throws Exception {
        String response = mockMvc.perform(post("/api/comments")
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", articleId,
                                "content", "this is a useful article comment"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long commentId = dataId(response);

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "ARTICLE")
                        .param("targetId", Long.toString(articleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));

        mockMvc.perform(put("/api/comments/{id}", commentId)
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "updated useful article comment"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", is("updated useful article comment")));

        mockMvc.perform(get("/api/me/comments")
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));

        long pendingCommentId = insertPendingComment(articleId);
        mockMvc.perform(get("/api/admin/comments")
                        .header("Authorization", bearer(adminToken))
                        .param("status", "PENDING")
                        .param("targetType", "ARTICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));
        mockMvc.perform(put("/api/admin/comments/{id}/approve", pendingCommentId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")));
        mockMvc.perform(put("/api/admin/comments/batch-reject")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ids", List.of(pendingCommentId)))))
                .andExpect(status().isOk());

        return commentId;
    }

    private void exerciseInteractions(
            String bobToken,
            long articleId,
            long projectId,
            long inspirationId,
            long guestbookId,
            long commentId
    ) throws Exception {
        likeAndUnlike(bobToken, "ARTICLE", articleId);
        likeAndUnlike(bobToken, "PROJECT", projectId);
        likeAndUnlike(bobToken, "INSPIRATION", inspirationId);
        likeAndUnlike(bobToken, "MESSAGE", guestbookId);

        favoriteAndUnfavorite(bobToken, "ARTICLE", articleId);
        favoriteAndUnfavorite(bobToken, "PROJECT", projectId);
        favoriteAndUnfavorite(bobToken, "INSPIRATION", inspirationId);

        mockMvc.perform(post("/api/me/comment-reactions")
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("commentId", commentId, "type", "LIKE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type", is("LIKE")));
        mockMvc.perform(get("/api/me/comment-reactions/status")
                        .header("Authorization", bearer(bobToken))
                        .param("commentId", Long.toString(commentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is(true)));
        mockMvc.perform(get("/api/me/comment-reactions/batch-status")
                        .header("Authorization", bearer(bobToken))
                        .param("commentIds", Long.toString(commentId), "99999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data['" + commentId + "']", is(true)));
        mockMvc.perform(delete("/api/me/comment-reactions")
                        .header("Authorization", bearer(bobToken))
                        .param("commentId", Long.toString(commentId))
                        .param("type", "LIKE"))
                .andExpect(status().isOk());
    }

    private void likeAndUnlike(String token, String targetType, long targetId) throws Exception {
        mockMvc.perform(post("/api/me/likes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("targetType", targetType, "targetId", targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetType", is(targetType)));
        mockMvc.perform(get("/api/me/likes/status")
                        .header("Authorization", bearer(token))
                        .param("targetType", targetType)
                        .param("targetId", Long.toString(targetId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is(true)));
        mockMvc.perform(get("/api/me/likes")
                        .header("Authorization", bearer(token))
                        .param("targetType", targetType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));
        mockMvc.perform(delete("/api/me/likes")
                        .header("Authorization", bearer(token))
                        .param("targetType", targetType)
                        .param("targetId", Long.toString(targetId)))
                .andExpect(status().isOk());
    }

    private void favoriteAndUnfavorite(String token, String targetType, long targetId) throws Exception {
        mockMvc.perform(post("/api/me/favorites")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("targetType", targetType, "targetId", targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetType", is(targetType)));
        mockMvc.perform(get("/api/me/favorites/status")
                        .header("Authorization", bearer(token))
                        .param("targetType", targetType)
                        .param("targetId", Long.toString(targetId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is(true)));
        mockMvc.perform(get("/api/me/favorites")
                        .header("Authorization", bearer(token))
                        .param("targetType", targetType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));
        mockMvc.perform(delete("/api/me/favorites")
                        .header("Authorization", bearer(token))
                        .param("targetType", targetType)
                        .param("targetId", Long.toString(targetId)))
                .andExpect(status().isOk());
    }

    private TestUser createUser(String username, String nickname, String role) {
        Long id = jdbcTemplate.queryForObject("""
                        insert into users (username, nickname, password_hash, status)
                        values (?, ?, ?, 'ACTIVE')
                        returning id
                        """,
                Long.class,
                username,
                nickname,
                ADMIN_PASSWORD_HASH);
        jdbcTemplate.update("""
                        insert into user_roles (user_id, role_id)
                        select ?, id from roles where code = ?
                        """,
                id,
                role);
        return new TestUser(id, username, List.of(role));
    }

    private String adminToken() {
        Long adminId = jdbcTemplate.queryForObject("select id from users where username = ?", Long.class, ADMIN_USERNAME);
        return jwtService.createAccessToken(new LoginUser(adminId, ADMIN_USERNAME, List.of("ADMIN")));
    }

    private String token(TestUser user) {
        return jwtService.createAccessToken(new LoginUser(user.id(), user.username(), user.roles()));
    }

    private long createArticle(Long authorId, String slug, String privacyType) {
        return jdbcTemplate.queryForObject("""
                        insert into articles (title, slug, summary, content_markdown, status, privacy_type, publish_time, created_by, updated_by)
                        values (?, ?, ?, ?, 'PUBLISHED', ?, now(), ?, ?)
                        returning id
                        """,
                Long.class,
                "Community article " + slug,
                slug,
                "Community summary " + slug,
                "Community markdown " + slug,
                privacyType,
                authorId,
                authorId);
    }

    private long createVisibleProject(Long authorId, String slug) {
        return jdbcTemplate.queryForObject("""
                        insert into portfolio_projects (title, slug, description, project_type, content_markdown, status, created_by, updated_by)
                        values (?, ?, ?, 'WEB', ?, 'VISIBLE', ?, ?)
                        returning id
                        """,
                Long.class,
                "Community project " + slug,
                slug,
                "Community project description",
                "Community project markdown",
                authorId,
                authorId);
    }

    private long createInspiration(Long authorId, String title) {
        return jdbcTemplate.queryForObject("""
                        insert into inspiration_cards (title, content, card_type, is_public, created_by)
                        values (?, 'Community inspiration body', 'TEXT', true, ?)
                        returning id
                        """,
                Long.class,
                title,
                authorId);
    }

    private long insertPendingComment(long articleId) {
        Long userId = jdbcTemplate.queryForObject("select id from users where username = ?", Long.class, ADMIN_USERNAME);
        return jdbcTemplate.queryForObject("""
                        insert into comments (target_type, target_id, user_id, content, status, depth)
                        values ('ARTICLE', ?, ?, 'pending moderation comment', 'PENDING', 0)
                        returning id
                        """,
                Long.class,
                articleId,
                userId);
    }

    private long insertApprovedComment(long articleId, Long userId) {
        return jdbcTemplate.queryForObject("""
                        insert into comments (target_type, target_id, user_id, content, status, depth)
                        values ('ARTICLE', ?, ?, 'approved ai context comment', 'APPROVED', 0)
                        returning id
                        """,
                Long.class,
                articleId,
                userId);
    }

    private void exerciseSensitiveWordAdministration(String adminToken, String suffix) throws Exception {
        String word = "blocked-" + suffix;
        String createResponse = mockMvc.perform(post("/api/admin/sensitive-words")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "word", word,
                                "matchType", " contains ",
                                "severity", " mask ",
                                "enabled", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.word", is(word)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long wordId = dataId(createResponse);

        mockMvc.perform(get("/api/admin/sensitive-words")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())));

        mockMvc.perform(put("/api/admin/sensitive-words/{id}", wordId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "word", word + "-updated",
                                "matchType", "REGEX",
                                "severity", "REVIEW",
                                "enabled", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchType", is("REGEX")));

        mockMvc.perform(put("/api/admin/sensitive-words/{id}/toggle", wordId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled", is(false)));

        mockMvc.perform(post("/api/admin/sensitive-words")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "word", "invalid-" + suffix,
                                "matchType", "UNKNOWN",
                                "severity", "MASK",
                                "enabled", true
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/admin/sensitive-words/{id}", wordId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    private void exerciseOperationLogQuery(String adminToken, Long operatorId) throws Exception {
        jdbcTemplate.update("""
                        insert into operation_logs (
                            operator_id, operation, module, target_type, target_id,
                            request_method, request_path, ip_address, user_agent, detail_json
                        )
                        values (?, 'CREATE_TEST', 'ARTICLE', 'ARTICLE', 7, 'POST', '/api/admin/articles', '127.0.0.1', 'JUnit', ?::jsonb)
                        """,
                operatorId,
                "{\"source\":\"integration\"}");

        mockMvc.perform(get("/api/admin/operation-logs")
                        .header("Authorization", bearer(adminToken))
                        .param("module", " article ")
                        .param("operation", "CREATE")
                        .param("operatorId", operatorId.toString())
                        .param("startTime", "2000-01-01T00:00:00Z")
                        .param("endTime", "2999-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", not(empty())))
                .andExpect(jsonPath("$.data.records[0].module", is("ARTICLE")));
    }

    private long dataId(String response) throws Exception {
        JsonNode data = objectMapper.readTree(response).path("data");
        return data.path("id").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime());
    }

    private record TestUser(Long id, String username, List<String> roles) {
    }
}
