import api from './api';

export const getAuditLogs = async (filtros, page = 0, size = 20) => {
    // Monta Query String
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('size', size);

    if (filtros.inicio) params.append('inicio', filtros.inicio);
    if (filtros.fim) params.append('fim', filtros.fim);
    if (filtros.usuario) params.append('usuario', filtros.usuario);
    if (filtros.entidade) params.append('entidade', filtros.entidade);
    if (filtros.acao) params.append('acao', filtros.acao);

    const response = await api.get(`/api/auditoria?${params.toString()}`);
    return response.data;
};