import { useState, useEffect } from 'react';
import {
    Box, TextField, Button, Grid, Paper, Typography, MenuItem,
    CircularProgress, Divider, Chip, FormControlLabel, Switch, Tabs, Tab, InputAdornment
} from '@mui/material';
import {
    Save, Search, Upload, ShieldCheck, Mail, Phone,
    CalendarCheck, AlertTriangle, MapPin, Building2, Settings, Globe
} from 'lucide-react';
import { toast } from 'react-toastify';
import {
    getEmpresaConfig, updateEmpresaConfig,
    consultarCnpjSefaz, uploadCertificadoConfig
} from '../../services/integracaoService';
import { getConfiguracoes, updateConfiguracao } from '../../services/configService';
import SearchableSelect from '../../components/SearchableSelect';
import dayjs from 'dayjs';

const ESTADOS_BR = ['AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'];
const UF_OPTIONS = ESTADOS_BR.map(uf => ({ value: uf, label: uf }));

const CRT_OPTIONS = [
    { value: '1', label: '1 - Simples Nacional' },
    { value: '2', label: '2 - Simples (Excesso)' },
    { value: '3', label: '3 - Regime Normal' },
    { value: '4', label: '4 - MEI' }
];

const MinhaEmpresa = () => {
    const [activeTab, setActiveTab] = useState(0);
    const [loading, setLoading] = useState(false);
    const [consultando, setConsultando] = useState(false);

    // Estado dos Dados da Empresa (Identidade)
    const [form, setForm] = useState({
        razaoSocial: '', nomeFantasia: '', cnpj: '', inscricaoEstadual: '', inscricaoMunicipal: '', cnaePrincipal: '', regimeTributario: '3',
        email: '', telefone: '', website: '',
        cep: '', logradouro: '', numero: '', complemento: '', bairro: '', cidade: '', uf: 'SP',
        nomeCertificado: '', validadeCertificado: null
    });

    // Estado das Configurações do Sistema (Lista Dinâmica)
    const [configs, setConfigs] = useState([]);

    // Estado do Certificado
    const [certFile, setCertFile] = useState(null);
    const [certSenha, setCertSenha] = useState('');
    const [uploadingCert, setUploadingCert] = useState(false);

    useEffect(() => { load(); }, []);

    const load = async () => {
        try {
            // Carrega em paralelo: Dados da Empresa + Lista de Configs do Sistema
            const [empresaRes, configRes] = await Promise.all([
                getEmpresaConfig(),
                getConfiguracoes()
            ]);

            // Atualiza formulário da empresa
            setForm(prev => ({ ...prev, ...empresaRes, uf: empresaRes.uf || 'SP' }));

            // Atualiza lista de configurações dinâmicas
            // configRes deve ser um array de objetos { chave, valor, descricao, tipo }
            setConfigs(configRes || []);

        } catch (e) {
            console.error(e);
            toast.error("Erro ao carregar dados. Verifique a conexão.");
        }
    };

    const handleSave = async () => {
        setLoading(true);
        try {
            // 1. Salva Dados da Empresa
            await updateEmpresaConfig(form);

            // 2. Salva Configurações do Sistema (Itera e salva uma a uma)
            // Idealmente o backend poderia ter um endpoint de update em lote, mas este loop funciona bem para poucas configs.
            const configPromises = configs.map(conf =>
                updateConfiguracao(conf.chave, conf.valor)
            );
            await Promise.all(configPromises);

            toast.success("Dados e parâmetros salvos com sucesso!");
        } catch (e) {
            console.error(e);
            toast.error("Erro ao salvar alterações.");
        } finally {
            setLoading(false);
        }
    };

    // Atualiza o estado local de uma configuração específica quando o usuário mexe na tela
    const handleConfigChange = (chave, novoValor) => {
        setConfigs(prev => prev.map(c =>
            c.chave === chave ? { ...c, valor: String(novoValor) } : c
        ));
    };

    const handleUploadCert = async () => {
        if (!certFile || !certSenha) return toast.warning("Informe o arquivo .pfx e a senha.");

        setUploadingCert(true);
        try {
            await uploadCertificadoConfig(certFile, certSenha);
            toast.success("Certificado enviado e validado!");

            // Recarrega para atualizar a validade na tela
            const dadosAtualizados = await getEmpresaConfig();
            setForm(prev => ({ ...prev, ...dadosAtualizados }));

            setCertFile(null);
            setCertSenha('');
        } catch (e) {
            toast.error("Erro ao salvar certificado. Verifique a senha.");
        } finally {
            setUploadingCert(false);
        }
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

                // Endereço
                cep: dados.cep || prev.cep,
                logradouro: dados.logradouro || prev.logradouro,
                numero: dados.numero || prev.numero,
                complemento: dados.complemento || prev.complemento,
                bairro: dados.bairro || prev.bairro,
                cidade: dados.cidade || prev.cidade,
                uf: dados.uf || prev.uf
            }));

            toast.success("Dados atualizados via SEFAZ!");
        } catch (e) {
            toast.error("Erro na consulta SEFAZ. Verifique o certificado.");
        } finally {
            setConsultando(false);
        }
    };

    const getCertStatus = () => {
        if (!form.nomeCertificado) return { label: 'Não Configurado', color: 'default', icon: <AlertTriangle size={16} /> };

        const validade = dayjs(form.validadeCertificado);
        const dias = validade.diff(dayjs(), 'day');

        if (dias < 0) return { label: 'Vencido', color: 'error', icon: <AlertTriangle size={16} /> };
        if (dias < 30) return { label: `Vence em ${dias} dias`, color: 'warning', icon: <AlertTriangle size={16} /> };
        return { label: `Válido até ${validade.format('DD/MM/YYYY')}`, color: 'success', icon: <ShieldCheck size={16} /> };
    };
    const certInfo = getCertStatus();

    // Formata a chave (ex: ESTOQUE_NEGATIVO -> Estoque Negativo)
    const formatLabel = (chave) => {
        return chave.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
    };

    return (
        <Box sx={{ width: '100%' }}>
            {/* CABEÇALHO */}
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                <Box>
                    <Typography variant="h5" fontWeight="bold" color="primary.main">Minha Empresa</Typography>
                    <Typography variant="body2" color="text.secondary">Gerencie identidade, certificado e regras do sistema.</Typography>
                </Box>
                <Button
                    variant="contained"
                    size="large"
                    startIcon={<Save size={20} />}
                    onClick={handleSave}
                    disabled={loading}
                >
                    {loading ? "Salvando..." : "Salvar Tudo"}
                </Button>
            </Box>

            <Paper sx={{ width: '100%', mb: 2, borderRadius: 2, overflow: 'hidden' }}>
                <Tabs
                    value={activeTab}
                    onChange={(e, v) => setActiveTab(v)}
                    sx={{ borderBottom: 1, borderColor: 'divider', px: 2, bgcolor: '#f8fafc' }}
                >
                    <Tab label="Dados Cadastrais" icon={<Building2 size={18} />} iconPosition="start" />
                    <Tab label="Endereço & Contato" icon={<MapPin size={18} />} iconPosition="start" />
                    <Tab label="Certificado Digital" icon={<ShieldCheck size={18} />} iconPosition="start" />
                    <Tab label="Parâmetros WMS" icon={<Settings size={18} />} iconPosition="start" />
                </Tabs>

                <Box sx={{ p: 4 }}>

                    {/* ABA 0: DADOS CADASTRAIS */}
                    {activeTab === 0 && (
                        <Grid container spacing={3}>
                            <Grid item xs={12} display="flex" justifyContent="flex-end">
                                <Button
                                    variant="outlined"
                                    onClick={handleConsultarSefaz}
                                    disabled={consultando || !form.cnpj}
                                    startIcon={consultando ? <CircularProgress size={16} /> : <Search size={16} />}
                                >
                                    Auto-completar via SEFAZ
                                </Button>
                            </Grid>

                            {/* UF COM PESQUISA */}
                            <Grid item xs={12} sm={2}>
                                <SearchableSelect
                                    label="UF Sede"
                                    value={form.uf || 'SP'}
                                    onChange={e => setForm({ ...form, uf: e.target.value })}
                                    options={UF_OPTIONS}
                                />
                            </Grid>
                            <Grid item xs={12} sm={4}>
                                <TextField label="CNPJ" fullWidth value={form.cnpj} onChange={e => setForm({ ...form, cnpj: e.target.value })} />
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField label="Razão Social" fullWidth value={form.razaoSocial} onChange={e => setForm({ ...form, razaoSocial: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>

                            <Grid item xs={12} sm={6}>
                                <TextField label="Nome Fantasia" fullWidth value={form.nomeFantasia} onChange={e => setForm({ ...form, nomeFantasia: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>
                            <Grid item xs={12} sm={3}>
                                <TextField label="Inscrição Estadual" fullWidth value={form.inscricaoEstadual} onChange={e => setForm({ ...form, inscricaoEstadual: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>
                            {/* CRT COM PESQUISA */}
                            <Grid item xs={12} sm={3}>
                                <SearchableSelect
                                    label="Regime (CRT)"
                                    value={form.regimeTributario}
                                    onChange={e => setForm({ ...form, regimeTributario: e.target.value })}
                                    options={CRT_OPTIONS}
                                />
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

                    {/* ABA 1: ENDEREÇO E CONTATO */}
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

                            <Grid item xs={12} sm={12}>
                                <TextField label="Complemento" fullWidth value={form.complemento} onChange={e => setForm({ ...form, complemento: e.target.value })} InputLabelProps={{ shrink: true }} />
                            </Grid>

                            <Grid item xs={12}><Divider sx={{ borderStyle: 'dashed' }} /></Grid>

                            <Grid item xs={12} sm={6}>
                                <TextField label="Email Comercial" fullWidth value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} InputProps={{ startAdornment: <InputAdornment position="start"><Mail size={16} /></InputAdornment> }} />
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField label="Telefone / Celular" fullWidth value={form.telefone} onChange={e => setForm({ ...form, telefone: e.target.value })} InputProps={{ startAdornment: <InputAdornment position="start"><Phone size={16} /></InputAdornment> }} />
                            </Grid>
                        </Grid>
                    )}

                    {/* ABA 2: CERTIFICADO */}
                    {activeTab === 2 && (
                        <Box maxWidth={600}>
                            <Paper variant="outlined" sx={{ p: 2, mb: 3, bgcolor: '#f8fafc', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <Box>
                                    <Typography variant="caption" color="text.secondary">Arquivo Instalado</Typography>
                                    <Typography variant="body2" fontWeight={600} mt={0.5}>
                                        {form.nomeCertificado || 'Nenhum certificado configurado'}
                                    </Typography>
                                </Box>
                                <Chip label={certInfo.label} color={certInfo.color} icon={certInfo.icon} size="small" />
                            </Paper>

                            <Typography variant="subtitle2" gutterBottom>Atualizar Certificado (.pfx)</Typography>
                            <Box display="flex" flexDirection="column" gap={2}>
                                <Button component="label" variant="outlined" fullWidth sx={{ justifyContent: 'flex-start' }}>
                                    <Upload size={18} style={{ marginRight: 10 }} />
                                    {certFile ? certFile.name : "Clique para selecionar o arquivo .pfx"}
                                    <input type="file" hidden accept=".pfx" onChange={e => setCertFile(e.target.files[0])} />
                                </Button>

                                <TextField
                                    type="password"
                                    size="small"
                                    label="Senha do Certificado"
                                    fullWidth
                                    value={certSenha}
                                    onChange={e => setCertSenha(e.target.value)}
                                />

                                <Box display="flex" justifyContent="flex-end" mt={1}>
                                    <Button
                                        variant="contained"
                                        onClick={handleUploadCert}
                                        disabled={uploadingCert || !certFile || !certSenha}
                                    >
                                        {uploadingCert ? <CircularProgress size={20} color="inherit" /> : "Enviar e Validar"}
                                    </Button>
                                </Box>
                            </Box>
                        </Box>
                    )}

                    {/* ABA 3: PARÂMETROS DO SISTEMA (DINÂMICO) */}
                    {activeTab === 3 && (
                        <Grid container spacing={3}>
                            {configs.length > 0 ? configs.map((conf) => (
                                <Grid item xs={12} key={conf.chave}>
                                    <Paper
                                        variant="outlined"
                                        sx={{
                                            p: 2,
                                            borderColor: conf.chave.includes('NEGATIVO') ? 'error.light' : 'divider',
                                            bgcolor: conf.chave.includes('NEGATIVO') ? '#fef2f2' : 'transparent',
                                            transition: '0.2s',
                                            '&:hover': { borderColor: 'primary.main' }
                                        }}
                                    >
                                        <Box display="flex" alignItems="center" justifyContent="space-between">
                                            <Box>
                                                {/* Label Dinâmico */}
                                                <Typography fontWeight={600} color={conf.chave.includes('NEGATIVO') ? 'error.main' : 'text.primary'}>
                                                    {formatLabel(conf.chave)}
                                                </Typography>
                                                {/* Descrição Dinâmica */}
                                                <Typography variant="caption" color="text.secondary">
                                                    {conf.descricao}
                                                </Typography>
                                            </Box>

                                            {/* Renderização Condicional do Input */}
                                            <Box>
                                                {conf.tipo === 'BOOLEAN' ? (
                                                    <Switch
                                                        checked={conf.valor === 'true'}
                                                        onChange={e => handleConfigChange(conf.chave, e.target.checked)}
                                                        color={conf.chave.includes('NEGATIVO') ? 'error' : 'primary'}
                                                    />
                                                ) : conf.tipo === 'INTEGER' ? (
                                                    <TextField
                                                        type="number"
                                                        size="small"
                                                        value={conf.valor}
                                                        onChange={e => handleConfigChange(conf.chave, e.target.value)}
                                                        sx={{ width: 100 }}
                                                    />
                                                ) : (
                                                    <TextField
                                                        size="small"
                                                        value={conf.valor}
                                                        onChange={e => handleConfigChange(conf.chave, e.target.value)}
                                                        sx={{ width: 200 }}
                                                    />
                                                )}
                                            </Box>
                                        </Box>
                                    </Paper>
                                </Grid>
                            )) : (
                                <Grid item xs={12}>
                                    <Box textAlign="center" py={4}>
                                        <Typography color="text.secondary">Nenhum parâmetro de sistema encontrado.</Typography>
                                    </Box>
                                </Grid>
                            )}
                        </Grid>
                    )}
                </Box>
            </Paper>
        </Box>
    );
};

export default MinhaEmpresa;