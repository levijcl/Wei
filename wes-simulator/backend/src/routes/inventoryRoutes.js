const express = require('express');
const router = express.Router();
const inventoryController = require('../controllers/inventoryController');

// Inventory management routes
router.get('/inventory', inventoryController.getInventorySnapshot);
router.get('/inventory/:sku', inventoryController.getInventoryBySku);
router.post('/inventory/adjust', inventoryController.adjustInventory);
router.get('/inventory-adjustments', inventoryController.getAdjustmentHistory);

module.exports = router;
