import { useState, useEffect } from 'react';
import { Box, Typography, Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, LinearProgress } from '@mui/material';
import { Plus, User, UserPlus, Shield } from 'lucide-react'; // Ícone Shield adicionado
import { toast } from 'react-toastify';
import { getUsuarios } from '../../services/usuarioService';
import UsuarioForm from './UsuarioForm';
import { useNavigate } from 'react-router-dom'; // Import useNavigate
import Can from '../../components/Can'; // Import Can para proteção

const UsuariosList = () => {
    const [usuarios, setUsuarios] = useState([]);
    const [loading, setLoading] = useState(true);
    const [modalOpen, setModalOpen] = useState(false);
    const navigate = useNavigate(); // Hook de navegação

    const loadData = async () => {
        try {
            setLoading(true);
            const data = await getUsuarios();
            setUsuarios(data);
        } catch (error) {
            toast.error("Erro ao carregar usuários");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadData(); }, []);

    return (
        <Box>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
                <Typography variant="h5" fontWeight="bold">Gestão de Usuários</Typography>

                <Box display="flex" gap={2}>
                    {/* BOTÃO NOVO: Gerenciar Perfis */}
                    <Can I="PERFIL_GERENCIAR">
                        <Button
                            variant="outlined"
                            startIcon={<Shield size={20} />}
                            onClick={() => navigate('/perfis')}
                        >
                            Gerenciar Perfis
                        </Button>
                    </Can>

                    {/* Botão Novo Usuário */}
                    <Can I="USUARIO_CRIAR">
                        <Button
                            variant="contained"
                            startIcon={<UserPlus size={20} />}
                            onClick={() => setModalOpen(true)}
                        >
                            Novo Usuário
                        </Button>
                    </Can>
                </Box>
            </Box>

            <Paper sx={{ width: '100%', overflow: 'hidden', borderRadius: 2 }}>
                {loading && <LinearProgress />}
                <TableContainer>
                    <Table>
                        <TableHead sx={{ bgcolor: '#f8fafc' }}>
                            <TableRow>
                                <TableCell><b>Login</b></TableCell>
                                <TableCell><b>Perfil</b></TableCell>
                                <TableCell><b>Status</b></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {usuarios.map((u) => (
                                <TableRow key={u.id} hover>
                                    <TableCell>
                                        <Box display="flex" alignItems="center" gap={1}>
                                            <User size={16} /> {u.login}
                                        </Box>
                                    </TableCell>
                                    <TableCell>
                                        <Chip label={u.perfilNome} size="small" color="primary" variant="outlined" />
                                    </TableCell>
                                    <TableCell>
                                        <Chip label={u.ativo ? "Ativo" : "Inativo"} color={u.ativo ? "success" : "default"} size="small" />
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>

            <UsuarioForm open={modalOpen} onClose={() => setModalOpen(false)} onSuccess={() => { setModalOpen(false); loadData(); }} />
        </Box>
    );
};

export default UsuariosList;