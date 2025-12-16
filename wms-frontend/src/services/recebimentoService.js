import api from './api';

export const getRecebimentos = async () => {
    const response = await api.get('/api/recebimentos');
    return response.data;
};

export const getRecebimentoById = async (id) => {
    const response = await api.get(`/api/recebimentos/${id}`);
    return response.data;
};

export const deleteRecebimento = async (id) => {
    await api.delete(`/api/recebimentos/${id}`);
};

// NOVO: Envia a contagem e gera o LPN
export const conferirProduto = async (recebimentoId, sku, quantidade) => {
    // O endpoint espera: { sku, quantidade, lpnExterno: null }
    const payload = {
        sku: sku,
        quantidade: Number(quantidade),
        lpnExterno: null
    };

    const response = await api.post(`/api/recebimentos/${recebimentoId}/volume`, payload);
    return response.data; // Retorna o código LPN (String)
};

export const finalizarConferencia = async (id) => {
    const response = await api.post(`/api/recebimentos/${id}/finalizar`);
    return response.data; // Retorna o objeto Recebimento atualizado (com o novo status)
};

export const cancelarConferencia = async (id) => {
    await api.post(`/api/recebimentos/${id}/cancelar`);
};

export const getVolumesDoItem = async (recebimentoId, produtoId) => {
    const response = await api.get(`/api/recebimentos/${recebimentoId}/produtos/${produtoId}/volumes`);
    return response.data;
};