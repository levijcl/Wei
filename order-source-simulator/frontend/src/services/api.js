import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const orderAPI = {
  createOrder: async (orderData) => {
    const response = await api.post('/orders', orderData);
    return response.data;
  },

  getAllOrders: async (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.status) params.append('status', filters.status);
    if (filters.warehouse_id) params.append('warehouse_id', filters.warehouse_id);
    if (filters.from_date) params.append('from_date', filters.from_date);

    const response = await api.get(`/orders?${params.toString()}`);
    return response.data;
  },

  getOrderById: async (orderId) => {
    const response = await api.get(`/orders/${orderId}`);
    return response.data;
  },

  updateOrderStatus: async (orderId, status) => {
    const response = await api.put(`/orders/${orderId}/status`, { status });
    return response.data;
  },

  pollNewOrders: async (limit = 50) => {
    const response = await api.get(`/orders/poll?limit=${limit}`);
    return response.data;
  },
};

export default api;
