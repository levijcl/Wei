const express = require('express');
const router = express.Router();
const reservationController = require('../controllers/reservationController');

// GET /api/reservations - Get all reservations
router.get('/', reservationController.getAll);

// GET /api/reservations/:reservationId - Get reservation by ID
router.get('/:reservationId', reservationController.getById);

// POST /api/reservations - Create reservation
router.post('/', reservationController.create);

// POST /api/reservations/:reservationId/consume - Consume reservation
router.post('/:reservationId/consume', reservationController.consume);

// POST /api/reservations/:reservationId/release - Release reservation
router.post('/:reservationId/release', reservationController.release);

module.exports = router;
