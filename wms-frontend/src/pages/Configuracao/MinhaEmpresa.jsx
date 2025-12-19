import { useState, useEffect } from 'react';
import {
    Box, TextField, Button, Grid, Paper, Typography, InputAdornment,
    MenuItem, CircularProgress, Divider, Chip, FormControlLabel, Switch, Tabs, Tab
} from '@mui/material';
import {
    Save, Search, Upload, ShieldCheck, Mail, Phone,
    CalendarCheck, AlertTriangle, MapPin, FileText, Settings, Globe, Building2
} from 'lucide-react';
import { toast } from 'react-toastify';
import {
    getEmpresaConfig, updateEmpresaConfig,
    consultarCnpjSefaz, uploadCertificadoConfig
} from '../../services/integracaoService';
import dayjs from 'dayjs';

const ESTADOS_BR = [
    'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS',
    'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
];

const MinhaEmpresa = () => {
    const [activeTab, setActiveTab] = useState(0);
    const [loading, setLoading] = useState(false);
    const [consultando, setConsultando] = useState(false);

    const [form, setForm] = useState({
        razaoSocial: '', nomeFantasia: '', cnpj: '', inscricaoEstadual: '', inscricaoMunicipal: '', cnaePrincipal: '', regimeTributario: '3',
        email: '', telefone: '', website: '',
        cep: '', logradouro: '', numero: '', complemento: '', bairro: '', cidade: '', uf: 'SP',
        nomeCertificado: '', validadeCertificado: null,
        recebimentoCegoObrigatorio: true, permiteEstoqueNegativo: false
    });

    const [certFile, setCertFile] = useState(null);
    const [certSenha, setCertSenha] = useState('');
    const [uploadingCert, setUploadingCert] = useState(false);

    useEffect(() => { load(); }, []);

    const load = async () => {
        try {
            const data = await getEmpresaConfig();
            setForm(prev => ({ ...prev, ...data, uf: data.uf || 'SP' }));
        } catch (e) { toast.error("Erro ao carregar dados."); }
    };

    const handleSave = async () => {
        setLoading(true);
        try { await updateEmpresaConfig(form); toast.success("Configurações salvas com sucesso!"); }
        catch (e) { toast.error("Erro ao salvar."); }
        finally { setLoading(false); }
    };

    const handleUploadCert = async () => {
        if (!certFile || !certSenha) return toast.warning("Informe arquivo e senha.");
        setUploadingCert(true);
        try {
            await uploadCertificadoConfig(certFile, certSenha);
            toast.success("Certificado atualizado!");
            setCertFile(null); setCertSenha('');
            load();
        } catch (e) { toast.error("Erro ao salvar certificado."); }
        finally { setUploadingCert(false); }
    };

    const handleConsultarSefaz = async () => {
        if (!form.cnpj || form.cnpj.length < 14) return toast.warning("CNPJ inválido.");
        setConsultando(true);
        try {
            const dados = await consultarCnpjSefaz(form.uf, form.cnpj.replace(/\D/g, ''));
            setForm(prev => ({
                ...prev,
                razaoSocial: dados.razaoSocial || prev.razaoSocial,
                nomeFantasia: dados.nomeFantasia || prev.nomeFantasia,
                inscricaoEstadual: (dados.ie && dados.ie !== 'ISENTO') ? dados.ie : prev.inscricaoEstadual,
                regimeTributario: dados.regimeTributario || prev.regimeTributario,
                cnaePrincipal: dados.cnaePrincipal || prev.cnaePrincipal,
                cep: dados.cep || prev.cep,
                logradouro: dados.logradouro || prev.logradouro,
                numero: dados.numero || prev.numero,
                complemento: dados.complemento || prev.complemento,
                bairro: dados.bairro || prev.bairro,
                cidade: dados.cidade || prev.cidade,
                uf: dados.uf || prev.uf
            }));
            toast.success("Dados atualizados via SEFAZ!");
        } catch (error) { toast.error("Erro na consulta SEFAZ."); }
        finally { setConsultando(false); }
    };

    const getCertStatus = () => {
        if (!form.nomeCertificado) return { label: 'Não Configurado', color: 'default', icon: <AlertTriangle size={16} /> };
        const dias = dayjs(form.validadeCertificado).diff(dayjs(), 'day');
        if (dias < 0) return { label: 'Vencido', color: 'error', icon: <AlertTriangle size={16} /> };
        return { label: `Válido até ${dayjs(form.validadeCertificado).format('DD/MM/YYYY')}`, color: 'success', icon: <ShieldCheck size={16} /> };
    };
    const certInfo = getCertStatus();

    return (
        <Box sx={{ width: '100%' }}> {/* Garante que o box pai ocupe o espaço correto */}
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                <Box>
                    <Typography variant="h5" fontWeight="bold">Configurações da Empresa</Typography>
                    <Typography variant="body2" color="text.secondary">Gerencie os dados e parâmetros do ambiente atual.</Typography>
                </Box>
                <Button variant="contained" startIcon={<Save size={20} />} onClick={handleSave} disabled={loading} size="large">
                    Salvar Alterações
                </Button>
            </Box>

            <Paper sx={{ width: '100%', mb: 2, borderRadius: 2 }}>
                <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}>
                    <Tab label="Dados Cadastrais" icon={<Building2 size={18} />} iconPosition="start" />
                    <Tab label="Endereço & Contato" icon={<MapPin size={18} />} iconPosition="start" />
                    <Tab label="Certificado Digital" icon={<ShieldCheck size={18} />} iconPosition="start" />
                    <Tab label="Parâmetros WMS" icon={<Settings size={18} />} iconPosition="start" />
                </Tabs>

                <Box sx={{ p: 4 }}>
                    {/* ABA 1: DADOS CADASTRAIS */}
                    {activeTab === 0 && (
                        <Grid container spacing={3}>
                            <Grid item xs={12} display="flex" justifyContent="flex-end">
                                <Button variant="outlined" startIcon={consultando ? <CircularProgress size={16} /> : <Search size={16} />} onClick={handleConsultarSefaz} disabled={consultando}>
                                    Consultar na SEFAZ
                                </Button>
                            </Grid>
                            <Grid item xs={12} sm={2}>
                                <TextField select label="UF Sede" fullWidth value={form.uf} onChange={e => setForm({ ...form, uf: e.target.value })}>
                                    {ESTADOS_BR.map(u => <MenuItem key={u} value={u}>{u}</MenuItem>)}
                                </TextField>
                            </Grid>
                            <Grid item xs={12} sm={4}>
                                <TextField label="CNPJ" fullWidth value={form.cnpj} onChange={e => setForm({ ...form, cnpj: e.target.value })} />
                            </Grid>
                            <Grid item xs={12} sm={3}>
                                <TextField label="Insc. Estadual" fullWidth value={form.inscricaoEstadual} onChange={e => setForm({ ...form, inscricaoEstadual: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>
                            <Grid item xs={12} sm={3}>
                                <TextField select label="Regime (CRT)" fullWidth value={form.regimeTributario} onChange={e => setForm({ ...form, regimeTributario: e.target.value })}>
                                    <MenuItem value="1">1 - Simples Nacional</MenuItem>
                                    <MenuItem value="2">2 - Simples (Excesso)</MenuItem>
                                    <MenuItem value="3">3 - Regime Normal</MenuItem>
                                    <MenuItem value="4">4 - MEI</MenuItem>
                                </TextField>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField label="Razão Social" fullWidth value={form.razaoSocial} onChange={e => setForm({ ...form, razaoSocial: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField label="Nome Fantasia" fullWidth value={form.nomeFantasia} onChange={e => setForm({ ...form, nomeFantasia: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>
                            <Grid item xs={12} sm={4}>
                                <TextField label="CNAE Principal" fullWidth value={form.cnaePrincipal} onChange={e => setForm({ ...form, cnaePrincipal: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>
                            <Grid item xs={12} sm={4}>
                                <TextField label="Insc. Municipal" fullWidth value={form.inscricaoMunicipal} onChange={e => setForm({ ...form, inscricaoMunicipal: e.target.value })} />
                            </Grid>
                            <Grid item xs={12} sm={4}>
                                <TextField label="Website" fullWidth value={form.website} onChange={e => setForm({ ...form, website: e.target.value })} InputProps={{ startAdornment: <InputAdornment position="start"><Globe size={16} /></InputAdornment> }} />
                            </Grid>
                        </Grid>
                    )}

                    {/* ABA 2: ENDEREÇO */}
                    {activeTab === 1 && (
                        <Grid container spacing={3}>
                            <Grid item xs={12} sm={3}>
                                <TextField label="CEP" fullWidth value={form.cep} onChange={e => setForm({ ...form, cep: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>
                            <Grid item xs={12} sm={7}>
                                <TextField label="Logradouro" fullWidth value={form.logradouro} onChange={e => setForm({ ...form, logradouro: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>
                            <Grid item xs={12} sm={2}>
                                <TextField label="Número" fullWidth value={form.numero} onChange={e => setForm({ ...form, numero: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>
                            <Grid item xs={12} sm={5}>
                                <TextField label="Bairro" fullWidth value={form.bairro} onChange={e => setForm({ ...form, bairro: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>
                            <Grid item xs={12} sm={5}>
                                <TextField label="Cidade" fullWidth value={form.cidade} onChange={e => setForm({ ...form, cidade: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>
                            <Grid item xs={12} sm={2}>
                                <TextField disabled label="UF" fullWidth value={form.uf} InputLabelProps={{ shrink: true }} />
                            </Grid>
                            <Grid item xs={12}><Divider sx={{ borderStyle: 'dashed' }} /></Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField label="Email Comercial" fullWidth value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} InputProps={{ startAdornment: <InputAdornment position="start"><Mail size={16} /></InputAdornment> }} />
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField label="Telefone" fullWidth value={form.telefone} onChange={e => setForm({ ...form, telefone: e.target.value })} InputProps={{ startAdornment: <InputAdornment position="start"><Phone size={16} /></InputAdornment> }} />
                            </Grid>
                        </Grid>
                    )}

                    {/* ABA 3: CERTIFICADO */}
                    {activeTab === 2 && (
                        <Box maxWidth={600}>
                            <Box sx={{ p: 2, bgcolor: '#f8fafc', borderRadius: 2, border: '1px solid #e2e8f0', mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <Box>
                                    <Typography variant="subtitle2" color="text.secondary">Certificado Atual</Typography>
                                    <Typography variant="body1" fontWeight={600}>{form.nomeCertificado || 'Nenhum certificado instalado'}</Typography>
                                </Box>
                                <Chip label={certInfo.label} color={certInfo.color} icon={certInfo.icon} />
                            </Box>

                            <Typography variant="subtitle2" gutterBottom>Atualizar Arquivo (.pfx)</Typography>
                            <Box display="flex" gap={2} alignItems="center">
                                <Button component="label" variant="outlined" startIcon={<Upload size={16} />}>
                                    {certFile ? certFile.name : "Selecionar Arquivo"}
                                    <input type="file" hidden accept=".pfx" onChange={e => setCertFile(e.target.files[0])} />
                                </Button>
                                <TextField type="password" size="small" label="Senha do PFX" value={certSenha} onChange={e => setCertSenha(e.target.value)} sx={{ flexGrow: 1 }} />
                                <Button variant="contained" disabled={!certFile || uploadingCert} onClick={handleUploadCert}>
                                    {uploadingCert ? <CircularProgress size={20} /> : "Enviar"}
                                </Button>
                            </Box>
                        </Box>
                    )}

                    {/* ABA 4: PARÂMETROS */}
                    {activeTab === 3 && (
                        <Grid container spacing={3}>
                            <Grid item xs={12}>
                                <Paper variant="outlined" sx={{ p: 2 }}>
                                    <FormControlLabel control={<Switch checked={form.recebimentoCegoObrigatorio} onChange={e => setForm({ ...form, recebimentoCegoObrigatorio: e.target.checked })} />} label={<Typography fontWeight={500}>Recebimento Cego Obrigatório</Typography>} />
                                    <Typography variant="caption" display="block" color="text.secondary" ml={4}>
                                        Se ativo, oculta as quantidades da nota fiscal na tela de conferência do operador.
                                    </Typography>
                                </Paper>
                            </Grid>
                            <Grid item xs={12}>
                                <Paper variant="outlined" sx={{ p: 2, borderColor: 'error.main', bgcolor: '#fef2f2' }}>
                                    <FormControlLabel control={<Switch color="error" checked={form.permiteEstoqueNegativo} onChange={e => setForm({ ...form, permiteEstoqueNegativo: e.target.checked })} />} label={<Typography fontWeight={500} color="error.main">Permitir Estoque Negativo</Typography>} />
                                    <Typography variant="caption" display="block" color="error.main" ml={4}>
                                        Não recomendado. Permite expedir produtos mesmo sem saldo no sistema.
                                    </Typography>
                                </Paper>
                            </Grid>
                        </Grid>
                    )}
                </Box>
            </Paper>
        </Box>
    );
};

export default MinhaEmpresa;