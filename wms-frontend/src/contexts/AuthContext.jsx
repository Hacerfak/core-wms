import { createContext, useState, useEffect, useCallback } from 'react';
import api from '../services/api';
import { jwtDecode } from 'jwt-decode';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [permissions, setPermissions] = useState([]); // Nova lista de permissões
    const [loading, setLoading] = useState(true);


    // Função auxiliar para processar o token e extrair permissões
    const processToken = useCallback((token) => {
        if (!token) return;
        try {
            const decoded = jwtDecode(token);
            // O backend manda as permissões na claim "roles"
            const userPermissions = decoded.roles || [];
            setPermissions(userPermissions);
            return decoded; // Retorna para uso imediato se precisar
        } catch (error) {
            console.error("Erro ao decodificar token", error);
            return null;
        }
    }, []);

    useEffect(() => {
        const recoveredUser = localStorage.getItem('wms_user');
        const token = localStorage.getItem('wms_token');

        if (token && recoveredUser) {
            try {
                const decoded = processToken(token); // Processa permissões ao carregar
                if (decoded && decoded.exp * 1000 < Date.now()) {
                    logout();
                } else {
                    setUser(JSON.parse(recoveredUser));
                    api.defaults.headers.Authorization = `Bearer ${token}`;
                }
            } catch (error) {
                logout();
            }
        }
        setLoading(false);
    }, [processToken]);

    const login = async (username, password) => {
        try {
            const response = await api.post('/auth/login', { login: username, password });

            const { token, empresas, usuario, role } = response.data;

            localStorage.setItem('wms_token', token);
            api.defaults.headers.Authorization = `Bearer ${token}`;

            // Processa as permissões do token inicial
            processToken(token);

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
            const response = await api.post('/auth/selecionar-empresa', { tenantId });
            const newToken = response.data.token;

            localStorage.setItem('wms_token', newToken);
            api.defaults.headers.Authorization = `Bearer ${newToken}`;

            // ATUALIZA AS PERMISSÕES COM O NOVO TOKEN DO TENANT
            processToken(newToken);

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
        setPermissions([]);
    };

    // --- A NOVA FUNÇÃO MÁGICA: userCan ---
    const userCan = (permission) => {
        if (!user) return false;

        // 1. GOD MODE: Admin Global pode tudo
        if (user.role === 'ADMIN') return true;

        // 2. Admin Local (ROLE_ADMIN no Token) também pode tudo dentro da empresa
        if (permissions.includes('ROLE_ADMIN')) return true;

        // 3. Checa a permissão específica (ex: RECEBIMENTO_CRIAR)
        return permissions.includes(permission);
    };

    const refreshUserCompanies = async () => {
        try {
            // Chama o endpoint que já existe no EmpresaController
            const response = await api.get('/empresas/meus-acessos');
            const novasEmpresas = response.data;

            if (user) {
                // Atualiza o estado e o LocalStorage
                const updatedUser = { ...user, empresas: novasEmpresas };
                setUser(updatedUser);
                localStorage.setItem('wms_user', JSON.stringify(updatedUser));
                return true;
            }
        } catch (error) {
            console.error("Erro ao atualizar lista de empresas", error);
            return false;
        }
    };

    return (
        <AuthContext.Provider value={{
            authenticated: !!user,
            user,
            permissions, // Expõe a lista crua se precisar
            userCan,     // Expõe a função de verificação
            login,
            logout,
            loading,
            selecionarEmpresa,
            refreshUserCompanies
        }}>
            {children}
        </AuthContext.Provider>
    );
};