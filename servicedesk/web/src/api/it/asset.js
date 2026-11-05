import request from '@/utils/request'

export function listAsset(query) {
  return request({
    url: '/it/asset/list',
    method: 'get',
    params: query
  })
}

export function getAsset(assetId) {
  return request({
    url: '/it/asset/' + assetId,
    method: 'get'
  })
}

export function addAsset(data) {
  return request({
    url: '/it/asset',
    method: 'post',
    data: data
  })
}

export function updateAsset(data) {
  return request({
    url: '/it/asset',
    method: 'put',
    data: data
  })
}

export function delAsset(assetId) {
  return request({
    url: '/it/asset/' + assetId,
    method: 'delete'
  })
}
