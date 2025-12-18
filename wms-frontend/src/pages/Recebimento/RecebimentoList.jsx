import { useState, useEffect } from 'react';
import { Box, Typography, Button, Paper, Chip, IconButton, Tooltip, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, LinearProgress } from '@mui/material';
import { Plus, Search, Eye, PlayCircle, Trash2, FileText, CheckCircle2, Clock } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getRecebimentos, deleteRecebimento } from '../../services/recebimentoService';
import api from '../../services/api';
import RecebimentoDetalhesModal from './RecebimentoDetalhesModal';
import Can from '../../components/Can';
import ConfirmDialog from '../../components/ConfirmDialog'; // <--- Import

const statusConfig = {
    AGUARDANDO: { color: 'warning', label: 'Aguardando', icon: <Clock size={14} /> },
    EM_CONFERENCIA: { color: 'info', label: 'Conferindo', icon: <PlayCircle size={14} /> },
    FINALIZADO: { color: 'success', label: 'Finalizado', icon: <CheckCircle2 size={14} /> },
    DIVERGENTE: { color: 'error', label: 'Divergente', icon: <Search size={14} /> }
};

const RecebimentoList = () => {
    const navigate = useNavigate();
    const [recebimentos, setRecebimentos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [modalOpen, setModalOpen] = useState(false);
    const [selectedRecebimento, setSelectedRecebimento] = useState(null);

    // Confirm State
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmAction, setConfirmAction] = useState(null);

    useEffect(() => { loadData(); }, []);

    const loadData = async () => {
        try {
            setLoading(true);
            const data = await getRecebimentos();
            setRecebimentos(data);
        } catch (error) {
            toast.error("Erro ao carregar recebimentos");
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteClick = (id, status) => {
        if (status === 'FINALIZADO') {
            toast.warning("Não é possível excluir um recebimento já finalizado!");
            return;
        }

        setConfirmAction(() => async () => {
            try {
                await deleteRecebimento(id);
                toast.success("Recebimento excluído");
                loadData();
            } catch (error) {
                toast.error("Erro ao excluir");
            }
        });
        setConfirmOpen(true);
    };

    const handleIniciarConferencia = (id) => navigate(`/recebimento/${id}/conferencia`);

    const handleViewDetails = async (id) => {
        try {
            const response = await api.get(`/api/recebimentos/${id}`);
            setSelectedRecebimento(response.data);
            setModalOpen(true);
        } catch (error) {
            toast.error("Erro ao carregar detalhes");
        }
    };

    return (
        <Box>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
                <Box>
                    <Typography variant="h5" fontWeight="bold" color="text.primary">Recebimentos</Typography>
                    <Typography variant="body2" color="text.secondary">Gerencie a entrada de notas fiscais e conferências</Typography>
                </Box>
                <Can I="RECEBIMENTO_IMPORTAR_XML">
                    <Button variant="contained" startIcon={<Plus size={20} />} onClick={() => navigate('/recebimento/novo')} sx={{ color: 'white' }}>
                        Nova Importação
                    </Button>
                </Can>
            </Box>

            <Paper sx={{ width: '100%', overflow: 'hidden', borderRadius: 2, boxShadow: '0 2px 10px rgba(0,0,0,0.05)' }}>
                {loading && <LinearProgress />}
                <TableContainer>
                    <Table sx={{ minWidth: 650 }} size="medium">
                        <TableHead sx={{ bgcolor: '#f8fafc' }}>
                            <TableRow>
                                <TableCell sx={{ fontWeight: 'bold' }}>Nota Fiscal</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }}>Depositante</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }}>Emissão</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }}>Importação</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }}>Status</TableCell>
                                <TableCell align="center" sx={{ fontWeight: 'bold' }}>Ações</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {recebimentos.length === 0 && !loading ? (
                                <TableRow><TableCell colSpan={5} align="center" sx={{ py: 4 }}><Typography color="text.secondary">Nenhum recebimento encontrado.</Typography></TableCell></TableRow>
                            ) : (
                                recebimentos.map((row) => {
                                    const statusInfo = statusConfig[row.status] || statusConfig.AGUARDANDO;
                                    const formatarData = (dataStr) => { if (!dataStr) return '-'; try { return new Date(dataStr).toLocaleDateString(); } catch { return '-'; } };

                                    return (
                                        <TableRow key={row.id} hover sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                                            <TableCell>
                                                <Box display="flex" alignItems="center" gap={1.5}>
                                                    <Box p={1} bgcolor="primary.light" borderRadius={1} color="white"><FileText size={18} /></Box>
                                                    <Typography fontWeight={500}>{row.numNotaFiscal}</Typography>
                                                </Box>
                                            </TableCell>
                                            <TableCell>{row.fornecedor}</TableCell>
                                            <TableCell>{formatarData(row.dataEmissao)}</TableCell>
                                            <TableCell>{formatarData(row.dataCriacao)}</TableCell>
                                            <TableCell><Chip icon={statusInfo.icon} label={statusInfo.label} color={statusInfo.color} size="small" variant="outlined" sx={{ fontWeight: 600 }} /></TableCell>
                                            <TableCell align="center">
                                                <Box display="flex" justifyContent="center" gap={1}>
                                                    {row.status !== 'FINALIZADO' && (
                                                        <Tooltip title="Conferir / Bipar"><IconButton color="primary" size="small" onClick={() => handleIniciarConferencia(row.id)}><PlayCircle size={18} /></IconButton></Tooltip>
                                                    )}
                                                    <Tooltip title="Detalhes"><IconButton size="small" onClick={() => handleViewDetails(row.id)}><Eye size={18} /></IconButton></Tooltip>
                                                    <Can I="RECEBIMENTO_CANCELAR">
                                                        <Tooltip title="Excluir"><IconButton color="error" size="small" onClick={() => handleDeleteClick(row.id, row.status)}><Trash2 size={18} /></IconButton></Tooltip>
                                                    </Can>
                                                </Box>
                                            </TableCell>
                                        </TableRow>
                                    );
                                })
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>
            {modalOpen && <RecebimentoDetalhesModal open={modalOpen} onClose={() => setModalOpen(false)} data={selectedRecebimento} />}

            {/* Dialog */}
            <ConfirmDialog
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                onConfirm={confirmAction}
                title="Excluir Recebimento"
                message="Tem certeza que deseja excluir esta nota?"
            />
        </Box>
    );
};

export default RecebimentoList;