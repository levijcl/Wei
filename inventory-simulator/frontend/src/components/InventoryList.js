import React, { useState, useEffect } from 'react';
import { inventoryAPI } from '../services/api';
import '../styles/InventoryList.css';

function InventoryList() {
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [warehouseFilter, setWarehouseFilter] = useState('');
  const [showAdjustModal, setShowAdjustModal] = useState(false);
  const [selectedItem, setSelectedItem] = useState(null);
  const [adjustmentData, setAdjustmentData] = useState({
    adjustment: '',
    reason: ''
  });

  const fetchInventory = async () => {
    try {
      const response = await inventoryAPI.getAll(warehouseFilter || null);
      setInventory(response.data.data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to fetch inventory');
      console.error('Error fetching inventory:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInventory();
    const interval = setInterval(fetchInventory, 5000); // Auto-refresh every 5 seconds
    return () => clearInterval(interval);
  }, [warehouseFilter]);

  const handleAdjustClick = (item) => {
    setSelectedItem(item);
    setAdjustmentData({ adjustment: '', reason: '' });
    setShowAdjustModal(true);
  };

  const handleAdjustSubmit = async (e) => {
    e.preventDefault();

    if (!adjustmentData.adjustment || !adjustmentData.reason) {
      alert('Please provide both adjustment value and reason');
      return;
    }

    try {
      await inventoryAPI.adjust({
        sku: selectedItem.SKU,
        warehouse_id: selectedItem.WAREHOUSE_ID,
        adjustment: parseInt(adjustmentData.adjustment),
        reason: adjustmentData.reason
      });

      setShowAdjustModal(false);
      setSelectedItem(null);
      setAdjustmentData({ adjustment: '', reason: '' });
      fetchInventory();
      alert('Inventory adjusted successfully');
    } catch (err) {
      alert(err.response?.data?.details || 'Failed to adjust inventory');
    }
  };

  const getAvailabilityClass = (available) => {
    if (available < 10) return 'low-stock';
    if (available < 50) return 'medium-stock';
    return 'good-stock';
  };

  if (loading) {
    return <div className="loading">Loading inventory...</div>;
  }

  return (
    <div className="inventory-container">
      <div className="inventory-header">
        <h2>Inventory Dashboard</h2>
        <div className="filters">
          <select
            value={warehouseFilter}
            onChange={(e) => setWarehouseFilter(e.target.value)}
            className="warehouse-filter"
          >
            <option value="">All Warehouses</option>
            <option value="WH001">WH001</option>
            <option value="WH002">WH002</option>
          </select>
          <button onClick={fetchInventory} className="refresh-btn">
            Refresh
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="inventory-stats">
        <div className="stat-card">
          <h3>{inventory.length}</h3>
          <p>Total SKUs</p>
        </div>
        <div className="stat-card">
          <h3>{inventory.reduce((sum, item) => sum + item.TOTAL_QUANTITY, 0)}</h3>
          <p>Total Units</p>
        </div>
        <div className="stat-card">
          <h3>{inventory.reduce((sum, item) => sum + item.RESERVED_QUANTITY, 0)}</h3>
          <p>Reserved Units</p>
        </div>
        <div className="stat-card">
          <h3>{inventory.reduce((sum, item) => sum + item.AVAILABLE_QUANTITY, 0)}</h3>
          <p>Available Units</p>
        </div>
      </div>

      <table className="inventory-table">
        <thead>
          <tr>
            <th>SKU</th>
            <th>Product</th>
            <th>Warehouse</th>
            <th>Total</th>
            <th>Reserved</th>
            <th>Available</th>
            <th>Location</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {inventory.map((item) => (
            <tr key={`${item.SKU}-${item.WAREHOUSE_ID}`}>
              <td className="sku-cell">{item.SKU}</td>
              <td>{item.PRODUCT_NAME}</td>
              <td><span className="warehouse-badge">{item.WAREHOUSE_ID}</span></td>
              <td>{item.TOTAL_QUANTITY}</td>
              <td>{item.RESERVED_QUANTITY}</td>
              <td className={getAvailabilityClass(item.AVAILABLE_QUANTITY)}>
                {item.AVAILABLE_QUANTITY}
              </td>
              <td>{item.LOCATION}</td>
              <td>
                <button
                  onClick={() => handleAdjustClick(item)}
                  className="adjust-btn"
                >
                  Adjust
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Adjustment Modal */}
      {showAdjustModal && selectedItem && (
        <div className="modal-overlay" onClick={() => setShowAdjustModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h3>Adjust Inventory</h3>
            <p>
              <strong>SKU:</strong> {selectedItem.SKU} ({selectedItem.PRODUCT_NAME})
              <br />
              <strong>Warehouse:</strong> {selectedItem.WAREHOUSE_ID}
              <br />
              <strong>Current Total:</strong> {selectedItem.TOTAL_QUANTITY}
            </p>
            <form onSubmit={handleAdjustSubmit}>
              <div className="form-group">
                <label>Adjustment (+ to increase, - to decrease):</label>
                <input
                  type="number"
                  value={adjustmentData.adjustment}
                  onChange={(e) =>
                    setAdjustmentData({ ...adjustmentData, adjustment: e.target.value })
                  }
                  placeholder="e.g., 10 or -5"
                  required
                />
              </div>
              <div className="form-group">
                <label>Reason:</label>
                <textarea
                  value={adjustmentData.reason}
                  onChange={(e) =>
                    setAdjustmentData({ ...adjustmentData, reason: e.target.value })
                  }
                  placeholder="Explain the reason for adjustment"
                  rows="3"
                  required
                />
              </div>
              <div className="modal-actions">
                <button type="submit" className="submit-btn">
                  Apply Adjustment
                </button>
                <button
                  type="button"
                  onClick={() => setShowAdjustModal(false)}
                  className="cancel-btn"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default InventoryList;
