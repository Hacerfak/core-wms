import { useState, useEffect } from 'react';
import { Box, Typography, Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, LinearProgress } from '@mui/material';
import { Plus, User, UserPlus } from 'lucide-react';
import { toast } from 'react-toastify';
import { getUsuarios } from '../../services/usuarioService';
import UsuarioForm from './UsuarioForm';

const UsuariosList = () => {
    const [usuarios, setUsuarios] = useState([]);
    const [loading, setLoading] = useState(true);
    const [modalOpen, setModalOpen] = useState(false);

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
                <Button variant="contained" startIcon={<UserPlus size={20} />} onClick={() => setModalOpen(true)}>
                    Novo Usuário
                </Button>
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