import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'ya-auth-token'
const USER_ID_KEY = 'ya-user-id'
const USERNAME_KEY = 'ya-username'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userId = ref<number>(0)
  const username = ref<string>('')

  const isLoggedIn = computed(() => !!token.value)

  function setLoginInfo(newToken: string, newUserId: number, newUsername: string) {
    token.value = newToken
    userId.value = newUserId
    username.value = newUsername
    localStorage.setItem(TOKEN_KEY, newToken)
    localStorage.setItem(USER_ID_KEY, String(newUserId))
    localStorage.setItem(USERNAME_KEY, newUsername)
  }

  function logout() {
    token.value = ''
    userId.value = 0
    username.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_ID_KEY)
    localStorage.removeItem(USERNAME_KEY)
  }

  function loadFromStorage() {
    const storedToken = localStorage.getItem(TOKEN_KEY)
    const storedUserId = localStorage.getItem(USER_ID_KEY)
    const storedUsername = localStorage.getItem(USERNAME_KEY)
    if (storedToken) {
      token.value = storedToken
      userId.value = storedUserId ? Number(storedUserId) : 0
      username.value = storedUsername ?? ''
    }
  }

  return { token, userId, username, isLoggedIn, setLoginInfo, logout, loadFromStorage }
})
