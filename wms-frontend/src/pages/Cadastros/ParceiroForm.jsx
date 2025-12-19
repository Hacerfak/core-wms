import { useState, useEffect } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField,
    Grid, Typography, Divider, MenuItem, InputAdornment, IconButton,
    CircularProgress, Paper, Switch, FormControlLabel
} from '@mui/material';
import { Save, X, Search, MapPin, Briefcase, Settings } from 'lucide-react';
import { toast } from 'react-toastify';
import { salvarParceiro } from '../../services/parceiroService';
import { consultarCnpjSefaz } from '../../services/integracaoService';

const ESTADOS_BR = [
    'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS',
    'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
];

const ParceiroForm = ({ open, onClose, onSuccess, initialData }) => {
    const [loading, setLoading] = useState(false);
    const [buscandoCnpj, setBuscandoCnpj] = useState(false);

    // Estado Plano (Flat)
    const [form, setForm] = useState({
        id: '', documento: '', nome: '', nomeFantasia: '', ie: '', tipo: 'AMBOS', crt: '3',
        cep: '', logradouro: '', numero: '', complemento: '', bairro: '', cidade: '', uf: 'SP',
        telefone: '', email: '',
        recebimentoCego: false, padraoControlaLote: false, padraoControlaValidade: false, padraoControlaSerie: false, ativo: true
    });

    useEffect(() => {
        if (open) {
            if (initialData) {
                // Se vier do banco (lista), preenche o form
                setForm(initialData);
            } else {
                // Reset para novo cadastro
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
        if (docLimpo.length !== 14) return toast.warning("CNPJ inválido.");
        if (form.uf === 'MA') return toast.warning("Consulta indisponível para MA.");

        setBuscandoCnpj(true);
        try {
            const dados = await consultarCnpjSefaz(form.uf, docLimpo);

            // CORREÇÃO: Não sobrescrever se vier vazio da SEFAZ
            setForm(prev => ({
                ...prev,
                nome: dados.razaoSocial || prev.nome,
                nomeFantasia: dados.nomeFantasia || prev.nomeFantasia,
                ie: (dados.ie && dados.ie !== 'ISENTO') ? dados.ie : prev.ie,
                crt: dados.regimeTributario || prev.crt,

                cep: dados.cep || prev.cep,
                logradouro: dados.logradouro || prev.logradouro,
                numero: dados.numero || prev.numero,
                complemento: dados.complemento || prev.complemento,
                bairro: dados.bairro || prev.bairro,
                cidade: dados.cidade || prev.cidade,

                // Se a SEFAZ retornar UF diferente (ex: matriz em outro estado), atualiza.
                uf: dados.uf || prev.uf
            }));

            toast.success("Dados preenchidos via SEFAZ!");
        } catch (error) {
            toast.error("Erro na consulta.");
        } finally {
            setBuscandoCnpj(false);
        }
    };

    const handleSubmit = async () => {
        setLoading(true);
        try {
            await salvarParceiro(form);
            toast.success("Parceiro salvo com sucesso!");
            onSuccess();
            onClose();
        } catch (error) {
            toast.error(error.response?.data?.message || "Erro ao salvar.");
        } finally { setLoading(false); }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6" fontWeight="bold">
                    {form.id ? 'Editar Parceiro' : 'Novo Parceiro'}
                </Typography>
                <IconButton onClick={onClose}><X size={20} /></IconButton>
            </DialogTitle>
            <Divider />

            <DialogContent sx={{ bgcolor: '#f8fafc', p: 3 }}>

                {/* DADOS FISCAIS */}
                <Paper elevation={0} sx={{ p: 2, mb: 2, border: '1px solid #e2e8f0' }}>
                    <Typography variant="subtitle2" color="primary" fontWeight="bold" mb={2} display="flex" gap={1}>
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
                                            <IconButton onClick={handleBuscarSefaz} disabled={buscandoCnpj} edge="end">
                                                {buscandoCnpj ? <CircularProgress size={16} /> : <Search size={18} color="#1976d2" />}
                                            </IconButton>
                                        </InputAdornment>
                                    )
                                }}
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
                            <TextField label="Razão Social" fullWidth size="small" required value={form.nome} onChange={e => handleChange('nome', e.target.value)} InputLabelProps={{ shrink: true }} />
                        </Grid>
                        <Grid item xs={12} sm={3}>
                            <TextField label="Nome Fantasia" fullWidth size="small" value={form.nomeFantasia} onChange={e => handleChange('nomeFantasia', e.target.value)} InputLabelProps={{ shrink: true }} />
                        </Grid>
                        <Grid item xs={12} sm={3}>
                            <TextField label="Insc. Estadual" fullWidth size="small" value={form.ie} onChange={e => handleChange('ie', e.target.value)} InputLabelProps={{ shrink: true }} />
                        </Grid>
                    </Grid>
                </Paper>

                {/* ENDEREÇO */}
                <Paper elevation={0} sx={{ p: 2, mb: 2, border: '1px solid #e2e8f0' }}>
                    <Typography variant="subtitle2" color="primary" fontWeight="bold" mb={2} display="flex" gap={1}>
                        <MapPin size={18} /> Endereço e Contato
                    </Typography>
                    <Grid container spacing={2}>
                        <Grid item xs={12} sm={3}>
                            <TextField
                                label="CEP" fullWidth size="small"
                                value={form.cep} onChange={e => handleChange('cep', e.target.value)}
                                InputLabelProps={{ shrink: true }} // <--- EVITA SOBREPOSIÇÃO
                            />
                        </Grid>
                        <Grid item xs={12} sm={7}>
                            <TextField
                                label="Logradouro" fullWidth size="small"
                                value={form.logradouro} onChange={e => handleChange('logradouro', e.target.value)}
                                InputLabelProps={{ shrink: true }}
                            />
                        </Grid>
                        <Grid item xs={12} sm={2}>
                            <TextField
                                label="Número" fullWidth size="small"
                                value={form.numero} onChange={e => handleChange('numero', e.target.value)}
                                InputLabelProps={{ shrink: true }}
                            />
                        </Grid>
                        <Grid item xs={12} sm={5}>
                            <TextField
                                label="Bairro" fullWidth size="small"
                                value={form.bairro} onChange={e => handleChange('bairro', e.target.value)}
                                InputLabelProps={{ shrink: true }}
                            />
                        </Grid>
                        <Grid item xs={12} sm={5}>
                            <TextField
                                label="Cidade" fullWidth size="small"
                                value={form.cidade} onChange={e => handleChange('cidade', e.target.value)}
                                InputLabelProps={{ shrink: true }}
                            />
                        </Grid>
                        <Grid item xs={12} sm={2}>
                            <TextField disabled label="UF" fullWidth size="small" value={form.uf} InputLabelProps={{ shrink: true }} />
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
                    <Typography variant="subtitle2" color="text.secondary" fontWeight="bold" mb={2} display="flex" gap={1}>
                        <Settings size={16} /> Configurações
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