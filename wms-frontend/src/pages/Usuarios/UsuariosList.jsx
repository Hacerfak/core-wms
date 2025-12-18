import { useState, useEffect, useContext } from 'react'; // useContext adicionado
import { Box, Typography, Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, LinearProgress, IconButton, Tooltip } from '@mui/material';
import { Plus, User, UserPlus, Shield, Trash2 } from 'lucide-react';
import { toast } from 'react-toastify';
import { getUsuarios, removerUsuarioLocal, excluirUsuarioGlobal } from '../../services/usuarioService'; // Novos imports
import UsuarioForm from './UsuarioForm';
import { useNavigate } from 'react-router-dom';
import Can from '../../components/Can';
import { AuthContext } from '../../contexts/AuthContext'; // Importar AuthContext

const UsuariosList = () => {
    const [usuarios, setUsuarios] = useState([]);
    const [loading, setLoading] = useState(true);
    const [modalOpen, setModalOpen] = useState(false);
    const navigate = useNavigate();
    const { user } = useContext(AuthContext); // Pegar usuário logado para saber se é ADMIN MASTER

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

    const handleDelete = async (usuarioAlvo) => {
        const isMasterAdmin = user?.role === 'ADMIN'; // Verifica se EU sou o Master
        const isSelf = user?.login === usuarioAlvo.login;

        if (isSelf) {
            toast.warning("Você não pode se excluir.");
            return;
        }

        // Lógica de Confirmação Diferenciada
        if (isMasterAdmin) {
            if (window.confirm(`ATENÇÃO MASTER:\nIsso excluirá o usuário "${usuarioAlvo.login}" do sistema GLOBALMENTE e de TODAS as empresas.\n\nDeseja continuar?`)) {
                try {
                    await excluirUsuarioGlobal(usuarioAlvo.id);
                    toast.success("Usuário excluído globalmente.");
                    loadData();
                } catch (error) {
                    toast.error("Erro ao excluir usuário global.");
                }
            }
        } else {
            // Admin Local
            if (window.confirm(`Deseja remover o acesso de "${usuarioAlvo.login}" desta empresa?\n(A conta dele continuará existindo em outras empresas)`)) {
                try {
                    await removerUsuarioLocal(usuarioAlvo.id);
                    toast.success("Acesso removido com sucesso.");
                    loadData();
                } catch (error) {
                    toast.error("Erro ao remover acesso local.");
                }
            }
        }
    };

    return (
        <Box>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
                <Typography variant="h5" fontWeight="bold">Gestão de Usuários</Typography>

                <Box display="flex" gap={2}>
                    <Can I="PERFIL_GERENCIAR">
                        <Button
                            variant="outlined"
                            startIcon={<Shield size={20} />}
                            onClick={() => navigate('/perfis')}
                        >
                            Gerenciar Perfis
                        </Button>
                    </Can>

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
                        <TableHead sx={{ bgcolor: 'background.subtle' }}>
                            <TableRow>
                                <TableCell><b>Login</b></TableCell>
                                <TableCell><b>Perfil</b></TableCell>
                                <TableCell><b>Status</b></TableCell>
                                <TableCell align="center"><b>Ações</b></TableCell>
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
                                    <TableCell align="center">
                                        {/* Botão de Excluir protegido */}
                                        <Can I="USUARIO_EXCLUIR">
                                            {/* Não permite excluir o próprio admin master ou super users protegidos visualmente */}
                                            {!u.perfilNome.includes('MASTER') && (
                                                <Tooltip title="Remover Acesso / Excluir">
                                                    <IconButton
                                                        size="small"
                                                        color="error"
                                                        onClick={() => handleDelete(u)}
                                                    >
                                                        <Trash2 size={18} />
                                                    </IconButton>
                                                </Tooltip>
                                            )}
                                        </Can>
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