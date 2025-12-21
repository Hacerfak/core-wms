import axios from 'axios';

// Utiliza variável de ambiente do Vite (VITE_API_URL) ou fallback para localhost
const baseURL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
    baseURL: baseURL,
});

// Interceptor: Antes de cada requisição, insere o Token
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('wms_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default api;