package com.cardservice.card.controller;

import com.cardservice.card.dto.*;
import com.cardservice.card.service.CardBatchService;
import com.cardservice.card.service.CardService;
import com.cardservice.exception.ApiError;
import com.cardservice.exception.InvalidBatchFileException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/cards")
@Tag(name = "Cards", description = "Register and look up credit card numbers securely")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private static final Logger log = LoggerFactory.getLogger(CardController.class);

    private final CardService cardService;
    private final CardBatchService cardBatchService;

    public CardController(CardService cardService, CardBatchService cardBatchService) {
        this.cardService = cardService;
        this.cardBatchService = cardBatchService;
    }

    @Operation(
            summary = "Register a single card",
            description = "Stores a credit card number securely. The PAN is never persisted in plain text — " +
                          "only an HMAC-SHA256 hash is stored. Returns the generated UUID and creation timestamp."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Card registered successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateCardResponse.class),
                            examples = @ExampleObject(value = """
                                    {"id":"550e8400-e29b-41d4-a716-446655440000","createdAt":"2026-05-09T10:00:00"}
                                    """))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Card already registered",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"error":"DUPLICATE_CARD","message":"Card already registered"}
                                    """))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Card number is invalid (format or Luhn check when enabled)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"error":"INVALID_CARD_NUMBER","message":"Invalid card number"}
                                    """))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or malformed request body",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"error":"VALIDATION_ERROR","message":"Validation failed"}
                                    """))
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"error":"UNAUTHORIZED","message":"Authentication required"}
                                    """)))
    })
    @PostMapping
    public ResponseEntity<CreateCardResponse> create(@Valid @RequestBody CreateCardRequest request) {
        log.info("POST /cards received");
        CreateCardResponse response = cardService.create(request.cardNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Register cards from a TXT batch file",
            description = """
                    Processes a fixed-position ASCII TXT file containing multiple card numbers.
                    Each card is inserted independently — a failure on one card does not roll back the others.

                    **File format:**
                    - Header (line 1): `[01-29]` name, `[30-37]` date YYYYMMDD, `[38-45]` batch ID, `[46-51]` record count
                    - Card lines: `[01]` "C", `[02-07]` sequence, `[08-26]` card number
                    - Trailer (last line): `[01-08]` batch ID, `[09-14]` record count
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "File processed — check `totalRejected` for partial failures",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BatchUploadResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "batchId": "LOTE0001",
                                      "totalDeclared": 2,
                                      "totalInserted": 2,
                                      "totalRejected": 0,
                                      "results": [
                                        {"sequence":"000001","status":"SUCCESS","id":"550e8400-e29b-41d4-a716-446655440000"},
                                        {"sequence":"000002","status":"SUCCESS","id":"661f9511-f3ac-52e5-b827-557766551111"}
                                      ]
                                    }
                                    """))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "File is missing, unreadable, or has an invalid format",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"error":"INVALID_BATCH_FILE","message":"Invalid batch file"}
                                    """))
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"error":"UNAUTHORIZED","message":"Authentication required"}
                                    """)))
    })
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchUploadResponse> uploadBatch(@RequestParam("file") MultipartFile file) {
        log.info("POST /cards/batch received filename={} size={}", file.getOriginalFilename(), file.getSize());
        try {
            BatchUploadResponse response = cardBatchService.process(file.getInputStream());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            throw new InvalidBatchFileException("Could not read uploaded file");
        }
    }

    @Operation(
            summary = "Look up a card",
            description = "Checks whether a card number is already registered. " +
                          "The PAN is sent in the request body (never in the URL) to prevent exposure in server logs or proxy access logs. " +
                          "Returns the UUID if found."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lookup completed — `found` indicates whether the card exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SearchCardResponse.class),
                            examples = {
                                    @ExampleObject(name = "Found", value = """
                                            {"found":true,"id":"550e8400-e29b-41d4-a716-446655440000"}
                                            """),
                                    @ExampleObject(name = "Not found", value = """
                                            {"found":false}
                                            """)
                            })
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or malformed request body",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"error":"VALIDATION_ERROR","message":"Validation failed"}
                                    """))
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"error":"UNAUTHORIZED","message":"Authentication required"}
                                    """)))
    })
    @PostMapping("/search")
    public ResponseEntity<SearchCardResponse> search(@Valid @RequestBody SearchCardRequest request) {
        log.info("POST /cards/search received");
        SearchCardResponse response = cardService.search(request.cardNumber());
        return ResponseEntity.ok(response);
    }
}
