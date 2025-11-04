import React, { useState } from 'react';
import InventoryList from './components/InventoryList';
import ReservationList from './components/ReservationList';
import TransactionHistory from './components/TransactionHistory';
import QuickActions from './components/QuickActions';
import './styles/App.css';

function App() {
  const [activeTab, setActiveTab] = useState('inventory');

  const renderTabContent = () => {
    switch (activeTab) {
      case 'inventory':
        return <InventoryList />;
      case 'reservations':
        return <ReservationList />;
      case 'transactions':
        return <TransactionHistory />;
      case 'actions':
        return <QuickActions />;
      default:
        return <InventoryList />;
    }
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>Inventory Simulator</h1>
        <p className="subtitle">External Inventory Service for Wei Orchestrator</p>
      </header>

      <nav className="tab-navigation">
        <button
          className={`tab-button ${activeTab === 'inventory' ? 'active' : ''}`}
          onClick={() => setActiveTab('inventory')}
        >
          Inventory
        </button>
        <button
          className={`tab-button ${activeTab === 'reservations' ? 'active' : ''}`}
          onClick={() => setActiveTab('reservations')}
        >
          Reservations
        </button>
        <button
          className={`tab-button ${activeTab === 'transactions' ? 'active' : ''}`}
          onClick={() => setActiveTab('transactions')}
        >
          Transactions
        </button>
        <button
          className={`tab-button ${activeTab === 'actions' ? 'active' : ''}`}
          onClick={() => setActiveTab('actions')}
        >
          Quick Actions
        </button>
      </nav>

      <main className="app-content">
        {renderTabContent()}
      </main>

      <footer className="app-footer">
        <p>Inventory Simulator v1.0.0 | Backend: http://localhost:3778 | Frontend: http://localhost:3779</p>
      </footer>
    </div>
  );
}

export default App;
