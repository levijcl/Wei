const express = require('express');
const router = express.Router();
const transactionController = require('../controllers/transactionController');

// GET /api/transactions - Get transaction history
router.get('/', transactionController.getAll);

module.exports = router;
