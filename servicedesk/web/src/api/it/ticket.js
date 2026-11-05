import request from '@/utils/request'

export function listTicket(query) {
  return request({
    url: '/it/ticket/list',
    method: 'get',
    params: query
  })
}

export function getTicket(ticketId) {
  return request({
    url: '/it/ticket/' + ticketId,
    method: 'get'
  })
}

export function addTicket(data) {
  return request({
    url: '/it/ticket',
    method: 'post',
    data: data
  })
}

export function assignTicket(ticketId, data) {
  return request({
    url: '/it/ticket/' + ticketId + '/assign',
    method: 'put',
    data: data
  })
}

export function processTicket(ticketId, data) {
  return request({
    url: '/it/ticket/' + ticketId + '/process',
    method: 'put',
    data: data
  })
}

export function rejectTicket(ticketId, data) {
  return request({
    url: '/it/ticket/' + ticketId + '/reject',
    method: 'put',
    data: data
  })
}

export function closeTicket(ticketId, data) {
  return request({
    url: '/it/ticket/' + ticketId + '/close',
    method: 'put',
    data: data
  })
}

export function delTicket(ticketId) {
  return request({
    url: '/it/ticket/' + ticketId,
    method: 'delete'
  })
}

export function scanOverdue() {
  return request({
    url: '/it/ticket/scanOverdue',
    method: 'post'
  })
}
