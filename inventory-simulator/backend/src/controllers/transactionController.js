const Transaction = require('../models/Transaction');

const transactionController = {
  /**
   * GET /api/transactions
   * Get transaction history
   * Query params: transaction_type, sku, warehouse_id, limit, offset
   */
  async getAll(req, res) {
    try {
      const {
        transaction_type,
        sku,
        warehouse_id,
        limit = 100,
        offset = 0
      } = req.query;

      const result = await Transaction.findAll({
        transactionType: transaction_type,
        sku,
        warehouseId: warehouse_id,
        limit: parseInt(limit),
        offset: parseInt(offset)
      });

      res.status(200).json({
        success: true,
        count: result.transactions.length,
        total: result.total,
        limit: result.limit,
        offset: result.offset,
        data: result.transactions
      });
    } catch (err) {
      console.error('Error in transactionController.getAll:', err);
      res.status(500).json({
        success: false,
        error: 'Failed to retrieve transactions',
        details: err.message
      });
    }
  }
};

module.exports = transactionController;
