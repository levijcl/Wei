package com.wei.orchestrator.order.query;

import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.query.dto.OrderDetailDto;
import com.wei.orchestrator.order.query.dto.OrderProcessStatusDto;
import com.wei.orchestrator.order.query.dto.OrderSummaryDto;
import com.wei.orchestrator.order.query.dto.ProcessStepDetailDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderQueryService {

    Page<OrderSummaryDto> getOrders(List<OrderStatus> statuses, Pageable pageable);

    OrderDetailDto getOrderDetail(String orderId);

    OrderProcessStatusDto getOrderProcessStatus(String orderId);

    ProcessStepDetailDto getOrderProcessStepDetail(String orderId, int stepNumber);
}
