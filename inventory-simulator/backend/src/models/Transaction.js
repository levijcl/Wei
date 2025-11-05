const { getConnection } = require('../config/database');
const { v4: uuidv4 } = require('uuid');

const Transaction = {
  /**
   * Create a new transaction record
   * @param {Object} data - { transactionType, sku, warehouseId, quantityChange, orderId, reservationId, reason }
   * @returns {Object} Created transaction
   */
  async create(data) {
    const {
      transactionType,
      sku,
      warehouseId,
      quantityChange,
      orderId = null,
      reservationId = null,
      reason = null
    } = data;

    const transactionId = uuidv4();
    let connection;

    try {
      connection = await getConnection();

      await connection.execute(
        `INSERT INTO inventory_transactions
         (transaction_id, transaction_type, sku, warehouse_id, quantity_change, order_id, reservation_id, reason)
         VALUES (:transactionId, :transactionType, :sku, :warehouseId, :quantityChange, :orderId, :reservationId, :reason)`,
        {
          transactionId,
          transactionType,
          sku,
          warehouseId,
          quantityChange,
          orderId,
          reservationId,
          reason
        }
      );

      await connection.commit();

      // Return created transaction
      const created = await this.findById(transactionId);
      return created;
    } catch (err) {
      if (connection) {
        try {
          await connection.rollback();
        } catch (rollbackErr) {
          console.error('Error rolling back:', rollbackErr);
        }
      }
      console.error('Error in Transaction.create:', err);
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
   * Get transaction by ID
   * @param {string} transactionId
   * @returns {Object|null} Transaction record
   */
  async findById(transactionId) {
    let connection;
    try {
      connection = await getConnection();

      const result = await connection.execute(
        `SELECT
          t.transaction_id,
          t.transaction_type,
          t.sku,
          t.warehouse_id,
          t.quantity_change,
          t.order_id,
          t.reservation_id,
          t.reason,
          t.created_at,
          s.product_name
        FROM inventory_transactions t
        LEFT JOIN inventory_stock s ON t.sku = s.sku AND t.warehouse_id = s.warehouse_id
        WHERE t.transaction_id = :transactionId`,
        { transactionId },
        { outFormat: require('oracledb').OUT_FORMAT_OBJECT }
      );

      return result.rows.length > 0 ? result.rows[0] : null;
    } catch (err) {
      console.error('Error in Transaction.findById:', err);
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
   * Get all transactions with optional filters and pagination
   * @param {Object} options - { transactionType, sku, warehouseId, limit, offset }
   * @returns {Object} { transactions: Array, total: number }
   */
  async findAll(options = {}) {
    const {
      transactionType = null,
      sku = null,
      warehouseId = null,
      limit = 100,
      offset = 0
    } = options;

    let connection;
    try {
      connection = await getConnection();

      // Build query
      let query = `
        SELECT
          t.transaction_id,
          t.transaction_type,
          t.sku,
          t.warehouse_id,
          t.quantity_change,
          t.order_id,
          t.reservation_id,
          t.reason,
          t.created_at,
          s.product_name
        FROM inventory_transactions t
        LEFT JOIN inventory_stock s ON t.sku = s.sku AND t.warehouse_id = s.warehouse_id
      `;

      const binds = {};
      const conditions = [];

      if (transactionType) {
        conditions.push('t.transaction_type = :transactionType');
        binds.transactionType = transactionType;
      }

      if (sku) {
        conditions.push('t.sku = :sku');
        binds.sku = sku;
      }

      if (warehouseId) {
        conditions.push('t.warehouse_id = :warehouseId');
        binds.warehouseId = warehouseId;
      }

      if (conditions.length > 0) {
        query += ' WHERE ' + conditions.join(' AND ');
      }

      query += ' ORDER BY t.created_at DESC';

      // Get total count
      let countQuery = `
        SELECT COUNT(*) as total
        FROM inventory_transactions t
      `;
      if (conditions.length > 0) {
        countQuery += ' WHERE ' + conditions.join(' AND ');
      }

      const countResult = await connection.execute(countQuery, binds, {
        outFormat: require('oracledb').OUT_FORMAT_OBJECT
      });
      const total = countResult.rows[0].TOTAL;

      // Add pagination
      binds.limit = limit;
      binds.offset = offset;
      query = `
        SELECT * FROM (
          SELECT a.*, ROWNUM rnum FROM (
            ${query}
          ) a WHERE ROWNUM <= :offset + :limit
        ) WHERE rnum > :offset
      `;

      const result = await connection.execute(query, binds, {
        outFormat: require('oracledb').OUT_FORMAT_OBJECT
      });

      return {
        transactions: result.rows,
        total,
        limit,
        offset
      };
    } catch (err) {
      console.error('Error in Transaction.findAll:', err);
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

module.exports = Transaction;
