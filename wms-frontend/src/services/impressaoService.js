import api from './api';

export const getImpressoras = async () => {
    const response = await api.get('/api/impressao/impressoras');
    // Retorna apenas as ativas
    return response.data.filter(imp => imp.ativo);
};

export const imprimirLpn = async (lpnId, impressoraId) => {
    // O backend espera POST /api/estoque/lpns/{id}/imprimir?impressoraId=...
    await api.post(`/api/estoque/lpns/${lpnId}/imprimir`, null, {
        params: { impressoraId }
    });
};