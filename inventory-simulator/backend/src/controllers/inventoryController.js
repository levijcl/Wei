const Inventory = require('../models/Inventory');
const Transaction = require('../models/Transaction');

const inventoryController = {
  /**
   * GET /api/inventory
   * Get all inventory records
   * Query params: warehouse_id (optional)
   */
  async getAll(req, res) {
    try {
      const { warehouse_id } = req.query;
      const inventory = await Inventory.findAll(warehouse_id);

      res.status(200).json({
        success: true,
        count: inventory.length,
        data: inventory
      });
    } catch (err) {
      console.error('Error in inventoryController.getAll:', err);
      res.status(500).json({
        success: false,
        error: 'Failed to retrieve inventory',
        details: err.message
      });
    }
  },

  /**
   * GET /api/inventory/:sku
   * Get inventory for specific SKU across all warehouses
   */
  async getBySku(req, res) {
    try {
      const { sku } = req.params;
      const inventory = await Inventory.findBySku(sku);

      if (inventory.length === 0) {
        return res.status(404).json({
          success: false,
          error: `Inventory not found for SKU ${sku}`
        });
      }

      res.status(200).json({
        success: true,
        count: inventory.length,
        data: inventory
      });
    } catch (err) {
      console.error('Error in inventoryController.getBySku:', err);
      res.status(500).json({
        success: false,
        error: 'Failed to retrieve inventory',
        details: err.message
      });
    }
  },

  /**
   * POST /api/inventory/increase
   * Increase inventory (INBOUND transaction)
   * Body: { sku, warehouse_id, quantity, reason, product_name?, location? }
   */
  async increase(req, res) {
    try {
      const { sku, warehouse_id, quantity, reason, product_name, location } = req.body;

      // Validation
      if (!sku || !warehouse_id || !quantity) {
        return res.status(400).json({
          success: false,
          error: 'Missing required fields: sku, warehouse_id, quantity'
        });
      }

      if (quantity <= 0) {
        return res.status(400).json({
          success: false,
          error: 'Quantity must be greater than 0'
        });
      }

      // Increase inventory
      const updated = await Inventory.increase({
        sku,
        warehouseId: warehouse_id,
        quantity,
        reason,
        productName: product_name,
        location
      });

      // Create INBOUND transaction record
      await Transaction.create({
        transactionType: 'INBOUND',
        sku,
        warehouseId: warehouse_id,
        quantityChange: quantity,
        reason: reason || 'Inventory increase'
      });

      res.status(200).json({
        success: true,
        message: `Increased inventory for ${sku} by ${quantity} units`,
        data: updated
      });
    } catch (err) {
      console.error('Error in inventoryController.increase:', err);
      res.status(500).json({
        success: false,
        error: 'Failed to increase inventory',
        details: err.message
      });
    }
  },

  /**
   * POST /api/inventory/adjust
   * Adjust inventory (ADJUSTMENT transaction)
   * Body: { sku, warehouse_id, adjustment, reason }
   */
  async adjust(req, res) {
    try {
      const { sku, warehouse_id, adjustment, reason } = req.body;

      // Validation
      if (!sku || !warehouse_id || adjustment === undefined || adjustment === null) {
        return res.status(400).json({
          success: false,
          error: 'Missing required fields: sku, warehouse_id, adjustment'
        });
      }

      if (adjustment === 0) {
        return res.status(400).json({
          success: false,
          error: 'Adjustment cannot be 0'
        });
      }

      if (!reason) {
        return res.status(400).json({
          success: false,
          error: 'Reason is required for inventory adjustments'
        });
      }

      // Adjust inventory
      const updated = await Inventory.adjust({
        sku,
        warehouseId: warehouse_id,
        adjustment,
        reason
      });

      // Create ADJUSTMENT transaction record
      await Transaction.create({
        transactionType: 'ADJUSTMENT',
        sku,
        warehouseId: warehouse_id,
        quantityChange: adjustment,
        reason
      });

      res.status(200).json({
        success: true,
        message: `Adjusted inventory for ${sku} by ${adjustment} units`,
        data: updated
      });
    } catch (err) {
      console.error('Error in inventoryController.adjust:', err);
      res.status(400).json({
        success: false,
        error: 'Failed to adjust inventory',
        details: err.message
      });
    }
  }
};

module.exports = inventoryController;
