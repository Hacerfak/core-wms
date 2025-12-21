import { createContext, useState, useEffect, useCallback } from 'react';
import api from '../services/api';
import { jwtDecode } from 'jwt-decode';
import { useNavigate, useLocation } from 'react-router-dom';

export const AuthContext = createContext();

// Mapa de rotas principais e suas permissões
const ROUTE_PERMISSIONS = {
    '/recebimento': 'RECEBIMENTO_VISUALIZAR',
    '/estoque': 'ESTOQUE_VISUALIZAR',
    '/expedicao': 'PEDIDO_VISUALIZAR',
    '/usuarios': 'USUARIO_LISTAR',
    '/perfis': 'PERFIL_GERENCIAR',
    '/config': 'CONFIG_GERENCIAR'
};

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [permissions, setPermissions] = useState([]);
    const [loading, setLoading] = useState(true);

    const navigate = useNavigate();
    const location = useLocation();

    const processToken = useCallback((token) => {
        if (!token) return null;
        try {
            const decoded = jwtDecode(token);
            const userPermissions = decoded.roles || [];
            setPermissions(userPermissions);
            return { decoded, userPermissions };
        } catch (error) {
            console.error("Erro ao decodificar token", error);
            return null;
        }
    }, []);

    useEffect(() => {
        const recoveredUser = localStorage.getItem('wms_user');
        const token = localStorage.getItem('wms_token');
        // Recupera também o Tenant salvo, se houver
        const savedTenant = localStorage.getItem('@App:tenant');

        if (token && recoveredUser) {
            try {
                const result = processToken(token);
                if (result && result.decoded.exp * 1000 < Date.now()) {
                    logout();
                } else {
                    const parsedUser = JSON.parse(recoveredUser);
                    // Mescla o tenant salvo no objeto de usuário para consistência
                    setUser({ ...parsedUser, tenantId: savedTenant || parsedUser.tenantId });
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

            // CORREÇÃO: Desestruturar o 'id' que agora vem do backend
            const { token, id, empresas, usuario, role } = response.data;

            localStorage.setItem('wms_token', token);
            api.defaults.headers.Authorization = `Bearer ${token}`;

            processToken(token);

            // CORREÇÃO: Incluir 'id' no objeto userData
            const userData = { id, login: usuario, role, empresas };

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

            // 1. Salva Token
            localStorage.setItem('wms_token', newToken);

            // 2. CORREÇÃO PRINCIPAL: Salva o Tenant ID para o Front saber onde está
            localStorage.setItem('@App:tenant', tenantId);

            api.defaults.headers.Authorization = `Bearer ${newToken}`;

            // 3. Atualiza Estado do Usuário imediatamente (para não precisar de reload)
            setUser(prev => ({ ...prev, tenantId: tenantId }));

            // Processa o novo token e PEGA AS NOVAS PERMISSÕES
            const result = processToken(newToken);
            const newPermissions = result ? result.userPermissions : [];

            // --- REDIRECIONAMENTO INTELIGENTE ---
            const currentPath = location.pathname;
            let requiredPermission = null;

            for (const route in ROUTE_PERMISSIONS) {
                if (currentPath.startsWith(route)) {
                    requiredPermission = ROUTE_PERMISSIONS[route];
                    break;
                }
            }

            if (requiredPermission) {
                const isMaster = user?.role === 'ADMIN';
                const hasPermission = newPermissions.includes(requiredPermission) ||
                    newPermissions.includes('ROLE_ADMIN') ||
                    isMaster;

                if (!hasPermission) {
                    console.warn(`Redirecionando para Dashboard: Sem permissão ${requiredPermission} na nova empresa.`);
                    navigate('/dashboard');
                }
            }

            return true;
        } catch (error) {
            console.error("Erro ao selecionar empresa", error);
            return false;
        }
    };

    const logout = () => {
        localStorage.removeItem('wms_token');
        localStorage.removeItem('wms_user');
        localStorage.removeItem('@App:tenant'); // Limpa o tenant também
        api.defaults.headers.Authorization = null;
        setUser(null);
        setPermissions([]);
        navigate('/login');
    };

    const userCan = (permission) => {
        if (!user) return false;
        if (user.role === 'ADMIN') return true;
        if (permissions.includes('ROLE_ADMIN')) return true;
        return permissions.includes(permission);
    };

    const forceUpdatePermissions = async () => {
        try {
            const response = await api.post('/auth/refresh-permissions');
            const newToken = response.data.token;
            localStorage.setItem('wms_token', newToken);
            api.defaults.headers.Authorization = `Bearer ${newToken}`;
            processToken(newToken);
            return true;
        } catch (error) {
            console.error("Erro ao atualizar permissões", error);
            return false;
        }
    };

    const refreshUserCompanies = async () => {
        try {
            const response = await api.get('api/empresas/meus-acessos');
            const novasEmpresas = response.data;
            if (user) {
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
            permissions,
            userCan,
            login,
            logout,
            loading,
            selecionarEmpresa,
            switchTenant: selecionarEmpresa,
            refreshUserCompanies,
            forceUpdatePermissions
        }}>
            {children}
        </AuthContext.Provider>
    );
};