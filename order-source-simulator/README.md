# Order Source Simulator

A Docker-based order source simulator for the Wei Orchestrator System. This application provides a web UI for generating orders that are stored in an Oracle database, which can be polled by the orchestrator system.

## Architecture

- **Backend**: Node.js + Express with Oracle Database connectivity
- **Frontend**: React with modern UI
- **Database**: Oracle XE 21c
- **Deployment**: Docker + Docker Compose

## Features

- Create orders with multiple line items through a web UI
- View and manage orders with filtering capabilities
- Update order status (NEW, IN_PROGRESS, COMPLETED, FAILED)
- RESTful API for order polling by orchestrator
- Oracle database with proper schema and indexes
- Fully containerized with Docker

## Prerequisites

- Docker (version 20.10 or higher)
- Docker Compose (version 2.0 or higher)
- 4GB+ available RAM for Oracle database

## Quick Start

### 1. Start the Application

```bash
cd order-source-simulator
docker-compose up -d
```

This will start three services:
- Oracle Database (port 1522)
- Backend API (port 3579)
- Frontend UI (port 3578)

### 2. Initialize the Database

Wait for the Oracle database to be healthy (about 60 seconds), then run:

```bash
docker-compose exec backend npm run init-db
```

### 3. Access the Application

- Frontend UI: http://localhost:3578
- Backend API: http://localhost:3579/api
- Health Check: http://localhost:3579/health

## API Endpoints

### For UI Usage

- `POST /api/orders` - Create a new order
- `GET /api/orders` - Get all orders (with optional filters)
- `GET /api/orders/:orderId` - Get order by ID
- `PUT /api/orders/:orderId/status` - Update order status

### For Orchestrator Polling

- `GET /api/orders/poll?limit=50` - Poll for new orders (status=NEW)

Example: `GET http://localhost:3579/api/orders/poll?limit=50`

Query Parameters:
- `limit`: Maximum number of orders to return (default: 50)

## Database Schema

### Orders Table

```sql
CREATE TABLE orders (
  order_id VARCHAR2(36) PRIMARY KEY,
  customer_name VARCHAR2(255) NOT NULL,
  customer_email VARCHAR2(255),
  shipping_address VARCHAR2(500),
  order_type VARCHAR2(50) DEFAULT 'TYPE_A',
  warehouse_id VARCHAR2(50) DEFAULT 'WH001',
  status VARCHAR2(50) DEFAULT 'NEW',
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
  updated_at TIMESTAMP DEFAULT SYSTIMESTAMP
);
```

### Order Items Table

```sql
CREATE TABLE order_items (
  order_item_id VARCHAR2(36) PRIMARY KEY,
  order_id VARCHAR2(36) NOT NULL,
  sku VARCHAR2(100) NOT NULL,
  product_name VARCHAR2(255) NOT NULL,
  quantity NUMBER(10) NOT NULL,
  price NUMBER(10,2),
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
  CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);
```

### Order Status Values

- `NEW` - Order created, ready for polling
- `IN_PROGRESS` - Order being processed by orchestrator
- `COMPLETED` - Order successfully completed
- `FAILED` - Order processing failed

### Order Types

- `TYPE_A` - Picking Zone workflow (automatic warehouse picking → picking zone → delivery)
- `TYPE_B` - Packing List workflow (automatic warehouse picking + packing list printing)

## Development

### Local Development (Without Docker)

1. Install dependencies:

```bash
cd backend
npm install

cd ../frontend
npm install
```

2. Set up environment variables:

```bash
cd backend
cp .env.example .env
```

Edit `.env` with your local Oracle database connection details.

3. Run the database initialization:

```bash
cd backend
npm run init-db
```

4. Start the backend:

```bash
cd backend
npm run dev
```

5. Start the frontend:

```bash
cd frontend
npm start
```

### Environment Variables

#### Backend (.env)

```
PORT=3579
NODE_ENV=development
DB_USER=order_source
DB_PASSWORD=ordersource123
DB_CONNECTION_STRING=localhost:1521/XEPDB1
DB_POOL_MIN=2
DB_POOL_MAX=10
DB_POOL_INCREMENT=2
```

#### Frontend

```
REACT_APP_API_URL=http://localhost:3579/api
```

## Integration with Orchestrator

The orchestrator system should poll the following endpoint periodically:

```
GET http://localhost:3579/api/orders/poll?limit=50
```

Response format:

```json
{
  "count": 2,
  "orders": [
    {
      "ORDER_ID": "uuid-here",
      "CUSTOMER_NAME": "John Doe",
      "CUSTOMER_EMAIL": "john@example.com",
      "SHIPPING_ADDRESS": "123 Main St",
      "ORDER_TYPE": "TYPE_A",
      "WAREHOUSE_ID": "WH001",
      "STATUS": "NEW",
      "CREATED_AT": "2024-10-31T10:00:00.000Z",
      "UPDATED_AT": "2024-10-31T10:00:00.000Z",
      "items": [
        {
          "ORDER_ITEM_ID": "uuid-here",
          "SKU": "SKU001",
          "PRODUCT_NAME": "Product Name",
          "QUANTITY": 5,
          "PRICE": 29.99,
          "CREATED_AT": "2024-10-31T10:00:00.000Z"
        }
      ]
    }
  ]
}
```

After processing an order, update its status:

```bash
PUT http://localhost:3579/api/orders/:orderId/status
Content-Type: application/json

{
  "status": "IN_PROGRESS"
}
```

## Stopping the Application

```bash
docker-compose down
```

To remove all data including database volumes:

```bash
docker-compose down -v
```

## Troubleshooting

### Database Connection Issues

If you see connection errors, ensure the Oracle database is fully started:

```bash
docker-compose logs oracle-db
```

Wait for the message: "DATABASE IS READY TO USE!"

### Port Conflicts

If ports 1522, 3578, or 3579 are already in use, modify the port mappings in `docker-compose.yml`:

```yaml
ports:
  - "NEW_PORT:CONTAINER_PORT"
```

### Frontend Cannot Connect to Backend

If running locally, ensure the proxy setting in `frontend/package.json` points to the correct backend URL:

```json
"proxy": "http://localhost:3579"
```

## Project Structure

```
order-source-simulator/
├── backend/
│   ├── src/
│   │   ├── config/
│   │   │   └── database.js
│   │   ├── models/
│   │   │   └── Order.js
│   │   ├── controllers/
│   │   │   └── orderController.js
│   │   ├── routes/
│   │   │   └── orderRoutes.js
│   │   ├── scripts/
│   │   │   └── initDatabase.js
│   │   └── app.js
│   ├── Dockerfile
│   ├── package.json
│   └── .env.example
├── frontend/
│   ├── public/
│   │   └── index.html
│   ├── src/
│   │   ├── components/
│   │   │   ├── OrderForm.js
│   │   │   └── OrderList.js
│   │   ├── services/
│   │   │   └── api.js
│   │   ├── styles/
│   │   │   ├── App.css
│   │   │   ├── OrderForm.css
│   │   │   └── OrderList.css
│   │   ├── App.js
│   │   └── index.js
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── docker-compose.yml
└── README.md
```

## License

MIT
