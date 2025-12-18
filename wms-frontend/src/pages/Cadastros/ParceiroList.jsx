import { useState, useEffect } from 'react';
import {
    Box, Typography, Button, Paper, Table, TableBody, TableCell,
    TableContainer, TableHead, TableRow, Chip, IconButton, Tooltip, LinearProgress
} from '@mui/material';
import { Plus, Trash2, Edit, Building2, MapPin } from 'lucide-react';
import { toast } from 'react-toastify';
import { getParceiros, excluirParceiro } from '../../services/parceiroService';
import ParceiroForm from './ParceiroForm';
import ConfirmDialog from '../../components/ConfirmDialog';
import Can from '../../components/Can';

const ParceiroList = () => {
    const [parceiros, setParceiros] = useState([]);
    const [loading, setLoading] = useState(true);

    const [modalOpen, setModalOpen] = useState(false);
    const [parceiroEditando, setParceiroEditando] = useState(null);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmAction, setConfirmAction] = useState(null);

    useEffect(() => { loadData(); }, []);

    const loadData = async () => {
        try {
            setLoading(true);
            const data = await getParceiros();
            setParceiros(data);
        } catch (error) {
            toast.error("Erro ao carregar parceiros");
        } finally {
            setLoading(false);
        }
    };

    const handleNew = () => { setParceiroEditando(null); setModalOpen(true); };
    const handleEdit = (parceiro) => { setParceiroEditando(parceiro); setModalOpen(true); };

    const handleDeleteClick = (id) => {
        setConfirmAction(() => async () => {
            try {
                await excluirParceiro(id);
                toast.success("Parceiro excluído com sucesso.");
                loadData();
            } catch (error) {
                toast.error("Erro ao excluir parceiro.");
            }
        });
        setConfirmOpen(true);
    };

    return (
        <Box>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
                <Box>
                    <Typography variant="h5" fontWeight="bold">Parceiros de Negócio</Typography>
                    <Typography variant="body2" color="text.secondary">Fornecedores e Clientes</Typography>
                </Box>
                <Can I="PARCEIRO_CRIAR">
                    <Button variant="contained" startIcon={<Plus size={20} />} onClick={handleNew}>
                        Novo Parceiro
                    </Button>
                </Can>
            </Box>

            <Paper sx={{ width: '100%', overflow: 'hidden', borderRadius: 2 }}>
                {loading && <LinearProgress />}
                <TableContainer>
                    <Table>
                        <TableHead sx={{ bgcolor: 'background.subtle' }}>
                            <TableRow>
                                <TableCell><b>Razão Social / Nome</b></TableCell>
                                <TableCell><b>Documento</b></TableCell>
                                <TableCell><b>Tipo</b></TableCell>
                                <TableCell><b>Localização</b></TableCell>
                                <TableCell><b>Status</b></TableCell>
                                <TableCell align="center"><b>Ações</b></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {parceiros.map((p) => (
                                <TableRow key={p.id} hover>
                                    <TableCell>
                                        <Box display="flex" alignItems="center" gap={1.5}>
                                            <Box p={1} bgcolor="primary.light" borderRadius={1} color="white">
                                                <Building2 size={18} />
                                            </Box>
                                            <Box>
                                                <Typography fontWeight={500} variant="body2">{p.nome}</Typography>
                                                <Typography variant="caption" color="text.secondary">
                                                    {p.email || p.telefone || 'Sem contato'}
                                                </Typography>
                                            </Box>
                                        </Box>
                                    </TableCell>
                                    {/* CORREÇÃO AQUI: Usa p.documento ao invés de p.cnpjCpf */}
                                    <TableCell>{p.documento}</TableCell>
                                    <TableCell>
                                        <Chip
                                            label={p.tipo}
                                            size="small"
                                            color={p.tipo === 'FORNECEDOR' ? 'info' : 'secondary'}
                                            variant="outlined"
                                        />
                                    </TableCell>
                                    {/* CORREÇÃO AQUI: Usa p.cidade direto (não tem p.endereco) */}
                                    <TableCell>
                                        <Box display="flex" alignItems="center" gap={0.5} color="text.secondary">
                                            <MapPin size={14} />
                                            <Typography variant="body2">
                                                {p.cidade ? `${p.cidade}/${p.uf}` : '-'}
                                            </Typography>
                                        </Box>
                                    </TableCell>
                                    <TableCell>
                                        <Chip
                                            label={p.ativo ? "Ativo" : "Inativo"}
                                            color={p.ativo ? "success" : "default"}
                                            size="small"
                                        />
                                    </TableCell>
                                    <TableCell align="center">
                                        <Box display="flex" justifyContent="center" gap={1}>
                                            <Can I="PARCEIRO_EDITAR">
                                                <Tooltip title="Editar">
                                                    <IconButton size="small" color="primary" onClick={() => handleEdit(p)}>
                                                        <Edit size={18} />
                                                    </IconButton>
                                                </Tooltip>
                                            </Can>
                                            <Can I="PARCEIRO_EXCLUIR">
                                                <Tooltip title="Excluir">
                                                    <IconButton size="small" color="error" onClick={() => handleDeleteClick(p.id)}>
                                                        <Trash2 size={18} />
                                                    </IconButton>
                                                </Tooltip>
                                            </Can>
                                        </Box>
                                    </TableCell>
                                </TableRow>
                            ))}
                            {!loading && parceiros.length === 0 && (
                                <TableRow>
                                    <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                                        <Typography color="text.secondary">Nenhum parceiro cadastrado.</Typography>
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>

            <ParceiroForm
                open={modalOpen}
                onClose={() => setModalOpen(false)}
                parceiro={parceiroEditando}
                onSuccess={() => { setModalOpen(false); loadData(); }}
            />

            <ConfirmDialog
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                onConfirm={confirmAction}
                title="Excluir Parceiro"
                message="Deseja realmente excluir este parceiro? Histórico de movimentações pode ser afetado."
            />
        </Box>
    );
};

export default ParceiroList;