import { Box, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Typography } from '@mui/material';
import { Home, PackagePlus, Box as BoxIcon, Truck, Settings, Users } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import theme from '../theme/theme';
import { useContext } from 'react';
import { AuthContext } from '../contexts/AuthContext';

const Sidebar = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { userCan } = useContext(AuthContext);

    const menuItems = [
        { text: 'Dashboard', icon: <Home size={20} />, path: '/dashboard', permission: null },
        { text: 'Recebimento', icon: <PackagePlus size={20} />, path: '/recebimento', permission: 'RECEBIMENTO_VISUALIZAR' },
        { text: 'Estoque', icon: <BoxIcon size={20} />, path: '/estoque', permission: 'ESTOQUE_VISUALIZAR' },
        { text: 'Expedição', icon: <Truck size={20} />, path: '/expedicao', permission: 'PEDIDO_VISUALIZAR' },
        { text: 'Usuários', icon: <Users size={20} />, path: '/usuarios', permission: 'USUARIO_LISTAR' },
        { text: 'Configurações', icon: <Settings size={20} />, path: '/config', permission: 'CONFIG_GERENCIAR' },
    ];

    return (
        <Box sx={{
            width: 260,
            bgcolor: 'background.paper', // Uso do tema
            borderRight: 1,
            borderColor: 'divider', // Uso do tema
            height: '100vh',
            display: 'flex',
            flexDirection: 'column'
        }}>
            <Box sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 2, borderBottom: 1, borderColor: 'divider' }}>
                <Box sx={{
                    width: 32, height: 32, bgcolor: 'primary.main', borderRadius: 1,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white'
                }}>
                    <BoxIcon size={20} />
                </Box>
                <Typography variant="h6" color="text.primary">WMS Core</Typography>
            </Box>

            <List sx={{ px: 2, pt: 2 }}>
                {menuItems
                    // Filtra itens que o usuário não pode ver ANTES de renderizar
                    .filter(item => !item.permission || userCan(item.permission))
                    .map((item) => {
                        const isActive = location.pathname.startsWith(item.path);
                        return (
                            <ListItem key={item.text} disablePadding sx={{ mb: 1 }}>
                                <ListItemButton
                                    onClick={() => navigate(item.path)}
                                    sx={{
                                        borderRadius: 2,
                                        // Usa cores do tema com alpha para transparência
                                        bgcolor: isActive ? (theme) => theme.palette.primary.light + '20' : 'transparent',
                                        color: isActive ? 'primary.main' : 'text.secondary',
                                        '&:hover': {
                                            bgcolor: isActive
                                                ? (theme) => theme.palette.primary.light + '30'
                                                : 'background.subtle' // Uso da nova cor subtle
                                        }
                                    }}
                                >
                                    <ListItemIcon sx={{ minWidth: 40, color: isActive ? 'primary.main' : 'text.secondary' }}>
                                        {item.icon}
                                    </ListItemIcon>
                                    <ListItemText
                                        primary={item.text}
                                        primaryTypographyProps={{ fontWeight: isActive ? 600 : 500, fontSize: '0.95rem' }}
                                    />
                                </ListItemButton>
                            </ListItem>
                        );
                    })}
            </List>
        </Box>
    );
};

export default Sidebar;