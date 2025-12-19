import { useState, useEffect } from 'react';
import {
    Box, TextField, Button, Grid, Paper, Typography, InputAdornment,
    MenuItem, CircularProgress, Divider, Chip, FormControlLabel, Switch
} from '@mui/material';
import {
    Save, Search, Upload, ShieldCheck, ArrowLeft, Mail, Phone,
    CalendarCheck, AlertTriangle, MapPin, FileText, Settings, Globe
} from 'lucide-react';
import { toast } from 'react-toastify';
import {
    getEmpresaConfig, updateEmpresaConfig,
    consultarCnpjSefaz, uploadCertificadoConfig
} from '../../services/integracaoService';
import Can from '../../components/Can';
import dayjs from 'dayjs';

const ESTADOS_BR = [
    'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS',
    'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
];

const EmpresaDados = ({ onBack }) => {
    const [loading, setLoading] = useState(false);
    const [consultando, setConsultando] = useState(false);

    // Estado do Formulário
    const [form, setForm] = useState({
        razaoSocial: '', nomeFantasia: '',
        cnpj: '', inscricaoEstadual: '', inscricaoMunicipal: '',
        cnaePrincipal: '', regimeTributario: '3', // 3 = Normal

        email: '', telefone: '', website: '',
        cep: '', logradouro: '', numero: '', complemento: '',
        bairro: '', cidade: '', uf: 'SP',

        nomeCertificado: '', validadeCertificado: null,
        recebimentoCegoObrigatorio: true, permiteEstoqueNegativo: false
    });

    // Upload State
    const [certFile, setCertFile] = useState(null);
    const [certSenha, setCertSenha] = useState('');
    const [uploadingCert, setUploadingCert] = useState(false);

    useEffect(() => { load(); }, []);

    const load = async () => {
        try {
            const data = await getEmpresaConfig();
            // Garante que não tenhamos campos undefined e preserva a UF do banco
            setForm(prev => ({
                ...prev,
                ...data,
                uf: data.uf || 'SP'
            }));
        } catch (e) {
            console.error(e);
            toast.error("Erro ao carregar dados da empresa.");
        }
    };

    const handleUploadCert = async () => {
        if (!certFile || !certSenha) return toast.warning("Informe arquivo (.pfx) e senha.");
        setUploadingCert(true);
        try {
            await uploadCertificadoConfig(certFile, certSenha);
            toast.success("Certificado atualizado!");
            setCertFile(null); setCertSenha('');
            load();
        } catch (e) {
            toast.error("Erro ao salvar certificado. Verifique a senha.");
        } finally {
            setUploadingCert(false);
        }
    };

    const handleConsultarProprio = async () => {
        const cnpjLimpo = form.cnpj.replace(/\D/g, '');
        if (cnpjLimpo.length !== 14) return toast.warning("CNPJ inválido.");
        if (form.uf === 'MA') return toast.warning("Consulta indisponível para MA.");

        setConsultando(true);
        try {
            const dados = await consultarCnpjSefaz(form.uf, cnpjLimpo);

            // LÓGICA DE PROTEÇÃO: Só sobrescreve se o dado da Sefaz não for vazio.
            // Se vier vazio, mantém o que o usuário já digitou/salvou.
            setForm(prev => ({
                ...prev,
                razaoSocial: dados.razaoSocial || prev.razaoSocial,
                nomeFantasia: dados.nomeFantasia || prev.nomeFantasia,

                // IE: Sefaz às vezes retorna "ISENTO", nesse caso aceitamos.
                inscricaoEstadual: (dados.ie && dados.ie !== 'ISENTO') ? dados.ie : prev.inscricaoEstadual,

                regimeTributario: dados.regimeTributario || prev.regimeTributario,
                cnaePrincipal: dados.cnaePrincipal || prev.cnaePrincipal,

                cep: dados.cep || prev.cep,
                logradouro: dados.logradouro || prev.logradouro,
                numero: dados.numero || prev.numero,
                complemento: dados.complemento || prev.complemento,
                bairro: dados.bairro || prev.bairro,
                cidade: dados.cidade || prev.cidade,

                // Se a SEFAZ retornou a UF correta, atualizamos. Senão mantém a selecionada.
                uf: dados.uf || prev.uf
            }));

            toast.success("Dados preenchidos via SEFAZ!");
        } catch (error) {
            console.error(error);
            toast.error("Erro na consulta. Verifique o Certificado e a UF.");
        } finally {
            setConsultando(false);
        }
    };

    const handleSave = async () => {
        setLoading(true);
        try { await updateEmpresaConfig(form); toast.success("Dados salvos!"); }
        catch (e) { toast.error("Erro ao salvar."); }
        finally { setLoading(false); }
    };

    const getCertStatus = () => {
        if (!form.nomeCertificado) return { label: 'Não Instalado', color: 'default', icon: <AlertTriangle size={16} /> };
        const dias = dayjs(form.validadeCertificado).diff(dayjs(), 'day');
        if (dias < 0) return { label: 'Vencido', color: 'error', icon: <AlertTriangle size={16} /> };
        if (dias < 30) return { label: `Vence em ${dias} dias`, color: 'warning', icon: <AlertTriangle size={16} /> };
        return { label: 'Válido', color: 'success', icon: <ShieldCheck size={16} /> };
    };
    const certInfo = getCertStatus();

    return (
        <Box mt={2} mb={5}>
            {/* Header */}
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                <Button onClick={onBack} startIcon={<ArrowLeft />}>Voltar</Button>
                <Can I="CONFIG_GERENCIAR">
                    <Button variant="contained" onClick={handleSave} disabled={loading} startIcon={<Save />}>
                        Salvar Alterações
                    </Button>
                </Can>
            </Box>

            <Grid container spacing={3}>
                <Grid item xs={12} md={8}>

                    {/* DADOS FISCAIS */}
                    <Paper elevation={0} sx={{ p: 3, border: '1px solid #e0e0e0', borderRadius: 2, mb: 3 }}>
                        <Box display="flex" justifyContent="space-between" mb={2}>
                            <Typography variant="subtitle1" fontWeight="bold" display="flex" gap={1} alignItems="center">
                                <FileText size={18} className="text-blue-600" /> Dados Fiscais
                            </Typography>
                            <Button
                                variant="outlined" size="small" onClick={handleConsultarProprio}
                                disabled={consultando || !form.cnpj}
                                startIcon={consultando ? <CircularProgress size={14} /> : <Search size={14} />}
                            >
                                Auto-completar (SEFAZ {form.uf})
                            </Button>
                        </Box>
                        <Divider sx={{ mb: 3 }} />

                        <Grid container spacing={2}>
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
                    </Paper>

                    {/* ENDEREÇO */}
                    <Paper elevation={0} sx={{ p: 3, border: '1px solid #e0e0e0', borderRadius: 2 }}>
                        <Typography variant="subtitle1" fontWeight="bold" display="flex" gap={1} alignItems="center" mb={2}>
                            <MapPin size={18} className="text-orange-600" /> Endereço e Contato
                        </Typography>

                        <Grid container spacing={2}>
                            <Grid item xs={12} sm={3}>
                                <TextField
                                    label="CEP" fullWidth value={form.cep}
                                    onChange={e => setForm({ ...form, cep: e.target.value })}
                                    InputLabelProps={{ shrink: true }}
                                />
                            </Grid>
                            <Grid item xs={12} sm={7}>
                                <TextField
                                    label="Logradouro" fullWidth value={form.logradouro}
                                    onChange={e => setForm({ ...form, logradouro: e.target.value })}
                                    InputLabelProps={{ shrink: true }}
                                />
                            </Grid>
                            <Grid item xs={12} sm={2}>
                                <TextField
                                    label="Número" fullWidth value={form.numero}
                                    onChange={e => setForm({ ...form, numero: e.target.value })}
                                    InputLabelProps={{ shrink: true }}
                                />
                            </Grid>

                            <Grid item xs={12} sm={5}>
                                <TextField
                                    label="Bairro" fullWidth value={form.bairro}
                                    onChange={e => setForm({ ...form, bairro: e.target.value })}
                                    InputLabelProps={{ shrink: true }}
                                />
                            </Grid>
                            <Grid item xs={12} sm={4}>
                                <TextField
                                    label="Cidade" fullWidth value={form.cidade}
                                    onChange={e => setForm({ ...form, cidade: e.target.value })}
                                    InputLabelProps={{ shrink: true }}
                                />
                            </Grid>
                            <Grid item xs={12} sm={3}>
                                <TextField
                                    label="Complemento" fullWidth value={form.complemento}
                                    onChange={e => setForm({ ...form, complemento: e.target.value })}
                                    InputLabelProps={{ shrink: true }}
                                />
                            </Grid>

                            <Grid item xs={12}><Divider sx={{ borderStyle: 'dashed', my: 1 }} /></Grid>

                            <Grid item xs={12} sm={6}>
                                <TextField label="Email" fullWidth value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} InputProps={{ startAdornment: <InputAdornment position="start"><Mail size={16} /></InputAdornment> }} />
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField label="Telefone" fullWidth value={form.telefone} onChange={e => setForm({ ...form, telefone: e.target.value })} InputProps={{ startAdornment: <InputAdornment position="start"><Phone size={16} /></InputAdornment> }} />
                            </Grid>
                        </Grid>
                    </Paper>
                </Grid>

                {/* COLUNA DIREITA */}
                <Grid item xs={12} md={4}>
                    <Paper elevation={0} sx={{ p: 3, border: '1px solid #e0e0e0', borderRadius: 2, mb: 3, bgcolor: '#fafafa' }}>
                        <Typography variant="subtitle1" fontWeight="bold" display="flex" gap={1} alignItems="center" mb={2}>
                            <ShieldCheck size={18} className="text-green-600" /> Certificado Digital
                        </Typography>

                        <Box sx={{ p: 2, bgcolor: 'white', borderRadius: 1, border: '1px solid #e0e0e0', mb: 2 }}>
                            <Box display="flex" justifyContent="space-between" alignItems="center">
                                <Typography variant="caption" color="text.secondary">Status</Typography>
                                <Chip label={certInfo.label} color={certInfo.color} icon={certInfo.icon} size="small" />
                            </Box>
                            <Typography variant="body2" fontWeight={600} mt={1} sx={{ wordBreak: 'break-all' }}>
                                {form.nomeCertificado || "Nenhum arquivo"}
                            </Typography>
                        </Box>

                        <Button component="label" variant="outlined" fullWidth startIcon={<Upload size={16} />} sx={{ mb: 2, justifyContent: 'flex-start' }}>
                            {certFile ? certFile.name : "Selecionar .pfx"}
                            <input type="file" hidden accept=".pfx" onChange={e => setCertFile(e.target.files[0])} />
                        </Button>
                        <TextField type="password" size="small" label="Senha" fullWidth value={certSenha} onChange={e => setCertSenha(e.target.value)} sx={{ mb: 2 }} />

                        <Can I="CONFIG_GERENCIAR">
                            <Button variant="contained" fullWidth onClick={handleUploadCert} disabled={!certFile || uploadingCert}>
                                {uploadingCert ? <CircularProgress size={20} /> : "Atualizar Certificado"}
                            </Button>
                        </Can>
                    </Paper>

                    <Paper elevation={0} sx={{ p: 3, border: '1px solid #e0e0e0', borderRadius: 2 }}>
                        <Typography variant="subtitle1" fontWeight="bold" display="flex" gap={1} alignItems="center" mb={2}>
                            <Settings size={18} className="text-gray-600" /> Parâmetros
                        </Typography>
                        <FormControlLabel control={<Switch checked={form.recebimentoCegoObrigatorio} onChange={e => setForm({ ...form, recebimentoCegoObrigatorio: e.target.checked })} />} label="Recebimento Cego" />
                        <FormControlLabel control={<Switch checked={form.permiteEstoqueNegativo} onChange={e => setForm({ ...form, permiteEstoqueNegativo: e.target.checked })} color="error" />} label="Estoque Negativo" />
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
};
export default EmpresaDados;