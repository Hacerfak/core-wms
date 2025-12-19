import { useState, useEffect, useContext } from 'react';
import {
    Box, Typography, Paper, Switch, List, ListItem, ListItemText,
    ListItemSecondaryAction, Divider, Tabs, Tab, Button, Card,
    CardContent, Grid, TextField, IconButton, Tooltip, Chip
} from '@mui/material';
import { toast } from 'react-toastify';
import { getConfiguracoes, updateConfiguracao } from '../../services/configService';
import { AuthContext } from '../../contexts/AuthContext';
import { Building2, ServerCog, Settings, Save, LogIn } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Can from '../../components/Can';
import EmpresaDados from './EmpresaDados';

const Configuracoes = () => {
    const { user, switchTenant } = useContext(AuthContext);
    const isAdmin = user?.role === 'ADMIN';
    const navigate = useNavigate();

    const [tabIndex, setTabIndex] = useState(0);
    const [configs, setConfigs] = useState([]);
    const [editValues, setEditValues] = useState({});
    const [modoEdicaoEmpresa, setModoEdicaoEmpresa] = useState(false);

    // --- CORREÇÃO AQUI: Recuperação Robusta do Tenant Atual ---
    // Tenta pegar do LocalStorage, ou falha para o user.tenantId, ou string vazia
    const [currentTenantId, setCurrentTenantId] = useState(
        localStorage.getItem('@App:tenant') || user?.tenantId || ''
    );

    // Monitora mudanças no localStorage (caso mude em outra aba ou no login)
    useEffect(() => {
        const stored = localStorage.getItem('@App:tenant');
        if (stored) setCurrentTenantId(stored);

        // Debug para você ver no console do navegador
        console.log("Tenant Atual (Storage):", stored);
        console.log("Tenant Atual (User):", user?.tenantId);
    }, [user]);

    useEffect(() => { loadData(); }, []);

    const loadData = async () => {
        try {
            const data = await getConfiguracoes();
            setConfigs(data);
            const initialEdits = {};
            data.forEach(c => initialEdits[c.chave] = c.valor);
            setEditValues(initialEdits);
        } catch (error) {
            console.log("Erro ao carregar configs globais (normal se não for admin)");
        }
    };

    const handleSwitchToggle = async (config) => {
        const novoValor = config.valor === 'true' ? 'false' : 'true';
        try {
            await updateConfiguracao(config.chave, novoValor);
            setConfigs(configs.map(c => c.chave === config.chave ? { ...c, valor: novoValor } : c));
            toast.success("Atualizado!");
        } catch (error) { toast.error("Erro ao salvar."); }
    };

    const handleTextSave = async (chave) => {
        try {
            await updateConfiguracao(chave, editValues[chave]);
            setConfigs(configs.map(c => c.chave === chave ? { ...c, valor: editValues[chave] } : c));
            toast.success("Salvo!");
        } catch (error) { toast.error("Erro ao salvar."); }
    };

    const handleAcessarEmpresa = async (tenantId) => {
        // Comparação segura convertendo ambos para String
        if (String(tenantId) === String(currentTenantId)) {
            toast.info("Você já está nesta empresa.");
            return;
        }
        try {
            await switchTenant(tenantId);
            toast.success("Ambiente alterado com sucesso!");
            // Pequeno delay para garantir que o storage atualizou antes do reload
            setTimeout(() => window.location.reload(), 100);
        } catch (error) {
            toast.error("Erro ao trocar de empresa.");
        }
    };

    const handleConfigurarEmpresa = (tenantId) => {
        // Comparação segura convertendo ambos para String
        if (String(tenantId) !== String(currentTenantId)) {
            toast.warning("Você só pode configurar a empresa em que está logado no momento.");
            return;
        }
        setModoEdicaoEmpresa(true);
    };

    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" mb={3}>Configurações do Sistema</Typography>

            {modoEdicaoEmpresa ? (
                <EmpresaDados onBack={() => setModoEdicaoEmpresa(false)} />
            ) : (
                <>
                    <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
                        <Tabs value={tabIndex} onChange={(e, v) => setTabIndex(v)}>
                            <Tab label="Parâmetros Gerais" iconPosition="start" icon={<ServerCog size={18} />} />
                            <Tab label="Minhas Empresas" iconPosition="start" icon={<Building2 size={18} />} />
                        </Tabs>
                    </Box>

                    {tabIndex === 0 && (
                        <Paper sx={{ borderRadius: 2, overflow: 'hidden' }}>
                            <List>
                                {configs.map((conf, index) => (
                                    <Box key={conf.chave}>
                                        <ListItem sx={{ py: 2 }}>
                                            <Box mr={2} color="primary.main"><ServerCog size={24} /></Box>
                                            <ListItemText
                                                primary={<Typography fontWeight={600}>{conf.descricao || conf.chave}</Typography>}
                                                secondary={conf.chave}
                                            />
                                            <ListItemSecondaryAction>
                                                {conf.valor === 'true' || conf.valor === 'false' ? (
                                                    <Switch checked={conf.valor === 'true'} onChange={() => handleSwitchToggle(conf)} />
                                                ) : (
                                                    <Box display="flex" gap={1}>
                                                        <TextField size="small" value={editValues[conf.chave] || ''} onChange={e => setEditValues({ ...editValues, [conf.chave]: e.target.value })} sx={{ width: 100 }} />
                                                        <IconButton onClick={() => handleTextSave(conf.chave)}><Save size={18} /></IconButton>
                                                    </Box>
                                                )}
                                            </ListItemSecondaryAction>
                                        </ListItem>
                                        {index < configs.length - 1 && <Divider component="li" />}
                                    </Box>
                                ))}
                                {configs.length === 0 && (
                                    <ListItem><ListItemText primary="Nenhuma configuração global disponível." /></ListItem>
                                )}
                            </List>
                        </Paper>
                    )}

                    {tabIndex === 1 && (
                        <Box>
                            {isAdmin && (
                                <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
                                    <Button variant="outlined" startIcon={<Building2 size={18} />} onClick={() => navigate('/onboarding')}>
                                        Nova Empresa
                                    </Button>
                                </Box>
                            )}

                            <Grid container spacing={2}>
                                {user?.empresas?.map((empresa) => {
                                    // --- CORREÇÃO CRÍTICA AQUI ---
                                    // Converte ambos para String para garantir igualdade (ex: "1" == 1)
                                    const isCurrent = String(empresa.tenantId) === String(currentTenantId);

                                    return (
                                        <Grid item xs={12} md={6} lg={4} key={empresa.tenantId}>
                                            <Card variant="outlined" sx={{
                                                borderColor: isCurrent ? 'primary.main' : 'divider',
                                                bgcolor: isCurrent ? 'primary.nopacity' : 'background.paper'
                                            }}>
                                                <CardContent>
                                                    <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
                                                        <Box display="flex" gap={2} alignItems="center">
                                                            <Box sx={{ p: 1.5, bgcolor: isCurrent ? 'primary.main' : 'grey.200', borderRadius: 2, color: isCurrent ? 'white' : 'grey.600' }}>
                                                                <Building2 size={24} />
                                                            </Box>
                                                            <Box>
                                                                <Typography variant="h6" sx={{ fontSize: '1rem', fontWeight: 600 }}>
                                                                    {empresa.razaoSocial || `Empresa ${empresa.tenantId}`}
                                                                </Typography>
                                                                <Typography variant="caption" color="text.secondary">
                                                                    ID: {empresa.tenantId}
                                                                </Typography>
                                                            </Box>
                                                        </Box>
                                                        {isCurrent && <Chip label="Atual" color="primary" size="small" />}
                                                    </Box>

                                                    <Divider sx={{ my: 1.5 }} />

                                                    <Box display="flex" justifyContent="space-between" alignItems="center">
                                                        <Typography variant="body2" color="text.secondary">
                                                            Perfil: <strong>{empresa.role}</strong>
                                                        </Typography>

                                                        <Box display="flex" gap={1}>
                                                            {/* Botão ACESSAR (Desabilitado se já for atual) */}
                                                            {!isCurrent && (
                                                                <Tooltip title="Trocar para este ambiente">
                                                                    <Button
                                                                        size="small"
                                                                        variant="text"
                                                                        startIcon={<LogIn size={16} />}
                                                                        onClick={() => handleAcessarEmpresa(empresa.tenantId)}
                                                                    >
                                                                        Acessar
                                                                    </Button>
                                                                </Tooltip>
                                                            )}

                                                            {/* Botão CONFIGURAR (Só aparece se for Atual) */}
                                                            <Can I="CONFIG_GERENCIAR">
                                                                <Tooltip title={isCurrent ? "Configurar dados fiscais e sistema" : "Acesse a empresa para configurar"}>
                                                                    <span>
                                                                        <Button
                                                                            size="small"
                                                                            variant={isCurrent ? "contained" : "outlined"}
                                                                            color="primary"
                                                                            startIcon={<Settings size={16} />}
                                                                            disabled={!isCurrent} // <--- AQUI ESTAVA O PROBLEMA
                                                                            onClick={() => handleConfigurarEmpresa(empresa.tenantId)}
                                                                        >
                                                                            Configurar
                                                                        </Button>
                                                                    </span>
                                                                </Tooltip>
                                                            </Can>
                                                        </Box>
                                                    </Box>
                                                </CardContent>
                                            </Card>
                                        </Grid>
                                    );
                                })}
                            </Grid>
                        </Box>
                    )}
                </>
            )}
        </Box>
    );
};

export default Configuracoes;