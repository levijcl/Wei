import React, { useState, useEffect } from 'react';
import OrderForm from './components/OrderForm';
import OrderList from './components/OrderList';
import './styles/App.css';

function App() {
  const [activeTab, setActiveTab] = useState('create');
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleOrderCreated = () => {
    setRefreshTrigger(prev => prev + 1);
    setActiveTab('list');
  };

  return (
    <div className="App">
      <header className="app-header">
        <h1>Order Source Simulator</h1>
        <p>Wei Orchestrator System - Order Generation Tool</p>
      </header>

      <div className="tab-container">
        <button
          className={`tab ${activeTab === 'create' ? 'active' : ''}`}
          onClick={() => setActiveTab('create')}
        >
          Create Order
        </button>
        <button
          className={`tab ${activeTab === 'list' ? 'active' : ''}`}
          onClick={() => setActiveTab('list')}
        >
          Order List
        </button>
      </div>

      <div className="content-container">
        {activeTab === 'create' && (
          <OrderForm onOrderCreated={handleOrderCreated} />
        )}
        {activeTab === 'list' && (
          <OrderList refreshTrigger={refreshTrigger} />
        )}
      </div>
    </div>
  );
}

export default App;
