# Inventory Simulator

External inventory service simulator for the Wei orchestrator system.

## Overview

This simulator provides a complete inventory management system with multi-warehouse support, reservation capabilities, and transaction tracking. It simulates an external inventory service that the Wei orchestrator can integrate with.

## Features

### 1. Inventory Management
- **Multi-warehouse support**: Track inventory across multiple warehouses (WH001, WH002)
- **Real-time availability**: Calculate available inventory (Total - Reserved)
- **Inventory increase**: Add new stock (INBOUND transactions)
- **Manual adjustments**: Correct discrepancies with reason tracking

### 2. Reservation System
- **Reserve inventory**: Allocate inventory for specific orders
- **Consume reservations**: Convert reservations to actual outbound transactions
- **Release reservations**: Cancel reservations and return inventory to available pool
- **Manual release only**: No auto-expiration (as per requirements)

### 3. Transaction Audit Trail
- **Complete history**: Track all INBOUND, OUTBOUND, and ADJUSTMENT transactions
- **Pagination support**: Handle large transaction volumes
- **Filtering**: Filter by transaction type, SKU, warehouse
- **Reason tracking**: Every transaction includes reason/context

### 4. Full Management UI
- **Inventory Dashboard**: View stock levels with color-coded availability
- **Reservation Manager**: Manage active, consumed, and released reservations
- **Transaction History**: Browse complete audit trail with filters
- **Quick Actions**: Forms for rapid inventory operations

## Architecture

### Tech Stack
- **Backend**: Node.js + Express + Oracle Database
- **Frontend**: React + Nginx
- **Database**: Oracle Free 23c
- **Containerization**: Docker + Docker Compose

### Ports
- **Frontend**: http://localhost:3779
- **Backend**: http://localhost:3778
- **Database**: localhost:1524

## API Endpoints

### Inventory
- `GET /api/inventory` - Get all inventory (optional: ?warehouse_id=WH001)
- `GET /api/inventory/:sku` - Get inventory by SKU across warehouses
- `POST /api/inventory/increase` - Add stock (INBOUND)
- `POST /api/inventory/adjust` - Manual adjustment with reason

### Reservations
- `GET /api/reservations` - Get all reservations (optional: ?status=ACTIVE)
- `GET /api/reservations/:reservationId` - Get reservation details
- `POST /api/reservations` - Create new reservation
- `POST /api/reservations/:reservationId/consume` - Consume reservation (OUTBOUND)
- `POST /api/reservations/:reservationId/release` - Release reservation (cancel)

### Transactions
- `GET /api/transactions` - Get transaction history (supports pagination and filters)

## Quick Start

### Prerequisites
- Docker
- Docker Compose

### Running the Simulator

1. Navigate to the inventory-simulator directory:
```bash
cd inventory-simulator
```

2. Start all services:
```bash
docker-compose up --build
```

3. Wait for services to be ready (approximately 30-60 seconds):
   - Database initialization
   - Backend server startup
   - Frontend build and deployment

4. Access the UI:
   - **Frontend**: http://localhost:3779
   - **Backend API**: http://localhost:3778
   - **Health Check**: http://localhost:3778/health

### Stopping the Simulator

```bash
docker-compose down
```

To remove volumes (reset database):
```bash
docker-compose down -v
```

## Database Schema

### inventory_stock
- Primary Key: (sku, warehouse_id)
- Fields: total_quantity, reserved_quantity, location, product_name

### inventory_reservations
- Primary Key: reservation_id (UUID)
- Fields: sku, warehouse_id, order_id, quantity, status (ACTIVE/CONSUMED/RELEASED)
- Foreign Key: (sku, warehouse_id) -> inventory_stock

### inventory_transactions
- Primary Key: transaction_id (UUID)
- Fields: transaction_type (INBOUND/OUTBOUND/ADJUSTMENT), sku, warehouse_id, quantity_change, reason

## Seed Data

The system initializes with:
- **10 SKUs** (SKU001 - SKU010)
- **2 Warehouses** (WH001, WH002)
- **20 inventory records** (each SKU in each warehouse)
- **Sample products**: Laptops, phones, tablets, etc.

## Integration with Wei Orchestrator

### Expected Integration Points

1. **Order Creation** → `POST /api/reservations`
   - Reserve inventory when order is created

2. **Order Cancellation** → `POST /api/reservations/:id/release`
   - Release reservation when order is cancelled

3. **Picking Task Completion** → `POST /api/reservations/:id/consume`
   - Convert reservation to outbound when picking completes

4. **Inventory Reconciliation** → `GET /api/inventory`
   - Get current snapshot for comparison

5. **Discrepancy Resolution** → `POST /api/inventory/adjust`
   - Adjust inventory when discrepancies detected

## Development

### Running Backend Locally

```bash
cd backend
npm install
node src/scripts/initDatabase.js  # Initialize DB first
npm start
```

### Running Frontend Locally

```bash
cd frontend
npm install
npm start
```

## API Examples

### Create Reservation
```bash
curl -X POST http://localhost:3778/api/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "SKU001",
    "warehouse_id": "WH001",
    "order_id": "ORD-12345",
    "quantity": 5
  }'
```

### Increase Inventory
```bash
curl -X POST http://localhost:3778/api/inventory/increase \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "SKU001",
    "warehouse_id": "WH001",
    "quantity": 100,
    "reason": "New stock arrival"
  }'
```

### Get Inventory
```bash
curl http://localhost:3778/api/inventory?warehouse_id=WH001
```

## Troubleshooting

### Database Connection Issues
- Ensure Oracle container is healthy: `docker-compose ps`
- Check logs: `docker-compose logs oracle-db`
- Wait longer for database initialization (can take 30-60 seconds)

### Backend Not Starting
- Check backend logs: `docker-compose logs backend`
- Verify database is ready before backend starts
- Check environment variables in docker-compose.yml

### Frontend Build Issues
- Check frontend logs: `docker-compose logs frontend`
- Verify REACT_APP_API_URL is set correctly
- Rebuild with: `docker-compose up --build frontend`

## Future Enhancements

- [ ] Reservation expiration timer (auto-release after timeout)
- [ ] Batch operations (reserve multiple SKUs at once)
- [ ] Inventory forecasting
- [ ] Low-stock alerts
- [ ] Export to CSV/Excel
- [ ] GraphQL API support
