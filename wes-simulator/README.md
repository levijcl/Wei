# WES Simulator

A Warehouse Execution System (WES) simulator for the Wei Orchestrator System. This simulator provides a complete WES implementation with task management, inventory tracking, and a web-based UI.

## Features

### Task Management
- **Create Picking Tasks**: Simulate warehouse picking operations
- **Create Putaway Tasks**: Simulate warehouse putaway/return operations
- **Automatic Task Progress**: Tasks automatically progress from PENDING → IN_PROGRESS → COMPLETED in 1 minute
- **Task Priority Management**: Adjust task priorities dynamically
- **Task Status Tracking**: Real-time task status monitoring
- **Task Cancellation**: Cancel pending or in-progress tasks

### Inventory Management
- **Inventory Snapshot**: View current warehouse inventory
- **Inventory Adjustment**: Manually adjust inventory quantities
- **Adjustment History**: Track all inventory changes
- **Low Stock Indicators**: Visual alerts for low inventory levels

### UI Features
- **Task Status Dashboard**: Real-time view of all tasks with auto-refresh
- **Inventory Dashboard**: Complete inventory view with quantity tracking
- **Create Tasks**: Simple form to create new picking/putaway tasks
- **Adjust Inventory**: Easy-to-use inventory adjustment interface

## Architecture

```
wes-simulator/
├── backend/                 # Node.js + Express backend
│   ├── src/
│   │   ├── config/         # Database configuration
│   │   ├── models/         # Task and Inventory models
│   │   ├── controllers/    # API controllers
│   │   ├── routes/         # API routes
│   │   └── scripts/        # Database initialization
│   └── Dockerfile
├── frontend/               # React frontend
│   ├── src/
│   │   ├── components/     # React components
│   │   ├── services/       # API services
│   │   └── styles/         # CSS styles
│   └── Dockerfile
└── docker-compose.yml
```

## Technology Stack

- **Backend**: Node.js, Express.js, Oracle Database
- **Frontend**: React, Axios
- **Database**: Oracle Database Free 23c
- **Containerization**: Docker, Docker Compose

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- Ports 3678, 3679, and 1523 available

### Installation & Setup

1. **Clone the repository**
   ```bash
   cd wes-simulator
   ```

2. **Start the services**
   ```bash
   docker-compose up -d
   ```

3. **Wait for database initialization**
   ```bash
   # Wait about 60 seconds for Oracle DB to be healthy
   docker-compose logs -f oracle-db
   ```

4. **Initialize the database**
   ```bash
   docker-compose exec backend npm run init-db
   ```

5. **Access the application**
   - Frontend UI: http://localhost:3679
   - Backend API: http://localhost:3678
   - Health Check: http://localhost:3678/health

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3679 | Web UI (React) |
| Backend | 3678 | REST API (Express) |
| Database | 1523 | Oracle Database |

## API Documentation

### Task APIs

#### Create Task
```http
POST /api/tasks
Content-Type: application/json

{
  "task_type": "PICKING",
  "order_id": "ORDER-123",
  "warehouse_id": "WH001",
  "priority": 5,
  "items": [
    {
      "sku": "SKU001",
      "product_name": "Widget A",
      "quantity": 10,
      "location": "A-01-01"
    }
  ]
}
```

#### Get All Tasks
```http
GET /api/tasks?status=PENDING&task_type=PICKING
```

#### Get Task by ID
```http
GET /api/tasks/{taskId}
```

#### Update Task Status
```http
PUT /api/tasks/{taskId}/status
Content-Type: application/json

{
  "status": "COMPLETED"
}
```

Valid statuses: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `CANCELLED`

#### Update Task Priority
```http
PUT /api/tasks/{taskId}/priority
Content-Type: application/json

{
  "priority": 8
}
```

Priority range: 1-10 (higher number = higher priority)

#### Cancel Task
```http
DELETE /api/tasks/{taskId}
```

#### Poll Tasks (Auto-progress)
```http
GET /api/tasks/poll
```

### Inventory APIs

#### Get Inventory Snapshot
```http
GET /api/inventory?warehouse_id=WH001
```

#### Get Inventory by SKU
```http
GET /api/inventory/{sku}?warehouse_id=WH001
```

#### Adjust Inventory
```http
POST /api/inventory/adjust
Content-Type: application/json

{
  "sku": "SKU001",
  "warehouse_id": "WH001",
  "quantity_change": 50,
  "reason": "Manual count adjustment"
}
```

#### Get Adjustment History
```http
GET /api/inventory-adjustments?sku=SKU001
```

## Task Lifecycle

Tasks automatically progress through the following states:

### Single-Task Processing

**Important**: Only ONE task can be IN_PROGRESS at a time. The system processes tasks sequentially.

1. **PENDING**: Task created, waiting in queue
2. **IN_PROGRESS**: Task is being executed (takes 60 seconds)
3. **COMPLETED**: Task finished successfully

### Queue Management

- **Priority Order**: Tasks are selected by priority (highest first), then creation time (oldest first)
- **No Preemption**: Once a task starts, it runs to completion (60 seconds)
- **Automatic Progression**: When a task completes/cancels/fails, the next PENDING task immediately starts
- **Example**:
  - Create 3 tasks: Task A (priority 5), Task B (priority 8), Task C (priority 5)
  - Execution order: Task B → Task A → Task C
  - If A and C have same priority, the one created first executes first

Tasks can also transition to:
- **FAILED**: Manual status update when task encounters an error
- **CANCELLED**: Task cancelled before completion (next task starts immediately)

## Sample Data

The database is initialized with sample inventory:

| SKU | Product Name | Quantity | Location |
|-----|--------------|----------|----------|
| SKU001 | Widget A | 100 | A-01-01 |
| SKU002 | Widget B | 200 | A-01-02 |
| SKU003 | Gadget C | 150 | A-02-01 |
| SKU004 | Gadget D | 300 | A-02-02 |
| SKU005 | Tool E | 50 | B-01-01 |

## Integration with Wei Orchestrator

The WES simulator is designed to integrate with the Wei Orchestrator System. The orchestrator can:

1. **Create picking tasks** when orders need fulfillment
2. **Poll task status** to track picking progress
3. **Get inventory snapshots** for stock reconciliation
4. **Adjust inventory** based on discrepancies
5. **Create putaway tasks** for returns/restocking

### Example Integration Flow

```javascript
// 1. Orchestrator creates a picking task
const response = await fetch('http://localhost:3678/api/tasks', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    task_type: 'PICKING',
    order_id: 'ORDER-12345',
    priority: 7, // Higher priority executes first
    items: [
      { sku: 'SKU001', quantity: 5 },
      { sku: 'SKU002', quantity: 3 }
    ]
  })
});

const { task_id } = await response.json();

// 2. Poll task status every 5-10 seconds
// Note: Task will be PENDING until it becomes the highest priority task
// Only one task can be IN_PROGRESS at a time
const interval = setInterval(async () => {
  const status = await fetch(`http://localhost:3678/api/tasks/${task_id}`);
  const task = await status.json();

  if (task.STATUS === 'IN_PROGRESS') {
    console.log('Task started, will complete in ~60 seconds');
  } else if (task.STATUS === 'COMPLETED') {
    console.log('Task completed, commit inventory in Orchestrator');
    clearInterval(interval);
  }
}, 5000);

// 3. Get inventory snapshot for reconciliation
const inventory = await fetch('http://localhost:3678/api/inventory');
const snapshot = await inventory.json();
```

## Troubleshooting

### Database Connection Issues

```bash
# Check if Oracle DB is healthy
docker-compose ps

# View database logs
docker-compose logs oracle-db

# Restart database
docker-compose restart oracle-db
```

### Backend Not Starting

```bash
# Check backend logs
docker-compose logs backend

# Reinitialize database
docker-compose exec backend npm run init-db

# Restart backend
docker-compose restart backend
```

### Frontend Not Loading

```bash
# Check frontend logs
docker-compose logs frontend

# Rebuild frontend
docker-compose up -d --build frontend
```

## Development

### Running Locally (without Docker)

1. **Start Oracle Database**
   ```bash
   docker run -d -p 1523:1521 gvenzl/oracle-free:23-slim
   ```

2. **Backend**
   ```bash
   cd backend
   npm install
   cp .env.example .env
   npm run init-db
   npm run dev
   ```

3. **Frontend**
   ```bash
   cd frontend
   npm install
   npm start
   ```

## Stopping the Application

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (will delete all data)
docker-compose down -v
```

## License

MIT

## Support

For issues or questions, please refer to the Wei Orchestrator System documentation.
