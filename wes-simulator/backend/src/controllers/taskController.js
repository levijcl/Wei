const Task = require('../models/Task');

const taskController = {
  async createTask(req, res) {
    try {
      const taskData = req.body;

      if (!taskData.task_type || !taskData.items || taskData.items.length === 0) {
        return res.status(400).json({
          error: 'Missing required fields: task_type and items are required'
        });
      }

      const validTaskTypes = ['PICKING', 'PUTAWAY'];
      if (!validTaskTypes.includes(taskData.task_type)) {
        return res.status(400).json({
          error: 'Invalid task_type',
          validTaskTypes
        });
      }

      const taskId = await Task.create(taskData);

      res.status(201).json({
        message: 'Task created successfully',
        task_id: taskId,
        estimated_completion: '1 minute'
      });
    } catch (error) {
      console.error('Error creating task:', error);
      res.status(500).json({
        error: 'Failed to create task',
        details: error.message
      });
    }
  },

  async getAllTasks(req, res) {
    try {
      const filters = {
        status: req.query.status,
        task_type: req.query.task_type,
        warehouse_id: req.query.warehouse_id
      };

      const tasks = await Task.findAll(filters);

      res.json({
        count: tasks.length,
        tasks
      });
    } catch (error) {
      console.error('Error fetching tasks:', error);
      res.status(500).json({
        error: 'Failed to fetch tasks',
        details: error.message
      });
    }
  },

  async getTaskById(req, res) {
    try {
      const { taskId } = req.params;
      const task = await Task.findById(taskId);

      if (!task) {
        return res.status(404).json({
          error: 'Task not found'
        });
      }

      res.json(task);
    } catch (error) {
      console.error('Error fetching task:', error);
      res.status(500).json({
        error: 'Failed to fetch task',
        details: error.message
      });
    }
  },

  async updateTaskStatus(req, res) {
    try {
      const { taskId } = req.params;
      const { status } = req.body;

      const validStatuses = ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'];
      if (!validStatuses.includes(status)) {
        return res.status(400).json({
          error: 'Invalid status',
          validStatuses
        });
      }

      const updated = await Task.updateStatus(taskId, status);

      if (!updated) {
        return res.status(404).json({
          error: 'Task not found'
        });
      }

      res.json({
        message: 'Task status updated successfully',
        task_id: taskId,
        status
      });
    } catch (error) {
      console.error('Error updating task status:', error);
      res.status(500).json({
        error: 'Failed to update task status',
        details: error.message
      });
    }
  },

  async updateTaskPriority(req, res) {
    try {
      const { taskId } = req.params;
      const { priority } = req.body;

      if (typeof priority !== 'number' || priority < 1 || priority > 10) {
        return res.status(400).json({
          error: 'Priority must be a number between 1 and 10'
        });
      }

      const updated = await Task.updatePriority(taskId, priority);

      if (!updated) {
        return res.status(404).json({
          error: 'Task not found'
        });
      }

      res.json({
        message: 'Task priority updated successfully',
        task_id: taskId,
        priority
      });
    } catch (error) {
      console.error('Error updating task priority:', error);
      res.status(500).json({
        error: 'Failed to update task priority',
        details: error.message
      });
    }
  },

  async cancelTask(req, res) {
    try {
      const { taskId } = req.params;

      const cancelled = await Task.cancelTask(taskId);

      if (!cancelled) {
        return res.status(404).json({
          error: 'Task not found or already completed/cancelled'
        });
      }

      res.json({
        message: 'Task cancelled successfully',
        task_id: taskId
      });
    } catch (error) {
      console.error('Error cancelling task:', error);
      res.status(500).json({
        error: 'Failed to cancel task',
        details: error.message
      });
    }
  },

  async pollTasks(req, res) {
    try {
      // Auto-progress tasks first
      await Task.autoProgressTasks();

      // Return all non-completed tasks
      const tasks = await Task.findAll({});

      res.json({
        count: tasks.length,
        tasks
      });
    } catch (error) {
      console.error('Error polling tasks:', error);
      res.status(500).json({
        error: 'Failed to poll tasks',
        details: error.message
      });
    }
  }
};

module.exports = taskController;
