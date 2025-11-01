import React, { useState, useEffect } from 'react';
import { getInventory, adjustInventory } from '../services/api';
import '../styles/InventoryList.css';

function InventoryList() {
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showAdjustForm, setShowAdjustForm] = useState(false);

  // Adjustment form state
  const [adjustSku, setAdjustSku] = useState('');
  const [adjustQuantity, setAdjustQuantity] = useState(0);
  const [adjustReason, setAdjustReason] = useState('');

  useEffect(() => {
    fetchInventory();
    // Auto-refresh every 10 seconds
    const interval = setInterval(fetchInventory, 10000);
    return () => clearInterval(interval);
  }, []);

  const fetchInventory = async () => {
    try {
      setLoading(true);
      const response = await getInventory();
      setInventory(response.data.inventory);
      setError('');
    } catch (err) {
      setError('Failed to fetch inventory');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleAdjustInventory = async (e) => {
    e.preventDefault();
    try {
      await adjustInventory({
        sku: adjustSku,
        quantity_change: parseInt(adjustQuantity),
        reason: adjustReason || 'Manual adjustment'
      });

      setShowAdjustForm(false);
      setAdjustSku('');
      setAdjustQuantity(0);
      setAdjustReason('');
      fetchInventory();
    } catch (err) {
      setError('Failed to adjust inventory');
      console.error(err);
    }
  };

  const formatTimestamp = (timestamp) => {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleString();
  };

  const getTotalQuantity = () => {
    return inventory.reduce((sum, item) => sum + (item.QUANTITY || 0), 0);
  };

  return (
    <div className="inventory-list-container">
      <div className="inventory-list-header">
        <h2>Inventory</h2>
        <button className="btn-primary" onClick={() => setShowAdjustForm(!showAdjustForm)}>
          {showAdjustForm ? 'Cancel' : 'Adjust Inventory'}
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {showAdjustForm && (
        <div className="adjust-inventory-form">
          <h3>Adjust Inventory</h3>
          <form onSubmit={handleAdjustInventory}>
            <div className="form-group">
              <label>SKU:</label>
              <input
                type="text"
                value={adjustSku}
                onChange={(e) => setAdjustSku(e.target.value)}
                placeholder="SKU001"
                required
              />
            </div>

            <div className="form-group">
              <label>Quantity Change (+ or -):</label>
              <input
                type="number"
                value={adjustQuantity}
                onChange={(e) => setAdjustQuantity(e.target.value)}
                placeholder="e.g., +10 or -5"
                required
              />
            </div>

            <div className="form-group">
              <label>Reason:</label>
              <input
                type="text"
                value={adjustReason}
                onChange={(e) => setAdjustReason(e.target.value)}
                placeholder="Manual count adjustment"
              />
            </div>

            <button type="submit" className="btn-primary">Adjust Inventory</button>
          </form>
        </div>
      )}

      <div className="inventory-stats">
        <div className="stat">
          <span className="stat-label">Total SKUs:</span>
          <span className="stat-value">{inventory.length}</span>
        </div>
        <div className="stat">
          <span className="stat-label">Total Quantity:</span>
          <span className="stat-value">{getTotalQuantity()}</span>
        </div>
      </div>

      {loading && <div className="loading">Loading inventory...</div>}

      <div className="inventory-table">
        <table>
          <thead>
            <tr>
              <th>SKU</th>
              <th>Product Name</th>
              <th>Warehouse</th>
              <th>Quantity</th>
              <th>Location</th>
              <th>Last Updated</th>
            </tr>
          </thead>
          <tbody>
            {inventory.length === 0 ? (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center' }}>
                  No inventory found.
                </td>
              </tr>
            ) : (
              inventory.map((item, index) => (
                <tr key={index}>
                  <td><strong>{item.SKU}</strong></td>
                  <td>{item.PRODUCT_NAME || 'N/A'}</td>
                  <td>{item.WAREHOUSE_ID}</td>
                  <td>
                    <span className={item.QUANTITY < 50 ? 'low-stock' : 'normal-stock'}>
                      {item.QUANTITY}
                    </span>
                  </td>
                  <td>{item.LOCATION || 'N/A'}</td>
                  <td>{formatTimestamp(item.UPDATED_AT)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default InventoryList;
