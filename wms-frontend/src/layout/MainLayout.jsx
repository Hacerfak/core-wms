import { Box, AppBar, Toolbar, Typography, IconButton, Avatar, Menu, MenuItem, Divider, ListItemIcon, Tooltip, Chip } from '@mui/material';
import { LogOut, User, Building2, ChevronDown, Check } from 'lucide-react';
import { useContext, useState, useMemo } from 'react';
import { AuthContext } from '../contexts/AuthContext'; // Ajuste o caminho se necessário
import Sidebar from '../components/Sidebar';
import Can from '../components/Can';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';

const MainLayout = ({ children }) => {
    const { logout, user, selecionarEmpresa } = useContext(AuthContext);
    const navigate = useNavigate();
    const [anchorEl, setAnchorEl] = useState(null);

    const handleMenu = (event) => setAnchorEl(event.currentTarget);
    const handleClose = () => setAnchorEl(null);

    const handleLogout = () => {
        handleClose();
        logout();
        navigate('/login');
    };

    // Descobre qual é a empresa atual baseada no Token
    const empresaAtual = useMemo(() => {
        const token = localStorage.getItem('wms_token');
        if (!token || !user?.empresas) return null;
        try {
            const decoded = jwtDecode(token);
            // Procura na lista de empresas do usuário qual tem o tenantId do token
            return user.empresas.find(e => e.tenantId === decoded.tenant);
        } catch (e) {
            return null;
        }
    }, [user]); // Recalcula se o usuário mudar

    const handleTrocarEmpresa = async (tenantId) => {
        handleClose();
        if (empresaAtual?.tenantId === tenantId) return; // Já está nela

        const success = await selecionarEmpresa(tenantId);
        if (success) {
            navigate(0); // Recarrega a página para garantir limpeza de estados de telas anteriores
        }
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
                        {/* Nome da Empresa Atual em Destaque */}
                        <Box sx={{ flexGrow: 1, display: 'flex', alignItems: 'center', gap: 2 }}>
                            <Typography variant="h6" sx={{ color: 'text.secondary', fontSize: '1rem', fontWeight: 500 }}>
                                Visão Geral
                            </Typography>

                            {empresaAtual && (
                                <Chip
                                    icon={<Building2 size={16} />}
                                    label={empresaAtual.razaoSocial}
                                    color="primary"
                                    variant="outlined"
                                    size="small"
                                />
                            )}
                        </Box>

                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="body2" fontWeight={500} sx={{ display: { xs: 'none', sm: 'block' } }}>
                                {user?.login || 'Usuário'}
                            </Typography>

                            <Tooltip title="Conta e Empresas">
                                <IconButton onClick={handleMenu} size="small" sx={{ ml: 1 }}>
                                    <Avatar sx={{ width: 36, height: 36, bgcolor: 'primary.main', fontSize: '0.9rem' }}>
                                        {user?.login?.substring(0, 2).toUpperCase()}
                                    </Avatar>
                                </IconButton>
                            </Tooltip>

                            <Menu
                                anchorEl={anchorEl}
                                open={Boolean(anchorEl)}
                                onClose={handleClose}
                                PaperProps={{
                                    elevation: 2,
                                    sx: { mt: 1.5, minWidth: 220, borderRadius: 2 }
                                }}
                                transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                                anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                            >
                                <Box sx={{ px: 2, py: 1.5 }}>
                                    <Typography variant="subtitle2" color="text.secondary">
                                        Meus Ambientes
                                    </Typography>
                                </Box>

                                {/* Lista de Empresas para Troca Rápida */}
                                {user?.empresas?.map((empresa) => (
                                    <MenuItem
                                        key={empresa.tenantId}
                                        onClick={() => handleTrocarEmpresa(empresa.tenantId)}
                                        selected={empresaAtual?.tenantId === empresa.tenantId}
                                    >
                                        <ListItemIcon>
                                            {empresaAtual?.tenantId === empresa.tenantId ? <Check size={18} color="green" /> : <Building2 size={18} />}
                                        </ListItemIcon>
                                        <Typography variant="body2" noWrap sx={{ maxWidth: 150 }}>
                                            {empresa.razaoSocial}
                                        </Typography>
                                    </MenuItem>
                                ))}

                                <Divider sx={{ my: 1 }} />

                                <Can I="CONFIG_GERENCIAR">
                                    <MenuItem onClick={() => { handleClose(); navigate('/config'); }}>
                                        <ListItemIcon><Building2 size={18} /></ListItemIcon>
                                        Gerenciar Empresas
                                    </MenuItem>
                                </Can>

                                <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
                                    <ListItemIcon><LogOut size={18} color="#ef4444" /></ListItemIcon>
                                    Sair
                                </MenuItem>
                            </Menu>
                        </Box>
                    </Toolbar>
                </AppBar>

                {/* Conteúdo da Página */}
                <Box sx={{ flexGrow: 1, overflow: 'auto', p: 3 }}>
                    {children}
                </Box>
            </Box>
        </Box>
    );
};

export default MainLayout;