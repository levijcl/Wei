package com.wei.orchestrator.wes.query;

import com.wei.orchestrator.wes.query.dto.PickingTaskDetailDto;
import com.wei.orchestrator.wes.query.dto.PickingTaskSummaryDto;
import java.util.List;

public interface PickingTaskQueryService {

    PickingTaskDetailDto getPickingTask(String taskId);

    List<PickingTaskSummaryDto> getPickingTasksByOrderId(String orderId);
}
