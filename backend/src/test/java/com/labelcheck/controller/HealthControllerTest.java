package com.labelcheck.controller;

import com.labelcheck.config.AiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiProperties aiProperties;

    @Test
    @DisplayName("GET /api/v1/health returns HTTP 200 with UP status and service name when AI disabled")
    void getHealth_shouldReturnOkWithUpStatus() throws Exception {
        when(aiProperties.isEnabled()).thenReturn(false);

        mockMvc.perform(get("/api/v1/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("LabelCheck Backend"))
                .andExpect(jsonPath("$.aiEnabled").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/health returns aiEnabled=true, aiProvider=groq, and model=qwen/qwen3.6-27b")
    void getHealth_whenAiEnabled_shouldReturnAiMetadata() throws Exception {
        when(aiProperties.isEnabled()).thenReturn(true);
        when(aiProperties.getApiKey()).thenReturn("gsk_fakeGroqApiKey123456789");
        when(aiProperties.getProvider()).thenReturn("groq");
        when(aiProperties.getModel()).thenReturn("qwen/qwen3.6-27b");

        mockMvc.perform(get("/api/v1/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.aiEnabled").value(true))
                .andExpect(jsonPath("$.aiProvider").value("groq"))
                .andExpect(jsonPath("$.aiModel").value("qwen/qwen3.6-27b"))
                // Ensure secret API key is NEVER leaked in response JSON
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andExpect(jsonPath("$.key").doesNotExist());
    }
}

