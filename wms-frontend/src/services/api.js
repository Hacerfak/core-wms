import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080', // Endereço do nosso Backend Docker
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