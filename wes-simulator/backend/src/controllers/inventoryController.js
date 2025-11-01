const Inventory = require('../models/Inventory');

const inventoryController = {
  async getInventorySnapshot(req, res) {
    try {
      const warehouseId = req.query.warehouse_id;
      const inventory = await Inventory.getSnapshot(warehouseId);

      res.json({
        count: inventory.length,
        warehouse_id: warehouseId || 'ALL',
        inventory
      });
    } catch (error) {
      console.error('Error fetching inventory snapshot:', error);
      res.status(500).json({
        error: 'Failed to fetch inventory snapshot',
        details: error.message
      });
    }
  },

  async getInventoryBySku(req, res) {
    try {
      const { sku } = req.params;
      const warehouseId = req.query.warehouse_id;

      const inventory = await Inventory.getBySku(sku, warehouseId);

      if (inventory.length === 0) {
        return res.status(404).json({
          error: 'SKU not found in inventory'
        });
      }

      res.json({
        sku,
        inventory
      });
    } catch (error) {
      console.error('Error fetching inventory by SKU:', error);
      res.status(500).json({
        error: 'Failed to fetch inventory',
        details: error.message
      });
    }
  },

  async adjustInventory(req, res) {
    try {
      const adjustmentData = req.body;

      if (!adjustmentData.sku || typeof adjustmentData.quantity_change !== 'number') {
        return res.status(400).json({
          error: 'Missing required fields: sku and quantity_change are required'
        });
      }

      await Inventory.adjustInventory(adjustmentData);

      res.json({
        message: 'Inventory adjusted successfully',
        sku: adjustmentData.sku,
        quantity_change: adjustmentData.quantity_change
      });
    } catch (error) {
      console.error('Error adjusting inventory:', error);
      res.status(500).json({
        error: 'Failed to adjust inventory',
        details: error.message
      });
    }
  },

  async getAdjustmentHistory(req, res) {
    try {
      const filters = {
        sku: req.query.sku,
        warehouse_id: req.query.warehouse_id
      };

      const adjustments = await Inventory.getAdjustmentHistory(filters);

      res.json({
        count: adjustments.length,
        adjustments
      });
    } catch (error) {
      console.error('Error fetching adjustment history:', error);
      res.status(500).json({
        error: 'Failed to fetch adjustment history',
        details: error.message
      });
    }
  }
};

module.exports = inventoryController;
