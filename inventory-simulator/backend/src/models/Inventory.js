const { getConnection } = require('../config/database');

const Inventory = {
  /**
   * Get all inventory records with calculated available quantity
   * @param {string} warehouseId - Optional warehouse filter
   * @returns {Array} Inventory records
   */
  async findAll(warehouseId = null) {
    let connection;
    try {
      connection = await getConnection();

      let query = `
        SELECT
          sku,
          product_name,
          warehouse_id,
          total_quantity,
          reserved_quantity,
          (total_quantity - reserved_quantity) AS available_quantity,
          location,
          updated_at
        FROM inventory_stock
      `;

      const binds = {};
      if (warehouseId) {
        query += ' WHERE warehouse_id = :warehouseId';
        binds.warehouseId = warehouseId;
      }

      query += ' ORDER BY sku, warehouse_id';

      const result = await connection.execute(query, binds, {
        outFormat: require('oracledb').OUT_FORMAT_OBJECT
      });

      return result.rows;
    } catch (err) {
      console.error('Error in Inventory.findAll:', err);
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
   * Get inventory for a specific SKU across all warehouses
   * @param {string} sku - SKU to search for
   * @returns {Array} Inventory records for the SKU
   */
  async findBySku(sku) {
    let connection;
    try {
      connection = await getConnection();

      const result = await connection.execute(
        `SELECT
          sku,
          product_name,
          warehouse_id,
          total_quantity,
          reserved_quantity,
          (total_quantity - reserved_quantity) AS available_quantity,
          location,
          updated_at
        FROM inventory_stock
        WHERE sku = :sku
        ORDER BY warehouse_id`,
        { sku },
        { outFormat: require('oracledb').OUT_FORMAT_OBJECT }
      );

      return result.rows;
    } catch (err) {
      console.error('Error in Inventory.findBySku:', err);
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
   * Get inventory for a specific SKU and warehouse
   * @param {string} sku - SKU
   * @param {string} warehouseId - Warehouse ID
   * @returns {Object|null} Inventory record
   */
  async findBySkuAndWarehouse(sku, warehouseId) {
    let connection;
    try {
      connection = await getConnection();

      const result = await connection.execute(
        `SELECT
          sku,
          product_name,
          warehouse_id,
          total_quantity,
          reserved_quantity,
          (total_quantity - reserved_quantity) AS available_quantity,
          location,
          updated_at
        FROM inventory_stock
        WHERE sku = :sku AND warehouse_id = :warehouseId`,
        { sku, warehouseId },
        { outFormat: require('oracledb').OUT_FORMAT_OBJECT }
      );

      return result.rows.length > 0 ? result.rows[0] : null;
    } catch (err) {
      console.error('Error in Inventory.findBySkuAndWarehouse:', err);
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
   * Increase inventory (INBOUND transaction)
   * @param {Object} data - { sku, warehouseId, quantity, reason, productName, location }
   * @returns {Object} Updated inventory record
   */
  async increase(data) {
    const { sku, warehouseId, quantity, reason, productName, location } = data;
    let connection;

    try {
      connection = await getConnection();

      // Check if SKU exists
      const existing = await this.findBySkuAndWarehouse(sku, warehouseId);

      if (existing) {
        // Update existing stock
        await connection.execute(
          `UPDATE inventory_stock
           SET total_quantity = total_quantity + :quantity,
               updated_at = SYSTIMESTAMP
           WHERE sku = :sku AND warehouse_id = :warehouseId`,
          { quantity, sku, warehouseId }
        );
      } else {
        // Create new stock record
        await connection.execute(
          `INSERT INTO inventory_stock (sku, product_name, warehouse_id, total_quantity, reserved_quantity, location)
           VALUES (:sku, :productName, :warehouseId, :quantity, 0, :location)`,
          {
            sku,
            productName: productName || sku,
            warehouseId,
            quantity,
            location: location || 'N/A'
          }
        );
      }

      await connection.commit();

      // Return updated record
      const updated = await this.findBySkuAndWarehouse(sku, warehouseId);
      return updated;
    } catch (err) {
      if (connection) {
        try {
          await connection.rollback();
        } catch (rollbackErr) {
          console.error('Error rolling back:', rollbackErr);
        }
      }
      console.error('Error in Inventory.increase:', err);
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
   * Adjust inventory (ADJUSTMENT transaction)
   * @param {Object} data - { sku, warehouseId, adjustment, reason }
   * @returns {Object} Updated inventory record
   */
  async adjust(data) {
    const { sku, warehouseId, adjustment, reason } = data;
    let connection;

    try {
      connection = await getConnection();

      // Check if SKU exists
      const existing = await this.findBySkuAndWarehouse(sku, warehouseId);

      if (!existing) {
        throw new Error(`Inventory not found for SKU ${sku} in warehouse ${warehouseId}`);
      }

      const newTotal = existing.TOTAL_QUANTITY + adjustment;

      // Validate new total is not negative and not less than reserved
      if (newTotal < 0) {
        throw new Error(`Adjustment would result in negative inventory. Current: ${existing.TOTAL_QUANTITY}, Adjustment: ${adjustment}`);
      }

      if (newTotal < existing.RESERVED_QUANTITY) {
        throw new Error(`Adjustment would result in total (${newTotal}) less than reserved (${existing.RESERVED_QUANTITY})`);
      }

      // Update stock
      await connection.execute(
        `UPDATE inventory_stock
         SET total_quantity = :newTotal,
             updated_at = SYSTIMESTAMP
         WHERE sku = :sku AND warehouse_id = :warehouseId`,
        { newTotal, sku, warehouseId }
      );

      await connection.commit();

      // Return updated record
      const updated = await this.findBySkuAndWarehouse(sku, warehouseId);
      return updated;
    } catch (err) {
      if (connection) {
        try {
          await connection.rollback();
        } catch (rollbackErr) {
          console.error('Error rolling back:', rollbackErr);
        }
      }
      console.error('Error in Inventory.adjust:', err);
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
   * Update reserved quantity (called internally by Reservation model)
   * @param {string} sku
   * @param {string} warehouseId
   * @param {number} change - Positive to increase, negative to decrease
   */
  async updateReservedQuantity(sku, warehouseId, change, connection) {
    await connection.execute(
      `UPDATE inventory_stock
       SET reserved_quantity = reserved_quantity + :change,
           updated_at = SYSTIMESTAMP
       WHERE sku = :sku AND warehouse_id = :warehouseId`,
      { change, sku, warehouseId }
    );
  },

  /**
   * Decrease total quantity (called when reservation is consumed)
   * @param {string} sku
   * @param {string} warehouseId
   * @param {number} quantity
   */
  async decreaseTotal(sku, warehouseId, quantity, connection) {
    await connection.execute(
      `UPDATE inventory_stock
       SET total_quantity = total_quantity - :quantity,
           reserved_quantity = reserved_quantity - :quantity,
           updated_at = SYSTIMESTAMP
       WHERE sku = :sku AND warehouse_id = :warehouseId`,
      { quantity, sku, warehouseId }
    );
  }
};

module.exports = Inventory;
