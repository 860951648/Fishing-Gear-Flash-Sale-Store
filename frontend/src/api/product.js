import request from '@/utils/request'

/** 商品模块：对应后端 ProductController（§3.3 第 5、6 个接口） */

// GET /api/products?page&size&keyword
export function getProducts(params) {
  return request.get('/products', { params })
}

// GET /api/products/{id} 商品详情（含 SKU）
export function getProductDetail(id) {
  return request.get('/products/' + id)
}
