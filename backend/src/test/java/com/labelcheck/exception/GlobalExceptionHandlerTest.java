package com.labelcheck.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestErrorController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestErrorController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class TestErrorController {

        record SampleRequest(@NotBlank(message = "Field name cannot be blank") String name) {}

        @PostMapping("/test/validate")
        public String handleValidate(@Valid @RequestBody SampleRequest request) {
            return "ok: " + request.name();
        }

        @GetMapping("/test/fail")
        public String handleFail() {
            throw new RuntimeException("Simulated unexpected backend failure");
        }

        @GetMapping("/test/oversized")
        public String handleOversized() {
            throw new org.springframework.web.multipart.MaxUploadSizeExceededException(10 * 1024 * 1024);
        }
    }

    @Test
    @DisplayName("Malformed JSON payload produces structured HTTP 400 ErrorResponse")
    void malformedJson_shouldReturnStructuredBadRequest() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": invalid_json_syntax"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Malformed Request"))
                .andExpect(jsonPath("$.path").value("/test/validate"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Validation constraint violation produces structured HTTP 400 ErrorResponse")
    void validationError_shouldReturnStructuredValidationBadRequest() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").value("Field name cannot be blank"))
                .andExpect(jsonPath("$.path").value("/test/validate"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("MaxUploadSizeExceededException produces structured HTTP 413 Payload Too Large ErrorResponse")
    void oversizedUpload_shouldReturnStructuredPayloadTooLarge() throws Exception {
        mockMvc.perform(get("/test/oversized"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.error").value("Payload Too Large"))
                .andExpect(jsonPath("$.message").value("Uploaded file exceeds the maximum allowed size limit of 10MB"))
                .andExpect(jsonPath("$.path").value("/test/oversized"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("NoResourceFoundException produces structured HTTP 404 Not Found ErrorResponse")
    void unmappedRoute_shouldReturnStructuredNotFound() throws Exception {
        mockMvc.perform(get("/test/non-existent-endpoint-route"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("The requested resource was not found: /test/non-existent-endpoint-route"))
                .andExpect(jsonPath("$.path").value("/test/non-existent-endpoint-route"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Unexpected server exception produces sanitized HTTP 500 ErrorResponse without leaking stack trace")
    void unexpectedException_shouldReturnSanitizedInternalServerError() throws Exception {
        mockMvc.perform(get("/test/fail"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected internal error occurred. Please try again later."))
                .andExpect(jsonPath("$.path").value("/test/fail"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
