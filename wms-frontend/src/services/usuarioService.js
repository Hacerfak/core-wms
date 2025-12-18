import api from './api';

export const getUsuarios = async () => {
    const response = await api.get('/api/gestao-usuarios');
    return response.data;
};

export const getPerfis = async () => {
    const response = await api.get('/api/gestao-usuarios/perfis');
    return response.data;
};

export const criarUsuario = async (dados) => {
    await api.post('/api/gestao-usuarios', dados);
};