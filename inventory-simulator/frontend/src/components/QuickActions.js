import React, { useState } from 'react';
import { inventoryAPI, reservationAPI } from '../services/api';
import '../styles/QuickActions.css';

function QuickActions() {
  const [increaseData, setIncreaseData] = useState({
    sku: '',
    warehouse_id: 'WH001',
    quantity: '',
    product_name: '',
    location: '',
    reason: ''
  });

  const [reserveData, setReserveData] = useState({
    sku: '',
    warehouse_id: 'WH001',
    order_id: '',
    quantity: ''
  });

  const [adjustData, setAdjustData] = useState({
    sku: '',
    warehouse_id: 'WH001',
    adjustment: '',
    reason: ''
  });

  const [message, setMessage] = useState(null);

  const showMessage = (text, type = 'success') => {
    setMessage({ text, type });
    setTimeout(() => setMessage(null), 5000);
  };

  const handleIncrease = async (e) => {
    e.preventDefault();

    try {
      await inventoryAPI.increase({
        sku: increaseData.sku,
        warehouse_id: increaseData.warehouse_id,
        quantity: parseInt(increaseData.quantity),
        product_name: increaseData.product_name || increaseData.sku,
        location: increaseData.location,
        reason: increaseData.reason || 'Manual inventory increase'
      });

      showMessage(`Successfully increased inventory for ${increaseData.sku}`, 'success');
      setIncreaseData({
        sku: '',
        warehouse_id: 'WH001',
        quantity: '',
        product_name: '',
        location: '',
        reason: ''
      });
    } catch (err) {
      showMessage(err.response?.data?.details || 'Failed to increase inventory', 'error');
    }
  };

  const handleReserve = async (e) => {
    e.preventDefault();

    try {
      await reservationAPI.create({
        sku: reserveData.sku,
        warehouse_id: reserveData.warehouse_id,
        order_id: reserveData.order_id,
        quantity: parseInt(reserveData.quantity)
      });

      showMessage(`Successfully created reservation for order ${reserveData.order_id}`, 'success');
      setReserveData({
        sku: '',
        warehouse_id: 'WH001',
        order_id: '',
        quantity: ''
      });
    } catch (err) {
      showMessage(err.response?.data?.details || 'Failed to create reservation', 'error');
    }
  };

  const handleAdjust = async (e) => {
    e.preventDefault();

    try {
      await inventoryAPI.adjust({
        sku: adjustData.sku,
        warehouse_id: adjustData.warehouse_id,
        adjustment: parseInt(adjustData.adjustment),
        reason: adjustData.reason
      });

      showMessage(`Successfully adjusted inventory for ${adjustData.sku}`, 'success');
      setAdjustData({
        sku: '',
        warehouse_id: 'WH001',
        adjustment: '',
        reason: ''
      });
    } catch (err) {
      showMessage(err.response?.data?.details || 'Failed to adjust inventory', 'error');
    }
  };

  return (
    <div className="quick-actions-container">
      <h2>Quick Actions</h2>

      {message && (
        <div className={`message ${message.type}`}>
          {message.text}
        </div>
      )}

      <div className="actions-grid">
        {/* Increase Inventory Form */}
        <div className="action-card">
          <h3>Increase Inventory</h3>
          <p className="card-description">Add new stock to inventory (INBOUND transaction)</p>
          <form onSubmit={handleIncrease}>
            <div className="form-group">
              <label>SKU *</label>
              <input
                type="text"
                value={increaseData.sku}
                onChange={(e) => setIncreaseData({ ...increaseData, sku: e.target.value })}
                placeholder="e.g., SKU001"
                required
              />
            </div>
            <div className="form-group">
              <label>Warehouse *</label>
              <select
                value={increaseData.warehouse_id}
                onChange={(e) => setIncreaseData({ ...increaseData, warehouse_id: e.target.value })}
                required
              >
                <option value="WH001">WH001</option>
                <option value="WH002">WH002</option>
              </select>
            </div>
            <div className="form-group">
              <label>Quantity *</label>
              <input
                type="number"
                value={increaseData.quantity}
                onChange={(e) => setIncreaseData({ ...increaseData, quantity: e.target.value })}
                placeholder="e.g., 100"
                min="1"
                required
              />
            </div>
            <div className="form-group">
              <label>Product Name</label>
              <input
                type="text"
                value={increaseData.product_name}
                onChange={(e) => setIncreaseData({ ...increaseData, product_name: e.target.value })}
                placeholder="e.g., Laptop Dell XPS"
              />
            </div>
            <div className="form-group">
              <label>Location</label>
              <input
                type="text"
                value={increaseData.location}
                onChange={(e) => setIncreaseData({ ...increaseData, location: e.target.value })}
                placeholder="e.g., A-01-01"
              />
            </div>
            <div className="form-group">
              <label>Reason</label>
              <input
                type="text"
                value={increaseData.reason}
                onChange={(e) => setIncreaseData({ ...increaseData, reason: e.target.value })}
                placeholder="e.g., New stock arrival"
              />
            </div>
            <button type="submit" className="submit-btn">
              Increase Inventory
            </button>
          </form>
        </div>

        {/* Reserve Inventory Form */}
        <div className="action-card">
          <h3>Reserve Inventory</h3>
          <p className="card-description">Reserve inventory for an order (creates ACTIVE reservation)</p>
          <form onSubmit={handleReserve}>
            <div className="form-group">
              <label>SKU *</label>
              <input
                type="text"
                value={reserveData.sku}
                onChange={(e) => setReserveData({ ...reserveData, sku: e.target.value })}
                placeholder="e.g., SKU001"
                required
              />
            </div>
            <div className="form-group">
              <label>Warehouse *</label>
              <select
                value={reserveData.warehouse_id}
                onChange={(e) => setReserveData({ ...reserveData, warehouse_id: e.target.value })}
                required
              >
                <option value="WH001">WH001</option>
                <option value="WH002">WH002</option>
              </select>
            </div>
            <div className="form-group">
              <label>Order ID *</label>
              <input
                type="text"
                value={reserveData.order_id}
                onChange={(e) => setReserveData({ ...reserveData, order_id: e.target.value })}
                placeholder="e.g., ORD-12345"
                required
              />
            </div>
            <div className="form-group">
              <label>Quantity *</label>
              <input
                type="number"
                value={reserveData.quantity}
                onChange={(e) => setReserveData({ ...reserveData, quantity: e.target.value })}
                placeholder="e.g., 5"
                min="1"
                required
              />
            </div>
            <button type="submit" className="submit-btn">
              Create Reservation
            </button>
          </form>
        </div>

        {/* Adjust Inventory Form */}
        <div className="action-card">
          <h3>Adjust Inventory</h3>
          <p className="card-description">Manual inventory adjustment (ADJUSTMENT transaction)</p>
          <form onSubmit={handleAdjust}>
            <div className="form-group">
              <label>SKU *</label>
              <input
                type="text"
                value={adjustData.sku}
                onChange={(e) => setAdjustData({ ...adjustData, sku: e.target.value })}
                placeholder="e.g., SKU001"
                required
              />
            </div>
            <div className="form-group">
              <label>Warehouse *</label>
              <select
                value={adjustData.warehouse_id}
                onChange={(e) => setAdjustData({ ...adjustData, warehouse_id: e.target.value })}
                required
              >
                <option value="WH001">WH001</option>
                <option value="WH002">WH002</option>
              </select>
            </div>
            <div className="form-group">
              <label>Adjustment *</label>
              <input
                type="number"
                value={adjustData.adjustment}
                onChange={(e) => setAdjustData({ ...adjustData, adjustment: e.target.value })}
                placeholder="e.g., 10 or -5"
                required
              />
              <small>Use positive numbers to increase, negative to decrease</small>
            </div>
            <div className="form-group">
              <label>Reason *</label>
              <textarea
                value={adjustData.reason}
                onChange={(e) => setAdjustData({ ...adjustData, reason: e.target.value })}
                placeholder="Explain the reason for adjustment"
                rows="3"
                required
              />
            </div>
            <button type="submit" className="submit-btn">
              Apply Adjustment
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

export default QuickActions;
