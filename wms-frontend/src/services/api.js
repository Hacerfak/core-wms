import axios from 'axios';
import { toast } from 'react-toastify';

// Utiliza variável de ambiente do Vite (VITE_API_URL) ou fallback para localhost
const baseURL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
    baseURL: baseURL,
    headers: {
        'Content-Type': 'application/json'
    }
});

// Interceptor de Requisição (Anexa o Token e Tenant Context)
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('wms_token');
    // Recupera o tenant selecionado para garantir consistência em ambientes multi-abas
    const tenant = localStorage.getItem('@App:tenant');

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    // Header auxiliar de contexto (opcional, mas útil para debug no backend)
    if (tenant) {
        config.headers['X-Tenant-ID'] = tenant;
    }

    return config;
});

// Interceptor de Resposta (Tratamento de Erros Global)
api.interceptors.response.use(
    response => response,
    error => {
        // Erro de Rede/Conexão
        if (!error.response) {
            toast.error("Sem conexão com o servidor. Verifique sua internet.");
            return Promise.reject(error);
        }

        const { status, data } = error.response;

        // 1. Sessão Expirada ou Sem Permissão (401/403)
        if (status === 401 || status === 403) {
            // Ignora se for na tela de login (para deixar o form mostrar "senha incorreta")
            if (!error.config.url.includes('/login') && !error.config.url.includes('/auth')) {
                toast.warning("Sessão expirada ou acesso negado.");
            }
        }

        // 2. Erro de Validação de Campos (422) - Padrão ValidationError do Java
        else if (status === 422 && data.errors) {
            // Retorna o objeto modificado para que o componente React possa ler 'validationErrors'
            return Promise.reject({
                ...error,
                validationErrors: data.errors // Array [{field, message}]
            });
        }

        // 3. Regra de Negócio Genérica (400, 404, 409, 500)
        else if (data && data.message) {
            toast.error(data.message);
        } else {
            toast.error(`Erro inesperado (${status}).`);
        }

        return Promise.reject(error);
    }
);

export default api;