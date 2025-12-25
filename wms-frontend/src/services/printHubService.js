import api from './api';

// --- AGENTES ---
export const getAgentes = async () => {
    const response = await api.get('/api/impressao/agentes');
    return response.data;
};

export const criarAgente = async (dados) => {
    const response = await api.post('/api/impressao/agentes', dados);
    return response.data;
};

export const revogarAgente = async (id) => {
    await api.delete(`/api/impressao/agentes/${id}`);
};

// --- IMPRESSORAS ---
export const getImpressorasAdmin = async () => {
    const response = await api.get('/api/impressao/impressoras');
    return response.data;
};

export const salvarImpressora = async (dados) => {
    const payload = { ...dados, porta: parseInt(dados.porta) || 9100 };
    const response = await api.post('/api/impressao/impressoras', payload);
    return response.data;
};

export const testarImpressora = async (id) => {
    await api.post(`/api/impressao/impressoras/${id}/teste`);
};

// --- TEMPLATES ---
export const getTemplates = async () => {
    const response = await api.get('/api/impressao/templates');
    return response.data;
};

export const salvarTemplate = async (dados) => {
    const payload = {
        ...dados,
        larguraMm: parseInt(dados.larguraMm),
        alturaMm: parseInt(dados.alturaMm)
    };

    if (dados.id) {
        return (await api.put(`/api/impressao/templates/${dados.id}`, payload)).data;
    } else {
        return (await api.post('/api/impressao/templates', payload)).data;
    }
};

export const excluirTemplate = async (id) => {
    await api.delete(`/api/impressao/templates/${id}`);
};

// --- FILA ---
export const getFilaImpressao = async (page = 0, size = 20) => {
    // Requer o endpoint adicionado acima
    const response = await api.get(`/api/impressao/fila?page=${page}&size=${size}&sort=id,desc`);
    return response.data;
};

export const getDebugZpl = async (id) => {
    const response = await api.get(`/api/impressao/fila/${id}/debug-zpl`);
    return response.data;
};

/**
 * Busca apenas impressoras ATIVAS para exibir em combos de seleção
 */
export const getImpressorasAtivas = async () => {
    const response = await api.get('/api/impressao/impressoras');
    // Filtra no front, já que o endpoint admin retorna todas
    return response.data.filter(imp => imp.ativo);
};

/**
 * Envia comando de impressão de LPN
 */
export const imprimirLpn = async (lpnId, impressoraId) => {
    await api.post(`/api/estoque/lpns/${lpnId}/imprimir`, null, {
        params: { impressoraId }
    });
};