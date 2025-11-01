const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
require('dotenv').config();

const db = require('./config/database');
const taskRoutes = require('./routes/taskRoutes');
const inventoryRoutes = require('./routes/inventoryRoutes');
const Task = require('./models/Task');

const app = express();
const PORT = process.env.PORT || 3678;

app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

app.get('/health', (req, res) => {
  res.json({
    status: 'OK',
    service: 'WES Simulator',
    timestamp: new Date().toISOString()
  });
});

app.use('/api', taskRoutes);
app.use('/api', inventoryRoutes);

async function startServer() {
  try {
    await db.initialize();

    // Start auto-progress interval for tasks (every 5 seconds)
    setInterval(async () => {
      try {
        await Task.autoProgressTasks();
      } catch (err) {
        console.error('Error auto-progressing tasks:', err);
      }
    }, 5000);

    app.listen(PORT, () => {
      console.log(`WES Simulator Backend running on port ${PORT}`);
      console.log(`Health check: http://localhost:${PORT}/health`);
      console.log(`API endpoint: http://localhost:${PORT}/api`);
    });
  } catch (err) {
    console.error('Failed to start server:', err);
    process.exit(1);
  }
}

process.on('SIGINT', async () => {
  console.log('\nShutting down gracefully...');
  await db.close();
  process.exit(0);
});

process.on('SIGTERM', async () => {
  console.log('\nShutting down gracefully...');
  await db.close();
  process.exit(0);
});

startServer();
