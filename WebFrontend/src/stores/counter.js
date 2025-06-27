import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import axios from 'axios'

export const useCounterStore = defineStore('counter', () => {

  const token = ref('')

  const BaseUrl = 'http://localhost:8080/'

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const login = async (username, password) => {
    try {
      const response = await axios.post(BaseUrl + 'users/login', { username, password })
      setToken(response.data.token)
      return true
    } catch (error) {
      console.error('登录失败:', error)
      return false
    }
    
  }

  return { login }
})
