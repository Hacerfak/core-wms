import { useContext } from 'react';
import { AuthContext } from '../../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, Card, CardContent, Grid, Button, Container } from '@mui/material';
import { Building2, PlusCircle, Lock } from 'lucide-react';

const SelecaoEmpresa = () => {
    const { user, selecionarEmpresa } = useContext(AuthContext);
    const navigate = useNavigate();

    // Verifica se é ADMIN Global
    const isAdmin = user?.role === 'ADMIN';

    const handleSelect = async (tenantId) => {
        const success = await selecionarEmpresa(tenantId);
        if (success) {
            navigate('/dashboard');
        }
    };

    return (
        <Container maxWidth="md" sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', justifyContent: 'center', py: 4 }}>
            <Typography variant="h4" component="h1" gutterBottom fontWeight="bold" align="center" sx={{ mb: 4 }}>
                Selecione o Ambiente
            </Typography>

            <Grid container spacing={3}>
                {/* Botão Nova Empresa: SÓ APARECE SE FOR ADMIN */}
                {isAdmin && (
                    <Grid item xs={12} sm={6} md={4}>
                        <Card
                            sx={{
                                height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center',
                                cursor: 'pointer', border: '2px dashed #cbd5e1', boxShadow: 'none',
                                '&:hover': { bgcolor: '#f1f5f9', borderColor: '#94a3b8' }
                            }}
                            onClick={() => navigate('/onboarding')}
                        >
                            <CardContent sx={{ textAlign: 'center' }}>
                                <PlusCircle size={48} color="#64748b" style={{ marginBottom: 8 }} />
                                <Typography variant="h6" color="text.secondary">Nova Empresa</Typography>
                            </CardContent>
                        </Card>
                    </Grid>
                )}

                {/* Lista de Empresas */}
                {user?.empresas?.map((empresa) => (
                    <Grid item xs={12} sm={6} md={4} key={empresa.tenantId}>
                        <Card
                            sx={{
                                height: '100%',
                                cursor: 'pointer',
                                transition: '0.2s',
                                '&:hover': { transform: 'translateY(-4px)', boxShadow: 4, borderColor: 'primary.main' }
                            }}
                            onClick={() => handleSelect(empresa.tenantId)}
                        >
                            <CardContent sx={{ textAlign: 'center', py: 4 }}>
                                <Box sx={{
                                    width: 56, height: 56, bgcolor: 'primary.light', borderRadius: '50%',
                                    display: 'flex', alignItems: 'center', justifyContent: 'center', mx: 'auto', mb: 2,
                                    color: 'white'
                                }}>
                                    <Building2 size={28} />
                                </Box>
                                <Typography variant="h6" noWrap title={empresa.razaoSocial}>
                                    {empresa.razaoSocial}
                                </Typography>
                                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                    Perfil: <strong>{empresa.role}</strong>
                                </Typography>
                            </CardContent>
                        </Card>
                    </Grid>
                ))}
            </Grid>
        </Container>
    );
};

export default SelecaoEmpresa;