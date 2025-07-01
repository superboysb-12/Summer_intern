import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import axios from 'axios'

export const useCounterStore = defineStore('counter', () => {
  // 从本地存储恢复状态
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  const BaseUrl = 'http://localhost:8080/'

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (newUserInfo) => {
    userInfo.value = newUserInfo
    localStorage.setItem('userInfo', JSON.stringify(newUserInfo))
  }

  const login = async (username, password) => {
    try {
      const response = await axios.post(BaseUrl + 'users/login', { username, password })
      setToken(response.data.token)
      setUserInfo(response.data.user)
      return true
    } catch (error) {
      console.error('登录失败:', error)
      return false
    }
  }

  const register = async (username, password, email, phone) => {
    try {
      const response = await axios.post(BaseUrl + 'users/register', { 
        username, 
        password,
        email: email || undefined,
        phone: phone || undefined,
        userRole: 'ADMIN',
      })
      return true
    } catch (error) {
      console.error('注册失败:', error)
      return false
    }
  }

  const getUserInfo = () => {
    return userInfo.value
  }

  const logout = () => {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { login, register, getUserInfo, logout }
})
