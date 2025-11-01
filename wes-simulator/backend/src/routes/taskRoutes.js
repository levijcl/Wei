const express = require('express');
const router = express.Router();
const taskController = require('../controllers/taskController');

// Task management routes
router.post('/tasks', taskController.createTask);
router.get('/tasks', taskController.getAllTasks);
router.get('/tasks/poll', taskController.pollTasks);
router.get('/tasks/:taskId', taskController.getTaskById);
router.put('/tasks/:taskId/status', taskController.updateTaskStatus);
router.put('/tasks/:taskId/priority', taskController.updateTaskPriority);
router.delete('/tasks/:taskId', taskController.cancelTask);

module.exports = router;
