import api from './api';

// Busca dados cadastrais da empresa (Tabela tb_empresa_dados)
export const getEmpresaConfig = async () => {
    const response = await api.get('/api/empresa-dados');
    return response.data;
};

// Atualiza dados cadastrais
export const updateEmpresaConfig = async (dados) => {
    const response = await api.put('/api/empresa-dados', dados);
    return response.data;
};

// Upload do certificado
export const uploadCertificadoConfig = async (file, senha) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('senha', senha);

    const response = await api.post('/api/empresa-dados/certificado', formData);
    return response.data;
};

// --- CONSULTAS SEFAZ ---

export const consultarCnpjSefaz = async (uf, cnpj) => {
    // Remove caracteres não numéricos para evitar erros na URL
    const cnpjLimpo = cnpj.replace(/\D/g, '');
    const response = await api.get(`/api/integracao/sefaz/${uf}/cnpj/${cnpjLimpo}`);
    return response.data;
};

export const consultarIeSefaz = async (uf, ie) => {
    const ieLimpa = ie.replace(/\D/g, '');
    const response = await api.get(`/api/integracao/sefaz/${uf}/ie/${ieLimpa}`);
    return response.data;
};