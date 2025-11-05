const { getConnection } = require('../config/database');
const { v4: uuidv4 } = require('uuid');
const Inventory = require('./Inventory');

const Reservation = {
  /**
   * Get all reservations
   * @param {string} status - Optional status filter (ACTIVE, CONSUMED, RELEASED)
   * @returns {Array} Reservation records
   */
  async findAll(status = null) {
    let connection;
    try {
      connection = await getConnection();

      let query = `
        SELECT
          r.reservation_id,
          r.sku,
          r.warehouse_id,
          r.order_id,
          r.quantity,
          r.status,
          r.created_at,
          r.consumed_at,
          r.released_at,
          s.product_name
        FROM inventory_reservations r
        LEFT JOIN inventory_stock s ON r.sku = s.sku AND r.warehouse_id = s.warehouse_id
      `;

      const binds = {};
      if (status) {
        query += ' WHERE r.status = :status';
        binds.status = status;
      }

      query += ' ORDER BY r.created_at DESC';

      const result = await connection.execute(query, binds, {
        outFormat: require('oracledb').OUT_FORMAT_OBJECT
      });

      return result.rows;
    } catch (err) {
      console.error('Error in Reservation.findAll:', err);
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
  },

  /**
   * Get reservation by ID
   * @param {string} reservationId
   * @returns {Object|null} Reservation record
   */
  async findById(reservationId) {
    let connection;
    try {
      connection = await getConnection();

      const result = await connection.execute(
        `SELECT
          r.reservation_id,
          r.sku,
          r.warehouse_id,
          r.order_id,
          r.quantity,
          r.status,
          r.created_at,
          r.consumed_at,
          r.released_at,
          s.product_name
        FROM inventory_reservations r
        LEFT JOIN inventory_stock s ON r.sku = s.sku AND r.warehouse_id = s.warehouse_id
        WHERE r.reservation_id = :reservationId`,
        { reservationId },
        { outFormat: require('oracledb').OUT_FORMAT_OBJECT }
      );

      return result.rows.length > 0 ? result.rows[0] : null;
    } catch (err) {
      console.error('Error in Reservation.findById:', err);
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
  },

  /**
   * Create a new reservation
   * @param {Object} data - { sku, warehouseId, orderId, quantity }
   * @returns {Object} Created reservation
   */
  async create(data) {
    const { sku, warehouseId, orderId, quantity } = data;
    const reservationId = uuidv4();
    let connection;

    try {
      connection = await getConnection();

      // Check available inventory
      const inventory = await Inventory.findBySkuAndWarehouse(sku, warehouseId);

      if (!inventory) {
        throw new Error(`Inventory not found for SKU ${sku} in warehouse ${warehouseId}`);
      }

      const available = inventory.AVAILABLE_QUANTITY;

      if (available < quantity) {
        throw new Error(`Insufficient inventory. Available: ${available}, Requested: ${quantity}`);
      }

      // Create reservation
      await connection.execute(
        `INSERT INTO inventory_reservations
         (reservation_id, sku, warehouse_id, order_id, quantity, status)
         VALUES (:reservationId, :sku, :warehouseId, :orderId, :quantity, 'ACTIVE')`,
        { reservationId, sku, warehouseId, orderId, quantity }
      );

      // Update reserved quantity in inventory
      await Inventory.updateReservedQuantity(sku, warehouseId, quantity, connection);

      await connection.commit();

      // Return created reservation
      const created = await this.findById(reservationId);
      return created;
    } catch (err) {
      if (connection) {
        try {
          await connection.rollback();
        } catch (rollbackErr) {
          console.error('Error rolling back:', rollbackErr);
        }
      }
      console.error('Error in Reservation.create:', err);
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
  },

  /**
   * Consume a reservation (convert to outbound transaction)
   * @param {string} reservationId
   * @returns {Object} Updated reservation
   */
  async consume(reservationId) {
    let connection;

    try {
      connection = await getConnection();

      // Get reservation
      const reservation = await this.findById(reservationId);

      if (!reservation) {
        throw new Error(`Reservation ${reservationId} not found`);
      }

      if (reservation.STATUS !== 'ACTIVE') {
        throw new Error(`Reservation ${reservationId} is not ACTIVE (current status: ${reservation.STATUS})`);
      }

      // Update reservation status
      await connection.execute(
        `UPDATE inventory_reservations
         SET status = 'CONSUMED',
             consumed_at = SYSTIMESTAMP
         WHERE reservation_id = :reservationId`,
        { reservationId }
      );

      // Decrease total quantity and reserved quantity
      await Inventory.decreaseTotal(
        reservation.SKU,
        reservation.WAREHOUSE_ID,
        reservation.QUANTITY,
        connection
      );

      await connection.commit();

      // Return updated reservation
      const updated = await this.findById(reservationId);
      return updated;
    } catch (err) {
      if (connection) {
        try {
          await connection.rollback();
        } catch (rollbackErr) {
          console.error('Error rolling back:', rollbackErr);
        }
      }
      console.error('Error in Reservation.consume:', err);
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
  },

  /**
   * Release a reservation (cancel and return to available inventory)
   * @param {string} reservationId
   * @returns {Object} Updated reservation
   */
  async release(reservationId) {
    let connection;

    try {
      connection = await getConnection();

      // Get reservation
      const reservation = await this.findById(reservationId);

      if (!reservation) {
        throw new Error(`Reservation ${reservationId} not found`);
      }

      if (reservation.STATUS !== 'ACTIVE') {
        throw new Error(`Reservation ${reservationId} is not ACTIVE (current status: ${reservation.STATUS})`);
      }

      // Update reservation status
      await connection.execute(
        `UPDATE inventory_reservations
         SET status = 'RELEASED',
             released_at = SYSTIMESTAMP
         WHERE reservation_id = :reservationId`,
        { reservationId }
      );

      // Decrease reserved quantity (returns to available)
      await Inventory.updateReservedQuantity(
        reservation.SKU,
        reservation.WAREHOUSE_ID,
        -reservation.QUANTITY,
        connection
      );

      await connection.commit();

      // Return updated reservation
      const updated = await this.findById(reservationId);
      return updated;
    } catch (err) {
      if (connection) {
        try {
          await connection.rollback();
        } catch (rollbackErr) {
          console.error('Error rolling back:', rollbackErr);
        }
      }
      console.error('Error in Reservation.release:', err);
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
};

module.exports = Reservation;
