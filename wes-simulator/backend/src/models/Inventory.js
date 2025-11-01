const db = require('../config/database');
const { v4: uuidv4 } = require('uuid');

class Inventory {
  /**
   * Get inventory snapshot for a warehouse
   */
  static async getSnapshot(warehouseId = null) {
    let sql = `
      SELECT
        sku,
        product_name,
        warehouse_id,
        quantity,
        location,
        updated_at
      FROM wes_inventory
      WHERE 1=1
    `;

    const binds = {};

    if (warehouseId) {
      sql += ' AND warehouse_id = :warehouseId';
      binds.warehouseId = warehouseId;
    }

    sql += ' ORDER BY sku, warehouse_id';

    const result = await db.execute(sql, binds, {
      outFormat: db.oracledb.OUT_FORMAT_OBJECT
    });

    return result.rows;
  }

  /**
   * Get inventory for a specific SKU
   */
  static async getBySku(sku, warehouseId = null) {
    let sql = `
      SELECT
        sku,
        product_name,
        warehouse_id,
        quantity,
        location,
        updated_at
      FROM wes_inventory
      WHERE sku = :sku
    `;

    const binds = { sku };

    if (warehouseId) {
      sql += ' AND warehouse_id = :warehouseId';
      binds.warehouseId = warehouseId;
    }

    const result = await db.execute(sql, binds, {
      outFormat: db.oracledb.OUT_FORMAT_OBJECT
    });

    return result.rows;
  }

  /**
   * Adjust inventory quantity
   */
  static async adjustInventory(adjustmentData) {
    let connection;
    try {
      connection = await db.getPool().getConnection();

      const checkSql = `
        SELECT quantity
        FROM wes_inventory
        WHERE sku = :sku
          AND warehouse_id = :warehouseId
      `;

      const checkResult = await connection.execute(
        checkSql,
        {
          sku: adjustmentData.sku,
          warehouseId: adjustmentData.warehouse_id || 'WH001'
        },
        { outFormat: db.oracledb.OUT_FORMAT_OBJECT }
      );

      if (checkResult.rows.length === 0) {
        // Insert new inventory record
        const insertSql = `
          INSERT INTO wes_inventory (
            sku,
            product_name,
            warehouse_id,
            quantity,
            location,
            updated_at
          ) VALUES (
            :sku,
            :productName,
            :warehouseId,
            :quantity,
            :location,
            SYSTIMESTAMP
          )
        `;

        await connection.execute(
          insertSql,
          {
            sku: adjustmentData.sku,
            productName: adjustmentData.product_name || adjustmentData.sku,
            warehouseId: adjustmentData.warehouse_id || 'WH001',
            quantity: adjustmentData.quantity_change,
            location: adjustmentData.location || null
          },
          { autoCommit: false }
        );
      } else {
        // Update existing inventory
        const updateSql = `
          UPDATE wes_inventory
          SET quantity = quantity + :quantityChange,
              updated_at = SYSTIMESTAMP
          WHERE sku = :sku
            AND warehouse_id = :warehouseId
        `;

        await connection.execute(
          updateSql,
          {
            quantityChange: adjustmentData.quantity_change,
            sku: adjustmentData.sku,
            warehouseId: adjustmentData.warehouse_id || 'WH001'
          },
          { autoCommit: false }
        );
      }

      // Log the adjustment
      const logSql = `
        INSERT INTO wes_inventory_adjustments (
          adjustment_id,
          sku,
          warehouse_id,
          quantity_change,
          reason,
          created_at
        ) VALUES (
          :adjustmentId,
          :sku,
          :warehouseId,
          :quantityChange,
          :reason,
          SYSTIMESTAMP
        )
      `;

      await connection.execute(
        logSql,
        {
          adjustmentId: uuidv4(),
          sku: adjustmentData.sku,
          warehouseId: adjustmentData.warehouse_id || 'WH001',
          quantityChange: adjustmentData.quantity_change,
          reason: adjustmentData.reason || 'Manual adjustment'
        },
        { autoCommit: false }
      );

      await connection.commit();

      return true;
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

  /**
   * Get adjustment history
   */
  static async getAdjustmentHistory(filters = {}) {
    let sql = `
      SELECT
        adjustment_id,
        sku,
        warehouse_id,
        quantity_change,
        reason,
        created_at
      FROM wes_inventory_adjustments
      WHERE 1=1
    `;

    const binds = {};

    if (filters.sku) {
      sql += ' AND sku = :sku';
      binds.sku = filters.sku;
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
}

module.exports = Inventory;
