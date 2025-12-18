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
    // PREPARA O DTO: O Backend espera tudo plano na raiz da entidade Parceiro
    // Mas o nosso Form no React usa objetos aninhados (endereco, parametros) para organização.

    const payload = {
        id: form.id,
        nome: form.nome,
        documento: form.cnpjCpf, // Mapeando para o campo Java 'documento'
        tipo: form.tipo,
        email: form.email,
        telefone: form.telefone,
        ativo: form.ativo,

        // Achata Endereço
        cep: form.endereco.cep,
        logradouro: form.endereco.logradouro,
        numero: form.endereco.numero,
        complemento: form.endereco.complemento,
        bairro: form.endereco.bairro,
        cidade: form.endereco.cidade,
        uf: form.endereco.uf,

        // Achata Parâmetros
        recebimentoCego: form.parametros.recebimentoCego
    };

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
    // Pode usar uma API pública ou proxy no seu backend
    // Exemplo direto ViaCEP (Front-only) ou via seu Backend (Recomendado para segurança)
    try {
        const cleanCep = cep.replace(/\D/g, '');
        const response = await fetch(`https://viacep.com.br/ws/${cleanCep}/json/`);
        return await response.json();
    } catch (error) {
        console.error("Erro CEP", error);
        return null;
    }
};