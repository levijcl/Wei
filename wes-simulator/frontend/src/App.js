import React, { useState } from 'react';
import TaskList from './components/TaskList';
import InventoryList from './components/InventoryList';
import './styles/App.css';

function App() {
  const [activeTab, setActiveTab] = useState('tasks');

  return (
    <div className="App">
      <header className="app-header">
        <h1>WES Simulator</h1>
        <p>Warehouse Execution System - Task & Inventory Management</p>
      </header>

      <div className="tab-container">
        <button
          className={`tab ${activeTab === 'tasks' ? 'active' : ''}`}
          onClick={() => setActiveTab('tasks')}
        >
          Task Status
        </button>
        <button
          className={`tab ${activeTab === 'inventory' ? 'active' : ''}`}
          onClick={() => setActiveTab('inventory')}
        >
          Inventory
        </button>
      </div>

      <div className="content-container">
        {activeTab === 'tasks' && <TaskList />}
        {activeTab === 'inventory' && <InventoryList />}
      </div>
    </div>
  );
}

export default App;
