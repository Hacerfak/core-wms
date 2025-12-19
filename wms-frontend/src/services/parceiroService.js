import api from './api';

export const getParceiros = async () => {
    const response = await api.get('/api/parceiros');
    return response.data;
};

export const getParceiroById = async (id) => {
    const response = await api.get(`/api/parceiros/${id}`);
    return response.data;
};

export const salvarParceiro = async (form) => {
    // CORREÇÃO: O formulário agora é "plano" (flat), então não precisamos
    // desestruturar endereco ou parametros. Enviamos o objeto direto.
    // O Backend (ParceiroRequest) já espera esses campos na raiz.

    const payload = { ...form };

    // Garante que o campo 'documento' esteja preenchido 
    // (caso o form use nomes legados em algum lugar)
    if (!payload.documento && payload.cnpjCpf) {
        payload.documento = payload.cnpjCpf;
    }

    if (form.id) {
        await api.put(`/api/parceiros/${form.id}`, payload);
    } else {
        await api.post('/api/parceiros', payload);
    }
};

export const excluirParceiro = async (id) => {
    await api.delete(`/api/parceiros/${id}`);
};

export const buscarEnderecoPorCep = async (cep) => {
    try {
        const cleanCep = cep.replace(/\D/g, '');
        const response = await fetch(`https://viacep.com.br/ws/${cleanCep}/json/`);
        return await response.json();
    } catch (error) {
        console.error("Erro CEP", error);
        return null;
    }
};