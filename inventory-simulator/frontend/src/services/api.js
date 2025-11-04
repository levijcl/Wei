import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:3778';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Inventory API
export const inventoryAPI = {
  getAll: (warehouseId = null) => {
    const params = warehouseId ? { warehouse_id: warehouseId } : {};
    return api.get('/api/inventory', { params });
  },

  getBySku: (sku) => {
    return api.get(`/api/inventory/${sku}`);
  },

  increase: (data) => {
    return api.post('/api/inventory/increase', data);
  },

  adjust: (data) => {
    return api.post('/api/inventory/adjust', data);
  },
};

// Reservation API
export const reservationAPI = {
  getAll: (status = null) => {
    const params = status ? { status } : {};
    return api.get('/api/reservations', { params });
  },

  getById: (reservationId) => {
    return api.get(`/api/reservations/${reservationId}`);
  },

  create: (data) => {
    return api.post('/api/reservations', data);
  },

  consume: (reservationId) => {
    return api.post(`/api/reservations/${reservationId}/consume`);
  },

  release: (reservationId) => {
    return api.post(`/api/reservations/${reservationId}/release`);
  },
};

// Transaction API
export const transactionAPI = {
  getAll: (params = {}) => {
    return api.get('/api/transactions', { params });
  },
};

export default api;
