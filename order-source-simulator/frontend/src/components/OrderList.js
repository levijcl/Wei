import React, { useState, useEffect } from 'react';
import { orderAPI } from '../services/api';
import '../styles/OrderList.css';

const OrderList = ({ refreshTrigger }) => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filters, setFilters] = useState({
    status: '',
    warehouse_id: ''
  });
  const [selectedOrder, setSelectedOrder] = useState(null);

  useEffect(() => {
    fetchOrders();
  }, [refreshTrigger, filters]);

  const fetchOrders = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await orderAPI.getAllOrders(filters);
      setOrders(data.orders);
    } catch (err) {
      setError('Failed to fetch orders');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({ ...prev, [name]: value }));
  };

  const handleStatusUpdate = async (orderId, newStatus) => {
    try {
      await orderAPI.updateOrderStatus(orderId, newStatus);
      fetchOrders();
      if (selectedOrder && selectedOrder.ORDER_ID === orderId) {
        const updatedOrder = await orderAPI.getOrderById(orderId);
        setSelectedOrder(updatedOrder);
      }
    } catch (err) {
      alert('Failed to update order status');
      console.error(err);
    }
  };

  const viewOrderDetails = async (orderId) => {
    try {
      const order = await orderAPI.getOrderById(orderId);
      setSelectedOrder(order);
    } catch (err) {
      alert('Failed to fetch order details');
      console.error(err);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString();
  };

  const getStatusColor = (status) => {
    const colors = {
      'NEW': '#4caf50',
      'IN_PROGRESS': '#ff9800',
      'COMPLETED': '#2196f3',
      'FAILED': '#f44336'
    };
    return colors[status] || '#757575';
  };

  if (loading) {
    return <div className="loading">Loading orders...</div>;
  }

  return (
    <div className="order-list-container">
      <div className="order-list-header">
        <h2>Order List ({orders.length})</h2>

        <div className="filters">
          <select
            name="status"
            value={filters.status}
            onChange={handleFilterChange}
          >
            <option value="">All Statuses</option>
            <option value="NEW">NEW</option>
            <option value="IN_PROGRESS">IN_PROGRESS</option>
            <option value="COMPLETED">COMPLETED</option>
            <option value="FAILED">FAILED</option>
          </select>

          <select
            name="warehouse_id"
            value={filters.warehouse_id}
            onChange={handleFilterChange}
          >
            <option value="">All Warehouses</option>
            <option value="WH001">WH001</option>
            <option value="WH002">WH002</option>
            <option value="WH003">WH003</option>
          </select>

          <button onClick={fetchOrders} className="btn-refresh">
            Refresh
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="order-table-container">
        <table className="order-table">
          <thead>
            <tr>
              <th>Order ID</th>
              <th>Customer</th>
              <th>Type</th>
              <th>Warehouse</th>
              <th>Status</th>
              <th>Scheduled Pickup</th>
              <th>Created At</th>
              <th>Updated At</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {orders.length === 0 ? (
              <tr>
                <td colSpan="9" className="no-data">No orders found</td>
              </tr>
            ) : (
              orders.map((order) => (
                <tr key={order.ORDER_ID}>
                  <td className="order-id">{order.ORDER_ID.substring(0, 8)}...</td>
                  <td>{order.CUSTOMER_NAME}</td>
                  <td>{order.ORDER_TYPE}</td>
                  <td>{order.WAREHOUSE_ID}</td>
                  <td>
                    <span
                      className="status-badge"
                      style={{ backgroundColor: getStatusColor(order.STATUS) }}
                    >
                      {order.STATUS}
                    </span>
                  </td>
                  <td>{formatDate(order.SCHEDULED_PICKUP_TIME)}</td>
                  <td>{formatDate(order.CREATED_AT)}</td>
                  <td>{formatDate(order.UPDATED_AT)}</td>
                  <td className="actions">
                    <button
                      onClick={() => viewOrderDetails(order.ORDER_ID)}
                      className="btn-view"
                    >
                      View
                    </button>
                    {order.STATUS === 'NEW' && (
                      <button
                        onClick={() => handleStatusUpdate(order.ORDER_ID, 'IN_PROGRESS')}
                        className="btn-status"
                      >
                        Process
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {selectedOrder && (
        <div className="modal-overlay" onClick={() => setSelectedOrder(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Order Details</h3>
              <button onClick={() => setSelectedOrder(null)} className="btn-close">×</button>
            </div>

            <div className="modal-body">
              <div className="detail-section">
                <h4>Order Information</h4>
                <p><strong>Order ID:</strong> {selectedOrder.ORDER_ID}</p>
                <p><strong>Status:</strong>
                  <span
                    className="status-badge"
                    style={{ backgroundColor: getStatusColor(selectedOrder.STATUS) }}
                  >
                    {selectedOrder.STATUS}
                  </span>
                </p>
                <p><strong>Order Type:</strong> {selectedOrder.ORDER_TYPE}</p>
                <p><strong>Warehouse:</strong> {selectedOrder.WAREHOUSE_ID}</p>
                <p><strong>Scheduled Pickup:</strong> {formatDate(selectedOrder.SCHEDULED_PICKUP_TIME)}</p>
                <p><strong>Created At:</strong> {formatDate(selectedOrder.CREATED_AT)}</p>
                <p><strong>Updated At:</strong> {formatDate(selectedOrder.UPDATED_AT)}</p>
              </div>

              <div className="detail-section">
                <h4>Customer Information</h4>
                <p><strong>Name:</strong> {selectedOrder.CUSTOMER_NAME}</p>
                <p><strong>Email:</strong> {selectedOrder.CUSTOMER_EMAIL || 'N/A'}</p>
                <p><strong>Address:</strong> {selectedOrder.SHIPPING_ADDRESS || 'N/A'}</p>
              </div>

              <div className="detail-section">
                <h4>Order Items</h4>
                <table className="items-table">
                  <thead>
                    <tr>
                      <th>SKU</th>
                      <th>Product Name</th>
                      <th>Quantity</th>
                      <th>Price</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedOrder.items && selectedOrder.items.map((item, index) => (
                      <tr key={index}>
                        <td>{item.SKU}</td>
                        <td>{item.PRODUCT_NAME}</td>
                        <td>{item.QUANTITY}</td>
                        <td>${item.PRICE?.toFixed(2) || '0.00'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="detail-actions">
                <button
                  onClick={() => handleStatusUpdate(selectedOrder.ORDER_ID, 'IN_PROGRESS')}
                  disabled={selectedOrder.STATUS !== 'NEW'}
                  className="btn-action"
                >
                  Mark In Progress
                </button>
                <button
                  onClick={() => handleStatusUpdate(selectedOrder.ORDER_ID, 'COMPLETED')}
                  disabled={selectedOrder.STATUS === 'COMPLETED'}
                  className="btn-action"
                >
                  Mark Completed
                </button>
                <button
                  onClick={() => handleStatusUpdate(selectedOrder.ORDER_ID, 'FAILED')}
                  disabled={selectedOrder.STATUS === 'FAILED'}
                  className="btn-action btn-danger"
                >
                  Mark Failed
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default OrderList;
