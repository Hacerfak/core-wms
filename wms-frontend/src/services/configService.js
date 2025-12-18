import api from './api';

export const getConfiguracoes = async () => {
    const response = await api.get('/api/configuracoes');
    return response.data;
};

export const updateConfiguracao = async (chave, valor) => {
    await api.put(`/api/configuracoes/${chave}`, { valor: String(valor) });
};

export const checkExibirQtdRecebimento = async (recebimentoId) => {
    if (!recebimentoId) return true;
    try {
        const response = await api.get(`/api/recebimentos/${recebimentoId}/config-conferencia`);
        return response.data; // Retorna boolean (true = exibe, false = cego)
    } catch (error) {
        console.error("Erro ao checar config conferencia", error);
        return true; // Fallback seguro
    }
};