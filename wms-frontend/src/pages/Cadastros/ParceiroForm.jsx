import { useState, useEffect } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField,
    Grid, Typography, Divider, MenuItem, InputAdornment, IconButton,
    CircularProgress, Paper, Switch, FormControlLabel
} from '@mui/material';
import { Save, X, Search, MapPin, Briefcase, Phone, Settings, AlertTriangle } from 'lucide-react';
import { toast } from 'react-toastify';
import { salvarParceiro } from '../../services/parceiroService';
import { consultarCnpjSefaz } from '../../services/integracaoService';

// Lista Completa de UFs
const ESTADOS_BR = [
    'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS',
    'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
];

const ParceiroForm = ({ open, onClose, onSuccess, initialData }) => {
    const [loading, setLoading] = useState(false);
    const [buscandoCnpj, setBuscandoCnpj] = useState(false);

    // Estado Plano (Flat) para facilitar o envio para o Java
    const [form, setForm] = useState({
        id: '', documento: '', nome: '', nomeFantasia: '', ie: '', tipo: 'AMBOS', crt: '3',
        cep: '', logradouro: '', numero: '', complemento: '', bairro: '', cidade: '', uf: 'SP',
        telefone: '', email: '',
        recebimentoCego: false,
        padraoControlaLote: false,
        padraoControlaValidade: false,
        padraoControlaSerie: false,
        ativo: true
    });

    useEffect(() => {
        if (open) {
            if (initialData) {
                // Se o initialData vier com endereco aninhado (do grid antigo), converte.
                // Se vier plano (do backend novo), usa direto.
                const dados = { ...initialData };
                if (initialData.endereco && typeof initialData.endereco === 'object') {
                    dados.cep = initialData.endereco.cep;
                    dados.logradouro = initialData.endereco.logradouro;
                    dados.numero = initialData.endereco.numero;
                    dados.bairro = initialData.endereco.bairro;
                    dados.cidade = initialData.endereco.cidade;
                    dados.uf = initialData.endereco.uf;
                    dados.complemento = initialData.endereco.complemento;
                }
                setForm(dados);
            } else {
                setForm({
                    id: '', documento: '', nome: '', nomeFantasia: '', ie: '', tipo: 'AMBOS', crt: '3',
                    cep: '', logradouro: '', numero: '', complemento: '', bairro: '', cidade: '', uf: 'SP',
                    telefone: '', email: '',
                    recebimentoCego: false, padraoControlaLote: false, padraoControlaValidade: false, padraoControlaSerie: false, ativo: true
                });
            }
        }
    }, [open, initialData]);

    const handleChange = (field, value) => setForm(prev => ({ ...prev, [field]: value }));
    const handleSwitchChange = (field) => setForm(prev => ({ ...prev, [field]: !prev[field] }));

    const handleBuscarSefaz = async () => {
        const docLimpo = form.documento.replace(/\D/g, '');
        if (docLimpo.length !== 14) return toast.warning("CNPJ deve ter 14 dígitos.");

        // Regra do Maranhão
        if (form.uf === 'MA') return toast.warning("O estado do MA não permite consulta automática.");

        setBuscandoCnpj(true);
        try {
            const dados = await consultarCnpjSefaz(form.uf, docLimpo);
            setForm(prev => ({
                ...prev,
                nome: dados.razaoSocial,
                nomeFantasia: dados.nomeFantasia,
                ie: dados.ie && dados.ie !== 'ISENTO' ? dados.ie : prev.ie,
                crt: dados.regimeTributario || prev.crt,

                // Endereço (Mantém a UF original se a Sefaz não retornar ou retornar vazia)
                cep: dados.cep,
                logradouro: dados.logradouro,
                numero: dados.numero,
                complemento: dados.complemento,
                bairro: dados.bairro,
                cidade: dados.cidade,
                uf: dados.uf || prev.uf
            }));
            toast.success("Dados da SEFAZ aplicados!");
        } catch (error) {
            toast.error("Erro na consulta SEFAZ.");
        } finally { setBuscandoCnpj(false); }
    };

    const handleSubmit = async () => {
        setLoading(true);
        try {
            // Envia o form plano, que bate com a Entidade Java 'Parceiro'
            await salvarParceiro(form);
            toast.success("Parceiro salvo com sucesso!");
            onSuccess();
            onClose();
        } catch (error) {
            console.error(error);
            toast.error("Erro ao salvar parceiro.");
        } finally { setLoading(false); }
    };

    const isMa = form.uf === 'MA';

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', pb: 1 }}>
                <Typography variant="h6" fontWeight="bold">
                    {form.id ? 'Editar Parceiro' : 'Novo Parceiro'}
                </Typography>
                <IconButton onClick={onClose} size="small"><X size={20} /></IconButton>
            </DialogTitle>
            <Divider />

            <DialogContent sx={{ bgcolor: '#f8fafc', p: 3 }}>

                {/* DADOS PRINCIPAIS */}
                <Paper elevation={0} sx={{ p: 2, mb: 2, border: '1px solid #e2e8f0' }}>
                    <Typography variant="subtitle2" color="primary" sx={{ mb: 2, display: 'flex', gap: 1, alignItems: 'center', fontWeight: 'bold' }}>
                        <Briefcase size={18} /> Dados Fiscais
                    </Typography>
                    <Grid container spacing={2}>
                        <Grid item xs={12} sm={3}>
                            <TextField select label="Tipo" fullWidth size="small" value={form.tipo} onChange={e => handleChange('tipo', e.target.value)}>
                                <MenuItem value="CLIENTE">Cliente</MenuItem>
                                <MenuItem value="FORNECEDOR">Fornecedor</MenuItem>
                                <MenuItem value="TRANSPORTADORA">Transportadora</MenuItem>
                                <MenuItem value="AMBOS">Híbrido</MenuItem>
                            </TextField>
                        </Grid>

                        {/* UF PRINCIPAL (Controla a Busca) */}
                        <Grid item xs={12} sm={2}>
                            <TextField select label="UF" fullWidth size="small" value={form.uf} onChange={e => handleChange('uf', e.target.value)}>
                                {ESTADOS_BR.map(u => <MenuItem key={u} value={u}>{u}</MenuItem>)}
                            </TextField>
                        </Grid>

                        <Grid item xs={12} sm={4}>
                            <TextField label="CNPJ / CPF" fullWidth size="small" value={form.documento} onChange={e => handleChange('documento', e.target.value)}
                                InputProps={{
                                    endAdornment: (
                                        <InputAdornment position="end">
                                            <IconButton
                                                onClick={handleBuscarSefaz}
                                                disabled={buscandoCnpj || isMa}
                                                edge="end"
                                                title={isMa ? "Consulta indisponível para MA" : "Buscar na SEFAZ"}
                                            >
                                                {buscandoCnpj ? <CircularProgress size={16} /> : <Search size={18} color={isMa ? '#ccc' : '#1976d2'} />}
                                            </IconButton>
                                        </InputAdornment>
                                    )
                                }}
                                helperText={isMa ? "Consulta SEFAZ indisponível para MA" : ""}
                            />
                        </Grid>
                        <Grid item xs={12} sm={3}>
                            <TextField select label="Regime (CRT)" fullWidth size="small" value={form.crt} onChange={e => handleChange('crt', e.target.value)}>
                                <MenuItem value="1">1 - Simples Nacional</MenuItem>
                                <MenuItem value="2">2 - Simples (Excesso)</MenuItem>
                                <MenuItem value="3">3 - Regime Normal</MenuItem>
                                <MenuItem value="4">4 - MEI</MenuItem>
                            </TextField>
                        </Grid>

                        <Grid item xs={12} sm={6}>
                            <TextField label="Razão Social" fullWidth size="small" required value={form.nome} onChange={e => handleChange('nome', e.target.value)} />
                        </Grid>
                        <Grid item xs={12} sm={3}>
                            <TextField label="Nome Fantasia" fullWidth size="small" value={form.nomeFantasia} onChange={e => handleChange('nomeFantasia', e.target.value)} />
                        </Grid>
                        <Grid item xs={12} sm={3}>
                            <TextField label="Insc. Estadual" fullWidth size="small" value={form.ie} onChange={e => handleChange('ie', e.target.value)} />
                        </Grid>
                    </Grid>
                </Paper>

                {/* ENDEREÇO E CONTATO */}
                <Paper elevation={0} sx={{ p: 2, mb: 2, border: '1px solid #e2e8f0' }}>
                    <Typography variant="subtitle2" color="primary" sx={{ mb: 2, display: 'flex', gap: 1, alignItems: 'center', fontWeight: 'bold' }}>
                        <MapPin size={18} /> Endereço e Contato
                    </Typography>
                    <Grid container spacing={2}>
                        <Grid item xs={12} sm={3}>
                            <TextField label="CEP" fullWidth size="small" value={form.cep} onChange={e => handleChange('cep', e.target.value)} />
                        </Grid>
                        <Grid item xs={12} sm={7}>
                            <TextField label="Logradouro" fullWidth size="small" value={form.logradouro} onChange={e => handleChange('logradouro', e.target.value)} />
                        </Grid>
                        <Grid item xs={12} sm={2}>
                            <TextField label="Número" fullWidth size="small" value={form.numero} onChange={e => handleChange('numero', e.target.value)} />
                        </Grid>
                        <Grid item xs={12} sm={5}>
                            <TextField label="Bairro" fullWidth size="small" value={form.bairro} onChange={e => handleChange('bairro', e.target.value)} />
                        </Grid>
                        <Grid item xs={12} sm={5}>
                            <TextField label="Cidade" fullWidth size="small" value={form.cidade} onChange={e => handleChange('cidade', e.target.value)} />
                        </Grid>
                        <Grid item xs={12} sm={2}>
                            <TextField disabled label="UF" fullWidth size="small" value={form.uf} />
                        </Grid>

                        <Grid item xs={12}><Divider sx={{ borderStyle: 'dashed' }} /></Grid>

                        <Grid item xs={12} sm={6}>
                            <TextField label="Email" fullWidth size="small" value={form.email} onChange={e => handleChange('email', e.target.value)} />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField label="Telefone" fullWidth size="small" value={form.telefone} onChange={e => handleChange('telefone', e.target.value)} />
                        </Grid>
                    </Grid>
                </Paper>

                {/* PARÂMETROS */}
                <Paper elevation={0} sx={{ p: 2, border: '1px solid #e2e8f0', bgcolor: '#fff' }}>
                    <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1, display: 'flex', gap: 1, alignItems: 'center', fontWeight: 'bold' }}>
                        <Settings size={16} /> Configurações de Operação
                    </Typography>
                    <Grid container spacing={2}>
                        <Grid item xs={12} sm={6}>
                            <FormControlLabel control={<Switch size="small" checked={form.recebimentoCego} onChange={() => handleSwitchChange('recebimentoCego')} />} label="Recebimento Cego" />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <FormControlLabel control={<Switch size="small" checked={form.padraoControlaLote} onChange={() => handleSwitchChange('padraoControlaLote')} />} label="Controla Lote (Padrão)" />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <FormControlLabel control={<Switch size="small" checked={form.padraoControlaValidade} onChange={() => handleSwitchChange('padraoControlaValidade')} />} label="Controla Validade (Padrão)" />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <FormControlLabel control={<Switch size="small" checked={form.padraoControlaSerie} onChange={() => handleSwitchChange('padraoControlaSerie')} />} label="Controla Série (Padrão)" />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <FormControlLabel control={<Switch size="small" color="success" checked={form.ativo} onChange={() => handleSwitchChange('ativo')} />} label="Parceiro Ativo" />
                        </Grid>
                    </Grid>
                </Paper>
            </DialogContent>

            <DialogActions sx={{ p: 2, bgcolor: '#f8fafc', borderTop: '1px solid #e2e8f0' }}>
                <Button onClick={onClose} color="inherit">Cancelar</Button>
                <Button variant="contained" onClick={handleSubmit} disabled={loading} startIcon={<Save size={18} />}>
                    {loading ? "Salvando..." : "Salvar"}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
export default ParceiroForm;