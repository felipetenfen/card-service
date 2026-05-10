package com.cardservice.card.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cardservice.auth.service.JwtTokenService;
import com.cardservice.card.batch.BatchItemStatus;
import com.cardservice.card.dto.*;
import com.cardservice.card.service.CardBatchService;
import com.cardservice.card.service.CardService;
import com.cardservice.config.JwtAuthenticationFilter;
import com.cardservice.config.SecurityConfig;
import com.cardservice.exception.ApiExceptionHandler;
import com.cardservice.exception.DuplicateCardException;
import com.cardservice.exception.InvalidBatchFileException;
import com.cardservice.exception.InvalidCardException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class CardControllerTest {

    private static final String VALID_CARD_NUMBER = "4532015112830366";
    private static final String BEARER_TOKEN = "Bearer test-token";
    private static final String JWT_SUBJECT_USER_ID = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @MockBean
    private CardBatchService cardBatchService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    void shouldReturn401WhenCreatingCardWithoutAuthorizationHeader() throws Exception {
        CreateCardRequest request = new CreateCardRequest(VALID_CARD_NUMBER);

        mockMvc.perform(post("/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturn201WithCardWhenCreatedSuccessfully() throws Exception {
        CreateCardRequest request = new CreateCardRequest(VALID_CARD_NUMBER);
        UUID cardId = UUID.randomUUID();
        CreateCardResponse response = new CreateCardResponse(cardId, LocalDateTime.now());

        when(jwtTokenService.validateAndGetSubject("test-token")).thenReturn(JWT_SUBJECT_USER_ID);
        when(cardService.create(VALID_CARD_NUMBER)).thenReturn(response);

        mockMvc.perform(post("/cards")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn400WhenCardNumberIsBlank() throws Exception {
        CreateCardRequest request = new CreateCardRequest("");

        when(jwtTokenService.validateAndGetSubject("test-token")).thenReturn(JWT_SUBJECT_USER_ID);

        mockMvc.perform(post("/cards")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn409WhenCardAlreadyExists() throws Exception {
        CreateCardRequest request = new CreateCardRequest(VALID_CARD_NUMBER);

        when(jwtTokenService.validateAndGetSubject("test-token")).thenReturn(JWT_SUBJECT_USER_ID);
        when(cardService.create(VALID_CARD_NUMBER))
                .thenThrow(new DuplicateCardException());

        mockMvc.perform(post("/cards")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CARD_ALREADY_EXISTS"));
    }

    @Test
    void shouldReturn200WithoutIdFieldWhenCardIsNotFound() throws Exception {
        SearchCardRequest request = new SearchCardRequest(VALID_CARD_NUMBER);

        when(jwtTokenService.validateAndGetSubject("test-token")).thenReturn(JWT_SUBJECT_USER_ID);
        when(cardService.search(VALID_CARD_NUMBER)).thenReturn(new SearchCardResponse(false, null));

        mockMvc.perform(post("/cards/search")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void shouldReturn200WithIdFieldWhenCardIsFound() throws Exception {
        SearchCardRequest request = new SearchCardRequest(VALID_CARD_NUMBER);
        UUID cardId = UUID.randomUUID();

        when(jwtTokenService.validateAndGetSubject("test-token")).thenReturn(JWT_SUBJECT_USER_ID);
        when(cardService.search(VALID_CARD_NUMBER)).thenReturn(new SearchCardResponse(true, cardId));

        mockMvc.perform(post("/cards/search")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.id").value(cardId.toString()));
    }

    @Test
    void shouldReturn401WhenUploadingBatchWithoutAuthorizationHeader() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "batch.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/cards/batch").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturn200WithBatchSummaryWhenUploadIsValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "batch.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        UUID cardId = UUID.randomUUID();
        BatchItemResult itemResult = new BatchItemResult("000001", BatchItemStatus.SUCCESS, cardId, null);
        BatchUploadResponse response = new BatchUploadResponse("LOTE0001", 1, 1, 0, List.of(itemResult));

        when(jwtTokenService.validateAndGetSubject("test-token")).thenReturn(JWT_SUBJECT_USER_ID);
        when(cardBatchService.process(any(InputStream.class))).thenReturn(response);

        mockMvc.perform(multipart("/cards/batch")
                        .file(file)
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value("LOTE0001"))
                .andExpect(jsonPath("$.totalDeclared").value(1))
                .andExpect(jsonPath("$.totalInserted").value(1))
                .andExpect(jsonPath("$.totalRejected").value(0))
                .andExpect(jsonPath("$.results[0].status").value(BatchItemStatus.SUCCESS.name()));
    }

    @Test
    void shouldReturn422WhenCardFailsLuhnValidation() throws Exception {
        CreateCardRequest request = new CreateCardRequest(VALID_CARD_NUMBER);

        when(jwtTokenService.validateAndGetSubject("test-token")).thenReturn(JWT_SUBJECT_USER_ID);
        when(cardService.create(VALID_CARD_NUMBER))
                .thenThrow(new InvalidCardException("Card number failed Luhn validation"));

        mockMvc.perform(post("/cards")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INVALID_CARD_NUMBER"));
    }

    @Test
    void shouldReturn400WhenBatchFileIsMalformed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.txt", MediaType.TEXT_PLAIN_VALUE, "bad content".getBytes());

        when(jwtTokenService.validateAndGetSubject("test-token")).thenReturn(JWT_SUBJECT_USER_ID);
        when(cardBatchService.process(any(InputStream.class)))
                .thenThrow(new InvalidBatchFileException("Malformed batch file"));

        mockMvc.perform(multipart("/cards/batch")
                        .file(file)
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FILE_FORMAT"));
    }

    @Test
    void shouldReturn400WhenCardNumberFailsPatternValidation() throws Exception {
        CreateCardRequest request = new CreateCardRequest("123");

        when(jwtTokenService.validateAndGetSubject("test-token")).thenReturn(JWT_SUBJECT_USER_ID);

        mockMvc.perform(post("/cards")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn401WhenAuthorizationHeaderContainsInvalidToken() throws Exception {
        CreateCardRequest request = new CreateCardRequest(VALID_CARD_NUMBER);

        when(jwtTokenService.validateAndGetSubject("bad-token"))
                .thenThrow(new JwtException("Invalid token"));

        mockMvc.perform(post("/cards")
                        .header("Authorization", "Bearer bad-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
