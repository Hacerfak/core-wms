import api from './api';

// --- USUÁRIOS ---
export const getUsuarios = async () => {
    const response = await api.get('/api/gestao-usuarios');
    return response.data;
};

export const verificarUsuario = async (login) => {
    const response = await api.get(`/api/gestao-usuarios/verificar/${login}`);
    return response.data;
};

export const criarUsuario = async (dados) => {
    await api.post('/api/gestao-usuarios', dados);
};

// --- NOVOS MÉTODOS EXCLUSÃO ---
export const removerUsuarioLocal = async (id) => {
    await api.delete(`/api/gestao-usuarios/${id}/local`);
};

export const excluirUsuarioGlobal = async (id) => {
    await api.delete(`/api/gestao-usuarios/${id}/global`);
};

export const atualizarUsuario = async (id, dados) => {
    await api.put(`/api/gestao-usuarios/${id}`, dados);
};

// --- PERFIS (Novo) ---
export const getPerfis = async () => {
    const response = await api.get('/api/gestao-perfis'); // URL Corrigida para o novo controller
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