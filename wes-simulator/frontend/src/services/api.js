import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:3678/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Task APIs
export const getTasks = (filters = {}) => {
  return api.get('/tasks', { params: filters });
};

export const getTaskById = (taskId) => {
  return api.get(`/tasks/${taskId}`);
};

export const createTask = (taskData) => {
  return api.post('/tasks', taskData);
};

export const updateTaskStatus = (taskId, status) => {
  return api.put(`/tasks/${taskId}/status`, { status });
};

export const updateTaskPriority = (taskId, priority) => {
  return api.put(`/tasks/${taskId}/priority`, { priority });
};

export const cancelTask = (taskId) => {
  return api.delete(`/tasks/${taskId}`);
};

// Inventory APIs
export const getInventory = (warehouseId = null) => {
  const params = warehouseId ? { warehouse_id: warehouseId } : {};
  return api.get('/inventory', { params });
};

export const getInventoryBySku = (sku, warehouseId = null) => {
  const params = warehouseId ? { warehouse_id: warehouseId } : {};
  return api.get(`/inventory/${sku}`, { params });
};

export const adjustInventory = (adjustmentData) => {
  return api.post('/inventory/adjust', adjustmentData);
};

export const getAdjustmentHistory = (filters = {}) => {
  return api.get('/inventory-adjustments', { params: filters });
};

export default api;
