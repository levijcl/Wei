import React, { useState, useEffect } from 'react';
import { transactionAPI } from '../services/api';
import '../styles/TransactionHistory.css';

function TransactionHistory() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [typeFilter, setTypeFilter] = useState('');
  const [limit, setLimit] = useState(50);
  const [offset, setOffset] = useState(0);
  const [total, setTotal] = useState(0);

  const fetchTransactions = async () => {
    try {
      const params = {
        limit,
        offset,
      };
      if (typeFilter) {
        params.transaction_type = typeFilter;
      }

      const response = await transactionAPI.getAll(params);
      setTransactions(response.data.data);
      setTotal(response.data.total);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to fetch transactions');
      console.error('Error fetching transactions:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [typeFilter, limit, offset]);

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  const getTypeBadgeClass = (type) => {
    switch (type) {
      case 'INBOUND':
        return 'type-badge type-inbound';
      case 'OUTBOUND':
        return 'type-badge type-outbound';
      case 'ADJUSTMENT':
        return 'type-badge type-adjustment';
      default:
        return 'type-badge';
    }
  };

  const getQuantityClass = (change) => {
    return change > 0 ? 'quantity-positive' : 'quantity-negative';
  };

  const handleNextPage = () => {
    if (offset + limit < total) {
      setOffset(offset + limit);
    }
  };

  const handlePrevPage = () => {
    if (offset >= limit) {
      setOffset(offset - limit);
    }
  };

  if (loading) {
    return <div className="loading">Loading transactions...</div>;
  }

  const currentPage = Math.floor(offset / limit) + 1;
  const totalPages = Math.ceil(total / limit);

  return (
    <div className="transactions-container">
      <div className="transactions-header">
        <h2>Transaction History</h2>
        <div className="filters">
          <select
            value={typeFilter}
            onChange={(e) => {
              setTypeFilter(e.target.value);
              setOffset(0); // Reset to first page
            }}
            className="type-filter"
          >
            <option value="">All Types</option>
            <option value="INBOUND">Inbound</option>
            <option value="OUTBOUND">Outbound</option>
            <option value="ADJUSTMENT">Adjustment</option>
          </select>
          <select
            value={limit}
            onChange={(e) => {
              setLimit(parseInt(e.target.value));
              setOffset(0); // Reset to first page
            }}
            className="limit-filter"
          >
            <option value="25">25 per page</option>
            <option value="50">50 per page</option>
            <option value="100">100 per page</option>
          </select>
          <button onClick={fetchTransactions} className="refresh-btn">
            Refresh
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="transaction-stats">
        <div className="stat-card">
          <h3>{total}</h3>
          <p>Total Transactions</p>
        </div>
        <div className="stat-card">
          <h3>{transactions.filter((t) => t.TRANSACTION_TYPE === 'INBOUND').length}</h3>
          <p>Inbound (Current Page)</p>
        </div>
        <div className="stat-card">
          <h3>{transactions.filter((t) => t.TRANSACTION_TYPE === 'OUTBOUND').length}</h3>
          <p>Outbound (Current Page)</p>
        </div>
        <div className="stat-card">
          <h3>{transactions.filter((t) => t.TRANSACTION_TYPE === 'ADJUSTMENT').length}</h3>
          <p>Adjustments (Current Page)</p>
        </div>
      </div>

      <table className="transactions-table">
        <thead>
          <tr>
            <th>Timestamp</th>
            <th>Type</th>
            <th>SKU</th>
            <th>Product</th>
            <th>Warehouse</th>
            <th>Change</th>
            <th>Order ID</th>
            <th>Reason</th>
          </tr>
        </thead>
        <tbody>
          {transactions.length === 0 ? (
            <tr>
              <td colSpan="8" className="no-data">
                No transactions found
              </td>
            </tr>
          ) : (
            transactions.map((txn) => (
              <tr key={txn.TRANSACTION_ID}>
                <td className="date-cell">{formatDate(txn.CREATED_AT)}</td>
                <td>
                  <span className={getTypeBadgeClass(txn.TRANSACTION_TYPE)}>
                    {txn.TRANSACTION_TYPE}
                  </span>
                </td>
                <td>{txn.SKU}</td>
                <td>{txn.PRODUCT_NAME}</td>
                <td>
                  <span className="warehouse-badge">{txn.WAREHOUSE_ID}</span>
                </td>
                <td className={getQuantityClass(txn.QUANTITY_CHANGE)}>
                  {txn.QUANTITY_CHANGE > 0 ? '+' : ''}
                  {txn.QUANTITY_CHANGE}
                </td>
                <td className="order-id-cell">{txn.ORDER_ID || '-'}</td>
                <td className="reason-cell">{txn.REASON || '-'}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>

      {/* Pagination */}
      <div className="pagination">
        <button
          onClick={handlePrevPage}
          disabled={offset === 0}
          className="pagination-btn"
        >
          Previous
        </button>
        <span className="pagination-info">
          Page {currentPage} of {totalPages} ({total} total records)
        </span>
        <button
          onClick={handleNextPage}
          disabled={offset + limit >= total}
          className="pagination-btn"
        >
          Next
        </button>
      </div>
    </div>
  );
}

export default TransactionHistory;
