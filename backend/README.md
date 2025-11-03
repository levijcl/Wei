# 倉儲出貨流程協調系統（Orchestrator System）設計文件

## 1. 系統定位與角色

本系統為一套 **Orchestrator System（倉儲流程協調系統）**，負責協調與整合內部與外部系統間的出貨流程，包括：

- 訂單處理與流程分派（picking / packing）
- 庫存預約（reservation / commit）
- 自動倉揀貨任務建立與追蹤
- 出貨與物流資訊同步
- 回庫（return）流程
- 庫存一致性 Dashboard

系統角色定位：

- 並非傳統 WMS（Warehouse Management System），不直接管理貨架與儲位。
- 而是位於 **Inventory 系統、WES 系統、物流系統** 之上的協調層（Orchestration Layer）。
- 核心任務為：
    1. 驅動並追蹤整體出貨作業流程。
    2. 維持資料一致性與狀態同步。
    3. 管理異常重試與錯誤回復。

## 2. 系統整體架構

### 2.1 系統組成

| 系統名稱 | 說明 |
|-----------|------|
| **Order Source System** | 已開發完成的訂單來源系統，本系統需透過輪詢（polling）方式獲取新訂單 |
| **Orchestrator System** | 本系統，負責協調與整合流程 |
| **Inventory System** | 內部系統，提供庫存管理、reservation / commit API、與 WES 庫存同步 |
| **WES System** | 外包智慧倉儲控制系統，僅支援 API，不支援 webhook；需由本系統主動 polling 任務狀態 |
| **Logistics System** | 外包物流出貨系統，負責出貨單建立與配送狀態同步 |

## 模組

### Order Polling 模組

- 由 Orchestrator 系統定時（例如每 30 秒或 1 分鐘）呼叫 Order Source System Database
- 查詢新訂單（依狀態或建立時間區間）。
- 對於每筆新訂單：
    1. 寫入 Orchestrator 的訂單暫存表（Order Buffer Table）。
    2. 在原訂單系統中標記為「已接收」或「處理中」。
    3. 判斷訂單所屬流程類型（流程 A / 流程 B），建立對應的任務。

#### 重複偵測機制

- 每筆訂單以 `order_id` 進行 idempotent 檢查。
- 若偵測到重複訂單，忽略後續重複資料。
- 訂單狀態設計：
  - `NEW`：尚未處理
  - `IN_PROGRESS`：處理中
  - `COMPLETED`：已完成
  - `FAILED`：處理失敗，待重試或人工介入

#### 輪詢頻率與效能考量

- 預設每分鐘一次，可依負載調整。
- 若訂單量大，可採 **分區式輪詢**（依倉別或建立時間區段分批）。
- 應設計 **Job Lock 機制**，確保同時間僅一個 polling job 執行。
- 可記錄上次輪詢時間戳（last polled timestamp）以避免重疊區間。

### 資料同步模組

#### 庫存差異處理（Inventory ↔ WES）

#### 問題說明

在整合環境中，`Inventory 系統` 與 `WES 系統` 均維護庫存資料，但由於作業流程複雜、API 延遲或作業異常，兩者間可能出現以下狀況：

| 差異類型        | 說明                                                             |
| ----------- | -------------------------------------------------------------- |
| **數量差異**    | WES 回報實際庫存與 Inventory 記錄不同（例如 WES 有 95 件，但 Inventory 顯示 100 件） |
| **儲位差異**    | WES 的儲位資料與 Inventory 的倉別或區域設定不一致                               |
| **任務差異**    | WES 已完成 picking，但 Inventory 未 commit reservation               |
| **回庫/報廢差異** | WES 有異動，但 Inventory 未更新（或反之）                                   |

#### 差異可能來源

- WES API 任務回報延遲或失敗（callback/polling 超時）
- Inventory reservation/commit 流程中斷（例如系統重啟、DB transaction rollback）
- 實體倉儲作業異常（操作員誤放貨物）
- 系統批次同步任務失敗

#### 差異偵測機制

系統應具備 **雙向庫存比對機制**：

1. **WES → Inventory 定期同步任務**

- WES 端提供倉別與 SKU 層級的「庫存快照」API。
- Orchestrator 每日（或每小時）呼叫此 API，與 Inventory 系統資料比對。
- 若發現差異，記錄在 `StockDiscrepancyLog`。

2. **Inventory → WES 對照同步**

- 當 Inventory 有人工調整、退貨、報廢或庫存更動時，應主動通知 Orchestrator。
- Orchestrator 再透過 WES API 更新對應數量。
- 若更新失敗，進入「待同步佇列」。

#### 差異處理策略

#### Case：**Inventory 有庫存但 WES 顯示無庫存**

**狀況**
代表實體倉庫缺貨，但 Inventory 資料未更新。

**解法**

1. 暫停該 SKU 的自動訂單分配。
2. 通知 Inventory 系統進行校正。
3. 可由 WES 提供的快照覆蓋同步數據，更新 Inventory。

#### Case：**WES 有庫存但 Inventory 顯示為 0**

**狀況**
通常為回庫或報廢流程未更新。

**解法**

1. Orchestrator 偵測差異後自動補上 Inventory 更新。
2. 若多筆 SKU 發生類似狀況，排程全倉同步任務（Full Sync Job）。

#### Case：**兩邊庫存差異持續超過閾值**

**狀況**
例如某 SKU 差異 >5%。

**解法**

1. 自動產生 `Stock Reconciliation Task`。
2. 指派給倉庫作業員進行盤點。
3. Orchestrator 在盤點完成後重新同步雙方數據。

### 差異紀錄與報表

建立 `StockDiscrepancyLog` 資料表，紀錄所有差異事件：

| 欄位            | 說明                  |
| ------------- | ------------------- |
| sku_code      | 商品代碼                |
| warehouse_id  | 倉別代碼                |
| inventory_qty | Inventory 系統數量      |
| wes_qty       | WES 系統數量            |
| discrepancy   | 差異量                 |
| detected_at   | 偵測時間                |
| status        | `OPEN` / `RESOLVED` |
| resolved_by   | 處理人員                |
| note          | 備註                  |

報表可依照倉別、商品、時間區間進行查詢，支援每日快照比較。

#### 庫存同步策略總覽

| 情境     | 主導系統      | 同步方向        | 機制               |
| ------ | --------- | ----------- | ---------------- |
| 出貨任務完成 | WES       | → Inventory | commit API       |
| 回庫任務完成 | WES       | → Inventory | restock API      |
| 人工調整庫存 | Inventory | → WES       | update stock API |
| 定期盤點同步 | 雙向        | ↔           | 每日 Full Sync Job |

#### 建議實作要點

- 每筆庫存變動皆附帶 `transaction_id` 以追蹤來源。
- 若兩系統都支援 version number 或 updated_at 欄位，可用作增量同步依據。
- Polling 任務應有重試機制與防止重疊執行的 lock。
- 建議在 Orchestrator 加入「庫存一致性 Dashboard」，即時顯示差異統計。

## 4. 系統核心流程

### 4.1 流程 A：自動倉揀貨 → Picking Zone 待取貨

**流程說明：**
適用於需要 operator 揀貨、delivery man 取貨的情境。

**流程步驟：**

1. Polling 偵測到新訂單。
2. 呼叫 Inventory API 進行 `reservation`。
3. 呼叫 WES API 建立 picking 任務。
4. Orchestrator 定期 **polling WES 任務狀態**（由我方主動輪詢，而非依賴對方 webhook）。
5. 若 WES 回報任務完成：
    - 呼叫 Inventory API 進行 `commit`。
    - 更新內部任務狀態為「已完成」。
6. 通知內部系統或介面顯示「可取貨」。
7. Delivery man 取貨 → 呼叫 Logistics API 更新出貨狀態（例如 `dispatched`）。

### 4.2 流程 B：自動倉揀貨 + Packing List 印製

**流程說明：**
適用於由 operator 負責揀貨與包裝的情境。

**流程步驟：**

1. Polling 偵測到新訂單。
2. 呼叫 Inventory API 進行 `reservation`。
3. 呼叫 WES API 建立 picking 任務。
4. Orchestrator polling WES 任務狀態。
5. 若任務完成：
    - 呼叫 Inventory API 進行 `commit`。
    - 觸發 Packing List 印製流程。
6. 呼叫 Logistics 系統建立出貨單與標籤。
7. 更新 Orchestrator 訂單狀態為「已出貨」。

### 4.3 回庫（Return / Restock）流程

**流程說明：**
處理退貨或回庫場景。

**流程步驟：**

1. Logistics 系統或內部作業觸發回庫請求。
2. Orchestrator 建立回庫任務。
3. 呼叫 WES 建立 inbound 任務。
4. Polling WES 任務狀態。
5. 任務完成後：
    - 呼叫 Inventory API 更新庫存（增加庫存量）。
    - 更新訂單與任務狀態為「已回庫」。

### 4.4 人工盤點

**流程說明：**
適用於由 Inventory 系統與WES之間某SKU差異過大的時候。

**流程步驟：**

1. Operatro去盤點
2. 盤點完成之後更新Inventory 庫存以及 WES庫存

--

## 5. 系統整合介面（Integration Points）

| 系統 | 整合方式 | 功能 |
|------|------------|------|
| **Order Source System** | REST API（polling） | 取得新訂單清單、標記訂單狀態 |
| **Inventory System** | REST API | Reservation、Commit、庫存同步 |
| **WES System** | REST API（polling task status） | 建立揀貨任務、查詢任務狀態 |
| **Logistics System** | REST API | 建立出貨單、查詢配送狀態、建立回庫任務 |

## 6. Polling 策略與 WES 整合考量

由於 WES 雖提供 callback API，但考慮到穩定性與一致性，本系統選擇：

- **主動 polling** 模式：  
  由 Orchestrator 定期查詢 WES 任務狀態，避免遺漏 callback 或網路異常造成任務狀態錯誤。
- Polling 間隔建議：30 秒～1 分鐘，視任務量調整。
- 若發現任務長時間未更新，可觸發異常警報或人工介入。

此策略可確保：

- 任務狀態一致性。
- 降低外部系統誤觸發風險。
- 便於重試與追蹤。

--

## 7. 狀態與錯誤管理（概述）

| 狀態 | 說明 |
|------|------|
| `NEW` | 訂單新建立，尚未開始處理 |
| `IN_PROGRESS` | 任務執行中（包含 reservation / picking / packing） |
| `WAIT_FOR_PICKUP` | 已完成揀貨，等待取貨 |
| `SHIPPED` | 已出貨 |
| `RETURNING` | 回庫中 |
| `COMPLETED` | 全流程完成 |
| `FAILED` | 發生錯誤，待人工或自動重試 |

**錯誤處理策略：**

- 針對可恢復錯誤（如 API timeout、暫時性失敗）→ 自動重試（最多 3 次）。
- 不可恢復錯誤（如資料不一致、無庫存）→ 記錄 error log 並進入人工審核。
- 所有外部呼叫均應具備 **request log** 與 **correlation ID** 以利追蹤。

## 🧭 Tactical Design — Domain Aggregates Overview

本章節說明系統中的核心 Aggregate 設計與責任劃分。
系統整體由多個 Context 組成，包含：

- **Order Context**
- **Inventory Context**
- **WES Context**
- **Observation Context**

## 🏷️ Aggregate Summary

| Aggregate               | 所屬 Context         | 責任                                                                 | 關聯物件                                       |
| ----------------------- | -------------------- | -------------------------------------------------------------------- | ------------------------------------------ |
| **Order**               | Order Context        | 表示出貨流程主體，包含狀態、任務鏈、對應的 reservation 與 logistics info | `OrderLineItem`, `Reservation`, `Shipment` |
| **PickingTask**         | WES Context          | 管理揀貨任務（出庫），支援雙來源模型 (ORCHESTRATOR_SUBMITTED / WES_DIRECT)，完成時減少庫存 | `TaskItem`, `WesTaskId`, `TaskOrigin`, `TaskStatus` |
| **PutawayTask**         | WES Context          | 管理上架任務（入庫），支援雙來源模型，完成時增加庫存，處理退貨與收貨場景 | `TaskItem`, `WesTaskId`, `TaskOrigin`, `SourceType` |
| **InventoryTransaction**| Inventory Context    | 表示庫存異動（入庫、出庫、調撥等），是實際改變庫存數量的行為主體                | `TransactionLine`, `TransactionType`, `WarehouseLocation` |
| **InventoryAdjustment** | Inventory Context    | 偵測與修正庫存差異，建立對應的 `InventoryTransaction` 校正庫存                | `StockSnapshot`, `DiscrepancyLog`          |
| **OrderObserver**       | Observation Context  | 觀察外部訂單來源資料庫（Oracle），透過 OrderSourcePort 查詢新訂單完整資料，內部收集 NewOrderObservedEvent 並發佈 | `SourceEndpoint`, `PollingInterval`, `ObservationResult`, `ObservedOrderItem` |
| **InventoryObserver**   | Observation Context  | 定期比對內外部庫存數據，偵測差異並產生同步事件                                 | `StockSnapshot`, `ObservationResult`       |
| **WesObserver**         | Observation Context  | 持續輪詢 WES 系統，發現新任務 (task discovery) 並同步所有任務狀態，確保庫存一致性 | `TaskEndpoint`, `WesTaskDto`      |

## ⚙️ Aggregate Relationships Overview

```mermaid
graph TD

%% =========================
%% ORDER CONTEXT
%% =========================
subgraph OrderContext["📦 Order Context"]
  OR[Order]
end

%% =========================
%% WES CONTEXT
%% =========================
subgraph WesContext["🏭 WES Context"]
  PT[PickingTask<br/>揀貨任務 - 減少庫存]
  PUT[PutawayTask<br/>上架任務 - 增加庫存]
end

%% =========================
%% INVENTORY CONTEXT
%% =========================
subgraph InventoryContext["🏬 Inventory Context"]
  IT[InventoryTransaction]
  IA[InventoryAdjustment]
end

%% =========================
%% OBSERVATION CONTEXT
%% =========================
subgraph ObservationContext["👁️ Observation Context"]
  OO[OrderObserver]
  IO[InventoryObserver]
  WO[WesObserver<br/>任務發現 + 狀態同步]
end

%% =========================
%% EXTERNAL SYSTEMS
%% =========================
subgraph ExternalSystems["🌐 External Systems"]
  WES[WES System<br/>wes_tasks table]
end

%% =========================
%% CROSS-CONTEXT INTERACTIONS
%% =========================

%% Observation Context → Order
OO -->|偵測新訂單 / 發事件| OR

%% Order → Inventory
OR -->|建立 Reservation / Commit| IT

%% Order → WES (建立揀貨任務)
OR -->|建立 Picking 任務| PT

%% PickingTask → Inventory (出庫完成時)
PT -->|任務完成 / 減少庫存| IT

%% PutawayTask → Inventory (入庫完成時)
PUT -->|任務完成 / 增加庫存| IT

%% WES → Inventory Adjustment (差異偵測)
IO -->|偵測差異 / 觸發修正| IA
IA -->|生成校正交易| IT

%% WesObserver 任務發現與同步
WO -->|發現新任務<br/>ORCHESTRATOR_SUBMITTED| PT
WO -->|發現新任務<br/>WES_DIRECT| PT
WO -->|發現新任務<br/>ORCHESTRATOR_SUBMITTED| PUT
WO -->|發現新任務<br/>WES_DIRECT| PUT
WO -->|同步狀態| PT
WO -->|同步狀態| PUT

%% PickingTask/PutawayTask 與 WES 整合
PT -.->|submitToWes<br/>WesPort| WES
PUT -.->|submitToWes<br/>WesPort| WES
WO -.->|pollAllTasks<br/>WesPort| WES

%% Observation 觀察
IO -->|監控庫存快照| IA
OO -->|監控訂單來源| OR

```

## 🧭 Tactical Design — Detailed Domain Model

## 1. Contexts & Aggregates Overview

以下是目前的 Context 劃分：

| Context                 | Aggregate                                                   |
| ----------------------- | ----------------------------------------------------------- |
| **Order Context**       | `Order`                                                     |
| **WES Context**         | `PickingTask`, `PutawayTask`                                |
| **Inventory Context**   | `InventoryTransaction`, `InventoryAdjustment`               |
| **Observation Context** | `OrderObserver`, `InventoryObserver`, `WesObserver`         |

---

## 2. Aggregate Command & Domain Event 定義

### 🧩 **Order Context**

#### Aggregate: `Order`

| 類型          | 名稱                       | 說明                             |
| ----------- | ------------------------ | ------------------------------ |
| **Command** | `CreateOrder(orderData)` | 建立新訂單（由 OrderObserver 或上游系統觸發） |
| **Command** | `ReserveInventory()`     | 呼叫 Inventory Context 進行預約庫存    |
| **Command** | `CommitInventory()`      | 庫存扣減完成，確認出貨                    |
| **Command** | `CreatePickingTask()`    | 產生對應的 WES picking 任務           |
| **Event**   | `OrderCreated`           | 訂單建立完成                         |
| **Event**   | `OrderReserved`          | 完成庫存預約                         |
| **Event**   | `OrderCommitted`         | 完成庫存扣減                         |
| **Event**   | `OrderReadyForPickup`    | 任務完成、等待出貨                      |
| **Event**   | `OrderShipped`           | 已出貨                            |
| **Event**   | `OrderFailed`            | 處理異常                           |

---

### 🏭 **WES Context**

WES Context 負責管理倉儲執行系統（WES）中的揀貨與上架任務。
本 Context 採用 **Customer-Supplier Pattern**，Orchestrator 為 Customer（上游），WES 為 Supplier（下游）。
透過 **Anti-Corruption Layer (WesPort)** 隔離外部系統，確保領域模型純淨。

**核心設計原則：**
- **管理所有 WES 任務**（包含 orchestrator 提交的任務及 WES 系統直接建立的任務）
- **雙來源模型 (Dual-Origin Model)**：區分任務來源 (ORCHESTRATOR_SUBMITTED vs WES_DIRECT)
- **獨立的 Aggregate 設計**：PickingTask（出庫）與 PutawayTask（入庫）為獨立聚合根
- **統一的 WesObserver**：透過 WesObserver 持續同步所有 WES 任務狀態，確保庫存一致性

---

#### Aggregate: `PickingTask` (揀貨任務)

**責任：** 管理出庫揀貨任務，完成後**減少庫存**

**設計要點：**
- **Dual-Origin Model**：
  - `ORCHESTRATOR_SUBMITTED`：由 orchestrator 為訂單建立的任務 (orderId 有值)
  - `WES_DIRECT`：使用者直接在 WES 系統建立的任務 (orderId 為 null)
- **Inventory Impact**：任務完成時觸發庫存扣減 (consume stock)
- **Priority Management**：支援動態調整任務優先權 (1-10)
- **One Order → Multiple Tasks**：一個訂單可建立多個揀貨任務

**Aggregate 欄位：**
- `taskId` (String) - Orchestrator 內部任務 ID
- `wesTaskId` (WesTaskId) - WES 系統任務 ID (Value Object)
- `orderId` (String, nullable) - 關聯的訂單 ID (若為 WES_DIRECT 則為 null)
- `origin` (TaskOrigin) - 任務來源：ORCHESTRATOR_SUBMITTED | WES_DIRECT
- `priority` (int) - 優先權 (1-10，數字越大優先權越高)
- `status` (TaskStatus) - 任務狀態：PENDING | SUBMITTED | IN_PROGRESS | COMPLETED | FAILED
- `taskItems` (List<TaskItem>) - 任務明細 (SKU, 數量, 儲位)
- `createdAt`, `submittedAt`, `completedAt` (Timestamp)

**Behaviors：**
- `createForOrder(orderId, items, priority)` - 為訂單建立揀貨任務 (origin: ORCHESTRATOR_SUBMITTED)
- `createFromWesTask(wesTask)` - 從 WES 發現的任務建立 (origin: WES_DIRECT)
- `submitToWes(WesPort)` - 提交任務至 WES 系統，取得 wesTaskId
- `updateStatusFromWes(newStatus)` - 由 WesObserver 同步 WES 狀態
- `adjustPriority(newPriority)` - 調整任務優先權 (1-10)
- `markCompleted()` - 標記完成，觸發庫存扣減
- `markFailed(reason)` - 標記失敗

| 類型          | 名稱                                         | 說明                                             |
| ----------- | ------------------------------------------ | ---------------------------------------------- |
| **Command** | `CreatePickingTaskForOrder(orderId, items, priority)` | 為訂單建立揀貨任務 (origin: ORCHESTRATOR_SUBMITTED)      |
| **Command** | `CreatePickingTaskFromWes(wesTask)`        | 從 WES 發現的任務建立 PickingTask (origin: WES_DIRECT)   |
| **Command** | `SubmitPickingTaskToWes(taskId)`           | 將任務提交至 WES 系統                                   |
| **Command** | `UpdateTaskStatusFromWes(taskId, status)`  | WesObserver 同步 WES 狀態                           |
| **Command** | `AdjustTaskPriority(taskId, newPriority)`  | 調整單一任務優先權                                        |
| **Command** | `AdjustOrderPriority(orderId, newPriority, taskIds?)` | 調整訂單相關任務優先權（可批次或選擇性調整）                           |
| **Event**   | `PickingTaskCreated`                       | 任務建立成功                                           |
| **Event**   | `PickingTaskSubmitted`                     | 任務已提交至 WES (包含 wesTaskId)                       |
| **Event**   | `PickingTaskCompleted`                     | 任務完成 → 觸發 InventoryTransaction (OUTBOUND，減少庫存)    |
| **Event**   | `PickingTaskFailed`                        | 任務異常                                             |
| **Event**   | `PickingTaskPriorityAdjusted`              | 優先權已調整                                           |

---

#### Aggregate: `PutawayTask` (上架任務)

**責任：** 管理入庫上架任務，完成後**增加庫存**

**設計要點：**
- **Dual-Origin Model**：
  - `ORCHESTRATOR_SUBMITTED`：由 orchestrator 為退貨/入庫建立的任務 (returnId/receivingId 有值)
  - `WES_DIRECT`：使用者直接在 WES 系統建立的任務 (無關聯 ID)
- **Inventory Impact**：任務完成時觸發庫存增加 (increase stock)
- **Priority Management**：支援動態調整任務優先權 (1-10)
- **Triggers**：退貨 (Return) 或收貨 (Receiving) 皆可觸發

**Aggregate 欄位：**
- `taskId` (String) - Orchestrator 內部任務 ID
- `wesTaskId` (WesTaskId) - WES 系統任務 ID (Value Object)
- `sourceId` (String, nullable) - 來源 ID (returnId 或 receivingId，若為 WES_DIRECT 則為 null)
- `sourceType` (SourceType) - 來源類型：RETURN | RECEIVING | DIRECT
- `origin` (TaskOrigin) - 任務來源：ORCHESTRATOR_SUBMITTED | WES_DIRECT
- `priority` (int) - 優先權 (1-10)
- `status` (TaskStatus) - 任務狀態：PENDING | SUBMITTED | IN_PROGRESS | COMPLETED | FAILED
- `taskItems` (List<TaskItem>) - 任務明細
- `createdAt`, `submittedAt`, `completedAt` (Timestamp)

**Behaviors：**
- `createForReturn(returnId, items, priority)` - 為退貨建立上架任務
- `createForReceiving(receivingId, items, priority)` - 為收貨建立上架任務
- `createFromWesTask(wesTask)` - 從 WES 發現的任務建立 (origin: WES_DIRECT)
- `submitToWes(WesPort)` - 提交任務至 WES 系統
- `updateStatusFromWes(newStatus)` - 由 WesObserver 同步 WES 狀態
- `adjustPriority(newPriority)` - 調整任務優先權
- `markCompleted()` - 標記完成，觸發庫存增加
- `markFailed(reason)` - 標記失敗

| 類型          | 名稱                                         | 說明                                             |
| ----------- | ------------------------------------------ | ---------------------------------------------- |
| **Command** | `CreatePutawayTaskForReturn(returnId, items, priority)` | 為退貨建立上架任務 (origin: ORCHESTRATOR_SUBMITTED)         |
| **Command** | `CreatePutawayTaskForReceiving(receivingId, items, priority)` | 為收貨建立上架任務 (origin: ORCHESTRATOR_SUBMITTED)         |
| **Command** | `CreatePutawayTaskFromWes(wesTask)`        | 從 WES 發現的任務建立 PutawayTask (origin: WES_DIRECT)     |
| **Command** | `SubmitPutawayTaskToWes(taskId)`           | 將任務提交至 WES 系統                                   |
| **Command** | `UpdateTaskStatusFromWes(taskId, status)`  | WesObserver 同步 WES 狀態                           |
| **Command** | `AdjustTaskPriority(taskId, newPriority)`  | 調整單一任務優先權                                        |
| **Event**   | `PutawayTaskCreated`                       | 任務建立成功                                           |
| **Event**   | `PutawayTaskSubmitted`                     | 任務已提交至 WES (包含 wesTaskId)                       |
| **Event**   | `PutawayTaskCompleted`                     | 任務完成 → 觸發 InventoryTransaction (INBOUND，增加庫存)     |
| **Event**   | `PutawayTaskFailed`                        | 任務異常                                             |
| **Event**   | `PutawayTaskPriorityAdjusted`              | 優先權已調整                                           |

---

#### Port Interface: `WesPort`

**Anti-Corruption Layer** 隔離外部 WES 系統

```java
interface WesPort {
    WesTaskId submitPickingTask(PickingTask task);
    WesTaskId submitPutawayTask(PutawayTask task);
    WesTaskStatus getTaskStatus(WesTaskId wesTaskId);
    List<WesTaskDto> pollAllTasks();  // 用於 WesObserver
    void updateTaskPriority(WesTaskId wesTaskId, int priority);
    void cancelTask(WesTaskId wesTaskId);
}
```

---

#### Priority Management (優先權管理)

**場景 1: 調整單一任務優先權**
```
Command: AdjustTaskPriority(taskId, newPriority)
→ PickingTask/PutawayTask.adjustPriority(newPriority)
→ WesPort.updateTaskPriority(wesTaskId, newPriority)
→ Event: TaskPriorityAdjusted
```

**場景 2: 調整訂單相關所有任務優先權（批次）**
```
Command: AdjustOrderPriority(orderId, newPriority, applyToAll=true)
→ Query: 查詢所有 orderId 相關的 PickingTask
→ 批次調整所有任務優先權
→ 批次呼叫 WesPort.updateTaskPriority()
```

**場景 3: 選擇性調整訂單任務優先權**
```
Command: AdjustOrderPriority(orderId, newPriority, taskIds=[id1, id2])
→ 僅調整指定的 taskIds
→ 允許使用者靈活控制優先權
```

---

### 🏬 **Inventory Context**

#### Aggregate: `InventoryTransaction`

| 類型          | 名稱                                            | 說明          |
| ----------- | --------------------------------------------- | ----------- |
| **Command** | `CreateInboundTransaction(source, sku, qty)`  | 入庫交易（回庫或補貨） |
| **Command** | `CreateOutboundTransaction(source, sku, qty)` | 出庫交易（出貨或報廢） |
| **Command** | `ApplyAdjustment(adjustmentId, sku, diffQty)` | 根據調整任務修正庫存  |
| **Event**   | `InventoryIncreased`                          | 庫存增加        |
| **Event**   | `InventoryDecreased`                          | 庫存減少        |
| **Event**   | `InventoryTransactionCompleted`               | 庫存異動完成      |

---

#### Aggregate: `InventoryAdjustment`

| 類型          | 名稱                                        | 說明          |
| ----------- | ----------------------------------------- | ----------- |
| **Command** | `DetectDiscrepancy(snapshotA, snapshotB)` | 比對內外庫存，偵測差異 |
| **Command** | `ResolveDiscrepancy(sku, adjustmentQty)`  | 修正庫存差異      |
| **Event**   | `InventoryDiscrepancyDetected`            | 發現庫存差異      |
| **Event**   | `InventoryAdjusted`                       | 差異修正完成      |

### 👁️ **Observation Context**

#### Aggregate: `OrderObserver`

| 類型          | 名稱                                      | 說明                                       |
| ----------- | --------------------------------------- | ---------------------------------------- |
| **Command** | `CreateOrderObserver(observerData)`     | 建立新的訂單觀察者                                |
| **Command** | `PollOrderSource(OrderSourcePort)`      | 定期輪詢訂單來源系統，透過 Port 查詢外部資料庫              |
| **Command** | `ActivateObserver()`                    | 啟用觀察者                                    |
| **Command** | `DeactivateObserver()`                  | 停用觀察者                                    |
| **Event**   | `NewOrderObservedEvent(ObservationResult)` | 偵測到新訂單，包含完整訂單資料（客戶、品項等），發送給 Order Context |

#### Aggregate: `InventoryObserver`

| 類型          | 名稱                             | 說明                             |
| ----------- | ------------------------------ | ------------------------------ |
| **Command** | `PollInventorySnapshot()`      | 取得最新庫存快照                       |
| **Event**   | `InventorySnapshotObserved`    | 偵測到庫存快照                        |
| **Event**   | `InventoryDiscrepancyDetected` | 發現庫存差異（觸發 InventoryAdjustment） |

#### Aggregate: `WesObserver`

**責任：** 持續輪詢 WES 系統，發現新任務並同步所有任務狀態，確保 orchestrator 與 WES 的庫存一致性

**核心功能：**
- **任務發現 (Task Discovery)**：偵測 WES 系統中直接建立的任務 (WES_DIRECT)
- **狀態同步 (Status Sync)**：更新 orchestrator 中 PickingTask/PutawayTask 的狀態
- **庫存一致性保障**：確保所有 WES 任務完成時都能正確觸發庫存異動

**輪詢邏輯：**
```
1. 呼叫 WesPort.pollAllTasks() 取得所有 WES 任務
2. 對每個 WES 任務：
   a. 查詢 orchestrator 中是否存在對應的 PickingTask/PutawayTask (by wesTaskId)
   b. 若存在 → 更新狀態 (UpdateTaskStatusFromWes)
   c. 若不存在 → 建立新 aggregate (CreatePickingTaskFromWes / CreatePutawayTaskFromWes)
      - origin: WES_DIRECT
      - orderId/sourceId: null
3. 發佈事件 (WesTaskDiscovered, WesTaskStatusUpdated)
```

| 類型          | 名稱                     | 說明                     |
| ----------- | ---------------------- | ---------------------- |
| **Command** | `PollWesTaskStatus()`  | 輪詢 WES 所有任務狀態（PICKING + PUTAWAY） |
| **Event**   | `WesTaskDiscovered`    | 發現 WES 系統中的新任務（觸發建立 PickingTask/PutawayTask）|
| **Event**   | `WesTaskStatusUpdated` | 任務狀態更新（通知 PickingTask/PutawayTask） |

--

## 3. 戰術實作層（Tactical Implementation Layer）

```
src/
└── main/
    └── java/
        └── com/
            └── wei/
                └── orchestrator/
                    ├── order/
                    │   ├── api/
                    │   │   ├── OrderController.java
                    │   │   └── dto/
                    │   │       ├── CreateOrderRequest.java
                    │   │       └── OrderResponse.java
                    │   │
                    │   ├── application/
                    │   │   ├── OrderApplicationService.java
                    │   │   ├── command/
                    │   │   │   ├── CreateOrderCommand.java
                    │   │   │   ├── ReserveInventoryCommand.java
                    │   │   │   └── MarkAsShippedCommand.java
                    │   │   └── handler/
                    │   │       └── OrderCommandHandler.java
                    │   │
                    │   ├── domain/
                    │   │   ├── model/
                    │   │   │   ├── Order.java
                    │   │   │   ├── OrderLineItem.java
                    │   │   │   ├── ReservationInfo.java
                    │   │   │   ├── ShipmentInfo.java
                    │   │   │   └── valueobject/
                    │   │   │       └── OrderStatus.java
                    │   │   ├── event/
                    │   │   │   ├── OrderCreatedEvent.java
                    │   │   │   ├── OrderReservedEvent.java
                    │   │   │   └── OrderShippedEvent.java
                    │   │   ├── repository/
                    │   │   │   └── OrderRepository.java
                    │   │   └── service/
                    │   │       └── OrderDomainService.java
                    │   │
                    │   └── infrastructure/
                    │       ├── repository/
                    │       │   └── JpaOrderRepository.java
                    │       ├── mapper/
                    │       │   └── OrderMapper.java
                    │       └── persistence/
                    │           └── OrderEntity.java
                    │
                    ├── inventory/
                    │   ├── api/
                    │   │   └── InventoryController.java
                    │   ├── application/
                    │   │   ├── InventoryApplicationService.java
                    │   │   └── command/
                    │   │       ├── CreateInboundTransactionCommand.java
                    │   │       ├── CreateOutboundTransactionCommand.java
                    │   │       ├── DetectDiscrepancyCommand.java
                    │   │       └── ResolveDiscrepancyCommand.java
                    │   ├── domain/
                    │   │   ├── model/
                    │   │   │   ├── InventoryTransaction.java
                    │   │   │   ├── InventoryAdjustment.java
                    │   │   │   ├── TransactionLine.java
                    │   │   │   └── valueobject/
                    │   │   │       ├── TransactionType.java
                    │   │   │       ├── TransactionStatus.java
                    │   │   │       └── WarehouseLocation.java
                    │   │   ├── event/
                    │   │   │   ├── InventoryAdjustedEvent.java
                    │   │   │   ├── InventoryIncreasedEvent.java
                    │   │   │   ├── InventoryDecreasedEvent.java
                    │   │   │   └── TransactionPostedEvent.java
                    │   │   ├── repository/
                    │   │   │   └── InventoryRepository.java
                    │   │   └── service/
                    │   │       └── InventoryDomainService.java
                    │   └── infrastructure/
                    │       ├── repository/
                    │       │   └── JpaInventoryRepository.java
                    │       ├── mapper/
                    │       │   └── InventoryMapper.java
                    │       └── adapter/
                    │           └── ExternalInventoryAdapter.java
                    │
                    ├── wes/
                    │   ├── application/
                    │   │   ├── PickingTaskApplicationService.java
                    │   │   ├── PutawayTaskApplicationService.java
                    │   │   └── command/
                    │   │       ├── CreatePickingTaskForOrderCommand.java
                    │   │       ├── CreatePickingTaskFromWesCommand.java
                    │   │       ├── CreatePutawayTaskForReturnCommand.java
                    │   │       ├── CreatePutawayTaskFromWesCommand.java
                    │   │       ├── UpdateTaskStatusFromWesCommand.java
                    │   │       ├── AdjustTaskPriorityCommand.java
                    │   │       ├── AdjustOrderPriorityCommand.java
                    │   │       ├── MarkTaskCompletedCommand.java
                    │   │       ├── MarkTaskFailedCommand.java
                    │   │       └── CancelTaskCommand.java
                    │   ├── domain/
                    │   │   ├── model/
                    │   │   │   ├── PickingTask.java
                    │   │   │   ├── PutawayTask.java
                    │   │   │   └── valueobject/
                    │   │   │       ├── WesTaskId.java
                    │   │   │       ├── TaskItem.java
                    │   │   │       ├── TaskStatus.java
                    │   │   │       ├── TaskOrigin.java
                    │   │   │       └── SourceType.java
                    │   │   ├── event/
                    │   │   │   ├── PickingTaskCreatedEvent.java
                    │   │   │   ├── PickingTaskSubmittedEvent.java
                    │   │   │   ├── PickingTaskCompletedEvent.java
                    │   │   │   ├── PickingTaskFailedEvent.java
                    │   │   │   ├── PickingTaskCanceledEvent.java
                    │   │   │   ├── PickingTaskPriorityAdjustedEvent.java
                    │   │   │   ├── PutawayTaskCreatedEvent.java
                    │   │   │   ├── PutawayTaskSubmittedEvent.java
                    │   │   │   ├── PutawayTaskCompletedEvent.java
                    │   │   │   ├── PutawayTaskFailedEvent.java
                    │   │   │   └── PutawayTaskPriorityAdjustedEvent.java
                    │   │   ├── repository/
                    │   │   │   ├── PickingTaskRepository.java
                    │   │   │   └── PutawayTaskRepository.java
                    │   │   ├── port/
                    │   │   │   └── WesPort.java
                    │   │   └── service/
                    │   │       └── WesTaskDomainService.java
                    │   └── infrastructure/
                    │       ├── persistence/
                    │       │   ├── PickingTaskEntity.java
                    │       │   ├── PutawayTaskEntity.java
                    │       │   └── TaskItemEntity.java
                    │       ├── mapper/
                    │       │   ├── PickingTaskMapper.java
                    │       │   └── PutawayTaskMapper.java
                    │       ├── repository/
                    │       │   ├── JpaPickingTaskRepository.java
                    │       │   ├── JpaPutawayTaskRepository.java
                    │       │   ├── JpaTaskItemRepository.java
                    │       │   ├── PickingTaskRepositoryImpl.java
                    │       │   └── PutawayTaskRepositoryImpl.java
                    │       └── adapter/
                    │           └── WesHttpAdapter.java
                    │
                    ├── observation/
                    │   ├── application/
                    │   │   ├── OrderObserverApplicationService.java
                    │   │   └── command/
                    │   │       ├── CreateOrderObserverCommand.java
                    │   │       └── PollOrderSourceCommand.java
                    │   ├── domain/
                    │   │   ├── model/
                    │   │   │   ├── OrderObserver.java
                    │   │   │   ├── InventoryObserver.java
                    │   │   │   ├── WesObserver.java
                    │   │   │   └── valueobject/
                    │   │   │       ├── SourceEndpoint.java
                    │   │   │       ├── PollingInterval.java
                    │   │   │       ├── ObservationResult.java
                    │   │   │       ├── ObservedOrderItem.java
                    │   │   │       └── ObservationRule.java
                    │   │   ├── event/
                    │   │   │   ├── NewOrderObservedEvent.java
                    │   │   │   └── WesTaskPolledEvent.java
                    │   │   ├── port/
                    │   │   │   └── OrderSourcePort.java
                    │   │   └── repository/
                    │   │       └── OrderObserverRepository.java
                    │   └── infrastructure/
                    │       ├── adapter/
                    │       │   └── ExternalOrderSourceAdapter.java
                    │       ├── mapper/
                    │       │   └── OrderObserverMapper.java
                    │       ├── persistence/
                    │       │   └── OrderObserverEntity.java
                    │       └── repository/
                    │           ├── JpaOrderObserverRepository.java
                    │           └── OrderObserverRepositoryImpl.java
                    │
                    └── shared/
                        ├── domain/
                        │   ├── model/
                        │   │   ├── AuditRecord.java
                        │   │   └── valueobject/
                        │   │       └── EventMetadata.java
                        │   └── service/
                        │       ├── DomainEventPublisher.java
                        │       └── AuditService.java
                        └── infrastructure/
                            ├── repository/
                            │   └── AuditRepositoryImpl.java
                            └── persistence/
                                └── AuditRecordEntity.java
```

## 4. Audit Logging 的戰術設計

因為 **Audit Logging** 是全域關注點（Cross-cutting Concern），
最適合放在一個 **Shared Kernel / Shared Context** 中，
以 **事件訂閱 (Event Subscriber)** 或 **Decorator Pattern** 的方式自動記錄。

### ✅ 建議設計

| 類型                | 名稱                   | 說明                          |
| ----------------- | -------------------- | --------------------------- |
| **Entity**        | `AuditLog`           | 紀錄發生的事件內容與執行命令              |
| **Event Handler** | `AuditLogSubscriber` | 訂閱所有 Domain Event，自動寫入紀錄    |
| **Repository**    | `AuditLogRepository` | 儲存審計紀錄（DB or ElasticSearch） |

## 🧭 **Command–Event Flow (跨 Context 互動圖)**

```mermaid
flowchart TD

%% ===== Order Context =====
subgraph ORDER[Order Context]
    OC1[CreateOrder Command]
    OC2[ReserveInventory Command]
    OC3[CommitInventory Command]
    OC4[CreatePickingTask Command]

    OE1[OrderCreated Event]
    OE2[OrderReserved Event]
    OE3[OrderCommitted Event]
    OE4[OrderReadyForPickup Event]
    OE5[OrderShipped Event]
end

%% ===== WES Context =====
subgraph WES[WES Context]
    WC1[CreatePickingTask Command]
    WC2[UpdateTaskStatus Command]

    WE1[PickingTaskCreated Event]
    WE2[PickingTaskCompleted Event]
    WE3[PickingTaskFailed Event]
end

%% ===== Inventory Context =====
subgraph INV[Inventory Context]
    IC1[CreateInboundTransaction Command]
    IC2[CreateOutboundTransaction Command]
    IC3[ApplyAdjustment Command]
    IC4[DetectDiscrepancy Command]
    IC5[ResolveDiscrepancy Command]

    IE1[InventoryIncreased Event]
    IE2[InventoryDecreased Event]
    IE3[InventoryTransactionCompleted Event]
    IE4[InventoryDiscrepancyDetected Event]
    IE5[InventoryAdjusted Event]
end

%% ===== Observation Context =====
subgraph OBS[Observation Context]
    OB1[PollOrderSource Command]
    OB2[PollInventorySnapshot Command]
    OB3[PollWesTaskStatus Command]

    OBE1[NewOrderObserved Event]
    OBE2[InventorySnapshotObserved Event]
    OBE3[WesTaskStatusUpdated Event]
end

%% ===== Shared: Audit Logging =====
subgraph AUDIT[Audit Logging Shared Context]
    AL[AuditLogSubscriber]
end

%% ========== Flow Relations ==========

%% Observation -> Order
OB1 --> OBE1
OBE1 --> OC1

%% Order internal flow
OC1 --> OE1 --> OC2
OC2 --> OE2 --> IC2
OC3 --> OE3 --> WC1
OC4 --> OE4 --> WC1
OE5 --> AL

%% WES flow
WC1 --> WE1
WE1 --> AL
WC2 --> WE2
WE2 --> IC2
WE3 --> AL

%% Inventory flow
IC1 --> IE1 --> IE3
IC2 --> IE2 --> IE3
IC3 --> IE5 --> AL
IC4 --> IE4 --> IC5
IC5 --> IE5 --> AL

%% Observation - Inventory
OB2 --> OBE2 --> IC4
OB3 --> OBE3 --> WC2

%% Event Logging
OE1 --> AL
OE2 --> AL
OE3 --> AL
OE4 --> AL
WE1 --> AL
WE2 --> AL
WE3 --> AL
IE1 --> AL
IE2 --> AL
IE3 --> AL
IE4 --> AL
IE5 --> AL
OBE1 --> AL
OBE2 --> AL
OBE3 --> AL
```

--

### 🟦 1. Observation Context

- 定期輪詢上游資料源（例如 ERP / WES / WMS）。
- 當偵測到新訂單或庫存異常，觸發對應事件：

  - `NewOrderObserved` → 觸發 `CreateOrder`
  - `InventorySnapshotObserved` → 觸發 `DetectDiscrepancy`
  - `WesTaskStatusUpdated` → 觸發 `UpdateTaskStatus`

---

### 🟧 2. Order Context

- 收到 `NewOrderObserved` 後建立 `Order`。
- 預約庫存 (`ReserveInventory`) → 由 Inventory Context 執行。
- 出貨完成後 (`OrderCommitted`、`OrderShipped`) 通知 Audit Logging。

---

### 🟨 3. WES Context

- `CreatePickingTask` 由 Order Context 觸發。
- 任務完成 (`PickingTaskCompleted`) 後，觸發 Inventory 出庫 (`CreateOutboundTransaction`)。
- 任務異常 (`PickingTaskFailed`) 則回報 Audit。

---

### 🟩 4. Inventory Context

- `InventoryTransaction` 處理所有入庫、出庫交易。
- `InventoryAdjustment` 處理庫存差異。
- `ReturnTask` 處理回庫與退貨。
- 所有異動事件（Increased / Decreased / Adjusted）皆被 Audit 記錄。

---

### 🟪 5. Audit Logging

- 為 **全域訂閱者 (Event Subscriber)**。
- 訂閱所有 `DomainEvent`。
- 記錄：

  - Aggregate ID
  - Command / Event Type
  - Timestamp
  - Payload（含來源 Context）

## ⚙️ 延伸建議

若要實作此事件流：

- 使用 **Event Bus（例如 Spring ApplicationEventPublisher / Kafka）**。
- 各 Context 不直接依賴彼此，而是透過事件通訊。
- `AuditLogSubscriber` 可以 async 模式記錄，不影響主流程性能。

--

## 領域模型結構圖（Domain Model Structure Diagram）

```mermaid
%% DDD Tactical Design Diagram: Aggregates, Entities, Value Objects

classDiagram
direction LR

%% ===========================
%%  Order Context
%% ===========================
class Order {
  +orderId
  +status: OrderStatus
  +reservationInfo: ReservationInfo
  +shipmentInfo: ShipmentInfo
  +List~OrderLineItem~
  --
  +createOrder()
  +reserveInventory()
  +commitOrder()
  +markAsShipped()
}

class OrderLineItem {
  +sku
  +quantity
  +price
}

class ReservationInfo {
  +warehouseId
  +reservedQty
  +status
}

class ShipmentInfo {
  +carrier
  +trackingNumber
}

class OrderStatus {
  <<ValueObject>>
  +status: CREATED | RESERVED | COMMITTED | SHIPPED
}

Order "1" --> "many" OrderLineItem
Order --> ReservationInfo
Order --> ShipmentInfo
Order --> OrderStatus

%% ===========================
%%  WES Context
%% ===========================
class PickingTask {
  +taskId: String
  +wesTaskId: WesTaskId
  +orderId: String (nullable)
  +origin: TaskOrigin
  +priority: int (1-10)
  +status: TaskStatus
  +List~TaskItem~ items
  +createdAt, submittedAt, completedAt
  --
  +createForOrder()
  +createFromWesTask()
  +submitToWes()
  +updateStatusFromWes()
  +adjustPriority()
  +markCompleted()
  +markFailed()
}

class PutawayTask {
  +taskId: String
  +wesTaskId: WesTaskId
  +sourceId: String (nullable)
  +sourceType: SourceType
  +origin: TaskOrigin
  +priority: int (1-10)
  +status: TaskStatus
  +List~TaskItem~ items
  +createdAt, submittedAt, completedAt
  --
  +createForReturn()
  +createForReceiving()
  +createFromWesTask()
  +submitToWes()
  +updateStatusFromWes()
  +adjustPriority()
  +markCompleted()
  +markFailed()
}

class TaskItem {
  <<ValueObject>>
  +sku: String
  +quantity: int
  +location: String
}

class TaskStatus {
  <<ValueObject>>
  +status: PENDING | SUBMITTED | IN_PROGRESS | COMPLETED | FAILED
}

class TaskOrigin {
  <<ValueObject>>
  +origin: ORCHESTRATOR_SUBMITTED | WES_DIRECT
}

class SourceType {
  <<ValueObject>>
  +type: RETURN | RECEIVING | DIRECT
}

class WesTaskId {
  <<ValueObject>>
  +value: String
}

PickingTask "1" --> "many" TaskItem
PickingTask --> TaskStatus
PickingTask --> TaskOrigin
PickingTask --> WesTaskId

PutawayTask "1" --> "many" TaskItem
PutawayTask --> TaskStatus
PutawayTask --> TaskOrigin
PutawayTask --> SourceType
PutawayTask --> WesTaskId

%% ===========================
%%  Inventory Context
%% ===========================

class InventoryTransaction {
  +transactionId
  +type: TransactionType
  +warehouseLocation: WarehouseLocation
  +status: TransactionStatus
  +List~TransactionLine~
  --
  +createInboundTransaction()
  +createOutboundTransaction()
  +applyAdjustment()
}

class TransactionLine {
  +sku
  +quantity
}

class TransactionType {
  <<ValueObject>>
  +type: INBOUND | OUTBOUND | ADJUSTMENT
}

class WarehouseLocation {
  <<ValueObject>>
  +warehouseId
  +zone
}

class TransactionStatus {
  <<ValueObject>>
  +status: CREATED | POSTED | FAILED
}

InventoryTransaction "1" --> "many" TransactionLine
InventoryTransaction --> TransactionType
InventoryTransaction --> WarehouseLocation
InventoryTransaction --> TransactionStatus

%% --- InventoryAdjustment Aggregate ---
class InventoryAdjustment {
  +adjustmentId
  +status
  +List~DiscrepancyLog~
  --
  +detectDiscrepancy()
  +resolveDiscrepancy()
}

class StockSnapshot {
  <<ValueObject>>
  +sku
  +quantity
  +warehouseId
}

class DiscrepancyLog {
  <<ValueObject>>
  +sku
  +inventoryQty
  +wesQty
  +difference
}

InventoryAdjustment --> StockSnapshot
InventoryAdjustment "1" --> "many" DiscrepancyLog

%% ===========================
%%  Observation Context
%% ===========================

class OrderObserver {
  +observerId
  +sourceEndpoint: SourceEndpoint
  +pollingInterval: PollingInterval
  +lastPolledTimestamp
  +active
  +List~Object~ domainEvents
  --
  +pollOrderSource(OrderSourcePort)
  +getDomainEvents()
  +clearDomainEvents()
  +activate()
  +deactivate()
  +shouldPoll()
}

class SourceEndpoint {
  <<ValueObject>>
  +jdbcUrl
  +username
  +password
}

class PollingInterval {
  <<ValueObject>>
  +seconds
}

class ObservationResult {
  <<ValueObject>>
  +orderId
  +customerName
  +customerEmail
  +shippingAddress
  +orderType
  +warehouseId
  +status
  +List~ObservedOrderItem~ items
  +observedAt
}

class ObservedOrderItem {
  <<ValueObject>>
  +sku
  +productName
  +quantity
  +price
}

OrderObserver --> SourceEndpoint
OrderObserver --> PollingInterval
ObservationResult "1" --> "many" ObservedOrderItem

class InventoryObserver {
  +observerId
  +observationRule: ObservationRule
  --
  +pollInventorySnapshot()
}

class ObservationRule {
  <<ValueObject>>
  +thresholdPercent
  +checkFrequency
}

InventoryObserver --> ObservationRule

class WesObserver {
  +observerId
  +taskEndpoint: TaskEndpoint
  --
  +pollWesTaskStatus()
}

class TaskEndpoint {
  <<ValueObject>>
  +url
  +authToken
}

WesObserver --> TaskEndpoint

%% ===========================
%%  Shared Context (Audit Logging)
%% ===========================
class AuditRecord {
  +recordId
  +aggregateType
  +aggregateId
  +eventName
  +eventTimestamp
  +eventMetadata: EventMetadata
  --
  +recordAuditLog()
}

class EventMetadata {
  <<ValueObject>>
  +context
  +correlationId
  +triggerSource
}

AuditRecord --> EventMetadata
```

## Event 流程圖

```mermaid
%% DDD Command → Event 流程圖
flowchart TD

subgraph OrderContext [Order Context]
  CMD_CreateOrder[Command: CreateOrder]
  CMD_ConfirmOrder[Command: ConfirmOrder]
  CMD_CompleteOrder[Command: CompleteOrder]
  EVT_OrderCreated[Event: OrderCreated]
  EVT_OrderConfirmed[Event: OrderConfirmed]
  EVT_OrderCompleted[Event: OrderCompleted]

  CMD_CreateOrder -->|validates and builds| EVT_OrderCreated
  CMD_ConfirmOrder -->|transitions state| EVT_OrderConfirmed
  CMD_CompleteOrder -->|finalizes workflow| EVT_OrderCompleted
end

subgraph InventoryContext [Inventory Context]
  CMD_CreateInboundTransaction[Command: CreateInboundTransaction]
  CMD_CreateOutboundTransaction[Command: CreateOutboundTransaction]
  CMD_AdjustInventory[Command: AdjustInventoryDiscrepancy]

  EVT_InventoryIncreased[Event: InventoryIncreased]
  EVT_InventoryDecreased[Event: InventoryDecreased]
  EVT_InventoryAdjusted[Event: InventoryAdjusted]
  EVT_InventoryTransactionCompleted[Event: InventoryTransactionCompleted]

  CMD_CreateInboundTransaction --> EVT_InventoryIncreased
  CMD_CreateOutboundTransaction --> EVT_InventoryDecreased
  CMD_AdjustInventory --> EVT_InventoryAdjusted
  EVT_InventoryIncreased --> EVT_InventoryTransactionCompleted
  EVT_InventoryDecreased --> EVT_InventoryTransactionCompleted
end

subgraph WesContext [WES Context]
  CMD_CreatePickingTaskForOrder[Command: CreatePickingTaskForOrder]
  CMD_CreatePickingTaskFromWes[Command: CreatePickingTaskFromWes]
  CMD_SubmitPickingTask[Command: SubmitPickingTaskToWes]
  CMD_UpdatePickingTaskStatus[Command: UpdateTaskStatusFromWes]
  CMD_AdjustPriority[Command: AdjustTaskPriority]

  CMD_CreatePutawayTaskForReturn[Command: CreatePutawayTaskForReturn]
  CMD_CreatePutawayTaskFromWes[Command: CreatePutawayTaskFromWes]
  CMD_SubmitPutawayTask[Command: SubmitPutawayTaskToWes]

  EVT_PickingTaskCreated[Event: PickingTaskCreated]
  EVT_PickingTaskSubmitted[Event: PickingTaskSubmitted]
  EVT_PickingTaskCompleted[Event: PickingTaskCompleted]
  EVT_TaskPriorityAdjusted[Event: TaskPriorityAdjusted]

  EVT_PutawayTaskCreated[Event: PutawayTaskCreated]
  EVT_PutawayTaskSubmitted[Event: PutawayTaskSubmitted]
  EVT_PutawayTaskCompleted[Event: PutawayTaskCompleted]

  CMD_CreatePickingTaskForOrder --> EVT_PickingTaskCreated
  CMD_CreatePickingTaskFromWes --> EVT_PickingTaskCreated
  CMD_SubmitPickingTask --> EVT_PickingTaskSubmitted
  CMD_UpdatePickingTaskStatus --> EVT_PickingTaskCompleted
  CMD_AdjustPriority --> EVT_TaskPriorityAdjusted

  CMD_CreatePutawayTaskForReturn --> EVT_PutawayTaskCreated
  CMD_CreatePutawayTaskFromWes --> EVT_PutawayTaskCreated
  CMD_SubmitPutawayTask --> EVT_PutawayTaskSubmitted
end

subgraph ObservationContext [Observation Context]
  CMD_PollOrderSource[Command: PollOrderSource]
  CMD_PollInventory[Command: PollInventory]
  CMD_PollWes[Command: PollWes]

  EVT_OrderSourceObserved[Event: OrderSourceObserved]
  EVT_InventoryObserved[Event: InventoryObserved]
  EVT_WesObserved[Event: WesObserved]

  CMD_PollOrderSource --> EVT_OrderSourceObserved
  CMD_PollInventory --> EVT_InventoryObserved
  CMD_PollWes --> EVT_WesObserved
end

subgraph AuditContext [Audit Logging Context]
  CMD_RecordAudit[Command: RecordAuditLog]
  EVT_AuditRecorded[Event: AuditRecorded]
  CMD_RecordAudit --> EVT_AuditRecorded
end

%% Cross Context Event Flow

%% Order → WES (建立揀貨任務)
EVT_OrderCreated --> CMD_CreatePickingTaskForOrder

%% WES Observer 發現與同步
EVT_WesObserved --> CMD_CreatePickingTaskFromWes
EVT_WesObserved --> CMD_CreatePutawayTaskFromWes
EVT_WesObserved --> CMD_UpdatePickingTaskStatus

%% PickingTask → Inventory (出庫)
EVT_PickingTaskCompleted --> CMD_CreateOutboundTransaction

%% PutawayTask → Inventory (入庫)
EVT_PutawayTaskCompleted --> CMD_CreateInboundTransaction

%% Inventory → Order (完成訂單)
EVT_InventoryDecreased --> CMD_CompleteOrder

%% Audit Logging
EVT_InventoryAdjusted --> CMD_RecordAudit
EVT_OrderCompleted --> CMD_RecordAudit
EVT_PickingTaskCompleted --> CMD_RecordAudit
EVT_PutawayTaskCompleted --> CMD_RecordAudit
```
