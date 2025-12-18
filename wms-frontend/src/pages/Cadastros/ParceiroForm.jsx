import { useState, useEffect } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button,
    TextField, Box, Grid, Tabs, Tab, FormControlLabel, Switch,
    MenuItem, InputAdornment, Typography, Paper, IconButton
} from '@mui/material';
import { User, MapPin, Settings2, Search, Loader2 } from 'lucide-react';
import { toast } from 'react-toastify';
import { salvarParceiro, buscarEnderecoPorCep } from '../../services/parceiroService';

const ParceiroForm = ({ open, onClose, parceiro, onSuccess }) => {
    const [tabIndex, setTabIndex] = useState(0);
    const [loading, setLoading] = useState(false);
    const [loadingCep, setLoadingCep] = useState(false);

    const [form, setForm] = useState({
        nome: '',
        cnpjCpf: '',
        tipo: 'AMBOS',
        email: '',
        telefone: '',
        ativo: true,
        ie: '',
        crt: '',
        endereco: {
            cep: '', logradouro: '', numero: '', complemento: '',
            bairro: '', cidade: '', uf: ''
        },
        parametros: {
            recebimentoCego: false
        }
    });

    useEffect(() => {
        if (open) {
            setTabIndex(0);
            if (parceiro) {
                // --- CORREÇÃO DE MAPEAMENTO (API -> FORM) ---
                setForm({
                    id: parceiro.id,
                    nome: parceiro.nome || '',
                    cnpjCpf: parceiro.documento || '', // API manda 'documento', Form usa 'cnpjCpf'
                    tipo: parceiro.tipo || 'AMBOS',
                    email: parceiro.email || '',
                    telefone: parceiro.telefone || '',
                    ativo: parceiro.ativo !== false,
                    ie: parceiro.ie || '',
                    crt: parceiro.crt || '',

                    // Reconstrói o objeto endereço a partir dos campos planos da API
                    endereco: {
                        cep: parceiro.cep || '',
                        logradouro: parceiro.logradouro || '',
                        numero: parceiro.numero || '',
                        complemento: parceiro.complemento || '', // Se tiver
                        bairro: parceiro.bairro || '',
                        cidade: parceiro.cidade || '',
                        uf: parceiro.uf || ''
                    },

                    // Reconstrói parâmetros
                    parametros: {
                        recebimentoCego: parceiro.recebimentoCego || false
                    }
                });
            } else {
                // Reset para novo cadastro
                setForm({
                    nome: '', cnpjCpf: '', tipo: 'AMBOS', email: '', telefone: '', ativo: true, ie: '', crt: '',
                    endereco: { cep: '', logradouro: '', numero: '', complemento: '', bairro: '', cidade: '', uf: '' },
                    parametros: { recebimentoCego: false }
                });
            }
        }
    }, [open, parceiro]);

    const handleChange = (field, value) => {
        setForm(prev => ({ ...prev, [field]: value }));
    };

    const handleAddressChange = (field, value) => {
        setForm(prev => ({
            ...prev,
            endereco: { ...prev.endereco, [field]: value }
        }));
    };

    const handleParamChange = (field, value) => {
        setForm(prev => ({
            ...prev,
            parametros: { ...prev.parametros, [field]: value }
        }));
    };

    const handleBuscarCep = async () => {
        if (!form.endereco.cep || form.endereco.cep.length < 8) return;
        setLoadingCep(true);
        const data = await buscarEnderecoPorCep(form.endereco.cep);
        if (data && !data.erro) {
            setForm(prev => ({
                ...prev,
                endereco: {
                    ...prev.endereco,
                    logradouro: data.logradouro,
                    bairro: data.bairro,
                    cidade: data.localidade,
                    uf: data.uf
                }
            }));
        } else {
            toast.warning("CEP não encontrado.");
        }
        setLoadingCep(false);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            await salvarParceiro(form);
            toast.success("Parceiro salvo com sucesso!");
            onSuccess();
        } catch (error) {
            console.error(error);
            toast.error("Erro ao salvar parceiro.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>{parceiro ? "Editar Parceiro" : "Novo Parceiro"}</DialogTitle>
            <form onSubmit={handleSubmit}>
                <DialogContent dividers sx={{ p: 0 }}>
                    <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: '#f8fafc' }}>
                        <Tabs value={tabIndex} onChange={(e, v) => setTabIndex(v)} sx={{ px: 2 }}>
                            <Tab icon={<User size={18} />} iconPosition="start" label="Dados Gerais" />
                            <Tab icon={<MapPin size={18} />} iconPosition="start" label="Endereço" />
                            <Tab icon={<Settings2 size={18} />} iconPosition="start" label="Regras" />
                        </Tabs>
                    </Box>

                    <Box sx={{ p: 3 }}>
                        {/* ABA 0: DADOS GERAIS */}
                        {tabIndex === 0 && (
                            <Grid container spacing={2}>
                                <Grid item xs={12} display="flex" justifyContent="flex-end">
                                    <FormControlLabel control={<Switch checked={form.ativo} onChange={(e) => handleChange('ativo', e.target.checked)} color="success" />} label={form.ativo ? "Ativo" : "Inativo"} />
                                </Grid>
                                <Grid item xs={12} sm={8}>
                                    <TextField label="Razão Social / Nome" fullWidth required value={form.nome} onChange={e => handleChange('nome', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField select label="Tipo" fullWidth value={form.tipo} onChange={e => handleChange('tipo', e.target.value)}>
                                        <MenuItem value="FORNECEDOR">Fornecedor</MenuItem>
                                        <MenuItem value="CLIENTE">Cliente</MenuItem>
                                        <MenuItem value="AMBOS">Ambos</MenuItem>
                                    </TextField>
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="CNPJ / CPF" fullWidth required value={form.cnpjCpf} onChange={e => handleChange('cnpjCpf', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="Inscrição Estadual" fullWidth value={form.ie} onChange={e => handleChange('ie', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="CRT (Regime Trib.)" fullWidth value={form.crt} onChange={e => handleChange('crt', e.target.value)} placeholder="1=Simples, 3=Normal" />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <TextField label="Telefone" fullWidth value={form.telefone} onChange={e => handleChange('telefone', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <TextField label="E-mail" type="email" fullWidth value={form.email} onChange={e => handleChange('email', e.target.value)} />
                                </Grid>
                            </Grid>
                        )}

                        {/* ABA 1: ENDEREÇO */}
                        {tabIndex === 1 && (
                            <Grid container spacing={2}>
                                <Grid item xs={12} sm={4}>
                                    <TextField
                                        label="CEP" fullWidth value={form.endereco.cep}
                                        onChange={e => handleAddressChange('cep', e.target.value)}
                                        InputProps={{
                                            endAdornment: (
                                                <InputAdornment position="end">
                                                    <IconButton onClick={handleBuscarCep} edge="end" disabled={loadingCep}>
                                                        {loadingCep ? <Loader2 size={20} className="animate-spin" /> : <Search size={20} />}
                                                    </IconButton>
                                                </InputAdornment>
                                            )
                                        }}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={8}>
                                    <TextField label="Logradouro" fullWidth value={form.endereco.logradouro} onChange={e => handleAddressChange('logradouro', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="Número" fullWidth value={form.endereco.numero} onChange={e => handleAddressChange('numero', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={8}>
                                    <TextField label="Complemento" fullWidth value={form.endereco.complemento} onChange={e => handleAddressChange('complemento', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={5}>
                                    <TextField label="Bairro" fullWidth value={form.endereco.bairro} onChange={e => handleAddressChange('bairro', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={5}>
                                    <TextField label="Cidade" fullWidth value={form.endereco.cidade} onChange={e => handleAddressChange('cidade', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={2}>
                                    <TextField label="UF" fullWidth value={form.endereco.uf} onChange={e => handleAddressChange('uf', e.target.value)} />
                                </Grid>
                            </Grid>
                        )}

                        {/* ABA 2: PARÂMETROS */}
                        {tabIndex === 2 && (
                            <Box>
                                <Typography variant="subtitle2" color="primary" fontWeight="bold" gutterBottom>Recebimento</Typography>
                                <Paper variant="outlined" sx={{ p: 2 }}>
                                    <Grid container alignItems="center" justifyContent="space-between">
                                        <Grid item xs={9}>
                                            <Typography fontWeight="bold">Conferência Cega</Typography>
                                            <Typography variant="body2" color="text.secondary">Oculta as quantidades da Nota Fiscal na conferência.</Typography>
                                        </Grid>
                                        <Grid item xs={3} textAlign="right">
                                            <Switch checked={form.parametros.recebimentoCego} onChange={(e) => handleParamChange('recebimentoCego', e.target.checked)} />
                                        </Grid>
                                    </Grid>
                                </Paper>
                            </Box>
                        )}
                    </Box>
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={onClose}>Cancelar</Button>
                    <Button type="submit" variant="contained" disabled={loading}>Salvar</Button>
                </DialogActions>
            </form>
        </Dialog>
    );
};

export default ParceiroForm;