import { Box, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Typography } from '@mui/material';
import { Home, PackagePlus, Box as BoxIcon, Truck, Settings, Users } from 'lucide-react'; // Ícones
import { useLocation, useNavigate } from 'react-router-dom';
import theme from '../theme/theme';

const menuItems = [
    { text: 'Dashboard', icon: <Home size={20} />, path: '/dashboard' },
    { text: 'Recebimento', icon: <PackagePlus size={20} />, path: '/recebimento' },
    { text: 'Estoque', icon: <BoxIcon size={20} />, path: '/estoque' },
    { text: 'Expedição', icon: <Truck size={20} />, path: '/expedicao' },
    { text: 'Usuários', icon: <Users size={20} />, path: '/usuarios' },
    { text: 'Configurações', icon: <Settings size={20} />, path: '/config' },
];

const Sidebar = () => {
    const navigate = useNavigate();
    const location = useLocation();

    return (
        <Box sx={{
            width: 260,
            bgcolor: 'white',
            borderRight: '1px solid #e2e8f0',
            height: '100vh',
            display: 'flex',
            flexDirection: 'column'
        }}>
            {/* Logo Area */}
            <Box sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 2, borderBottom: '1px solid #f1f5f9' }}>
                <Box sx={{
                    width: 32, height: 32,
                    bgcolor: theme.palette.primary.main,
                    borderRadius: 1,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white'
                }}>
                    <BoxIcon size={20} />
                </Box>
                <Typography variant="h6" color="text.primary">WMS Core</Typography>
            </Box>

            {/* Menu Items */}
            <List sx={{ px: 2, pt: 2 }}>
                {menuItems.map((item) => {
                    const isActive = location.pathname.startsWith(item.path);

                    return (
                        <ListItem key={item.text} disablePadding sx={{ mb: 1 }}>
                            <ListItemButton
                                onClick={() => navigate(item.path)}
                                sx={{
                                    borderRadius: 2,
                                    bgcolor: isActive ? theme.palette.primary.light + '20' : 'transparent', // 20% opacidade
                                    color: isActive ? theme.palette.primary.main : theme.palette.text.secondary,
                                    '&:hover': {
                                        bgcolor: isActive ? theme.palette.primary.light + '30' : '#f8fafc',
                                    }
                                }}
                            >
                                <ListItemIcon sx={{
                                    minWidth: 40,
                                    color: isActive ? theme.palette.primary.main : theme.palette.text.secondary
                                }}>
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