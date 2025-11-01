const oracledb = require('oracledb');
require('dotenv').config();

try {
  oracledb.initOracleClient();
} catch (err) {
  console.log('Oracle Instant Client not found. Using thin mode.');
}

const dbConfig = {
  user: process.env.DB_USER || 'wes_user',
  password: process.env.DB_PASSWORD || 'wes123',
  connectString: process.env.DB_CONNECTION_STRING || 'localhost:1523/FREEPDB1',
  poolMin: parseInt(process.env.DB_POOL_MIN) || 2,
  poolMax: parseInt(process.env.DB_POOL_MAX) || 10,
  poolIncrement: parseInt(process.env.DB_POOL_INCREMENT) || 2,
  poolTimeout: 60,
  queueTimeout: 60000
};

let pool;

async function initialize() {
  try {
    console.log('Initializing Oracle connection pool...');
    pool = await oracledb.createPool(dbConfig);
    console.log('Oracle connection pool initialized successfully');
  } catch (err) {
    console.error('Error initializing Oracle connection pool:', err);
    throw err;
  }
}

async function close() {
  try {
    if (pool) {
      await pool.close(10);
      console.log('Oracle connection pool closed');
    }
  } catch (err) {
    console.error('Error closing Oracle connection pool:', err);
    throw err;
  }
}

function getPool() {
  return pool;
}

async function execute(sql, binds = [], options = {}) {
  let connection;
  try {
    connection = await pool.getConnection();
    const result = await connection.execute(sql, binds, options);
    return result;
  } catch (err) {
    console.error('Database execution error:', err);
    throw err;
  } finally {
    if (connection) {
      try {
        await connection.close();
      } catch (err) {
        console.error('Error closing connection:', err);
      }
    }
  }
}

module.exports = {
  initialize,
  close,
  getPool,
  execute,
  oracledb
};
