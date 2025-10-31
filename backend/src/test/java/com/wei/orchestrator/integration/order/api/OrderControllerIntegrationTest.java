package com.wei.orchestrator.integration.order.api;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wei.orchestrator.order.api.OrderController;
import com.wei.orchestrator.order.api.dto.CreateOrderRequest;
import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OrderApplicationService orderApplicationService;

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        "ORDER-001",
                        Arrays.asList(
                                new CreateOrderRequest.OrderLineItemDto(
                                        "SKU-001", 2, new BigDecimal("100.00")),
                                new CreateOrderRequest.OrderLineItemDto(
                                        "SKU-002", 1, new BigDecimal("50.00"))));

        Order mockOrder =
                createMockOrder(
                        "ORDER-001",
                        Arrays.asList(
                                new OrderLineItem("SKU-001", 2, new BigDecimal("100.00")),
                                new OrderLineItem("SKU-002", 1, new BigDecimal("50.00"))));

        when(orderApplicationService.createOrder(any())).thenReturn(mockOrder);

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("ORDER-001"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].price").value(100.00))
                .andExpect(jsonPath("$.items[1].sku").value("SKU-002"))
                .andExpect(jsonPath("$.items[1].quantity").value(1))
                .andExpect(jsonPath("$.items[1].price").value(50.00))
                .andExpect(jsonPath("$.reservationInfo").doesNotExist())
                .andExpect(jsonPath("$.shipmentInfo").doesNotExist());
    }

    @Test
    void shouldReturnBadRequestWhenOrderIdIsBlank() throws Exception {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        "",
                        List.of(
                                new CreateOrderRequest.OrderLineItemDto(
                                        "SKU-001", 2, new BigDecimal("100.00"))));

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenItemsIsEmpty() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("ORDER-001", Collections.emptyList());

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenSkuIsBlank() throws Exception {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        "ORDER-001",
                        Arrays.asList(
                                new CreateOrderRequest.OrderLineItemDto(
                                        "", 2, new BigDecimal("100.00"))));

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenQuantityIsZero() throws Exception {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        "ORDER-001",
                        Arrays.asList(
                                new CreateOrderRequest.OrderLineItemDto(
                                        "SKU-001", 0, new BigDecimal("100.00"))));

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenQuantityIsNegative() throws Exception {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        "ORDER-001",
                        Arrays.asList(
                                new CreateOrderRequest.OrderLineItemDto(
                                        "SKU-001", -1, new BigDecimal("100.00"))));

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenPriceIsNegative() throws Exception {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        "ORDER-001",
                        Arrays.asList(
                                new CreateOrderRequest.OrderLineItemDto(
                                        "SKU-001", 2, new BigDecimal("-10.00"))));

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateOrderWithZeroPrice() throws Exception {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        "ORDER-003",
                        Arrays.asList(
                                new CreateOrderRequest.OrderLineItemDto(
                                        "SKU-001", 1, BigDecimal.ZERO)));

        Order mockOrder =
                createMockOrder(
                        "ORDER-003", List.of(new OrderLineItem("SKU-001", 1, BigDecimal.ZERO)));

        when(orderApplicationService.createOrder(any())).thenReturn(mockOrder);

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].price").value(0));
    }

    @Test
    void shouldCreateOrderWithMultipleItems() throws Exception {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        "ORDER-001",
                        Arrays.asList(
                                new CreateOrderRequest.OrderLineItemDto(
                                        "SKU-001", 1, new BigDecimal("10.00")),
                                new CreateOrderRequest.OrderLineItemDto(
                                        "SKU-002", 2, new BigDecimal("20.00")),
                                new CreateOrderRequest.OrderLineItemDto(
                                        "SKU-003", 3, new BigDecimal("30.00"))));

        Order mockOrder =
                createMockOrder(
                        "ORDER-001",
                        Arrays.asList(
                                new OrderLineItem("SKU-001", 1, new BigDecimal("10.00")),
                                new OrderLineItem("SKU-002", 2, new BigDecimal("20.00")),
                                new OrderLineItem("SKU-003", 3, new BigDecimal("30.00"))));

        when(orderApplicationService.createOrder(any())).thenReturn(mockOrder);

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(3)));
    }

    private Order createMockOrder(String orderId, List<OrderLineItem> items) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(OrderStatus.CREATED);
        order.setOrderLineItems(items);
        return order;
    }
}
