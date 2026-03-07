/**
 * 用户 DTO（对应后端 YaUserDto）
 */
export interface YaUserDto {
  id?: number
  username: string
  passwordHash?: string
  email?: string
  avatarUrl?: string
  status?: number
}
