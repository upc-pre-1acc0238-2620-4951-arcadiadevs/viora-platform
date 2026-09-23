package com.arcadiadevs.viora.platform.shared.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerIntegrationTest {

    private MockMvc mockMvc;

    record SamplePayload(@NotBlank String name, @NotNull Integer quantity) {
    }

    @RestController
    @RequestMapping("/api/v1/test-exceptions")
    static class TestExceptionController {

        @PostMapping("/validation")
        public String triggerValidation(@Valid @RequestBody SamplePayload payload) {
            return "ok";
        }

        @GetMapping("/not-found")
        public String triggerNotFound(@RequestParam(defaultValue = "1") Long id) {
            throw new ResourceNotFoundException("Item", id);
        }

        @GetMapping("/business-rule")
        public String triggerBusinessRule() {
            throw new BusinessRuleException("OperationNotPermitted", "The requested entity cannot be processed in its current status");
        }

        @GetMapping("/illegal-argument")
        public String triggerIllegalArgument() {
            throw new IllegalArgumentException("Invalid parameter value provided");
        }

        @GetMapping("/server-error")
        public String triggerServerError() {
            throw new RuntimeException("internal-database-connection-loss");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void whenValidationFails_returns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/test-exceptions/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("https://api.viora.com/errors/validation-error"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/api/v1/test-exceptions/validation"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void whenResourceNotFound_returns404ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/not-found?id=99"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("https://api.viora.com/errors/resource-not-found"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Resource Item with id 99 was not found."))
                .andExpect(jsonPath("$.instance").value("/api/v1/test-exceptions/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void whenBusinessRuleViolated_returns422ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/business-rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("https://api.viora.com/errors/business-rule-violation"))
                .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.detail").value("Business rule 'OperationNotPermitted' violated: The requested entity cannot be processed in its current status"))
                .andExpect(jsonPath("$.instance").value("/api/v1/test-exceptions/business-rule"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void whenIllegalArgumentThrown_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("https://api.viora.com/errors/validation-error"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Invalid parameter value provided"))
                .andExpect(jsonPath("$.instance").value("/api/v1/test-exceptions/illegal-argument"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void whenServerErrorOccurs_returns500ProblemDetailAndSanitizesTrace() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.type").value("https://api.viora.com/errors/unexpected-error"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.detail").value(not(containsString("internal-database-connection-loss"))))
                .andExpect(jsonPath("$.instance").value("/api/v1/test-exceptions/server-error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
