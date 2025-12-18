import { useState, useEffect, useContext } from 'react';
import { Box, Typography, Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, LinearProgress, IconButton, Tooltip } from '@mui/material';
import { Plus, User, UserPlus, Shield, Trash2, Edit } from 'lucide-react';
import { toast } from 'react-toastify';
import { getUsuarios, removerUsuarioLocal, excluirUsuarioGlobal } from '../../services/usuarioService';
import UsuarioForm from './UsuarioForm';
import ConfirmDialog from '../../components/ConfirmDialog'; // <--- Importe o novo componente
import { useNavigate } from 'react-router-dom';
import Can from '../../components/Can';
import { AuthContext } from '../../contexts/AuthContext';

const UsuariosList = () => {
    const [usuarios, setUsuarios] = useState([]);
    const [loading, setLoading] = useState(true);

    // Estados do Modal de Edição
    const [modalOpen, setModalOpen] = useState(false);
    const [usuarioEditando, setUsuarioEditando] = useState(null);

    // Estados do Modal de Confirmação
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmData, setConfirmData] = useState({ title: '', message: '', action: null });

    const navigate = useNavigate();
    const { user } = useContext(AuthContext);

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

    // ... (handleNew, handleEdit, handleCloseModal mantêm-se iguais) ...
    const handleNew = () => { setUsuarioEditando(null); setModalOpen(true); };
    const handleEdit = (u) => { setUsuarioEditando(u); setModalOpen(true); };
    const handleCloseModal = () => { setModalOpen(false); setUsuarioEditando(null); };

    // --- NOVA LÓGICA DE DELETE COM DIÁLOGO ---
    const handleDeleteClick = (usuarioAlvo) => {
        const isMasterAdmin = user?.role === 'ADMIN';
        const isSelf = user?.login === usuarioAlvo.login;

        if (isSelf) {
            toast.warning("Você não pode se excluir.");
            return;
        }

        if (isMasterAdmin) {
            // Configura diálogo para EXCLUSÃO GLOBAL
            setConfirmData({
                title: 'Exclusão Global',
                message: `ATENÇÃO:\nIsso excluirá o usuário "${usuarioAlvo.login}" do sistema GLOBALMENTE e de TODAS as empresas.\n\nDeseja realmente continuar?`,
                action: async () => {
                    try {
                        await excluirUsuarioGlobal(usuarioAlvo.id);
                        toast.success("Usuário excluído globalmente.");
                        loadData();
                    } catch (error) {
                        toast.error("Erro ao excluir usuário global.");
                    }
                }
            });
        } else {
            // Configura diálogo para REMOÇÃO LOCAL
            setConfirmData({
                title: 'Remover Acesso',
                message: `Deseja remover o acesso de "${usuarioAlvo.login}" desta empresa?\n(A conta dele continuará existindo em outras empresas)`,
                action: async () => {
                    try {
                        await removerUsuarioLocal(usuarioAlvo.id);
                        toast.success("Acesso removido com sucesso.");
                        loadData();
                    } catch (error) {
                        toast.error("Erro ao remover acesso local.");
                    }
                }
            });
        }
        setConfirmOpen(true);
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
                            onClick={handleNew}
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
                            {usuarios.map((u) => {
                                const isMasterRow = u.perfilNome === 'MASTER' || u.login === 'master';
                                const isMe = user?.login === u.login;

                                return (
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
                                            <Box display="flex" justifyContent="center" gap={1}>
                                                <Can I="USUARIO_EDITAR">
                                                    {(!isMasterRow || (isMasterRow && isMe)) && (
                                                        <Tooltip title={isMasterRow ? "Alterar minha senha" : "Editar Usuário"}>
                                                            <IconButton size="small" color="primary" onClick={() => handleEdit(u)}>
                                                                <Edit size={18} />
                                                            </IconButton>
                                                        </Tooltip>
                                                    )}
                                                </Can>

                                                <Can I="USUARIO_EXCLUIR">
                                                    {!isMasterRow && (
                                                        <Tooltip title="Remover Acesso / Excluir">
                                                            <IconButton
                                                                size="small"
                                                                color="error"
                                                                onClick={() => handleDeleteClick(u)} // Chama a nova função
                                                            >
                                                                <Trash2 size={18} />
                                                            </IconButton>
                                                        </Tooltip>
                                                    )}
                                                </Can>
                                            </Box>
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>

            <UsuarioForm
                open={modalOpen}
                onClose={handleCloseModal}
                usuario={usuarioEditando}
                onSuccess={() => { handleCloseModal(); loadData(); }}
            />

            {/* COMPONENTE DE CONFIRMAÇÃO */}
            <ConfirmDialog
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                onConfirm={confirmData.action}
                title={confirmData.title}
                message={confirmData.message}
            />
        </Box>
    );
};

export default UsuariosList;