import { createContext, useState, useEffect } from 'react';
import api from '../services/api';
import { jwtDecode } from 'jwt-decode';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const recoveredUser = localStorage.getItem('wms_user');
        const token = localStorage.getItem('wms_token');

        if (token && recoveredUser) {
            try {
                const decoded = jwtDecode(token);
                if (decoded.exp * 1000 < Date.now()) {
                    logout();
                } else {
                    // Recupera os dados completos (incluindo lista de empresas)
                    setUser(JSON.parse(recoveredUser));
                    api.defaults.headers.Authorization = `Bearer ${token}`;
                }
            } catch (error) {
                logout();
            }
        }
        setLoading(false);
    }, []);

    const login = async (username, password) => {
        try {
            const response = await api.post('/auth/login', { login: username, password });

            // Captura a ROLE vinda do backend
            const { token, empresas, usuario, role } = response.data;

            localStorage.setItem('wms_token', token);
            api.defaults.headers.Authorization = `Bearer ${token}`;

            // Salva a role no objeto do usuário
            const userData = { login: usuario, role, empresas };
            localStorage.setItem('wms_user', JSON.stringify(userData));

            setUser(userData);
            return userData;
        } catch (error) {
            console.error("Erro login", error);
            throw error;
        }
    };

    const selecionarEmpresa = async (tenantId) => {
        try {
            // Troca o token Global pelo Token do Tenant
            const response = await api.post('/auth/selecionar-empresa', { tenantId });
            const newToken = response.data.token;

            // Atualiza tudo
            localStorage.setItem('wms_token', newToken);
            api.defaults.headers.Authorization = `Bearer ${newToken}`;

            return true;
        } catch (error) {
            console.error("Erro ao selecionar empresa", error);
            return false;
        }
    };

    const logout = () => {
        localStorage.removeItem('wms_token');
        localStorage.removeItem('wms_user');
        api.defaults.headers.Authorization = null;
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ authenticated: !!user, user, login, logout, loading, selecionarEmpresa }}>
            {children}
        </AuthContext.Provider>
    );
};