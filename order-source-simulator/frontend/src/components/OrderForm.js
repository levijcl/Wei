import React, { useState } from 'react';
import { orderAPI } from '../services/api';
import '../styles/OrderForm.css';

const OrderForm = ({ onOrderCreated }) => {
  const [formData, setFormData] = useState({
    customer_name: '',
    customer_email: '',
    shipping_address: '',
    order_type: 'TYPE_A',
    warehouse_id: 'WH001',
  });

  const [items, setItems] = useState([
    { sku: '', product_name: '', quantity: 1, price: 0 }
  ]);

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleItemChange = (index, field, value) => {
    const newItems = [...items];
    newItems[index][field] = value;
    setItems(newItems);
  };

  const addItem = () => {
    setItems([...items, { sku: '', product_name: '', quantity: 1, price: 0 }]);
  };

  const removeItem = (index) => {
    if (items.length > 1) {
      setItems(items.filter((_, i) => i !== index));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage({ type: '', text: '' });

    try {
      const orderData = {
        ...formData,
        items: items.map(item => ({
          ...item,
          quantity: parseInt(item.quantity),
          price: parseFloat(item.price)
        }))
      };

      const response = await orderAPI.createOrder(orderData);
      setMessage({ type: 'success', text: `Order created successfully! Order ID: ${response.order_id}` });

      setFormData({
        customer_name: '',
        customer_email: '',
        shipping_address: '',
        order_type: 'TYPE_A',
        warehouse_id: 'WH001',
      });
      setItems([{ sku: '', product_name: '', quantity: 1, price: 0 }]);

      if (onOrderCreated) {
        setTimeout(() => onOrderCreated(), 1500);
      }
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.response?.data?.error || 'Failed to create order'
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="order-form-container">
      <h2>Create New Order</h2>

      {message.text && (
        <div className={`message ${message.type}`}>
          {message.text}
        </div>
      )}

      <form onSubmit={handleSubmit} className="order-form">
        <div className="form-section">
          <h3>Customer Information</h3>

          <div className="form-group">
            <label>Customer Name *</label>
            <input
              type="text"
              name="customer_name"
              value={formData.customer_name}
              onChange={handleInputChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Customer Email</label>
            <input
              type="email"
              name="customer_email"
              value={formData.customer_email}
              onChange={handleInputChange}
            />
          </div>

          <div className="form-group">
            <label>Shipping Address</label>
            <textarea
              name="shipping_address"
              value={formData.shipping_address}
              onChange={handleInputChange}
              rows="3"
            />
          </div>
        </div>

        <div className="form-section">
          <h3>Order Details</h3>

          <div className="form-row">
            <div className="form-group">
              <label>Order Type</label>
              <select
                name="order_type"
                value={formData.order_type}
                onChange={handleInputChange}
              >
                <option value="TYPE_A">Type A (Picking Zone)</option>
                <option value="TYPE_B">Type B (Packing List)</option>
              </select>
            </div>

            <div className="form-group">
              <label>Warehouse ID</label>
              <select
                name="warehouse_id"
                value={formData.warehouse_id}
                onChange={handleInputChange}
              >
                <option value="WH001">WH001</option>
                <option value="WH002">WH002</option>
                <option value="WH003">WH003</option>
              </select>
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Order Items</h3>

          {items.map((item, index) => (
            <div key={index} className="item-row">
              <div className="form-group">
                <label>SKU *</label>
                <input
                  type="text"
                  value={item.sku}
                  onChange={(e) => handleItemChange(index, 'sku', e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label>Product Name *</label>
                <input
                  type="text"
                  value={item.product_name}
                  onChange={(e) => handleItemChange(index, 'product_name', e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label>Quantity *</label>
                <input
                  type="number"
                  value={item.quantity}
                  onChange={(e) => handleItemChange(index, 'quantity', e.target.value)}
                  min="1"
                  required
                />
              </div>

              <div className="form-group">
                <label>Price</label>
                <input
                  type="number"
                  value={item.price}
                  onChange={(e) => handleItemChange(index, 'price', e.target.value)}
                  min="0"
                  step="0.01"
                />
              </div>

              <button
                type="button"
                onClick={() => removeItem(index)}
                className="btn-remove"
                disabled={items.length === 1}
              >
                Remove
              </button>
            </div>
          ))}

          <button type="button" onClick={addItem} className="btn-add-item">
            + Add Item
          </button>
        </div>

        <button type="submit" className="btn-submit" disabled={loading}>
          {loading ? 'Creating...' : 'Create Order'}
        </button>
      </form>
    </div>
  );
};

export default OrderForm;
