import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

export async function getOrders(params = {}) {
  const {
    statuses = [],
    page = 0,
    size = 40,
    sort = 'scheduled_pickup_time',
  } = params

  const queryParams = new URLSearchParams()

  statuses.forEach((status) => {
    queryParams.append('status', status)
  })

  queryParams.append('page', page)
  queryParams.append('size', size)
  queryParams.append('sort', sort)

  const response = await apiClient.get(`/api/orders?${queryParams}`)
  return response.data
}

export async function getOrderDetail(orderId) {
  const response = await apiClient.get(`/api/orders/${orderId}`)
  return response.data
}

export async function getOrderDetailStatus(orderId, step) {
  const response = await apiClient.get(`/api/orders/${orderId}/process-status`)
  return response.data
}

export async function getOrderDetailStatusStep(orderId, step) {
  const response = await apiClient.get(
    `/api/orders/${orderId}/process-status/steps/${step}`
  )
  return response.data
}

export async function initiateFulfillment(orderId) {
  const resopnse = await apiClient.post(`/api/orders/${orderId}/fulfillment`)
  return resopnse.data
}

export { apiClient }
