# Module description

## Order Polling 模組

- 由 Orchestrator 系統定時（例如每 30 秒或 1 分鐘）呼叫 Order Source System Database
- 查詢新訂單（依狀態或建立時間區間）。
- 對於每筆新訂單：
    1. 寫入 Orchestrator 的訂單暫存表（Order Buffer Table）。
    2. 在原訂單系統中標記為「已接收」或「處理中」。
    3. 判斷訂單所屬流程類型（流程 A / 流程 B），建立對應的任務。

### 重複偵測機制

- 每筆訂單以 `order_id` 進行 idempotent 檢查。
- 若偵測到重複訂單，忽略後續重複資料。
- 訂單狀態設計：
  - `NEW`：尚未處理
  - `IN_PROGRESS`：處理中
  - `COMPLETED`：已完成
  - `FAILED`：處理失敗，待重試或人工介入

### 輪詢頻率與效能考量

- 預設每分鐘一次，可依負載調整。
- 若訂單量大，可採 **分區式輪詢**（依倉別或建立時間區段分批）。
- 應設計 **Job Lock 機制**，確保同時間僅一個 polling job 執行。
- 可記錄上次輪詢時間戳（last polled timestamp）以避免重疊區間。

## 資料同步模組

### 庫存差異處理（Inventory ↔ WES）

### 問題說明

在整合環境中，`Inventory 系統` 與 `WES 系統` 均維護庫存資料，但由於作業流程複雜、API 延遲或作業異常，兩者間可能出現以下狀況：

| 差異類型        | 說明                                                             |
| ----------- | -------------------------------------------------------------- |
| **數量差異**    | WES 回報實際庫存與 Inventory 記錄不同（例如 WES 有 95 件，但 Inventory 顯示 100 件） |
| **儲位差異**    | WES 的儲位資料與 Inventory 的倉別或區域設定不一致                               |
| **任務差異**    | WES 已完成 picking，但 Inventory 未 commit reservation               |
| **回庫/報廢差異** | WES 有異動，但 Inventory 未更新（或反之）                                   |

### 差異可能來源

- WES API 任務回報延遲或失敗（callback/polling 超時）
- Inventory reservation/commit 流程中斷（例如系統重啟、DB transaction rollback）
- 實體倉儲作業異常（操作員誤放貨物）
- 系統批次同步任務失敗

### 差異偵測機制

系統應具備 **雙向庫存比對機制**：

1. **WES → Inventory 定期同步任務**

- WES 端提供倉別與 SKU 層級的「庫存快照」API。
- Orchestrator 每日（或每小時）呼叫此 API，與 Inventory 系統資料比對。
- 若發現差異，記錄在 `StockDiscrepancyLog`。

2. **Inventory → WES 對照同步**

- 當 Inventory 有人工調整、退貨、報廢或庫存更動時，應主動通知 Orchestrator。
- Orchestrator 再透過 WES API 更新對應數量。
- 若更新失敗，進入「待同步佇列」。

### 差異處理策略

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

### 庫存同步策略總覽

| 情境     | 主導系統      | 同步方向        | 機制               |
| ------ | --------- | ----------- | ---------------- |
| 出貨任務完成 | WES       | → Inventory | commit API       |
| 回庫任務完成 | WES       | → Inventory | restock API      |
| 人工調整庫存 | Inventory | → WES       | update stock API |
| 定期盤點同步 | 雙向        | ↔           | 每日 Full Sync Job |

### 建議實作要點

- 每筆庫存變動皆附帶 `transaction_id` 以追蹤來源。
- 若兩系統都支援 version number 或 updated_at 欄位，可用作增量同步依據。
- Polling 任務應有重試機制與防止重疊執行的 lock。
- 建議在 Orchestrator 加入「庫存一致性 Dashboard」，即時顯示差異統計。
