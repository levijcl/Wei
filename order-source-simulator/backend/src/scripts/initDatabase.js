const oracledb = require('oracledb');
require('dotenv').config();

async function initDatabase() {
  let connection;

  try {
    console.log('Connecting to Oracle database as SYSTEM user...');

    connection = await oracledb.getConnection({
      user: process.env.SYSTEM_USER || 'system',
      password: process.env.SYSTEM_PASSWORD || 'oracle123',
      connectString: process.env.DB_CONNECTION_STRING || 'localhost:1521/XEPDB1'
    });

    console.log('Connected to Oracle database');

    console.log('Creating order_source user...');
    try {
      await connection.execute(`CREATE USER order_source IDENTIFIED BY ordersource123`);
      console.log('User order_source created');
    } catch (err) {
      if (err.errorNum === 1920) {
        console.log('User order_source already exists');
      } else {
        throw err;
      }
    }

    await connection.execute(`GRANT CONNECT, RESOURCE TO order_source`);
    await connection.execute(`GRANT CREATE SESSION TO order_source`);
    await connection.execute(`GRANT CREATE TABLE TO order_source`);
    await connection.execute(`GRANT UNLIMITED TABLESPACE TO order_source`);
    console.log('Privileges granted to order_source');

    await connection.close();

    console.log('Reconnecting as order_source user...');
    connection = await oracledb.getConnection({
      user: 'order_source',
      password: 'ordersource123',
      connectString: process.env.DB_CONNECTION_STRING || 'localhost:1521/XEPDB1'
    });

    console.log('Creating orders table...');
    try {
      await connection.execute(`
        CREATE TABLE orders (
          order_id VARCHAR2(36) PRIMARY KEY,
          customer_name VARCHAR2(255) NOT NULL,
          customer_email VARCHAR2(255),
          shipping_address VARCHAR2(500),
          order_type VARCHAR2(50) DEFAULT 'TYPE_A',
          warehouse_id VARCHAR2(50) DEFAULT 'WH001',
          status VARCHAR2(50) DEFAULT 'NEW',
          scheduled_pickup_time TIMESTAMP DEFAULT SYSTIMESTAMP + INTERVAL '2' HOUR,
          created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
          updated_at TIMESTAMP DEFAULT SYSTIMESTAMP
        )
      `);
      console.log('Orders table created');
    } catch (err) {
      if (err.errorNum === 955) {
        console.log('Orders table already exists, checking for scheduled_pickup_time column...');
        try {
          await connection.execute(`
            ALTER TABLE orders ADD scheduled_pickup_time TIMESTAMP DEFAULT SYSTIMESTAMP + INTERVAL '2' HOUR
          `);
          console.log('Added scheduled_pickup_time column to existing orders table');
        } catch (alterErr) {
          if (alterErr.errorNum === 1430) {
            console.log('scheduled_pickup_time column already exists');
          } else {
            throw alterErr;
          }
        }
      } else {
        throw err;
      }
    }

    console.log('Creating order_items table...');
    await connection.execute(`
      CREATE TABLE order_items (
        order_item_id VARCHAR2(36) PRIMARY KEY,
        order_id VARCHAR2(36) NOT NULL,
        sku VARCHAR2(100) NOT NULL,
        product_name VARCHAR2(255) NOT NULL,
        quantity NUMBER(10) NOT NULL,
        price NUMBER(10,2),
        created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
        CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
      )
    `);
    console.log('Order_items table created');

    console.log('Creating indexes...');
    await connection.execute(`CREATE INDEX idx_orders_status ON orders(status)`);
    await connection.execute(`CREATE INDEX idx_orders_created_at ON orders(created_at)`);
    await connection.execute(`CREATE INDEX idx_order_items_order_id ON order_items(order_id)`);
    console.log('Indexes created');

    console.log('Database initialization completed successfully!');

  } catch (err) {
    console.error('Error initializing database:', err);
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

initDatabase()
  .then(() => {
    console.log('Script completed successfully');
    process.exit(0);
  })
  .catch((err) => {
    console.error('Script failed:', err);
    process.exit(1);
  });
