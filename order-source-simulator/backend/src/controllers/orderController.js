const Order = require('../models/Order');

const orderController = {
  async createOrder(req, res) {
    try {
      const orderData = req.body;

      if (!orderData.customer_name || !orderData.items || orderData.items.length === 0) {
        return res.status(400).json({
          error: 'Missing required fields: customer_name and items are required'
        });
      }

      const orderId = await Order.create(orderData);

      res.status(201).json({
        message: 'Order created successfully',
        order_id: orderId
      });
    } catch (error) {
      console.error('Error creating order:', error);
      res.status(500).json({
        error: 'Failed to create order',
        details: error.message
      });
    }
  },

  async getAllOrders(req, res) {
    try {
      const filters = {
        status: req.query.status,
        fromDate: req.query.from_date,
        warehouse_id: req.query.warehouse_id
      };

      const orders = await Order.findAll(filters);

      res.json({
        count: orders.length,
        orders
      });
    } catch (error) {
      console.error('Error fetching orders:', error);
      res.status(500).json({
        error: 'Failed to fetch orders',
        details: error.message
      });
    }
  },

  async getOrderById(req, res) {
    try {
      const { orderId } = req.params;
      const order = await Order.findById(orderId);

      if (!order) {
        return res.status(404).json({
          error: 'Order not found'
        });
      }

      res.json(order);
    } catch (error) {
      console.error('Error fetching order:', error);
      res.status(500).json({
        error: 'Failed to fetch order',
        details: error.message
      });
    }
  },

  async updateOrderStatus(req, res) {
    try {
      const { orderId } = req.params;
      const { status } = req.body;

      const validStatuses = ['NEW', 'IN_PROGRESS', 'COMPLETED', 'FAILED'];
      if (!validStatuses.includes(status)) {
        return res.status(400).json({
          error: 'Invalid status',
          validStatuses
        });
      }

      const updated = await Order.updateStatus(orderId, status);

      if (!updated) {
        return res.status(404).json({
          error: 'Order not found'
        });
      }

      res.json({
        message: 'Order status updated successfully',
        order_id: orderId,
        status
      });
    } catch (error) {
      console.error('Error updating order status:', error);
      res.status(500).json({
        error: 'Failed to update order status',
        details: error.message
      });
    }
  },

  async pollNewOrders(req, res) {
    try {
      const limit = parseInt(req.query.limit) || 50;
      const orders = await Order.findNewOrders(limit);

      res.json({
        count: orders.length,
        orders
      });
    } catch (error) {
      console.error('Error polling new orders:', error);
      res.status(500).json({
        error: 'Failed to poll new orders',
        details: error.message
      });
    }
  }
};

module.exports = orderController;
