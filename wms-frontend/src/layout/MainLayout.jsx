import { Box, AppBar, Toolbar, Typography, IconButton, Avatar, Menu, MenuItem } from '@mui/material';
import { LogOut, User } from 'lucide-react';
import { useContext, useState } from 'react';
import { AuthContext } from '../contexts/AuthContext';
import Sidebar from '../components/Sidebar';

const MainLayout = ({ children }) => {
    const { logout, user } = useContext(AuthContext);
    const [anchorEl, setAnchorEl] = useState(null);

    const handleMenu = (event) => setAnchorEl(event.currentTarget);
    const handleClose = () => setAnchorEl(null);

    const handleLogout = () => {
        handleClose();
        logout();
    };

    return (
        <Box sx={{ display: 'flex', height: '100vh', bgcolor: 'background.default' }}>

            {/* 1. Menu Lateral Fixo */}
            <Sidebar />

            {/* 2. Área Principal */}
            <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>

                {/* Barra de Topo (Header) */}
                <AppBar position="static" color="transparent" elevation={0} sx={{ borderBottom: '1px solid #e2e8f0', bgcolor: 'white', px: 2 }}>
                    <Toolbar>
                        <Typography variant="h6" sx={{ flexGrow: 1, color: 'text.secondary', fontSize: '1rem' }}>
                            {/* Aqui poderia ir um Breadcrumb ou Título Dinâmico */}
                            Visão Geral
                        </Typography>

                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="body2" fontWeight={500}>
                                {user?.login || 'Usuário'}
                            </Typography>
                            <IconButton onClick={handleMenu} size="small">
                                <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main' }}>
                                    <User size={18} />
                                </Avatar>
                            </IconButton>
                            <Menu
                                anchorEl={anchorEl}
                                open={Boolean(anchorEl)}
                                onClose={handleClose}
                                transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                                anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                            >
                                <MenuItem onClick={handleLogout} sx={{ gap: 1 }}>
                                    <LogOut size={16} /> Sair
                                </MenuItem>
                            </Menu>
                        </Box>
                    </Toolbar>
                </AppBar>

                {/* Conteúdo da Página (Scrollável) */}
                <Box sx={{ flexGrow: 1, overflow: 'auto', p: 3 }}>
                    {children}
                </Box>
            </Box>
        </Box>
    );
};

export default MainLayout;