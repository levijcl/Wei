package com.wei.orchestrator.wes.query;

import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.query.dto.PickingTaskDetailDto;
import com.wei.orchestrator.wes.query.dto.PickingTaskSummaryDto;
import com.wei.orchestrator.wes.query.infrastructure.PickingTaskQueryRepository;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PickingTaskQueryServiceImpl implements PickingTaskQueryService {

    private final PickingTaskQueryRepository pickingTaskQueryRepository;

    public PickingTaskQueryServiceImpl(PickingTaskQueryRepository pickingTaskQueryRepository) {
        this.pickingTaskQueryRepository = pickingTaskQueryRepository;
    }

    @Override
    public PickingTaskDetailDto getPickingTask(String taskId) {
        List<Object[]> rows = pickingTaskQueryRepository.findTaskDetailById(taskId);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Picking task not found: " + taskId);
        }

        return mapRowsToDetailDto(rows);
    }

    @Override
    public List<PickingTaskSummaryDto> getPickingTasksByOrderId(String orderId) {
        List<Object[]> rows = pickingTaskQueryRepository.findTaskSummariesByOrderId(orderId);
        return rows.stream().map(this::mapRowToSummaryDto).collect(Collectors.toList());
    }

    private PickingTaskDetailDto mapRowsToDetailDto(List<Object[]> rows) {
        Object[] firstRow = rows.get(0);

        String taskId = (String) firstRow[0];
        String wesTaskId = (String) firstRow[1];
        String orderId = (String) firstRow[2];
        TaskOrigin origin = TaskOrigin.valueOf((String) firstRow[3]);
        Integer priority = (Integer) firstRow[4];
        TaskStatus status = TaskStatus.valueOf((String) firstRow[5]);
        LocalDateTime createdAt = toLocalDateTime(firstRow[6]);
        LocalDateTime canceledAt = toLocalDateTime(firstRow[7]);
        LocalDateTime completedAt = toLocalDateTime(firstRow[8]);
        String failureReason = (String) firstRow[9];

        List<PickingTaskDetailDto.TaskItemDto> items = new ArrayList<>();
        for (Object[] row : rows) {
            String sku = (String) row[10];
            if (sku != null) {
                Integer quantity = (Integer) row[11];
                String location = (String) row[12];
                items.add(new PickingTaskDetailDto.TaskItemDto(sku, quantity, location));
            }
        }

        return new PickingTaskDetailDto(
                taskId,
                wesTaskId,
                orderId,
                origin,
                priority,
                status,
                items,
                createdAt,
                null,
                completedAt,
                canceledAt,
                failureReason);
    }

    private PickingTaskSummaryDto mapRowToSummaryDto(Object[] row) {
        String taskId = (String) row[0];
        String wesTaskId = (String) row[1];
        String orderId = (String) row[2];
        TaskOrigin origin = TaskOrigin.valueOf((String) row[3]);
        Integer priority = (Integer) row[4];
        TaskStatus status = TaskStatus.valueOf((String) row[5]);
        LocalDateTime createdAt = toLocalDateTime(row[6]);
        LocalDateTime canceledAt = toLocalDateTime(row[7]);
        LocalDateTime completedAt = toLocalDateTime(row[8]);
        int itemCount =
                row[9] instanceof BigInteger
                        ? ((BigInteger) row[9]).intValue()
                        : ((Number) row[9]).intValue();

        return new PickingTaskSummaryDto(
                taskId,
                wesTaskId,
                orderId,
                origin,
                priority,
                status,
                itemCount,
                createdAt,
                canceledAt,
                completedAt);
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        throw new IllegalArgumentException("Unexpected datetime type: " + value.getClass());
    }
}
