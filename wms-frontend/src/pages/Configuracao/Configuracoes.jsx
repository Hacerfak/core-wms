import { useState, useEffect, useContext } from 'react';
import { Box, Typography, Paper, Switch, List, ListItem, ListItemText, ListItemSecondaryAction, Divider, Tabs, Tab, Button, Card, CardContent, Grid } from '@mui/material';
import { toast } from 'react-toastify';
import { getConfiguracoes, updateConfiguracao } from '../../services/configService';
import { AuthContext } from '../../contexts/AuthContext';
import { Building2, Plus, ServerCog, Bell } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Can from '../../components/Can';

const Configuracoes = () => {
    const { user } = useContext(AuthContext); // Pegamos as empresas do contexto
    const isAdmin = user?.role === 'ADMIN'; // Checa permissão
    const navigate = useNavigate();

    const [tabIndex, setTabIndex] = useState(0);
    const [configs, setConfigs] = useState([]);

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            const data = await getConfiguracoes();
            setConfigs(data);
        } catch (error) {
            toast.error("Erro ao carregar configurações");
        }
    };

    const handleToggle = async (config) => {
        const novoValor = config.valor === 'true' ? 'false' : 'true';
        try {
            await updateConfiguracao(config.chave, novoValor);
            setConfigs(configs.map(c => c.chave === config.chave ? { ...c, valor: novoValor } : c));
            toast.success("Configuração atualizada!");
        } catch (error) {
            toast.error("Erro ao salvar.");
        }
    };

    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" mb={3}>Configurações</Typography>

            <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
                <Tabs value={tabIndex} onChange={(e, v) => setTabIndex(v)}>
                    <Tab label="Geral" iconPosition="start" icon={<ServerCog size={18} />} />
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
                                        {conf.chave.includes('RECEBIMENTO') ? <Bell size={24} /> : <Settings size={24} />}
                                    </Box>
                                    <ListItemText
                                        primary={<Typography fontWeight={600}>{conf.descricao || conf.chave}</Typography>}
                                        secondary={conf.chave}
                                    />
                                    <ListItemSecondaryAction>
                                        {/* LÓGICA DE PERMISSÃO APLICADA AQUI */}
                                        <Can
                                            I="CONFIG_GERENCIAR"
                                            elseShow={
                                                <Switch
                                                    edge="end"
                                                    checked={conf.valor === 'true'}
                                                    disabled={true}
                                                />
                                            }
                                        >
                                            <Switch
                                                edge="end"
                                                checked={conf.valor === 'true'}
                                                onChange={() => handleToggle(conf)}
                                            />
                                        </Can>
                                    </ListItemSecondaryAction>
                                </ListItem>
                                {index < configs.length - 1 && <Divider component="li" />}
                            </Box>
                        ))}
                    </List>
                </Paper>
            )}

            {/* ABA 1: GERENCIAR EMPRESAS */}
            {tabIndex === 1 && (
                <Box>
                    {/* Só mostra botão se for ADMIN */}
                    {isAdmin && (
                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
                            <Button
                                variant="contained"
                                startIcon={<Plus size={18} />}
                                onClick={() => navigate('/onboarding')}
                            >
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

                                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                            <Typography variant="body2" color="text.secondary">
                                                Perfil
                                            </Typography>
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