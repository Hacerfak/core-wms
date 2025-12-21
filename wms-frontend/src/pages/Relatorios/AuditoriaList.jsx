import { useState, useEffect } from 'react';
import {
    Box, Typography, Paper, Table, TableBody, TableCell, TableContainer,
    TableHead, TableRow, Chip, IconButton, Tooltip, TextField, Button,
    Grid, MenuItem, Pagination, Dialog, DialogTitle, DialogContent, DialogActions
} from '@mui/material';
// CORREÇÃO 1: Alias no History para evitar conflito com window.History
import { Search, Eye, RefreshCw, History as HistoryIcon, FileJson } from 'lucide-react';
import { toast } from 'react-toastify';
import { getAuditLogs } from '../../services/auditService';
import dayjs from 'dayjs';

const AuditoriaList = () => {
    // Filtros Iniciais: Última Hora
    const [filtros, setFiltros] = useState({
        inicio: dayjs().subtract(1, 'hour').format('YYYY-MM-DDTHH:mm'),
        fim: dayjs().format('YYYY-MM-DDTHH:mm'),
        usuario: '',
        entidade: '',
        acao: 'TODAS' // CORREÇÃO 2: Valor explícito para o Select funcionar visualmente
    });

    const [logs, setLogs] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(false);

    // Modal de Detalhes (JSON)
    const [selectedLog, setSelectedLog] = useState(null);

    useEffect(() => {
        loadData();
    }, [page]);

    const loadData = async () => {
        setLoading(true);
        try {
            // Conversão para ISO string que o Java espera
            const filtrosApi = {
                ...filtros,
                inicio: filtros.inicio ? new Date(filtros.inicio).toISOString() : null,
                fim: filtros.fim ? new Date(filtros.fim).toISOString() : null,
                // CORREÇÃO 3: Se for 'TODAS', envia vazio para a API não filtrar
                acao: filtros.acao === 'TODAS' ? '' : filtros.acao
            };

            const data = await getAuditLogs(filtrosApi, page, 20);
            setLogs(data.content);
            setTotalPages(data.totalPages);
        } catch (error) {
            toast.error("Erro ao carregar logs de auditoria.");
        } finally {
            setLoading(false);
        }
    };

    const handleFilterChange = (e) => {
        setFiltros({ ...filtros, [e.target.name]: e.target.value });
    };

    const handleSearch = () => {
        setPage(0);
        loadData();
    };

    const getActionColor = (action) => {
        switch (action) {
            case 'INSERT': return 'success';
            case 'UPDATE': return 'info';
            case 'DELETE': return 'error';
            default: return 'default';
        }
    };

    return (
        // CORREÇÃO 4: width: '100%' garante que o componente preencha a área correta do layout
        <Box sx={{ width: '100%' }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                <Box>
                    <Typography variant="h5" fontWeight="bold" display="flex" alignItems="center" gap={1}>
                        <HistoryIcon size={28} /> Auditoria do Sistema
                    </Typography>
                    <Typography variant="body2" color="text.secondary">Rastreabilidade de eventos e alterações.</Typography>
                </Box>
                <Button variant="outlined" startIcon={<RefreshCw size={18} />} onClick={loadData} disabled={loading}>
                    Atualizar
                </Button>
            </Box>

            {/* FILTROS */}
            <Paper sx={{ p: 2, mb: 3 }}>
                <Grid container spacing={2} alignItems="center">
                    <Grid item xs={12} sm={6} md={2}>
                        <TextField
                            label="Início" type="datetime-local" fullWidth
                            name="inicio" value={filtros.inicio} onChange={handleFilterChange}
                            InputLabelProps={{ shrink: true }}
                        />
                    </Grid>
                    <Grid item xs={12} sm={6} md={2}>
                        <TextField
                            label="Fim" type="datetime-local" fullWidth
                            name="fim" value={filtros.fim} onChange={handleFilterChange}
                            InputLabelProps={{ shrink: true }}
                        />
                    </Grid>
                    <Grid item xs={12} sm={6} md={2}>
                        <TextField label="Usuário" fullWidth name="usuario" value={filtros.usuario} onChange={handleFilterChange} placeholder="Login..." />
                    </Grid>
                    <Grid item xs={12} sm={6} md={2}>
                        {/* O valor 'TODAS' agora bate com o MenuItem, exibindo o texto corretamente */}
                        <TextField select label="Ação" fullWidth name="acao" value={filtros.acao} onChange={handleFilterChange}>
                            <MenuItem value="TODAS">Todas</MenuItem>
                            <MenuItem value="INSERT">Criação (Insert)</MenuItem>
                            <MenuItem value="UPDATE">Edição (Update)</MenuItem>
                            <MenuItem value="DELETE">Exclusão (Delete)</MenuItem>
                        </TextField>
                    </Grid>
                    <Grid item xs={12} sm={6} md={2}>
                        <TextField label="Entidade" fullWidth name="entidade" value={filtros.entidade} onChange={handleFilterChange} placeholder="Ex: Produto" />
                    </Grid>
                    <Grid item xs={12} md={2}>
                        <Button variant="contained" fullWidth sx={{ height: 53 }} onClick={handleSearch} startIcon={<Search />}>
                            Filtrar
                        </Button>
                    </Grid>
                </Grid>
            </Paper>

            {/* TABELA */}
            <Paper sx={{ width: '100%', mb: 2, overflow: 'hidden' }}>
                <TableContainer>
                    <Table size="small">
                        <TableHead sx={{ bgcolor: 'background.subtle' }}>
                            <TableRow>
                                <TableCell><b>Data/Hora</b></TableCell>
                                <TableCell><b>Usuário</b></TableCell>
                                <TableCell><b>Ação</b></TableCell>
                                <TableCell><b>Entidade</b></TableCell>
                                <TableCell><b>ID Ref.</b></TableCell>
                                <TableCell align="center"><b>Detalhes</b></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {logs.map((log) => (
                                <TableRow key={log.id} hover>
                                    <TableCell>{new Date(log.dataHora).toLocaleString()}</TableCell>
                                    <TableCell>
                                        <Typography variant="body2" fontWeight="bold">{log.usuario}</Typography>
                                    </TableCell>
                                    <TableCell>
                                        <Chip label={log.action} color={getActionColor(log.action)} size="small" sx={{ fontWeight: 'bold', minWidth: 80 }} />
                                    </TableCell>
                                    <TableCell>{log.entityName}</TableCell>
                                    <TableCell>{log.entityId}</TableCell>
                                    <TableCell align="center">
                                        <Tooltip title="Ver Snapshot (JSON)">
                                            <IconButton size="small" color="primary" onClick={() => setSelectedLog(log)}>
                                                <Eye size={18} />
                                            </IconButton>
                                        </Tooltip>
                                    </TableCell>
                                </TableRow>
                            ))}
                            {!loading && logs.length === 0 && (
                                <TableRow><TableCell colSpan={6} align="center" sx={{ py: 4 }}>Nenhum registro encontrado no período.</TableCell></TableRow>
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>

                {/* PAGINAÇÃO */}
                <Box display="flex" justifyContent="center" p={2}>
                    <Pagination
                        count={totalPages}
                        page={page + 1}
                        onChange={(e, value) => setPage(value - 1)}
                        color="primary"
                    />
                </Box>
            </Paper>

            {/* MODAL JSON */}
            <Dialog open={!!selectedLog} onClose={() => setSelectedLog(null)} maxWidth="md" fullWidth>
                <DialogTitle display="flex" alignItems="center" gap={1}>
                    <FileJson size={24} /> Snapshot do Registro
                </DialogTitle>
                <DialogContent dividers>
                    {selectedLog && (
                        <Box>
                            <Typography variant="subtitle2" gutterBottom>
                                {selectedLog.action} em {selectedLog.entityName} (ID: {selectedLog.entityId})
                            </Typography>
                            <Typography variant="caption" color="text.secondary" display="block" mb={2}>
                                Realizado por {selectedLog.usuario} em {new Date(selectedLog.dataHora).toLocaleString()}
                            </Typography>

                            <Paper variant="outlined" sx={{ p: 2, bgcolor: '#1e293b', color: '#a5f3fc', overflowX: 'auto' }}>
                                <pre style={{ margin: 0, fontSize: '0.85rem' }}>
                                    {JSON.stringify(selectedLog.conteudo, null, 2)}
                                </pre>
                            </Paper>
                        </Box>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setSelectedLog(null)}>Fechar</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default AuditoriaList;