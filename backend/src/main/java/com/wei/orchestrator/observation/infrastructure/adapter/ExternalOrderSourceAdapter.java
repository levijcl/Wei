package com.wei.orchestrator.observation.infrastructure.adapter;

import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservedOrderItem;
import com.wei.orchestrator.observation.domain.model.valueobject.SourceEndpoint;
import com.wei.orchestrator.observation.domain.port.OrderSourcePort;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExternalOrderSourceAdapter implements OrderSourcePort {

    private static final Logger logger = LoggerFactory.getLogger(ExternalOrderSourceAdapter.class);

    @Override
    public List<ObservationResult> fetchNewOrders(
            SourceEndpoint sourceEndpoint, LocalDateTime since) {
        List<ObservationResult> results = new ArrayList<>();

        String sql = getString(since);

        try (Connection connection = createConnection(sourceEndpoint);
                PreparedStatement statement = connection.prepareStatement(sql)) {

            if (since != null) {
                statement.setTimestamp(1, Timestamp.valueOf(since));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String orderId = resultSet.getString("order_id");
                    String customerName = resultSet.getString("customer_name");
                    String customerEmail = resultSet.getString("customer_email");
                    String shippingAddress = resultSet.getString("shipping_address");
                    String orderType = resultSet.getString("order_type");
                    String warehouseId = resultSet.getString("warehouse_id");
                    String status = resultSet.getString("status");
                    Timestamp createdAt = resultSet.getTimestamp("created_at");

                    List<ObservedOrderItem> items = fetchOrderItems(connection, orderId);

                    if (!items.isEmpty()) {
                        ObservationResult result =
                                new ObservationResult(
                                        orderId,
                                        customerName,
                                        customerEmail,
                                        shippingAddress,
                                        orderType,
                                        warehouseId,
                                        status,
                                        items,
                                        createdAt.toLocalDateTime());

                        results.add(result);
                    }
                }
            }

            logger.info("Fetched {} new orders from external source", results.size());

        } catch (SQLException e) {
            logger.error("Error polling external order source", e);
            throw new RuntimeException("Failed to poll external order source", e);
        }

        return results;
    }

    private List<ObservedOrderItem> fetchOrderItems(Connection connection, String orderId)
            throws SQLException {
        List<ObservedOrderItem> items = new ArrayList<>();

        String sql =
                """
                SELECT sku, product_name, quantity, price
                FROM order_items
                WHERE order_id = ?
                ORDER BY created_at
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String sku = resultSet.getString("sku");
                    String productName = resultSet.getString("product_name");
                    int quantity = resultSet.getInt("quantity");
                    BigDecimal price = resultSet.getBigDecimal("price");

                    ObservedOrderItem item =
                            new ObservedOrderItem(sku, productName, quantity, price);
                    items.add(item);
                }
            }
        }

        return items;
    }

    private static String getString(LocalDateTime lastPolledTimestamp) {
        String sql;
        if (lastPolledTimestamp == null) {
            sql =
                    """
                    SELECT order_id, customer_name, customer_email, shipping_address,
                           order_type, warehouse_id, status, created_at
                    FROM orders
                    WHERE status = 'NEW'
                    ORDER BY created_at ASC
                    FETCH FIRST 50 ROWS ONLY
                    """;
        } else {
            sql =
                    """
                    SELECT order_id, customer_name, customer_email, shipping_address,
                           order_type, warehouse_id, status, created_at
                    FROM orders
                    WHERE status = 'NEW'
                    AND created_at > ?
                    ORDER BY created_at ASC
                    FETCH FIRST 50 ROWS ONLY
                    """;
        }
        return sql;
    }

    @Override
    public boolean markOrderAsProcessed(SourceEndpoint sourceEndpoint, String orderId) {
        String sql =
                """
                UPDATE orders
                SET status = 'IN_PROGRESS',
                    updated_at = SYSTIMESTAMP
                WHERE order_id = ?
                """;

        try (Connection connection = createConnection(sourceEndpoint);
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, orderId);
            int rowsAffected = statement.executeUpdate();

            logger.info("Marked order {} as IN_PROGRESS in external source", orderId);

            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error marking order as processed in external source", e);
            throw new RuntimeException("Failed to mark order as processed", e);
        }
    }

    private Connection createConnection(SourceEndpoint sourceEndpoint) throws SQLException {
        return DriverManager.getConnection(
                sourceEndpoint.getJdbcUrl(),
                sourceEndpoint.getUsername(),
                sourceEndpoint.getPassword());
    }
}
