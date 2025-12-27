import { useState, useEffect } from 'react';
import {
    Box, Typography, Button, Grid, Card, CardContent, Chip,
    IconButton, Tooltip, InputAdornment, TextField, Divider, Paper, MenuItem
} from '@mui/material';
import {
    Plus, Search, Copy, Truck, Calendar, FileText,
    ArrowRight, Anchor, PackagePlus, Trash2, Filter, Eye, AlertCircle
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import dayjs from 'dayjs';

import { getRecebimentos, excluirSolicitacao } from '../../services/recebimentoService';
import AtribuirDocaModal from './Components/AtribuirDocaModal';
import ConfirmDialog from '../../components/ConfirmDialog';

const STATUS_MAP = {
    'CRIADA': { label: 'NOVA', color: 'default' },
    'AGUARDANDO_EXECUCAO': { label: 'AGUARDANDO', color: 'info' },
    'EM_PROCESSAMENTO': { label: 'PROCESSANDO', color: 'primary' },
    'DIVERGENTE': { label: 'DIVERGENTE', color: 'warning' },
    'BLOQUEADA': { label: 'BLOQUEADA', color: 'error' },
    'CONCLUIDA': { label: 'CONCLUÍDA', color: 'success' },
    'CANCELADA': { label: 'CANCELADA', color: 'error' }
};

const RecebimentoList = () => {
    const navigate = useNavigate();
    const [items, setItems] = useState([]);
    const [filtered, setFiltered] = useState([]);
    const [loading, setLoading] = useState(true);

    // Filtros
    const [search, setSearch] = useState('');
    const [statusFilter, setStatusFilter] = useState('TODOS');
    const [dataFilter, setDataFilter] = useState('');

    // Modais
    const [docaModalOpen, setDocaModalOpen] = useState(false);
    const [selectedSolicitacao, setSelectedSolicitacao] = useState(null);
    const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
    const [itemToDelete, setItemToDelete] = useState(null);

    useEffect(() => { loadData(); }, []);

    // Lógica de Filtragem
    useEffect(() => {
        let result = items;
        const term = search.toLowerCase();

        if (search) {
            result = result.filter(i =>
                (i.notaFiscal && i.notaFiscal.toLowerCase().includes(term)) ||
                (i.fornecedorNome && i.fornecedorNome.toLowerCase().includes(term)) ||
                (i.codigoExterno && i.codigoExterno.toLowerCase().includes(term))
            );
        }
        if (statusFilter !== 'TODOS') {
            result = result.filter(i => i.status === statusFilter);
        }
        if (dataFilter) {
            result = result.filter(i => i.dataCriacao && i.dataCriacao.startsWith(dataFilter));
        }
        setFiltered(result);
    }, [search, statusFilter, dataFilter, items]);

    const loadData = async () => {
        setLoading(true);
        try {
            const data = await getRecebimentos();
            setItems(data);
            setFiltered(data);
        } catch (error) {
            toast.error("Erro ao carregar recebimentos.");
        } finally {
            setLoading(false);
        }
    };

    const handleCopyKey = (key) => {
        if (!key) return toast.info("Chave indisponível.");
        navigator.clipboard.writeText(key);
        toast.success("Chave copiada!");
    };

    const handleDeleteClick = (item) => {
        setItemToDelete(item);
        setConfirmDeleteOpen(true);
    };

    const confirmDelete = async () => {
        if (!itemToDelete) return;
        try {
            await excluirSolicitacao(itemToDelete.id);
            toast.success("Solicitação excluída/cancelada.");
            loadData();
        } catch (error) {
            toast.error("Erro ao excluir solicitação.");
        }
        setConfirmDeleteOpen(false);
    };

    return (
        <Box>
            {/* Header */}
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                <Box display="flex" alignItems="center" gap={2}>
                    <Typography variant="h5" fontWeight="bold">Recebimentos</Typography>
                    <Typography variant="body2" color="text.secondary"> | Gestão de NFe</Typography>
                </Box>
                <Button variant="contained" startIcon={<Plus />} onClick={() => navigate('/recebimento/novo')}>
                    Nova Entrada
                </Button>
            </Box>

            {/* Filtros */}
            <Paper sx={{ p: 2, mb: 3, borderRadius: 2 }}>
                <Grid container spacing={2} alignItems="center">
                    <Grid item xs={12} sm={2}>
                        <TextField
                            type="date" label="Data" fullWidth size="small"
                            InputLabelProps={{ shrink: true }}
                            value={dataFilter}
                            onChange={e => setDataFilter(e.target.value)}
                        />
                    </Grid>
                    <Grid item xs={12} sm={2}>
                        <TextField
                            select label="Status" fullWidth size="small"
                            value={statusFilter} onChange={e => setStatusFilter(e.target.value)}
                        >
                            <MenuItem value="TODOS">Todos</MenuItem>
                            {Object.keys(STATUS_MAP).map(key => (
                                <MenuItem key={key} value={key}>{STATUS_MAP[key].label}</MenuItem>
                            ))}
                        </TextField>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField
                            fullWidth placeholder="Buscar por Nota, Fornecedor ou ID..."
                            value={search} onChange={(e) => setSearch(e.target.value)}
                            size="small"
                            InputProps={{ startAdornment: <InputAdornment position="start"><Search size={20} color="#94a3b8" /></InputAdornment> }}
                        />
                    </Grid>
                    <Grid item xs={12} sm={2}>
                        <Button variant="outlined" fullWidth onClick={loadData} startIcon={<Filter size={18} />}>Atualizar</Button>
                    </Grid>
                </Grid>
            </Paper>

            {/* Grid de Cards (Estilo Agendamento) */}
            <Grid container spacing={2}>
                {filtered.map((item) => {
                    const statusInfo = STATUS_MAP[item.status] || { label: item.status, color: 'default' };

                    return (
                        <Grid item xs={12} key={item.id}>
                            <Card
                                elevation={0}
                                sx={{
                                    border: '1px solid #e2e8f0', borderRadius: 3,
                                    borderLeft: '6px solid', borderLeftColor: `${statusInfo.color}.main`,
                                    transition: '0.2s', opacity: item.status === 'CANCELADA' ? 0.6 : 1,
                                    '&:hover': { boxShadow: '0 4px 12px rgba(0,0,0,0.08)', transform: 'translateY(-2px)' }
                                }}
                            >
                                <CardContent sx={{ p: 3, '&:last-child': { pb: 3 } }}>
                                    <Grid container alignItems="center" spacing={3}>

                                        {/* COLUNA 1: HORA/DATA/ID */}
                                        <Grid item xs={12} sm={2} md={1.5} textAlign="center" sx={{ borderRight: { sm: '1px solid #f1f5f9' } }}>
                                            <Typography variant="h5" fontWeight="bold" color="text.primary">
                                                {item.notaFiscal || 'S/N'}
                                            </Typography>
                                            <Typography variant="caption" color="text.secondary" display="block">
                                                {dayjs(item.dataCriacao).format('DD/MM HH:mm')}
                                            </Typography>

                                            <Tooltip title="Copiar Chave">
                                                <Chip
                                                    label={`#${item.codigoExterno || item.id}`}
                                                    size="small"
                                                    icon={<Copy size={12} />}
                                                    onClick={() => handleCopyKey(item.chaveAcesso)}
                                                    sx={{ mt: 1, fontFamily: 'monospace', fontWeight: 'bold', bgcolor: '#f1f5f9', cursor: 'pointer', maxWidth: '100%' }}
                                                />
                                            </Tooltip>
                                        </Grid>

                                        {/* COLUNA 2: DADOS PRINCIPAIS */}
                                        <Grid item xs={12} sm={6} md={6.5}>
                                            <Box display="flex" alignItems="center" gap={1.5} mb={1} flexWrap="wrap">
                                                <Typography variant="h6" fontWeight="bold" noWrap sx={{ maxWidth: '100%' }}>
                                                    {item.fornecedorNome || 'Fornecedor Desconhecido'}
                                                </Typography>
                                                {item.chaveAcesso && <Chip label="XML" size="small" variant="outlined" color="primary" icon={<FileText size={14} />} />}
                                            </Box>

                                            <Box display="flex" gap={4} color="text.secondary" flexWrap="wrap">
                                                <Box display="flex" alignItems="center" gap={1}>
                                                    <Calendar size={18} />
                                                    <Typography variant="body2">Emissão: {item.dataEmissao ? dayjs(item.dataEmissao).format('DD/MM/YYYY') : '-'}</Typography>
                                                </Box>

                                                <Box display="flex" alignItems="center" gap={1}>
                                                    <Anchor size={18} color={item.docaNome ? "#059669" : "#dc2626"} />
                                                    {item.docaNome ? (
                                                        <Typography variant="body2" fontWeight="bold" color="success.main">{item.docaNome}</Typography>
                                                    ) : (
                                                        <Box display="flex" alignItems="center" gap={1}>
                                                            <Typography variant="body2" color="error">Sem Doca</Typography>
                                                            {item.status !== 'CONCLUIDA' && item.status !== 'CANCELADA' && (
                                                                <Button size="small" variant="outlined" color="warning"
                                                                    onClick={() => { setSelectedSolicitacao(item); setDocaModalOpen(true); }}
                                                                    sx={{ py: 0, fontSize: '0.7rem', height: 24 }}
                                                                >
                                                                    Atribuir
                                                                </Button>
                                                            )}
                                                        </Box>
                                                    )}
                                                </Box>
                                            </Box>
                                        </Grid>

                                        {/* COLUNA 3: AÇÕES E STATUS */}
                                        <Grid item xs={12} sm={4} md={4} display="flex" justifyContent={{ xs: 'flex-start', sm: 'flex-end' }} alignItems="center" gap={1} flexWrap="wrap">

                                            {/* Botões de Ação */}
                                            {item.status !== 'CANCELADA' && item.status !== 'CONCLUIDA' && (
                                                <>
                                                    <Tooltip title="Excluir">
                                                        <IconButton size="small" color="error" onClick={() => handleDeleteClick(item)}>
                                                            <Trash2 size={20} />
                                                        </IconButton>
                                                    </Tooltip>

                                                    {item.docaId && (
                                                        <Tooltip title="Conferência">
                                                            <IconButton color="primary" onClick={() => navigate(`/recebimento/${item.id}/conferencia`)}>
                                                                <PackagePlus size={22} />
                                                            </IconButton>
                                                        </Tooltip>
                                                    )}
                                                </>
                                            )}

                                            <Tooltip title="Detalhes">
                                                <IconButton onClick={() => navigate(`/recebimento/${item.id}/detalhes`)}>
                                                    <Eye size={20} />
                                                </IconButton>
                                            </Tooltip>

                                            <Chip
                                                label={statusInfo.label}
                                                color={statusInfo.color}
                                                size="small"
                                                sx={{ fontWeight: 'bold', ml: 1, height: 28 }}
                                            />
                                        </Grid>

                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>
                    );
                })}
            </Grid>

            <AtribuirDocaModal open={docaModalOpen} onClose={() => setDocaModalOpen(false)} solicitacao={selectedSolicitacao} onSuccess={loadData} />

            <ConfirmDialog
                open={confirmDeleteOpen} onClose={() => setConfirmDeleteOpen(false)} onConfirm={confirmDelete}
                title="Excluir Solicitação"
                message={`Tem certeza? Se houver movimentações, ela será apenas cancelada.\nCaso contrário, será removida do sistema.`}
                severity="error"
            />
        </Box>
    );
};

export default RecebimentoList;