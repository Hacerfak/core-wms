import { useState, useContext } from 'react';
import {
    Box, Typography, Grid, Card, CardContent, Button, Divider, Chip
} from '@mui/material';
import { Building2, PlusCircle, LogIn } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../../contexts/AuthContext';
import { toast } from 'react-toastify';

const GestaoEmpresas = () => {
    const { user, selecionarEmpresa } = useContext(AuthContext);
    const navigate = useNavigate();

    // ID do Tenant Atual para destacar
    const currentTenantId = localStorage.getItem('@App:tenant') || user?.tenantId;

    const handleAcessar = async (tenantId) => {
        if (String(tenantId) === String(currentTenantId)) {
            toast.info("Você já está nesta empresa.");
            return;
        }
        const success = await selecionarEmpresa(tenantId);
        if (success) {
            toast.success("Ambiente alterado!");
            navigate(0); // Reload
        }
    };

    return (
        <Box>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
                <Box>
                    <Typography variant="h5" fontWeight="bold">Gestão de Ambientes</Typography>
                    <Typography variant="body2" color="text.secondary">
                        Gerencie as empresas e tenants do sistema.
                    </Typography>
                </Box>
                <Button variant="contained" startIcon={<PlusCircle size={20} />} onClick={() => navigate('/onboarding')}>
                    Nova Empresa
                </Button>
            </Box>

            <Grid container spacing={3}>
                {user?.empresas?.map((empresa) => {
                    const isCurrent = String(empresa.tenantId) === String(currentTenantId);
                    return (
                        <Grid item xs={12} sm={6} md={4} key={empresa.tenantId}>
                            <Card variant="outlined" sx={{
                                borderColor: isCurrent ? 'primary.main' : 'divider',
                                bgcolor: isCurrent ? '#eff6ff' : 'background.paper',
                                transition: '0.2s',
                                '&:hover': { borderColor: 'primary.main', transform: 'translateY(-2px)', boxShadow: 2 }
                            }}>
                                <CardContent>
                                    <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
                                        <Box display="flex" gap={2} alignItems="center">
                                            <Box sx={{ p: 1.5, bgcolor: isCurrent ? 'primary.main' : 'grey.100', borderRadius: 2, color: isCurrent ? 'white' : 'grey.600' }}>
                                                <Building2 size={24} />
                                            </Box>
                                            <Box>
                                                <Typography variant="h6" fontSize="1rem" fontWeight={600} noWrap sx={{ maxWidth: 200 }}>
                                                    {empresa.razaoSocial}
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
                                        {!isCurrent && (
                                            <Button size="small" startIcon={<LogIn size={16} />} onClick={() => handleAcessar(empresa.tenantId)}>
                                                Acessar
                                            </Button>
                                        )}
                                    </Box>
                                </CardContent>
                            </Card>
                        </Grid>
                    );
                })}
            </Grid>
        </Box>
    );
};

export default GestaoEmpresas;