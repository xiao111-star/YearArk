import request from '@/utils/request'
import type { YaUserDto } from '@/types/user'

/** 用户注册 */
export function register(data: YaUserDto) {
  return request.post('/api/user/auth/register', data)
}

/** 用户登录 */
export function login(data: YaUserDto) {
  return request.post('/api/user/auth/login', data)
}

/** 用户退出登录 */
export function logout() {
  return request.post('/api/user/auth/logout')
}
