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

// Renomeado para manter consistência, mas aponta para o DELETE que agora exclui de verdade
export const excluirAgente = async (id) => {
    await api.delete(`/api/impressao/agentes/${id}`);
};

// NOVO: Atualizar Agente
export const atualizarAgente = async (id, dados, ativo) => {
    const url = `/api/impressao/agentes/${id}${ativo !== undefined ? `?ativo=${ativo}` : ''}`;
    const response = await api.put(url, dados);
    return response.data;
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

export const getImpressorasAtivas = async () => {
    const response = await api.get('/api/impressao/impressoras');
    // Filtra apenas as ativas
    return response.data.filter(i => i.ativo);
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

// LPNs
/**
 * Envia comando de impressão de LPN.
 * Agora suporta templateId opcional.
 */
export const imprimirLpn = async (lpnId, impressoraId, templateId = null) => {
    const params = { impressoraId };
    if (templateId) params.templateId = templateId;

    await api.post(`/api/estoque/lpns/${lpnId}/imprimir`, null, { params });
};