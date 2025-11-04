const Reservation = require('../models/Reservation');
const Transaction = require('../models/Transaction');

const reservationController = {
  /**
   * GET /api/reservations
   * Get all reservations
   * Query params: status (optional)
   */
  async getAll(req, res) {
    try {
      const { status } = req.query;
      const reservations = await Reservation.findAll(status);

      res.status(200).json({
        success: true,
        count: reservations.length,
        data: reservations
      });
    } catch (err) {
      console.error('Error in reservationController.getAll:', err);
      res.status(500).json({
        success: false,
        error: 'Failed to retrieve reservations',
        details: err.message
      });
    }
  },

  /**
   * GET /api/reservations/:reservationId
   * Get reservation by ID
   */
  async getById(req, res) {
    try {
      const { reservationId } = req.params;
      const reservation = await Reservation.findById(reservationId);

      if (!reservation) {
        return res.status(404).json({
          success: false,
          error: `Reservation ${reservationId} not found`
        });
      }

      res.status(200).json({
        success: true,
        data: reservation
      });
    } catch (err) {
      console.error('Error in reservationController.getById:', err);
      res.status(500).json({
        success: false,
        error: 'Failed to retrieve reservation',
        details: err.message
      });
    }
  },

  /**
   * POST /api/reservations
   * Create a new reservation (reserve inventory)
   * Body: { sku, warehouse_id, order_id, quantity }
   */
  async create(req, res) {
    try {
      const { sku, warehouse_id, order_id, quantity } = req.body;

      // Validation
      if (!sku || !warehouse_id || !order_id || !quantity) {
        return res.status(400).json({
          success: false,
          error: 'Missing required fields: sku, warehouse_id, order_id, quantity'
        });
      }

      if (quantity <= 0) {
        return res.status(400).json({
          success: false,
          error: 'Quantity must be greater than 0'
        });
      }

      // Create reservation
      const reservation = await Reservation.create({
        sku,
        warehouseId: warehouse_id,
        orderId: order_id,
        quantity
      });

      res.status(201).json({
        success: true,
        message: `Reserved ${quantity} units of ${sku} for order ${order_id}`,
        data: reservation
      });
    } catch (err) {
      console.error('Error in reservationController.create:', err);
      res.status(400).json({
        success: false,
        error: 'Failed to create reservation',
        details: err.message
      });
    }
  },

  /**
   * POST /api/reservations/:reservationId/consume
   * Consume a reservation (convert to outbound)
   */
  async consume(req, res) {
    try {
      const { reservationId } = req.params;

      // Get reservation before consuming (to create transaction)
      const reservationBefore = await Reservation.findById(reservationId);

      if (!reservationBefore) {
        return res.status(404).json({
          success: false,
          error: `Reservation ${reservationId} not found`
        });
      }

      // Consume reservation
      const reservation = await Reservation.consume(reservationId);

      // Create OUTBOUND transaction record
      await Transaction.create({
        transactionType: 'OUTBOUND',
        sku: reservation.SKU,
        warehouseId: reservation.WAREHOUSE_ID,
        quantityChange: -reservation.QUANTITY,
        orderId: reservation.ORDER_ID,
        reservationId: reservation.RESERVATION_ID,
        reason: `Consumed reservation for order ${reservation.ORDER_ID}`
      });

      res.status(200).json({
        success: true,
        message: `Consumed reservation ${reservationId}`,
        data: reservation
      });
    } catch (err) {
      console.error('Error in reservationController.consume:', err);
      res.status(400).json({
        success: false,
        error: 'Failed to consume reservation',
        details: err.message
      });
    }
  },

  /**
   * POST /api/reservations/:reservationId/release
   * Release a reservation (cancel and return to available)
   */
  async release(req, res) {
    try {
      const { reservationId } = req.params;

      const reservation = await Reservation.release(reservationId);

      res.status(200).json({
        success: true,
        message: `Released reservation ${reservationId}`,
        data: reservation
      });
    } catch (err) {
      console.error('Error in reservationController.release:', err);
      res.status(400).json({
        success: false,
        error: 'Failed to release reservation',
        details: err.message
      });
    }
  }
};

module.exports = reservationController;
