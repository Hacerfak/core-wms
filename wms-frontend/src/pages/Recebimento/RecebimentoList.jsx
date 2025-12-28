import { useState, useEffect } from 'react';
import {
    Box, Typography, Button, Paper, Chip, IconButton, Tooltip,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    LinearProgress, Dialog, DialogTitle, DialogContent, DialogActions
} from '@mui/material';
import {
    Plus, Search, PlayCircle, Trash2, FileText,
    CheckCircle2, Clock, Truck, Warehouse, LogOut, ArrowLeft, Anchor,
    HousePlus
} from 'lucide-react'; // Adicionado Anchor para "Encostar"
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../../services/api';
import { getRecebimentos, deleteRecebimento } from '../../services/recebimentoService';
import { getLocalizacoes } from '../../services/localizacaoService';
import SearchableSelect from '../../components/SearchableSelect';
import ConfirmDialog from '../../components/ConfirmDialog';
import Can from '../../components/Can';

const statusConfig = {
    CRIADA: { color: 'default', label: 'Nova', icon: <HousePlus size={14} /> },
    AGUARDANDO_EXECUCAO: { color: 'warning', label: 'Aguardando Início', icon: <Clock size={14} /> },
    EM_PROCESSAMENTO: { color: 'info', label: 'Conferindo', icon: <PlayCircle size={14} /> },
    CONCLUIDA: { color: 'success', label: 'Concluída', icon: <CheckCircle2 size={14} /> },
    BLOQUEDA: { color: 'error', label: 'Bloqueada', icon: <Search size={14} /> }
};

const RecebimentoList = () => {
    const navigate = useNavigate();
    const [recebimentos, setRecebimentos] = useState([]);
    const [loading, setLoading] = useState(true);

    // Modais
    const [modalEncostar, setModalEncostar] = useState(false);
    const [modalLiberar, setModalLiberar] = useState(false);
    const [docas, setDocas] = useState([]);
    const [selectedDoca, setSelectedDoca] = useState('');
    const [selectedItem, setSelectedItem] = useState(null);
    const [assinaturaFile, setAssinaturaFile] = useState(null);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmAction, setConfirmAction] = useState(null);

    useEffect(() => { loadData(); }, []);

    const loadData = async () => {
        setLoading(true);
        try {
            const data = await getRecebimentos();
            setRecebimentos(data);
        } catch (error) { toast.error("Erro ao carregar lista"); }
        finally { setLoading(false); }
    };

    // --- LÓGICA DE ESTADO REFINADA ---
    const getEstadoLogistico = (row) => {
        // 1. Agendado
        if (row.agendamentoId) {
            if (row.statusAgendamento === 'NA_DOCA') return 'NA_DOCA';
            if (row.statusAgendamento === 'AGUARDANDO_SAIDA') return 'LIBERADO';
            if (row.statusAgendamento === 'FINALIZADO') return 'SAIU';

            // Se está no Pátio (NA_PORTARIA) mas JÁ TEM doca definida (planejada)
            if (row.statusAgendamento === 'NA_PORTARIA' && row.docaNome) return 'DOCA_PLANEJADA';

            return 'NO_PATIO';
        }

        // 2. Manual
        if (row.docaNome) return 'NA_DOCA'; // No manual, se tem doca, consideramos encostado
        return 'NO_PATIO';
    };

    // Ação: Encostar (Atribui Doca se necessário e confirma chegada)
    const handleOpenEncostar = async (item) => {
        setSelectedItem(item);

        // Se já tem doca planejada, podemos pular a seleção e só confirmar
        // Mas vamos abrir o modal para permitir troca se necessário
        try {
            const locais = await getLocalizacoes('DOCA');
            setDocas(locais.map(d => ({ value: d.id, label: d.enderecoCompleto })));
            setSelectedDoca(item.docaId || '');
            setModalEncostar(true);
        } catch (e) { toast.error("Erro ao carregar docas"); }
    };

    const confirmEncostar = async () => {
        // Para Manual, Doca é obrigatória.
        // Para Agendado com doca já planejada, se o usuário não selecionar, usamos a existente no backend
        if (!selectedDoca && !selectedItem.docaId) return toast.warning("Selecione uma doca.");

        const docaFinal = selectedDoca || selectedItem.docaId;

        try {
            await api.post(`/api/operacao-patio/entrada/${selectedItem.id}/encostar?docaId=${docaFinal}`);
            toast.success("Veículo encostado! Tarefa gerada.");
            setModalEncostar(false);
            loadData();
        } catch (e) { toast.error(e.response?.data?.message || "Erro ao encostar."); }
    };

    const handleOpenLiberar = (item) => {
        setSelectedItem(item);
        setAssinaturaFile(null);
        setModalLiberar(true);
    };

    const confirmLiberar = async () => {
        if (!assinaturaFile) return toast.warning("Assinatura obrigatória.");
        const formData = new FormData();
        formData.append('assinatura', assinaturaFile);

        try {
            await api.post(`/api/operacao-patio/entrada/${selectedItem.id}/liberar`, formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
            toast.success("Doca liberada! Veículo enviado para saída.");
            setModalLiberar(false);
            loadData();
        } catch (e) { toast.error(e.response?.data?.message || "Erro ao liberar."); }
    };

    const handleIniciarConferencia = (id) => navigate(`/recebimento/${id}/conferencia`);

    const handleDeleteClick = (id, status) => {
        if (status !== 'CRIADA' && status !== 'AGUARDANDO_EXECUCAO') return toast.warning("Em andamento.");
        setConfirmAction(() => async () => {
            try { await deleteRecebimento(id); toast.success("Excluído"); loadData(); }
            catch (e) { toast.error("Erro ao excluir"); }
        });
        setConfirmOpen(true);
    };

    return (
        <Box>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
                <Box display="flex" alignItems="center" gap={2}>
                    <Button startIcon={<ArrowLeft />} onClick={() => navigate('/recebimento')} color="inherit">Voltar</Button>
                    <Box>
                        <Typography variant="h5" fontWeight="bold">Solicitações de Entrada</Typography>
                        <Typography variant="body2" color="text.secondary">Gestão de pátio e descarga.</Typography>
                    </Box>
                </Box>
                <Can I="RECEBIMENTO_IMPORTAR_XML">
                    <Button variant="contained" startIcon={<Plus size={18} />} onClick={() => navigate('/recebimento/importar')}>
                        Importar XML
                    </Button>
                </Can>
            </Box>

            <Paper sx={{ width: '100%', overflow: 'hidden', borderRadius: 2 }}>
                {loading && <LinearProgress />}
                <TableContainer>
                    <Table size="medium">
                        <TableHead sx={{ bgcolor: '#f8fafc' }}>
                            <TableRow>
                                <TableCell><b>Nota Fiscal</b></TableCell>
                                <TableCell><b>Fornecedor</b></TableCell>
                                <TableCell><b>Veículo</b></TableCell>
                                <TableCell><b>Doca</b></TableCell>
                                <TableCell><b>Status</b></TableCell>
                                <TableCell align="center"><b>Ações</b></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {recebimentos.map((row) => {
                                const statusProc = statusConfig[row.status] || statusConfig.CRIADA;
                                const estado = getEstadoLogistico(row);

                                return (
                                    <TableRow key={row.id} hover>
                                        <TableCell>
                                            <Box display="flex" alignItems="center" gap={1}>
                                                <FileText size={16} color="#64748b" />
                                                <Typography fontWeight={500}>{row.codigoExterno}</Typography>
                                            </Box>
                                        </TableCell>
                                        <TableCell>{row.fornecedorNome}</TableCell>

                                        <TableCell>
                                            {row.placaVeiculo ? (
                                                <Box>
                                                    <Typography variant="caption" fontWeight="bold">{row.placaVeiculo}</Typography>
                                                    <Chip label={row.agendamentoId ? "Agendado" : "Manual"} size="small" variant="outlined" sx={{ ml: 1, height: 20, fontSize: '0.6rem' }} />
                                                </Box>
                                            ) : <Typography variant="caption" color="text.secondary">Sem Veículo</Typography>}
                                        </TableCell>

                                        <TableCell>
                                            {/* Mostra doca se estiver definida, mesmo que não encostado */}
                                            {row.docaNome ? (
                                                <Chip
                                                    icon={<Warehouse size={12} />}
                                                    label={row.docaNome}
                                                    // Amarelo se planejado, Azul se encostado
                                                    color={estado === 'NA_DOCA' ? 'primary' : (estado === 'DOCA_PLANEJADA' ? 'warning' : 'default')}
                                                    size="small"
                                                />
                                            ) : '-'}
                                        </TableCell>

                                        <TableCell>
                                            <Chip label={statusProc.label} color={statusProc.color} size="small" icon={statusProc.icon} />
                                        </TableCell>

                                        <TableCell align="center">
                                            <Box display="flex" justifyContent="center" gap={1}>

                                                {/* 1. ENCOSTAR / CONFIRMAR CHEGADA
                                                    Aparece se:
                                                    - Está no pátio (Sem doca)
                                                    - OU tem doca planejada mas ainda não está NA_DOCA (Agendamento)
                                                */}
                                                {(estado === 'NO_PATIO' || estado === 'DOCA_PLANEJADA') && row.status !== 'CANCELADA' && (
                                                    <Tooltip title={estado === 'DOCA_PLANEJADA' ? "Confirmar chegada na doca" : "Atribuir Doca"}>
                                                        <IconButton
                                                            size="small"
                                                            color={estado === 'DOCA_PLANEJADA' ? "warning" : "primary"}
                                                            onClick={() => handleOpenEncostar(row)}
                                                        >
                                                            {estado === 'DOCA_PLANEJADA' ? <Anchor size={18} /> : <Truck size={18} />}
                                                        </IconButton>
                                                    </Tooltip>
                                                )}

                                                {/* 2. LIBERAR SAÍDA */}
                                                {estado === 'NA_DOCA' && (
                                                    <Tooltip title="Liberar Doca (Retornar ao Pátio)">
                                                        <IconButton size="small" color="success" onClick={() => handleOpenLiberar(row)}>
                                                            <LogOut size={18} />
                                                        </IconButton>
                                                    </Tooltip>
                                                )}

                                                {/* 3. CONFERIR (Somente se tarefa gerada) */}
                                                {(row.status === 'AGUARDANDO_EXECUCAO' || row.status === 'EM_PROCESSAMENTO') && (
                                                    <Tooltip title="Conferir">
                                                        <IconButton size="small" color="info" onClick={() => handleIniciarConferencia(row.id)}>
                                                            <PlayCircle size={18} />
                                                        </IconButton>
                                                    </Tooltip>
                                                )}

                                                {(row.status === 'CRIADA') && (
                                                    <IconButton size="small" color="error" onClick={() => handleDeleteClick(row.id, row.status)}>
                                                        <Trash2 size={18} />
                                                    </IconButton>
                                                )}
                                            </Box>
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>

            {/* MODAL ENCOSTAR */}
            <Dialog open={modalEncostar} onClose={() => setModalEncostar(false)} maxWidth="xs" fullWidth>
                <DialogTitle>
                    {selectedItem?.docaNome ? "Confirmar Chegada na Doca" : "Atribuir Doca"}
                </DialogTitle>
                <DialogContent>
                    <Typography variant="body2" gutterBottom>
                        {selectedItem?.docaNome
                            ? `O veículo está fisicamente na doca ${selectedItem.docaNome}?`
                            : "Selecione a doca para direcionar o veículo."}
                    </Typography>
                    <Box mt={2}>
                        <SearchableSelect
                            label="Doca"
                            options={docas}
                            value={selectedDoca}
                            onChange={e => setSelectedDoca(e.target.value)}
                        />
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setModalEncostar(false)}>Cancelar</Button>
                    <Button variant="contained" onClick={confirmEncostar}>
                        {selectedItem?.docaNome ? "Confirmar e Iniciar" : "Atribuir e Iniciar"}
                    </Button>
                </DialogActions>
            </Dialog>

            {/* MODAL LIBERAR */}
            <Dialog open={modalLiberar} onClose={() => setModalLiberar(false)} maxWidth="xs" fullWidth>
                <DialogTitle>Liberar Doca</DialogTitle>
                <DialogContent>
                    <Typography variant="body2" gutterBottom>Anexe a assinatura para liberar a doca e devolver o veículo ao pátio.</Typography>
                    <Button variant="outlined" component="label" fullWidth sx={{ mt: 2, height: 100, borderStyle: 'dashed' }}>
                        {assinaturaFile ? assinaturaFile.name : "Anexar Assinatura"}
                        <input type="file" hidden accept="image/*" onChange={e => setAssinaturaFile(e.target.files[0])} />
                    </Button>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setModalLiberar(false)}>Cancelar</Button>
                    <Button variant="contained" color="success" onClick={confirmLiberar} disabled={!assinaturaFile}>Liberar</Button>
                </DialogActions>
            </Dialog>

            <ConfirmDialog open={confirmOpen} onClose={() => setConfirmOpen(false)} onConfirm={confirmAction} title="Excluir" message="Confirma exclusão?" />
        </Box>
    );
};

export default RecebimentoList;