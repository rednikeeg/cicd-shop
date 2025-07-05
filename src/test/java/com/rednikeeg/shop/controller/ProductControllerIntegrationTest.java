package com.rednikeeg.shop.controller;

import com.rednikeeg.shop.model.Product;
import com.rednikeeg.shop.repository.ProductRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ProductControllerIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.2-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(java.time.Duration.ofSeconds(60));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void createProduct_ShouldReturnCreatedProduct() throws Exception {
        Product product = new Product("Test Product", "Test Description", BigDecimal.valueOf(99.99), 10);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(product.getName())))
                .andExpect(jsonPath("$.description", is(product.getDescription())))
                .andExpect(jsonPath("$.price", is(99.99)))
                .andExpect(jsonPath("$.stockQuantity", is(10)));
    }

    @Test
    void getProduct_WithExistingId_ShouldReturnProduct() throws Exception {
        Product savedProduct = productRepository.save(
                new Product("Test Product", "Test Description", BigDecimal.valueOf(99.99), 10)
        );

        mockMvc.perform(get("/api/products/" + savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedProduct.getId().intValue())))
                .andExpect(jsonPath("$.name", is(savedProduct.getName())));
    }

    @Test
    void getProduct_WithNonExistingId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProduct_WithValidData_ShouldReturnUpdatedProduct() throws Exception {
        Product savedProduct = productRepository.save(
                new Product("Test Product", "Test Description", BigDecimal.valueOf(99.99), 10)
        );

        Product updatedProduct = new Product("Updated Product", "Updated Description", BigDecimal.valueOf(199.99), 5);

        mockMvc.perform(put("/api/products/" + savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(updatedProduct.getName())))
                .andExpect(jsonPath("$.description", is(updatedProduct.getDescription())))
                .andExpect(jsonPath("$.price", is(199.99)))
                .andExpect(jsonPath("$.stockQuantity", is(5)));
    }

    @Test
    void deleteProduct_WithExistingId_ShouldReturnNoContent() throws Exception {
        Product savedProduct = productRepository.save(
                new Product("Test Product", "Test Description", BigDecimal.valueOf(99.99), 10)
        );

        mockMvc.perform(delete("/api/products/" + savedProduct.getId()))
                .andExpect(status().isNoContent());

        assertFalse(productRepository.existsById(savedProduct.getId()));
    }

    @Test
    void healthCheck_ShouldReturnStatusUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
