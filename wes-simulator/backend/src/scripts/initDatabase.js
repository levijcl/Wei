const oracledb = require('oracledb');
require('dotenv').config();

async function initDatabase() {
  let connection;

  try {
    console.log('Connecting to Oracle database as SYSTEM user...');

    connection = await oracledb.getConnection({
      user: process.env.SYSTEM_USER || 'system',
      password: process.env.SYSTEM_PASSWORD || 'oracle123',
      connectString: process.env.DB_CONNECTION_STRING || 'localhost:1523/FREEPDB1'
    });

    console.log('Connected to Oracle database');

    console.log('Creating wes_user...');
    try {
      await connection.execute(`CREATE USER wes_user IDENTIFIED BY wes123`);
      console.log('User wes_user created');
    } catch (err) {
      if (err.errorNum === 1920) {
        console.log('User wes_user already exists');
      } else {
        throw err;
      }
    }

    await connection.execute(`GRANT CONNECT, RESOURCE TO wes_user`);
    await connection.execute(`GRANT CREATE SESSION TO wes_user`);
    await connection.execute(`GRANT CREATE TABLE TO wes_user`);
    await connection.execute(`GRANT UNLIMITED TABLESPACE TO wes_user`);
    console.log('Privileges granted to wes_user');

    await connection.close();

    console.log('Reconnecting as wes_user...');
    connection = await oracledb.getConnection({
      user: 'wes_user',
      password: 'wes123',
      connectString: process.env.DB_CONNECTION_STRING || 'localhost:1523/FREEPDB1'
    });

    console.log('Creating wes_tasks table...');
    await connection.execute(`
      CREATE TABLE wes_tasks (
        task_id VARCHAR2(36) PRIMARY KEY,
        task_type VARCHAR2(50) NOT NULL,
        order_id VARCHAR2(36),
        warehouse_id VARCHAR2(50) DEFAULT 'WH001',
        priority NUMBER(2) DEFAULT 5,
        status VARCHAR2(50) DEFAULT 'PENDING',
        created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
        updated_at TIMESTAMP DEFAULT SYSTIMESTAMP,
        started_at TIMESTAMP,
        estimated_completion_at TIMESTAMP,
        completed_at TIMESTAMP
      )
    `);
    console.log('wes_tasks table created');

    console.log('Creating wes_task_items table...');
    await connection.execute(`
      CREATE TABLE wes_task_items (
        task_item_id VARCHAR2(36) PRIMARY KEY,
        task_id VARCHAR2(36) NOT NULL,
        sku VARCHAR2(100) NOT NULL,
        product_name VARCHAR2(255),
        quantity NUMBER(10) NOT NULL,
        location VARCHAR2(100),
        created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
        CONSTRAINT fk_task FOREIGN KEY (task_id) REFERENCES wes_tasks(task_id) ON DELETE CASCADE
      )
    `);
    console.log('wes_task_items table created');

    console.log('Creating wes_inventory table...');
    await connection.execute(`
      CREATE TABLE wes_inventory (
        sku VARCHAR2(100) NOT NULL,
        product_name VARCHAR2(255),
        warehouse_id VARCHAR2(50) DEFAULT 'WH001',
        quantity NUMBER(10) DEFAULT 0,
        location VARCHAR2(100),
        updated_at TIMESTAMP DEFAULT SYSTIMESTAMP,
        CONSTRAINT pk_inventory PRIMARY KEY (sku, warehouse_id)
      )
    `);
    console.log('wes_inventory table created');

    console.log('Creating wes_inventory_adjustments table...');
    await connection.execute(`
      CREATE TABLE wes_inventory_adjustments (
        adjustment_id VARCHAR2(36) PRIMARY KEY,
        sku VARCHAR2(100) NOT NULL,
        warehouse_id VARCHAR2(50) DEFAULT 'WH001',
        quantity_change NUMBER(10) NOT NULL,
        reason VARCHAR2(500),
        created_at TIMESTAMP DEFAULT SYSTIMESTAMP
      )
    `);
    console.log('wes_inventory_adjustments table created');

    console.log('Creating indexes...');
    await connection.execute(`CREATE INDEX idx_tasks_status ON wes_tasks(status)`);
    await connection.execute(`CREATE INDEX idx_tasks_created_at ON wes_tasks(created_at)`);
    await connection.execute(`CREATE INDEX idx_tasks_priority ON wes_tasks(priority)`);
    await connection.execute(`CREATE INDEX idx_task_items_task_id ON wes_task_items(task_id)`);
    await connection.execute(`CREATE INDEX idx_inventory_sku ON wes_inventory(sku)`);
    await connection.execute(`CREATE INDEX idx_adjustments_sku ON wes_inventory_adjustments(sku)`);
    console.log('Indexes created');

    console.log('Inserting sample inventory data...');
    const sampleInventory = [
      { sku: 'SKU001', name: 'Widget A', qty: 100, location: 'A-01-01' },
      { sku: 'SKU002', name: 'Widget B', qty: 200, location: 'A-01-02' },
      { sku: 'SKU003', name: 'Gadget C', qty: 150, location: 'A-02-01' },
      { sku: 'SKU004', name: 'Gadget D', qty: 300, location: 'A-02-02' },
      { sku: 'SKU005', name: 'Tool E', qty: 50, location: 'B-01-01' }
    ];

    for (const item of sampleInventory) {
      await connection.execute(`
        INSERT INTO wes_inventory (sku, product_name, warehouse_id, quantity, location, updated_at)
        VALUES (:sku, :name, 'WH001', :qty, :location, SYSTIMESTAMP)
      `, {
        sku: item.sku,
        name: item.name,
        qty: item.qty,
        location: item.location
      });
    }
    await connection.commit();
    console.log('Sample inventory data inserted');

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
