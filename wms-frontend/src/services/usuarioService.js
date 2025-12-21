import api from './api';

// --- CRUD BÁSICO ---
export const getUsuarios = async () => {
    const response = await api.get('/api/usuarios');
    return response.data;
};

export const getUsuarioById = async (id) => {
    const response = await api.get(`/api/usuarios/${id}`);
    return response.data;
};

export const salvarUsuario = async (dados) => {
    if (dados.id) {
        const response = await api.put(`/api/usuarios/${dados.id}`, dados);
        return response.data;
    } else {
        const response = await api.post('/api/usuarios', dados);
        return response.data;
    }
};

export const excluirUsuario = async (id) => {
    await api.delete(`/api/usuarios/${id}`);
};

/// --- PERFIS & VÍNCULOS ---

// NOVO: Busca perfis de uma empresa específica (para o combo de vinculação)
export const getPerfisDaEmpresa = async (empresaId) => {
    const response = await api.get(`/api/usuarios/perfis-disponiveis/${empresaId}`);
    return response.data;
};

export const getEmpresasDoUsuario = async (usuarioId) => {
    const response = await api.get(`/api/usuarios/${usuarioId}/empresas`);
    return response.data;
};

// ATUALIZADO: Agora passa perfilId
export const vincularUsuarioEmpresa = async (usuarioId, empresaId, perfilId) => {
    await api.post(`/api/usuarios/${usuarioId}/empresas`, { empresaId, perfilId });
};

export const desvincularUsuarioEmpresa = async (usuarioId, empresaId) => {
    await api.delete(`/api/usuarios/${usuarioId}/empresas/${empresaId}`);
};

// ... (getPerfis, salvarPerfil, excluirPerfil globais mantidos para a tela de gestão de perfis) ...
export const getPerfis = async () => {
    const response = await api.get('/api/gestao-perfis');
    return response.data;
};
export const getPermissoesDisponiveis = async () => {
    const response = await api.get('/api/gestao-perfis/permissoes-disponiveis');
    return response.data;
};
export const salvarPerfil = async (perfil) => {
    const response = await api.post('/api/gestao-perfis', perfil);
    return response.data;
};
export const excluirPerfil = async (id) => {
    await api.delete(`/api/gestao-perfis/${id}`);
};