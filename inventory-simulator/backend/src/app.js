const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const database = require('./config/database');

// Import routes
const inventoryRoutes = require('./routes/inventoryRoutes');
const reservationRoutes = require('./routes/reservationRoutes');
const transactionRoutes = require('./routes/transactionRoutes');

const app = express();
const PORT = process.env.PORT || 3778;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Request logging middleware
app.use((req, res, next) => {
  const timestamp = new Date().toISOString();
  console.log(`[${timestamp}] ${req.method} ${req.path}`);
  next();
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'healthy',
    service: 'inventory-simulator',
    timestamp: new Date().toISOString()
  });
});

// API Routes
app.use('/api/inventory', inventoryRoutes);
app.use('/api/reservations', reservationRoutes);
app.use('/api/transactions', transactionRoutes);

// Root endpoint
app.get('/', (req, res) => {
  res.status(200).json({
    service: 'Inventory Simulator',
    version: '1.0.0',
    description: 'External inventory service simulator for Wei orchestrator',
    endpoints: {
      health: 'GET /health',
      inventory: {
        getAll: 'GET /api/inventory',
        getBySku: 'GET /api/inventory/:sku',
        increase: 'POST /api/inventory/increase',
        adjust: 'POST /api/inventory/adjust'
      },
      reservations: {
        getAll: 'GET /api/reservations',
        getById: 'GET /api/reservations/:reservationId',
        create: 'POST /api/reservations',
        consume: 'POST /api/reservations/:reservationId/consume',
        release: 'POST /api/reservations/:reservationId/release'
      },
      transactions: {
        getAll: 'GET /api/transactions'
      }
    }
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    error: 'Endpoint not found',
    path: req.path,
    method: req.method
  });
});

// Error handler
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(500).json({
    error: 'Internal server error',
    message: err.message
  });
});

// Initialize database and start server
async function startServer() {
  try {
    console.log('=====================================');
    console.log('Inventory Simulator Backend');
    console.log('=====================================\n');

    // Initialize database connection pool
    await database.initialize();

    // Start Express server
    app.listen(PORT, () => {
      console.log(`\n✓ Server is running on port ${PORT}`);
      console.log(`  - Health check: http://localhost:${PORT}/health`);
      console.log(`  - API docs: http://localhost:${PORT}/`);
      console.log('  - Press Ctrl+C to stop\n');
      console.log('=====================================\n');
    });
  } catch (err) {
    console.error('Failed to start server:', err);
    process.exit(1);
  }
}

// Graceful shutdown
process.on('SIGINT', async () => {
  console.log('\n\nReceived SIGINT, shutting down gracefully...');
  await database.close();
  process.exit(0);
});

process.on('SIGTERM', async () => {
  console.log('\n\nReceived SIGTERM, shutting down gracefully...');
  await database.close();
  process.exit(0);
});

// Start the server
startServer();
