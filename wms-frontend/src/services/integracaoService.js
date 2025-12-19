import api from './api';

// --- CONFIGURAÇÃO DA EMPRESA (TENANT) ---

export const getEmpresaConfig = async () => {
    const response = await api.get('/api/empresa-config');
    return response.data;
};

export const updateEmpresaConfig = async (dados) => {
    const response = await api.put('/api/empresa-config', dados);
    return response.data;
};

export const uploadCertificadoConfig = async (file, senha) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('senha', senha);

    // O Content-Type multipart/form-data é gerado automaticamente pelo browser ao usar FormData
    await api.post('/api/empresa-config/certificado', formData);
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