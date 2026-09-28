import api from './api'

const authService = {
  async login(username, password) {
    const { data } = await api.post('/auth/login', { username, password })
    return data
  },

  async logout() {
    try {
      await api.post('/auth/logout')
    } catch (e) {
      // continuar con el cierre local aunque falle el servidor
    }
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('user')
  },

  async getMe() {
    const { data } = await api.get('/auth/me')
    return data
  },

  async cambiarPassword(password_actual, password_nueva) {
    const { data } = await api.post('/auth/cambiar-password', { password_actual, password_nueva })
    return data
  },

  getAccessToken() {
    return localStorage.getItem('access_token')
  },
  getStoredUser() {
    const u = localStorage.getItem('user')
    return u ? JSON.parse(u) : null
  },
}

export default authService
