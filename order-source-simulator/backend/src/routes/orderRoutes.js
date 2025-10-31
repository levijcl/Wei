const express = require('express');
const router = express.Router();
const orderController = require('../controllers/orderController');

router.post('/orders', orderController.createOrder);
router.get('/orders', orderController.getAllOrders);
router.get('/orders/poll', orderController.pollNewOrders);
router.get('/orders/:orderId', orderController.getOrderById);
router.put('/orders/:orderId/status', orderController.updateOrderStatus);

module.exports = router;
