import { useState, useEffect, useContext } from 'react';
import {
    Box, Typography, Paper, Switch, List, ListItem, ListItemText,
    ListItemSecondaryAction, Divider, Tabs, Tab, Button, Card,
    CardContent, Grid, TextField, IconButton
} from '@mui/material';
import { toast } from 'react-toastify';
import { getConfiguracoes, updateConfiguracao } from '../../services/configService';
import { AuthContext } from '../../contexts/AuthContext';
import { Building2, ServerCog, Bell, Settings, Save, Database, History } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Can from '../../components/Can';

const Configuracoes = () => {
    const { user } = useContext(AuthContext);
    const isAdmin = user?.role === 'ADMIN';
    const navigate = useNavigate();

    const [tabIndex, setTabIndex] = useState(0);
    const [configs, setConfigs] = useState([]);

    // Estado local para editar valores de texto/número antes de salvar
    const [editValues, setEditValues] = useState({});

    useEffect(() => { loadData(); }, []);

    const loadData = async () => {
        try {
            const data = await getConfiguracoes();
            setConfigs(data);

            // Inicializa os valores de edição
            const initialEdits = {};
            data.forEach(c => initialEdits[c.chave] = c.valor);
            setEditValues(initialEdits);

        } catch (error) {
            toast.error("Erro ao carregar configurações");
        }
    };

    const handleSwitchToggle = async (config) => {
        const novoValor = config.valor === 'true' ? 'false' : 'true';
        try {
            await updateConfiguracao(config.chave, novoValor);
            // Atualiza lista e o editValue local
            const novaLista = configs.map(c => c.chave === config.chave ? { ...c, valor: novoValor } : c);
            setConfigs(novaLista);
            setEditValues(prev => ({ ...prev, [config.chave]: novoValor }));

            toast.success("Atualizado com sucesso!");
        } catch (error) {
            toast.error("Erro ao salvar.");
        }
    };

    const handleTextSave = async (chave) => {
        try {
            await updateConfiguracao(chave, editValues[chave]);
            // Atualiza a lista oficial com o novo valor salvo
            setConfigs(configs.map(c => c.chave === chave ? { ...c, valor: editValues[chave] } : c));
            toast.success("Configuração salva!");
        } catch (error) {
            toast.error("Erro ao salvar valor.");
        }
    };

    const handleTextChange = (chave, valor) => {
        setEditValues(prev => ({ ...prev, [chave]: valor }));
    };

    const getIcon = (chave) => {
        if (chave.includes('RECEBIMENTO')) return <Bell size={24} />;
        if (chave.includes('AUDITORIA') || chave.includes('RETENCAO')) return <History size={24} />;
        if (chave.includes('BANCO') || chave.includes('DB')) return <Database size={24} />;
        return <Settings size={24} />;
    };

    // Função para decidir se renderiza Switch ou Input
    const renderControl = (conf) => {
        const isBoolean = conf.valor === 'true' || conf.valor === 'false';

        // 1. Controle Boolean (Switch)
        if (isBoolean) {
            return (
                <Can I="CONFIG_GERENCIAR" elseShow={<Switch checked={conf.valor === 'true'} disabled />}>
                    <Switch
                        edge="end"
                        checked={conf.valor === 'true'}
                        onChange={() => handleSwitchToggle(conf)}
                    />
                </Can>
            );
        }

        // 2. Controle Texto/Número (Input)
        return (
            <Can I="CONFIG_GERENCIAR" elseShow={<Typography>{conf.valor}</Typography>}>
                <Box display="flex" alignItems="center" gap={1}>
                    <TextField
                        size="small"
                        variant="outlined"
                        value={editValues[conf.chave] || ''}
                        onChange={(e) => handleTextChange(conf.chave, e.target.value)}
                        sx={{ width: 100 }}
                        type={conf.valor.match(/^\d+$/) ? "number" : "text"} // Se for só dígitos, usa type number
                    />
                    <IconButton size="small" color="primary" onClick={() => handleTextSave(conf.chave)}>
                        <Save size={18} />
                    </IconButton>
                </Box>
            </Can>
        );
    };

    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" mb={3}>Configurações do Sistema</Typography>

            <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
                <Tabs value={tabIndex} onChange={(e, v) => setTabIndex(v)}>
                    <Tab label="Parâmetros Gerais" iconPosition="start" icon={<ServerCog size={18} />} />
                    <Tab label="Minhas Empresas" iconPosition="start" icon={<Building2 size={18} />} />
                </Tabs>
            </Box>

            {/* ABA 0: CONFIGURAÇÕES GERAIS */}
            {tabIndex === 0 && (
                <Paper sx={{ borderRadius: 2, overflow: 'hidden' }}>
                    <List>
                        {configs.map((conf, index) => (
                            <Box key={conf.chave}>
                                <ListItem sx={{ py: 2 }}>
                                    <Box mr={2} color="primary.main">
                                        {getIcon(conf.chave)}
                                    </Box>
                                    <ListItemText
                                        primary={<Typography fontWeight={600}>{conf.descricao || conf.chave}</Typography>}
                                        secondary={<Typography variant="caption" color="text.secondary">{conf.chave}</Typography>}
                                    />
                                    <ListItemSecondaryAction>
                                        {renderControl(conf)}
                                    </ListItemSecondaryAction>
                                </ListItem>
                                {index < configs.length - 1 && <Divider component="li" />}
                            </Box>
                        ))}
                        {configs.length === 0 && (
                            <ListItem>
                                <ListItemText primary="Nenhuma configuração encontrada." />
                            </ListItem>
                        )}
                    </List>
                </Paper>
            )}

            {/* ABA 1: GERENCIAR EMPRESAS (Mantém igual) */}
            {tabIndex === 1 && (
                <Box>
                    {isAdmin && (
                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
                            <Button variant="contained" onClick={() => navigate('/onboarding')}>
                                Nova Empresa
                            </Button>
                        </Box>
                    )}
                    <Grid container spacing={2}>
                        {user?.empresas?.map((empresa) => (
                            <Grid item xs={12} md={6} lg={4} key={empresa.tenantId}>
                                <Card variant="outlined">
                                    <CardContent>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                                            <Box sx={{ p: 1.5, bgcolor: 'primary.light', borderRadius: 2, color: 'white' }}>
                                                <Building2 size={24} />
                                            </Box>
                                            <Box>
                                                <Typography variant="h6" sx={{ fontSize: '1rem', fontWeight: 600 }}>
                                                    {empresa.razaoSocial}
                                                </Typography>
                                                <Typography variant="caption" color="text.secondary">
                                                    ID: {empresa.tenantId}
                                                </Typography>
                                            </Box>
                                        </Box>
                                        <Divider sx={{ my: 1.5 }} />
                                        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                            <Typography variant="body2" color="text.secondary">Perfil</Typography>
                                            <Typography variant="body2" fontWeight="bold" color="primary">
                                                {empresa.role}
                                            </Typography>
                                        </Box>
                                    </CardContent>
                                </Card>
                            </Grid>
                        ))}
                    </Grid>
                </Box>
            )}
        </Box>
    );
};

export default Configuracoes;