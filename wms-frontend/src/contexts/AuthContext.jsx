import { createContext, useState, useEffect } from 'react';
import api from '../services/api';
import { jwtDecode } from 'jwt-decode';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const token = localStorage.getItem('wms_token');
        if (token) {
            try {
                const decoded = jwtDecode(token);
                // Verifica se expirou
                if (decoded.exp * 1000 < Date.now()) {
                    logout();
                } else {
                    setUser({ login: decoded.sub, ...decoded });
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
            // Endpoint que testamos no Swagger
            const response = await api.post('/auth/login', { login: username, password });

            const { token } = response.data;

            localStorage.setItem('wms_token', token);
            api.defaults.headers.Authorization = `Bearer ${token}`;

            const decoded = jwtDecode(token);
            setUser({ login: decoded.sub });

            return true;
        } catch (error) {
            console.error("Erro login", error);
            return false;
        }
    };

    const logout = () => {
        localStorage.removeItem('wms_token');
        api.defaults.headers.Authorization = null;
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ authenticated: !!user, user, login, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
};