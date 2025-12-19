import { useState, useEffect } from 'react';
import {
    Box, TextField, Button, Grid, Paper, Typography, InputAdornment,
    MenuItem, CircularProgress, Divider, Chip,
    FormControlLabel, Switch
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

// Lista completa de Estados Brasileiros
const ESTADOS_BR = [
    'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS',
    'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
];

const EmpresaDados = ({ onBack }) => {
    const [loading, setLoading] = useState(false);
    const [consultando, setConsultando] = useState(false);

    // Estado do Formulário Completo
    const [form, setForm] = useState({
        razaoSocial: '', nomeFantasia: '',
        cnpj: '', inscricaoEstadual: '', inscricaoMunicipal: '',
        cnaePrincipal: '', regimeTributario: '3', // 3 = Normal (Padrão)

        // Contato e Endereço
        email: '', telefone: '', website: '',
        cep: '', logradouro: '', numero: '', complemento: '',
        bairro: '', cidade: '', uf: 'SP', // UF Única para Busca e Endereço

        // Metadados Certificado (Leitura)
        nomeCertificado: '', validadeCertificado: null,

        // Parâmetros
        recebimentoCegoObrigatorio: true,
        permiteEstoqueNegativo: false
    });

    // Estado do Upload
    const [certFile, setCertFile] = useState(null);
    const [certSenha, setCertSenha] = useState('');
    const [uploadingCert, setUploadingCert] = useState(false);

    useEffect(() => { load(); }, []);

    const load = async () => {
        try {
            const data = await getEmpresaConfig();
            // Garante valores padrão e mantém UF 'SP' se vier nulo do banco
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

    // --- LÓGICA DO CERTIFICADO ---
    const handleUploadCert = async () => {
        if (!certFile || !certSenha) return toast.warning("Informe o arquivo .pfx e a senha.");

        setUploadingCert(true);
        try {
            await uploadCertificadoConfig(certFile, certSenha);
            toast.success("Certificado configurado e validado!");
            setCertFile(null);
            setCertSenha('');
            load(); // Recarrega para atualizar os metadados (validade, nome)
        } catch (e) {
            console.error(e);
            toast.error("Erro ao salvar certificado. Verifique a senha.");
        } finally {
            setUploadingCert(false);
        }
    };

    const getCertStatus = () => {
        if (!form.nomeCertificado) return { label: 'Não Instalado', color: 'default', icon: <AlertTriangle size={16} /> };

        const validade = dayjs(form.validadeCertificado);
        const diasRestantes = validade.diff(dayjs(), 'day');

        if (diasRestantes < 0) return { label: 'Vencido', color: 'error', icon: <AlertTriangle size={16} /> };
        if (diasRestantes < 30) return { label: `Vence em ${diasRestantes} dias`, color: 'warning', icon: <AlertTriangle size={16} /> };
        return { label: `Válido até ${validade.format('DD/MM/YYYY')}`, color: 'success', icon: <CalendarCheck size={16} /> };
    };
    const certStatus = getCertStatus();

    // --- LÓGICA DE DADOS / SEFAZ ---
    const handleConsultarProprio = async () => {
        const cnpjLimpo = form.cnpj.replace(/\D/g, '');
        if (cnpjLimpo.length !== 14) return toast.warning("CNPJ inválido (requer 14 dígitos).");

        // Bloqueio específico para Maranhão (Sefaz não fornece Webservice público padrão)
        if (form.uf === 'MA') {
            return toast.warning("Consulta automática indisponível para o estado do Maranhão (MA).");
        }

        setConsultando(true);
        try {
            // Usa a UF selecionada no formulário para a busca
            const dados = await consultarCnpjSefaz(form.uf, cnpjLimpo);

            setForm(prev => ({
                ...prev,
                razaoSocial: dados.razaoSocial,
                nomeFantasia: dados.nomeFantasia,
                inscricaoEstadual: dados.ie && dados.ie !== 'ISENTO' ? dados.ie : prev.inscricaoEstadual,

                // Mapeamento vindo do Backend (1, 2, 3 ou 4)
                regimeTributario: dados.regimeTributario || prev.regimeTributario,
                cnaePrincipal: dados.cnaePrincipal || prev.cnaePrincipal,

                // Endereço
                cep: dados.cep,
                logradouro: dados.logradouro,
                numero: dados.numero,
                complemento: dados.complemento,
                bairro: dados.bairro,
                cidade: dados.cidade,

                // CORREÇÃO CRÍTICA: Se a SEFAZ retornar UF nula ou vazia, mantemos a UF que o usuário selecionou
                uf: dados.uf || prev.uf
            }));

            toast.success("Dados preenchidos via SEFAZ!");
        } catch (error) {
            console.error(error);
            toast.error("Erro na consulta. Verifique se o certificado está válido e a UF correta.");
        } finally {
            setConsultando(false);
        }
    };

    const handleSave = async () => {
        setLoading(true);
        try {
            await updateEmpresaConfig(form);
            toast.success("Dados da empresa salvos com sucesso!");
        } catch (e) {
            toast.error("Erro ao salvar alterações.");
        } finally {
            setLoading(false);
        }
    };

    const isMa = form.uf === 'MA';

    return (
        <Box mt={2} mb={5}>
            {/* CABEÇALHO */}
            <Box display="flex" alignItems="center" justifyContent="space-between" mb={3}>
                <Box display="flex" alignItems="center" gap={2}>
                    <Button variant="outlined" startIcon={<ArrowLeft size={18} />} onClick={onBack} sx={{ color: 'text.secondary', borderColor: 'divider' }}>
                        Voltar
                    </Button>
                    <Box>
                        <Typography variant="h5" fontWeight="bold" color="primary.main">
                            {form.nomeFantasia || form.razaoSocial || "Configuração da Empresa"}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            Gerencie dados fiscais, endereço e certificado digital.
                        </Typography>
                    </Box>
                </Box>
                <Can I="CONFIG_GERENCIAR">
                    <Button
                        variant="contained"
                        size="large"
                        startIcon={<Save size={20} />}
                        onClick={handleSave}
                        disabled={loading}
                    >
                        Salvar Alterações
                    </Button>
                </Can>
            </Box>

            <Grid container spacing={3}>

                {/* COLUNA ESQUERDA (DADOS) */}
                <Grid item xs={12} md={8}>

                    {/* CARD 1: DADOS FISCAIS */}
                    <Paper elevation={0} sx={{ p: 3, border: '1px solid #e0e0e0', borderRadius: 2, mb: 3 }}>
                        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                            <Typography variant="subtitle1" fontWeight="bold" display="flex" alignItems="center" gap={1}>
                                <FileText size={18} className="text-blue-600" /> Dados Fiscais
                            </Typography>
                            <Button
                                variant="outlined" size="small"
                                startIcon={consultando ? <CircularProgress size={14} /> : <Search size={14} />}
                                onClick={handleConsultarProprio}
                                disabled={consultando || !form.cnpj || isMa}
                                sx={{ textTransform: 'none' }}
                            >
                                {isMa ? "Indisponível para MA" : `Auto-completar (SEFAZ ${form.uf})`}
                            </Button>
                        </Box>
                        <Divider sx={{ mb: 3 }} />

                        <Grid container spacing={2}>
                            {/* UF UNIFICADA EM DESTAQUE */}
                            <Grid item xs={12} sm={2}>
                                <TextField select label="UF Sede" fullWidth value={form.uf || 'SP'} onChange={e => setForm({ ...form, uf: e.target.value })}>
                                    {ESTADOS_BR.map(uf => (
                                        <MenuItem key={uf} value={uf}>{uf}</MenuItem>
                                    ))}
                                </TextField>
                            </Grid>

                            <Grid item xs={12} sm={4}>
                                <TextField label="CNPJ" fullWidth value={form.cnpj} onChange={e => setForm({ ...form, cnpj: e.target.value })} />
                            </Grid>
                            <Grid item xs={12} sm={3}>
                                <TextField label="Insc. Estadual" fullWidth value={form.inscricaoEstadual} onChange={e => setForm({ ...form, inscricaoEstadual: e.target.value })} />
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
                                <TextField label="Razão Social" fullWidth required value={form.razaoSocial} onChange={e => setForm({ ...form, razaoSocial: e.target.value })} />
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField label="Nome Fantasia" fullWidth value={form.nomeFantasia} onChange={e => setForm({ ...form, nomeFantasia: e.target.value })} />
                            </Grid>

                            <Grid item xs={12} sm={4}>
                                <TextField label="CNAE Principal" fullWidth value={form.cnaePrincipal} onChange={e => setForm({ ...form, cnaePrincipal: e.target.value })} />
                            </Grid>
                            <Grid item xs={12} sm={4}>
                                <TextField label="Insc. Municipal" fullWidth value={form.inscricaoMunicipal} onChange={e => setForm({ ...form, inscricaoMunicipal: e.target.value })} />
                            </Grid>
                            <Grid item xs={12} sm={4}>
                                <TextField label="Website" fullWidth value={form.website} onChange={e => setForm({ ...form, website: e.target.value })} InputProps={{ startAdornment: <InputAdornment position="start"><Globe size={16} /></InputAdornment> }} />
                            </Grid>
                        </Grid>
                    </Paper>

                    {/* CARD 2: ENDEREÇO E CONTATO */}
                    <Paper elevation={0} sx={{ p: 3, border: '1px solid #e0e0e0', borderRadius: 2 }}>
                        <Typography variant="subtitle1" fontWeight="bold" display="flex" alignItems="center" gap={1} mb={2}>
                            <MapPin size={18} className="text-orange-600" /> Endereço e Contato
                        </Typography>
                        <Divider sx={{ mb: 3 }} />

                        <Grid container spacing={2}>
                            <Grid item xs={12} sm={3}>
                                <TextField label="CEP" fullWidth value={form.cep} onChange={e => setForm({ ...form, cep: e.target.value })} />
                            </Grid>
                            <Grid item xs={12} sm={7}>
                                <TextField label="Logradouro" fullWidth value={form.logradouro} onChange={e => setForm({ ...form, logradouro: e.target.value })} />
                            </Grid>
                            <Grid item xs={12} sm={2}>
                                <TextField label="Número" fullWidth value={form.numero} onChange={e => setForm({ ...form, numero: e.target.value })} />
                            </Grid>

                            <Grid item xs={12} sm={5}>
                                <TextField label="Bairro" fullWidth value={form.bairro} onChange={e => setForm({ ...form, bairro: e.target.value })} />
                            </Grid>
                            <Grid item xs={12} sm={4}>
                                <TextField label="Cidade" fullWidth value={form.cidade} onChange={e => setForm({ ...form, cidade: e.target.value })} />
                            </Grid>
                            <Grid item xs={12} sm={3}>
                                <TextField label="Complemento" fullWidth value={form.complemento} onChange={e => setForm({ ...form, complemento: e.target.value })} />
                            </Grid>

                            <Grid item xs={12}><Divider sx={{ borderStyle: 'dashed', my: 1 }} /></Grid>

                            <Grid item xs={12} sm={6}>
                                <TextField label="Email Comercial" fullWidth value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} InputProps={{ startAdornment: <InputAdornment position="start"><Mail size={16} /></InputAdornment> }} />
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField label="Telefone / Celular" fullWidth value={form.telefone} onChange={e => setForm({ ...form, telefone: e.target.value })} InputProps={{ startAdornment: <InputAdornment position="start"><Phone size={16} /></InputAdornment> }} />
                            </Grid>
                        </Grid>
                    </Paper>

                </Grid>

                {/* COLUNA DIREITA (CERTIFICADO E CONFIGS) */}
                <Grid item xs={12} md={4}>

                    {/* CARD 3: CERTIFICADO */}
                    <Paper elevation={0} sx={{ p: 3, border: '1px solid #e0e0e0', borderRadius: 2, mb: 3, bgcolor: '#fafafa' }}>
                        <Typography variant="subtitle1" fontWeight="bold" display="flex" alignItems="center" gap={1} mb={2}>
                            <ShieldCheck size={18} className="text-green-600" /> Certificado Digital A1
                        </Typography>

                        {/* Status Box */}
                        <Box sx={{ p: 2, bgcolor: 'white', borderRadius: 1, border: '1px solid #e0e0e0', mb: 2 }}>
                            <Box display="flex" justifyContent="space-between" alignItems="center">
                                <Typography variant="caption" color="text.secondary">Situação</Typography>
                                <Chip label={certStatus.label} color={certStatus.color} icon={certStatus.icon} size="small" />
                            </Box>
                            {form.nomeCertificado ? (
                                <Typography variant="body2" fontWeight={600} mt={1} sx={{ wordBreak: 'break-all' }}>
                                    Arquivo: {form.nomeCertificado}
                                </Typography>
                            ) : (
                                <Typography variant="body2" color="text.secondary" mt={1}>Nenhum arquivo identificado.</Typography>
                            )}
                        </Box>

                        <Divider sx={{ mb: 2 }}>Atualizar</Divider>

                        <Box display="flex" flexDirection="column" gap={2}>
                            <Button
                                component="label"
                                variant="outlined"
                                fullWidth
                                startIcon={<Upload size={16} />}
                                sx={{ justifyContent: 'flex-start', textTransform: 'none', color: '#666', borderColor: '#ccc' }}
                            >
                                {certFile ? certFile.name : "Selecionar Arquivo .pfx"}
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

                            <Can I="CONFIG_GERENCIAR">
                                <Button
                                    variant="contained"
                                    color="primary"
                                    fullWidth
                                    onClick={handleUploadCert}
                                    disabled={!certFile || uploadingCert}
                                >
                                    {uploadingCert ? <CircularProgress size={20} color="inherit" /> : "Enviar e Testar"}
                                </Button>
                            </Can>
                        </Box>
                    </Paper>

                    {/* CARD 4: PARÂMETROS GERAIS */}
                    <Paper elevation={0} sx={{ p: 3, border: '1px solid #e0e0e0', borderRadius: 2 }}>
                        <Typography variant="subtitle1" fontWeight="bold" display="flex" alignItems="center" gap={1} mb={2}>
                            <Settings size={18} className="text-gray-600" /> Parâmetros WMS
                        </Typography>
                        <Divider sx={{ mb: 2 }} />

                        <Grid container spacing={2}>
                            <Grid item xs={12}>
                                <FormControlLabel
                                    control={
                                        <Switch
                                            checked={form.recebimentoCegoObrigatorio}
                                            onChange={(e) => setForm({ ...form, recebimentoCegoObrigatorio: e.target.checked })}
                                        />
                                    }
                                    label={<Typography variant="body2">Recebimento Cego Obrigatório</Typography>}
                                />
                                <Typography variant="caption" display="block" color="text.secondary" ml={4}>
                                    Oculta as quantidades da NF na conferência de entrada.
                                </Typography>
                            </Grid>

                            <Grid item xs={12}>
                                <FormControlLabel
                                    control={
                                        <Switch
                                            color="error"
                                            checked={form.permiteEstoqueNegativo}
                                            onChange={(e) => setForm({ ...form, permiteEstoqueNegativo: e.target.checked })}
                                        />
                                    }
                                    label={<Typography variant="body2">Permitir Estoque Negativo</Typography>}
                                />
                                <Typography variant="caption" display="block" color="text.secondary" ml={4}>
                                    Não recomendado. Permite expedir mais do que o saldo atual.
                                </Typography>
                            </Grid>
                        </Grid>
                    </Paper>

                </Grid>
            </Grid>
        </Box>
    );
};

export default EmpresaDados;