import React, { useState, useEffect } from 'react';
import { reservationAPI } from '../services/api';
import '../styles/ReservationList.css';

function ReservationList() {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('');

  const fetchReservations = async () => {
    try {
      const response = await reservationAPI.getAll(statusFilter || null);
      setReservations(response.data.data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to fetch reservations');
      console.error('Error fetching reservations:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReservations();
    const interval = setInterval(fetchReservations, 5000); // Auto-refresh every 5 seconds
    return () => clearInterval(interval);
  }, [statusFilter]);

  const handleConsume = async (reservationId) => {
    if (!window.confirm('Are you sure you want to consume this reservation?')) {
      return;
    }

    try {
      await reservationAPI.consume(reservationId);
      fetchReservations();
      alert('Reservation consumed successfully');
    } catch (err) {
      alert(err.response?.data?.details || 'Failed to consume reservation');
    }
  };

  const handleRelease = async (reservationId) => {
    if (!window.confirm('Are you sure you want to release this reservation?')) {
      return;
    }

    try {
      await reservationAPI.release(reservationId);
      fetchReservations();
      alert('Reservation released successfully');
    } catch (err) {
      alert(err.response?.data?.details || 'Failed to release reservation');
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString();
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'ACTIVE':
        return 'status-badge status-active';
      case 'CONSUMED':
        return 'status-badge status-consumed';
      case 'RELEASED':
        return 'status-badge status-released';
      default:
        return 'status-badge';
    }
  };

  if (loading) {
    return <div className="loading">Loading reservations...</div>;
  }

  const activeCount = reservations.filter((r) => r.STATUS === 'ACTIVE').length;
  const consumedCount = reservations.filter((r) => r.STATUS === 'CONSUMED').length;
  const releasedCount = reservations.filter((r) => r.STATUS === 'RELEASED').length;

  return (
    <div className="reservations-container">
      <div className="reservations-header">
        <h2>Reservation Manager</h2>
        <div className="filters">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="status-filter"
          >
            <option value="">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="CONSUMED">Consumed</option>
            <option value="RELEASED">Released</option>
          </select>
          <button onClick={fetchReservations} className="refresh-btn">
            Refresh
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="reservation-stats">
        <div className="stat-card">
          <h3>{activeCount}</h3>
          <p>Active</p>
        </div>
        <div className="stat-card">
          <h3>{consumedCount}</h3>
          <p>Consumed</p>
        </div>
        <div className="stat-card">
          <h3>{releasedCount}</h3>
          <p>Released</p>
        </div>
        <div className="stat-card">
          <h3>{reservations.length}</h3>
          <p>Total</p>
        </div>
      </div>

      <table className="reservations-table">
        <thead>
          <tr>
            <th>Reservation ID</th>
            <th>Order ID</th>
            <th>SKU</th>
            <th>Product</th>
            <th>Warehouse</th>
            <th>Quantity</th>
            <th>Status</th>
            <th>Created</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {reservations.length === 0 ? (
            <tr>
              <td colSpan="9" className="no-data">
                No reservations found
              </td>
            </tr>
          ) : (
            reservations.map((reservation) => (
              <tr key={reservation.RESERVATION_ID}>
                <td className="reservation-id-cell">
                  {reservation.RESERVATION_ID.substring(0, 8)}...
                </td>
                <td className="order-id-cell">{reservation.ORDER_ID}</td>
                <td>{reservation.SKU}</td>
                <td>{reservation.PRODUCT_NAME}</td>
                <td>
                  <span className="warehouse-badge">{reservation.WAREHOUSE_ID}</span>
                </td>
                <td>{reservation.QUANTITY}</td>
                <td>
                  <span className={getStatusBadgeClass(reservation.STATUS)}>
                    {reservation.STATUS}
                  </span>
                </td>
                <td className="date-cell">{formatDate(reservation.CREATED_AT)}</td>
                <td>
                  {reservation.STATUS === 'ACTIVE' && (
                    <div className="action-buttons">
                      <button
                        onClick={() => handleConsume(reservation.RESERVATION_ID)}
                        className="consume-btn"
                        title="Consume reservation"
                      >
                        Consume
                      </button>
                      <button
                        onClick={() => handleRelease(reservation.RESERVATION_ID)}
                        className="release-btn"
                        title="Release reservation"
                      >
                        Release
                      </button>
                    </div>
                  )}
                  {reservation.STATUS !== 'ACTIVE' && (
                    <span className="no-actions">-</span>
                  )}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

export default ReservationList;
