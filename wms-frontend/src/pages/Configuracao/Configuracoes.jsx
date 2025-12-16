import { useState, useEffect } from 'react';
import { Box, Typography, Paper, Switch, List, ListItem, ListItemText, ListItemSecondaryAction, Divider } from '@mui/material';
import { toast } from 'react-toastify';
import { getConfiguracoes, updateConfiguracao } from '../../services/configService';

const Configuracoes = () => {
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

            // Atualiza estado local
            setConfigs(configs.map(c =>
                c.chave === config.chave ? { ...c, valor: novoValor } : c
            ));
            toast.success("Configuração atualizada!");
        } catch (error) {
            toast.error("Erro ao salvar.");
        }
    };

    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" mb={3}>Configurações do Sistema</Typography>
            <Paper sx={{ maxWidth: 600 }}>
                <List>
                    {configs.map((conf, index) => (
                        <Box key={conf.chave}>
                            <ListItem>
                                <ListItemText
                                    primary={conf.descricao}
                                    secondary={`Chave: ${conf.chave}`}
                                />
                                <ListItemSecondaryAction>
                                    <Switch
                                        edge="end"
                                        checked={conf.valor === 'true'}
                                        onChange={() => handleToggle(conf)}
                                    />
                                </ListItemSecondaryAction>
                            </ListItem>
                            {index < configs.length - 1 && <Divider />}
                        </Box>
                    ))}
                </List>
            </Paper>
        </Box>
    );
};

export default Configuracoes;