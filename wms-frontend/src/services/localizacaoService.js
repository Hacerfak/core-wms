import api from './api';

export const getLocalizacoes = async () => {
    const response = await api.get('/api/locais');
    return response.data;
};

export const salvarLocalizacao = async (dados) => {
    const payload = { ...dados };
    if (payload.capacidadePesoKg) payload.capacidadePesoKg = parseFloat(payload.capacidadePesoKg);

    if (dados.id) {
        await api.put(`/api/locais/${dados.id}`, payload);
    } else {
        await api.post('/api/locais', payload);
    }
};

export const excluirLocalizacao = async (id) => {
    await api.delete(`/api/locais/${id}`);
};