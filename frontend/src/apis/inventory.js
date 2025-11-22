import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

export async function getTransactionDetail(transactionId) {
  const response = await apiClient.get(
    `/api/inventory/transactions/${transactionId}`
  )
  return response.data
}

export async function reserveInventory(reserveData) {
  const { orderId, sku, quantity } = reserveData

  const response = await apiClient.post('/api/inventory/reserve', {
    orderId,
    sku,
    quantity,
  })

  return response.data
}

export { apiClient }
