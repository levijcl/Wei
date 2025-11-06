const db = require('../config/database');
const { v4: uuidv4 } = require('uuid');

class Order {
  static async create(orderData) {
    let connection;
    try {
      connection = await db.getPool().getConnection();

      const orderId = uuidv4();

      // Default scheduled_pickup_time to current time + 2 hours if not provided
      const scheduledPickupTime = orderData.scheduled_pickup_time
        ? new Date(orderData.scheduled_pickup_time)
        : new Date(Date.now() + 2 * 60 * 60 * 1000);

      const sql = `
        INSERT INTO orders (
          order_id,
          customer_name,
          customer_email,
          shipping_address,
          order_type,
          warehouse_id,
          status,
          scheduled_pickup_time,
          created_at,
          updated_at
        ) VALUES (
          :orderId,
          :customerName,
          :customerEmail,
          :shippingAddress,
          :orderType,
          :warehouseId,
          :status,
          :scheduledPickupTime,
          SYSTIMESTAMP,
          SYSTIMESTAMP
        )
      `;

      const binds = {
        orderId,
        customerName: orderData.customer_name,
        customerEmail: orderData.customer_email,
        shippingAddress: orderData.shipping_address,
        orderType: orderData.order_type || 'TYPE_A',
        warehouseId: orderData.warehouse_id || 'WH001',
        status: 'NEW',
        scheduledPickupTime
      };

      await connection.execute(sql, binds, { autoCommit: false });

      for (const item of orderData.items) {
        const itemSql = `
          INSERT INTO order_items (
            order_item_id,
            order_id,
            sku,
            product_name,
            quantity,
            price,
            created_at
          ) VALUES (
            :orderItemId,
            :orderId,
            :sku,
            :productName,
            :quantity,
            :price,
            SYSTIMESTAMP
          )
        `;

        const itemBinds = {
          orderItemId: uuidv4(),
          orderId,
          sku: item.sku,
          productName: item.product_name,
          quantity: item.quantity,
          price: item.price
        };

        await connection.execute(itemSql, itemBinds, { autoCommit: false });
      }

      await connection.commit();

      return orderId;
    } catch (err) {
      if (connection) {
        try {
          await connection.rollback();
        } catch (rollbackErr) {
          console.error('Error rolling back transaction:', rollbackErr);
        }
      }
      throw err;
    } finally {
      if (connection) {
        try {
          await connection.close();
        } catch (closeErr) {
          console.error('Error closing connection:', closeErr);
        }
      }
    }
  }

  static async findAll(filters = {}) {
    let sql = `
      SELECT
        order_id,
        customer_name,
        customer_email,
        shipping_address,
        order_type,
        warehouse_id,
        status,
        scheduled_pickup_time,
        created_at,
        updated_at
      FROM orders
      WHERE 1=1
    `;

    const binds = {};

    if (filters.status) {
      sql += ' AND status = :status';
      binds.status = filters.status;
    }

    if (filters.fromDate) {
      sql += ' AND created_at >= :fromDate';
      binds.fromDate = new Date(filters.fromDate);
    }

    if (filters.warehouse_id) {
      sql += ' AND warehouse_id = :warehouseId';
      binds.warehouseId = filters.warehouse_id;
    }

    sql += ' ORDER BY created_at DESC';

    const result = await db.execute(sql, binds, {
      outFormat: db.oracledb.OUT_FORMAT_OBJECT
    });

    return result.rows;
  }

  static async findById(orderId) {
    const orderSql = `
      SELECT
        order_id,
        customer_name,
        customer_email,
        shipping_address,
        order_type,
        warehouse_id,
        status,
        scheduled_pickup_time,
        created_at,
        updated_at
      FROM orders
      WHERE order_id = :orderId
    `;

    const orderResult = await db.execute(orderSql, { orderId }, {
      outFormat: db.oracledb.OUT_FORMAT_OBJECT
    });

    if (orderResult.rows.length === 0) {
      return null;
    }

    const order = orderResult.rows[0];

    const itemsSql = `
      SELECT
        order_item_id,
        sku,
        product_name,
        quantity,
        price,
        created_at
      FROM order_items
      WHERE order_id = :orderId
      ORDER BY created_at
    `;

    const itemsResult = await db.execute(itemsSql, { orderId }, {
      outFormat: db.oracledb.OUT_FORMAT_OBJECT
    });

    order.items = itemsResult.rows;

    return order;
  }

  static async updateStatus(orderId, newStatus) {
    const sql = `
      UPDATE orders
      SET status = :status,
          updated_at = SYSTIMESTAMP
      WHERE order_id = :orderId
    `;

    const result = await db.execute(sql, { status: newStatus, orderId }, {
      autoCommit: true
    });

    return result.rowsAffected > 0;
  }

  static async findNewOrders(limit = 50) {
    const sql = `
      SELECT
        order_id,
        customer_name,
        customer_email,
        shipping_address,
        order_type,
        warehouse_id,
        status,
        scheduled_pickup_time,
        created_at,
        updated_at
      FROM orders
      WHERE status = 'NEW'
      ORDER BY created_at ASC
      FETCH FIRST :limit ROWS ONLY
    `;

    const result = await db.execute(sql, { limit }, {
      outFormat: db.oracledb.OUT_FORMAT_OBJECT
    });

    const orders = [];
    for (const order of result.rows) {
      const fullOrder = await Order.findById(order.ORDER_ID);
      orders.push(fullOrder);
    }

    return orders;
  }
}

module.exports = Order;
