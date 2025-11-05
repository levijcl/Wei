const express = require('express');
const router = express.Router();
const inventoryController = require('../controllers/inventoryController');

// GET /api/inventory - Get all inventory
router.get('/', inventoryController.getAll);

// GET /api/inventory/:sku - Get inventory by SKU
router.get('/:sku', inventoryController.getBySku);

// POST /api/inventory/increase - Increase inventory
router.post('/increase', inventoryController.increase);

// POST /api/inventory/adjust - Adjust inventory
router.post('/adjust', inventoryController.adjust);

module.exports = router;
