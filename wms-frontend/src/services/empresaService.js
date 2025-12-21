import api from './api';

// Usado na Seleção de Empresa (Login)
export const getMinhasEmpresas = async () => {
    const response = await api.get('/api/empresas/meus-acessos');
    return response.data;
};

// Usado no formulário de Usuário (Admin)
export const getTodasEmpresas = async () => {
    const response = await api.get('/api/empresas/lista-simples');
    return response.data;
};

export const salvarEmpresa = async (dados) => {
    // Se tem ID, é atualização. Se não, é criação (Onboarding manual)
    if (dados.id) {
        const response = await api.put(`/api/empresas/${dados.id}`, dados);
        return response.data;
    } else {
        const response = await api.post('/api/empresas', dados);
        return response.data;
    }
};

export const excluirEmpresa = async (id) => {
    await api.delete(`/api/empresas/${id}`);
};