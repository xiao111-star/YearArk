import request from '@/utils/request'

/** 用户注册（passwordHash 为前端 SHA-256 后的哈希值） */
export function register(data: { username: string; passwordHash: string; email: string }) {
  return request.post('/api/user/auth/register', data)
}

/** 用户登录（passwordHash 为前端 SHA-256 后的哈希值） */
export function login(data: { username: string; passwordHash: string }) {
  return request.post('/api/user/auth/login', data)
}

/** 用户退出登录 */
export function logout() {
  return request.post('/api/user/auth/logout')
}

/** 获取当前登录用户信息 */
export function getUserInfo() {
  return request.get('/api/user/auth/info')
}
