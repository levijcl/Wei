import React, { useState, useEffect } from 'react';
import { getTasks, createTask, updateTaskPriority, cancelTask } from '../services/api';
import '../styles/TaskList.css';

function TaskList() {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);

  // Create task form state
  const [taskType, setTaskType] = useState('PICKING');
  const [orderId, setOrderId] = useState('');
  const [items, setItems] = useState([{ sku: '', quantity: 1 }]);

  useEffect(() => {
    fetchTasks();
    // Auto-refresh every 5 seconds
    const interval = setInterval(fetchTasks, 5000);
    return () => clearInterval(interval);
  }, []);

  const fetchTasks = async () => {
    try {
      setLoading(true);
      const response = await getTasks();
      setTasks(response.data.tasks);
      setError('');
    } catch (err) {
      setError('Failed to fetch tasks');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateTask = async (e) => {
    e.preventDefault();
    try {
      const taskData = {
        task_type: taskType,
        order_id: orderId || undefined,
        items: items.filter(item => item.sku && item.quantity > 0)
      };

      await createTask(taskData);
      setShowCreateForm(false);
      setOrderId('');
      setItems([{ sku: '', quantity: 1 }]);
      fetchTasks();
    } catch (err) {
      setError('Failed to create task');
      console.error(err);
    }
  };

  const handleCancelTask = async (taskId) => {
    if (window.confirm('Are you sure you want to cancel this task?')) {
      try {
        await cancelTask(taskId);
        fetchTasks();
      } catch (err) {
        setError('Failed to cancel task');
        console.error(err);
      }
    }
  };

  const addItem = () => {
    setItems([...items, { sku: '', quantity: 1 }]);
  };

  const updateItem = (index, field, value) => {
    const newItems = [...items];
    newItems[index][field] = value;
    setItems(newItems);
  };

  const removeItem = (index) => {
    setItems(items.filter((_, i) => i !== index));
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'PENDING':
        return 'status-pending';
      case 'IN_PROGRESS':
        return 'status-in-progress';
      case 'COMPLETED':
        return 'status-completed';
      case 'FAILED':
        return 'status-failed';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return '';
    }
  };

  const formatTimestamp = (timestamp) => {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleString();
  };

  const getTimeRemaining = (task) => {
    // Only show countdown for IN_PROGRESS tasks
    if (task.STATUS !== 'IN_PROGRESS') {
      return null; // Will be handled by caller
    }

    // Use started_at timestamp for countdown
    const startedAt = task.STARTED_AT;
    if (!startedAt) {
      return '60s'; // Default if started_at is missing
    }

    const started = new Date(startedAt);
    const now = new Date();
    const elapsed = Math.floor((now - started) / 1000);
    const remaining = 60 - elapsed;
    return remaining > 0 ? `${remaining}s` : 'Completing...';
  };

  return (
    <div className="task-list-container">
      <div className="task-list-header">
        <h2>Task Status</h2>
        <button className="btn-primary" onClick={() => setShowCreateForm(!showCreateForm)}>
          {showCreateForm ? 'Cancel' : 'Create Task'}
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {showCreateForm && (
        <div className="create-task-form">
          <h3>Create New Task</h3>
          <form onSubmit={handleCreateTask}>
            <div className="form-group">
              <label>Task Type:</label>
              <select value={taskType} onChange={(e) => setTaskType(e.target.value)}>
                <option value="PICKING">Picking</option>
                <option value="PUTAWAY">Putaway</option>
              </select>
            </div>

            <div className="form-group">
              <label>Order ID (optional):</label>
              <input
                type="text"
                value={orderId}
                onChange={(e) => setOrderId(e.target.value)}
                placeholder="ORDER-123"
              />
            </div>

            <div className="form-group">
              <label>Items:</label>
              {items.map((item, index) => (
                <div key={index} className="item-row">
                  <input
                    type="text"
                    placeholder="SKU"
                    value={item.sku}
                    onChange={(e) => updateItem(index, 'sku', e.target.value)}
                    required
                  />
                  <input
                    type="number"
                    placeholder="Qty"
                    value={item.quantity}
                    onChange={(e) => updateItem(index, 'quantity', parseInt(e.target.value))}
                    min="1"
                    required
                  />
                  {items.length > 1 && (
                    <button type="button" onClick={() => removeItem(index)}>Remove</button>
                  )}
                </div>
              ))}
              <button type="button" onClick={addItem}>Add Item</button>
            </div>

            <button type="submit" className="btn-primary">Create Task</button>
          </form>
        </div>
      )}

      <div className="task-stats">
        <div className="stat">
          <span className="stat-label">Total:</span>
          <span className="stat-value">{tasks.length}</span>
        </div>
        <div className="stat">
          <span className="stat-label">Pending:</span>
          <span className="stat-value">{tasks.filter(t => t.STATUS === 'PENDING').length}</span>
        </div>
        <div className="stat">
          <span className="stat-label">In Progress:</span>
          <span className="stat-value">{tasks.filter(t => t.STATUS === 'IN_PROGRESS').length}</span>
        </div>
        <div className="stat">
          <span className="stat-label">Completed:</span>
          <span className="stat-value">{tasks.filter(t => t.STATUS === 'COMPLETED').length}</span>
        </div>
      </div>

      {loading && <div className="loading">Loading tasks...</div>}

      <div className="task-table">
        <table>
          <thead>
            <tr>
              <th>Task ID</th>
              <th>Type</th>
              <th>Order ID</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Time Remaining</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {tasks.length === 0 ? (
              <tr>
                <td colSpan="8" style={{ textAlign: 'center' }}>
                  No tasks found. Create a task to get started.
                </td>
              </tr>
            ) : (
              tasks.map(task => (
                <tr key={task.TASK_ID}>
                  <td>{task.TASK_ID.substring(0, 8)}...</td>
                  <td>{task.TASK_TYPE}</td>
                  <td>{task.ORDER_ID ? task.ORDER_ID.substring(0, 8) + '...' : 'N/A'}</td>
                  <td>
                    <span className={`status-badge ${getStatusBadgeClass(task.STATUS)}`}>
                      {task.STATUS}
                    </span>
                  </td>
                  <td>{task.PRIORITY}</td>
                  <td>
                    {task.STATUS === 'COMPLETED' ? 'Done' :
                     task.STATUS === 'CANCELLED' ? 'Cancelled' :
                     task.STATUS === 'FAILED' ? 'Failed' :
                     task.STATUS === 'PENDING' ? 'Waiting in queue...' :
                     getTimeRemaining(task)}
                  </td>
                  <td>{formatTimestamp(task.CREATED_AT)}</td>
                  <td>
                    {task.STATUS !== 'COMPLETED' && task.STATUS !== 'CANCELLED' && (
                      <button
                        className="btn-cancel"
                        onClick={() => handleCancelTask(task.TASK_ID)}
                      >
                        Cancel
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default TaskList;
