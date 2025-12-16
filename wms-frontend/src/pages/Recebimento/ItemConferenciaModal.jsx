import { useState, useEffect } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button,
    Typography, Box, Table, TableBody, TableCell, TableContainer,
    TableHead, TableRow, IconButton, CircularProgress, Tooltip
} from '@mui/material';
import { Printer, X, Box as BoxIcon, CheckCircle2, HelpCircle } from 'lucide-react'; // Adicionei HelpCircle
import { toast } from 'react-toastify';
import { getVolumesDoItem } from '../../services/recebimentoService';

// RECEBE A NOVA PROP: exibirQtdEsperada
const ItemConferenciaModal = ({ open, onClose, recebimentoId, item, exibirQtdEsperada }) => {
    const [volumes, setVolumes] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (open && item) {
            loadLpns();
        }
    }, [open, item]);

    const loadLpns = async () => {
        try {
            setLoading(true);
            const data = await getVolumesDoItem(recebimentoId, item.produto.id);
            setVolumes(data);
        } catch (error) {
            toast.error("Erro ao carregar LPNs do item");
        } finally {
            setLoading(false);
        }
    };

    const handleReprint = (lpn) => {
        toast.success(`Enviando LPN ${lpn} para impressora...`);
    };

    if (!item) return null;

    const qtdConf = item.quantidadeConferida || 0;
    const qtdNota = item.quantidadeNota;

    // LÓGICA BLINDADA: Só mostra completo se NÃO for cego
    const isCompleto = exibirQtdEsperada && (qtdConf >= qtdNota);

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', bgcolor: '#f8fafc' }}>
                <Box>
                    <Typography variant="subtitle2" color="text.secondary">Gerenciar Item</Typography>
                    <Typography variant="h6" fontWeight="bold">{item.produto.sku}</Typography>
                    <Typography variant="caption">{item.produto.nome}</Typography>
                </Box>
                <IconButton onClick={onClose}><X size={20} /></IconButton>
            </DialogTitle>

            <DialogContent dividers>
                {/* Card de Status do Item */}
                <Box sx={{
                    p: 2, mb: 3, borderRadius: 2,
                    // Se for cego, fica sempre com cor neutra (azul/cinza)
                    bgcolor: isCompleto ? '#dcfce7' : '#eff6ff',
                    border: '1px solid',
                    borderColor: isCompleto ? '#86efac' : '#bfdbfe'
                }}>
                    <Box display="flex" justifyContent="space-between" alignItems="center">
                        <Box display="flex" alignItems="center" gap={1}>
                            {/* Ícone muda se for cego */}
                            {isCompleto ? (
                                <CheckCircle2 color="#16a34a" />
                            ) : (
                                exibirQtdEsperada ? <BoxIcon color="#64748b" /> : <HelpCircle color="#3b82f6" />
                            )}

                            <Typography fontWeight="bold" color={isCompleto ? 'success.main' : 'text.primary'}>
                                {isCompleto ? "Conferência Concluída" : "Em Andamento"}
                            </Typography>
                        </Box>

                        {/* AQUI ESTÁ O SEGREDO: ESCONDER O TOTAL */}
                        <Typography variant="h5" fontWeight="bold">
                            {qtdConf}
                            {exibirQtdEsperada && (
                                <Typography component="span" variant="h6" color="text.secondary"> / {qtdNota}</Typography>
                            )}
                        </Typography>
                    </Box>

                    {!exibirQtdEsperada && (
                        <Typography variant="caption" color="primary" sx={{ mt: 1, display: 'block' }}>
                            * Modo Cego: Quantidade total oculta.
                        </Typography>
                    )}
                </Box>

                <Typography variant="subtitle2" fontWeight="bold" mb={1} display="flex" alignItems="center" gap={1}>
                    <Printer size={16} /> Etiquetas Geradas ({volumes.length})
                </Typography>

                {loading ? (
                    <Box display="flex" justifyContent="center" p={4}><CircularProgress /></Box>
                ) : (
                    <TableContainer sx={{ maxHeight: 300, border: '1px solid #e2e8f0', borderRadius: 1 }}>
                        <Table size="small" stickyHeader>
                            <TableHead>
                                <TableRow>
                                    <TableCell><b>LPN (Etiqueta)</b></TableCell>
                                    <TableCell align="right"><b>Qtd Vol.</b></TableCell>
                                    <TableCell align="center"><b>Ação</b></TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {volumes.length === 0 ? (
                                    <TableRow>
                                        <TableCell colSpan={3} align="center">Nenhuma etiqueta gerada ainda.</TableCell>
                                    </TableRow>
                                ) : (
                                    volumes.map((vol) => (
                                        <TableRow key={vol.id} hover>
                                            <TableCell sx={{ fontFamily: 'monospace', fontWeight: 'bold' }}>
                                                {vol.lpn}
                                            </TableCell>
                                            <TableCell align="right">{vol.quantidadeOriginal}</TableCell>
                                            <TableCell align="center">
                                                <Tooltip title="Reimprimir">
                                                    <IconButton size="small" color="primary" onClick={() => handleReprint(vol.lpn)}>
                                                        <Printer size={16} />
                                                    </IconButton>
                                                </Tooltip>
                                            </TableCell>
                                        </TableRow>
                                    ))
                                )}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            </DialogContent>

            <DialogActions sx={{ p: 2 }}>
                <Button onClick={onClose} variant="outlined" fullWidth>Fechar</Button>
            </DialogActions>
        </Dialog>
    );
};

export default ItemConferenciaModal;