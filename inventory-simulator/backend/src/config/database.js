const oracledb = require('oracledb');

// Oracle Database Configuration
const dbConfig = {
  user: process.env.DB_USER || 'inventory_user',
  password: process.env.DB_PASSWORD || 'inventory123',
  connectString: process.env.DB_CONNECT_STRING || 'localhost:1524/FREEPDB1'
};

// Connection pool settings
const poolConfig = {
  user: dbConfig.user,
  password: dbConfig.password,
  connectString: dbConfig.connectString,
  poolMin: 2,
  poolMax: 10,
  poolIncrement: 2,
  poolTimeout: 60
};

// Initialize connection pool
let pool;

async function initialize() {
  try {
    console.log('Creating Oracle DB connection pool...');
    pool = await oracledb.createPool(poolConfig);
    console.log('✓ Oracle DB connection pool created successfully');
    return pool;
  } catch (err) {
    console.error('Error creating connection pool:', err);
    throw err;
  }
}

async function getConnection() {
  if (!pool) {
    await initialize();
  }
  return pool.getConnection();
}

async function close() {
  if (pool) {
    try {
      await pool.close(10);
      console.log('Oracle DB connection pool closed');
    } catch (err) {
      console.error('Error closing connection pool:', err);
    }
  }
}

// Graceful shutdown
process.on('SIGINT', async () => {
  await close();
  process.exit(0);
});

process.on('SIGTERM', async () => {
  await close();
  process.exit(0);
});

module.exports = {
  initialize,
  getConnection,
  close,
  dbConfig
};
