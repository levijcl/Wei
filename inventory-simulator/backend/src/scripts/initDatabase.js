const oracledb = require('oracledb');

const systemConfig = {
  user: 'SYSTEM',
  password: process.env.ORACLE_PASSWORD || 'oracle',
  connectString: process.env.DB_CONNECT_STRING || 'localhost:1524/FREEPDB1'
};

const appConfig = {
  user: 'inventory_user',
  password: 'inventory123',
  connectString: process.env.DB_CONNECT_STRING || 'localhost:1524/FREEPDB1'
};

async function initializeDatabase() {
  let systemConn;
  let appConn;

  try {
    console.log('==========================================');
    console.log('Inventory Simulator Database Initialization');
    console.log('==========================================\n');

    // Step 1: Connect as SYSTEM to create user
    console.log('Step 1: Connecting as SYSTEM user...');
    systemConn = await oracledb.getConnection(systemConfig);
    console.log('✓ Connected as SYSTEM\n');

    // Step 2: Create application user
    console.log('Step 2: Creating inventory_user...');
    try {
      await systemConn.execute(`DROP USER ${appConfig.user} CASCADE`);
      console.log('  - Dropped existing user');
    } catch (err) {
      console.log('  - No existing user to drop');
    }

    await systemConn.execute(`CREATE USER ${appConfig.user} IDENTIFIED BY ${appConfig.password}`);
    console.log('  - User created');

    await systemConn.execute(`GRANT CONNECT, RESOURCE, CREATE SESSION, CREATE TABLE, UNLIMITED TABLESPACE TO ${appConfig.user}`);
    console.log('✓ Privileges granted\n');

    await systemConn.close();

    // Step 3: Connect as application user
    console.log('Step 3: Connecting as inventory_user...');
    appConn = await oracledb.getConnection(appConfig);
    console.log('✓ Connected as inventory_user\n');

    // Step 4: Create tables
    console.log('Step 4: Creating tables...');

    // Table 1: inventory_stock
    await appConn.execute(`
      CREATE TABLE inventory_stock (
        sku VARCHAR2(100) NOT NULL,
        product_name VARCHAR2(255),
        warehouse_id VARCHAR2(50) NOT NULL,
        total_quantity NUMBER(10) DEFAULT 0,
        reserved_quantity NUMBER(10) DEFAULT 0,
        location VARCHAR2(100),
        updated_at TIMESTAMP DEFAULT SYSTIMESTAMP,
        CONSTRAINT pk_inventory_stock PRIMARY KEY (sku, warehouse_id),
        CONSTRAINT chk_total_qty CHECK (total_quantity >= 0),
        CONSTRAINT chk_reserved_qty CHECK (reserved_quantity >= 0),
        CONSTRAINT chk_qty_balance CHECK (reserved_quantity <= total_quantity)
      )
    `);
    console.log('  ✓ inventory_stock table created');

    // Table 2: inventory_reservations
    await appConn.execute(`
      CREATE TABLE inventory_reservations (
        reservation_id VARCHAR2(36) PRIMARY KEY,
        sku VARCHAR2(100) NOT NULL,
        warehouse_id VARCHAR2(50) NOT NULL,
        order_id VARCHAR2(36) NOT NULL,
        quantity NUMBER(10) NOT NULL,
        status VARCHAR2(50) DEFAULT 'ACTIVE',
        created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
        consumed_at TIMESTAMP,
        released_at TIMESTAMP,
        CONSTRAINT chk_reservation_qty CHECK (quantity > 0),
        CONSTRAINT chk_reservation_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED')),
        CONSTRAINT fk_reservation_stock FOREIGN KEY (sku, warehouse_id)
          REFERENCES inventory_stock(sku, warehouse_id) ON DELETE CASCADE
      )
    `);
    console.log('  ✓ inventory_reservations table created');

    // Table 3: inventory_transactions
    await appConn.execute(`
      CREATE TABLE inventory_transactions (
        transaction_id VARCHAR2(36) PRIMARY KEY,
        transaction_type VARCHAR2(50) NOT NULL,
        sku VARCHAR2(100) NOT NULL,
        warehouse_id VARCHAR2(50) NOT NULL,
        quantity_change NUMBER(10) NOT NULL,
        order_id VARCHAR2(36),
        reservation_id VARCHAR2(36),
        reason VARCHAR2(500),
        created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
        CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('INBOUND', 'OUTBOUND', 'ADJUSTMENT'))
      )
    `);
    console.log('  ✓ inventory_transactions table created\n');

    // Step 5: Create indexes
    console.log('Step 5: Creating indexes...');
    await appConn.execute(`CREATE INDEX idx_reservations_order ON inventory_reservations(order_id)`);
    await appConn.execute(`CREATE INDEX idx_reservations_status ON inventory_reservations(status)`);
    await appConn.execute(`CREATE INDEX idx_transactions_sku ON inventory_transactions(sku, warehouse_id)`);
    await appConn.execute(`CREATE INDEX idx_transactions_type ON inventory_transactions(transaction_type)`);
    await appConn.execute(`CREATE INDEX idx_transactions_created ON inventory_transactions(created_at)`);
    console.log('  ✓ Indexes created\n');

    // Step 6: Insert seed data
    console.log('Step 6: Inserting seed data...');

    const products = [
      { sku: 'SKU001', name: 'Laptop Dell XPS 15', wh1Qty: 50, wh2Qty: 30, location1: 'A-01-01', location2: 'B-02-01' },
      { sku: 'SKU002', name: 'iPhone 15 Pro', wh1Qty: 120, wh2Qty: 80, location1: 'A-01-02', location2: 'B-02-02' },
      { sku: 'SKU003', name: 'Samsung Galaxy S24', wh1Qty: 95, wh2Qty: 65, location1: 'A-01-03', location2: 'B-02-03' },
      { sku: 'SKU004', name: 'iPad Air 11"', wh1Qty: 75, wh2Qty: 45, location1: 'A-02-01', location2: 'B-03-01' },
      { sku: 'SKU005', name: 'MacBook Pro 16"', wh1Qty: 40, wh2Qty: 25, location1: 'A-02-02', location2: 'B-03-02' },
      { sku: 'SKU006', name: 'Sony WH-1000XM5', wh1Qty: 200, wh2Qty: 150, location1: 'A-03-01', location2: 'B-04-01' },
      { sku: 'SKU007', name: 'AirPods Pro 2', wh1Qty: 180, wh2Qty: 120, location1: 'A-03-02', location2: 'B-04-02' },
      { sku: 'SKU008', name: 'LG 27" Monitor', wh1Qty: 60, wh2Qty: 40, location1: 'A-04-01', location2: 'B-05-01' },
      { sku: 'SKU009', name: 'Logitech MX Master 3', wh1Qty: 150, wh2Qty: 100, location1: 'A-04-02', location2: 'B-05-02' },
      { sku: 'SKU010', name: 'Kindle Paperwhite', wh1Qty: 110, wh2Qty: 70, location1: 'A-05-01', location2: 'B-06-01' }
    ];

    for (const product of products) {
      // Insert for WH001
      await appConn.execute(`
        INSERT INTO inventory_stock (sku, product_name, warehouse_id, total_quantity, reserved_quantity, location)
        VALUES (:sku, :name, 'WH001', :qty, 0, :location)
      `, { sku: product.sku, name: product.name, qty: product.wh1Qty, location: product.location1 });

      // Insert for WH002
      await appConn.execute(`
        INSERT INTO inventory_stock (sku, product_name, warehouse_id, total_quantity, reserved_quantity, location)
        VALUES (:sku, :name, 'WH002', :qty, 0, :location)
      `, { sku: product.sku, name: product.name, qty: product.wh2Qty, location: product.location2 });

      // Create initial INBOUND transactions
      const { v4: uuidv4 } = require('uuid');
      await appConn.execute(`
        INSERT INTO inventory_transactions (transaction_id, transaction_type, sku, warehouse_id, quantity_change, reason)
        VALUES (:txnId, 'INBOUND', :sku, 'WH001', :qty, 'Initial stock')
      `, { txnId: uuidv4(), sku: product.sku, qty: product.wh1Qty });

      await appConn.execute(`
        INSERT INTO inventory_transactions (transaction_id, transaction_type, sku, warehouse_id, quantity_change, reason)
        VALUES (:txnId, 'INBOUND', :sku, 'WH002', :qty, 'Initial stock')
      `, { txnId: uuidv4(), sku: product.sku, qty: product.wh2Qty });
    }

    await appConn.commit();
    console.log('  ✓ Inserted 10 products across 2 warehouses (20 inventory records)');
    console.log('  ✓ Created 20 initial INBOUND transactions\n');

    console.log('==========================================');
    console.log('✅ Database initialization completed successfully!');
    console.log('==========================================\n');
    console.log('Summary:');
    console.log('  - User: inventory_user');
    console.log('  - Tables: inventory_stock, inventory_reservations, inventory_transactions');
    console.log('  - Warehouses: WH001, WH002');
    console.log('  - Products: 10 SKUs (SKU001-SKU010)');
    console.log('  - Total records: 20 inventory + 20 transactions\n');

  } catch (err) {
    console.error('❌ Error during database initialization:', err);
    throw err;
  } finally {
    if (systemConn) {
      try {
        await systemConn.close();
      } catch (err) {
        console.error('Error closing system connection:', err);
      }
    }
    if (appConn) {
      try {
        await appConn.close();
      } catch (err) {
        console.error('Error closing app connection:', err);
      }
    }
  }
}

// Run if called directly
if (require.main === module) {
  initializeDatabase()
    .then(() => process.exit(0))
    .catch((err) => {
      console.error(err);
      process.exit(1);
    });
}

module.exports = { initializeDatabase };
