import api from './api';

export const getConfiguracoes = async () => {
    const response = await api.get('/api/configuracoes');
    return response.data;
};

export const updateConfiguracao = async (chave, valor) => {
    await api.put(`/api/configuracoes/${chave}`, { valor: String(valor) });
};

export const checkExibirQtdRecebimento = async () => {
    const response = await api.get('/api/configuracoes/recebimento-exibir-qtd');
    return response.data;
};