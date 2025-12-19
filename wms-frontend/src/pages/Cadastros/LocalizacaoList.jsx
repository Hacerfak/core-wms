import { useState, useEffect } from 'react';
import {
    Box, Typography, Button, Paper, Table, TableBody, TableCell,
    TableContainer, TableHead, TableRow, Chip, IconButton, Tooltip, LinearProgress
} from '@mui/material';
import { Plus, Trash2, Edit, MapPin, Package } from 'lucide-react';
import { toast } from 'react-toastify';
import { getLocalizacoes, excluirLocalizacao } from '../../services/localizacaoService';
import LocalizacaoForm from './LocalizacaoForm';
import ConfirmDialog from '../../components/ConfirmDialog';
import Can from '../../components/Can';

const LocalizacaoList = () => {
    const [locais, setLocais] = useState([]);
    const [loading, setLoading] = useState(true);
    const [modalOpen, setModalOpen] = useState(false);
    const [localEditando, setLocalEditando] = useState(null);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmAction, setConfirmAction] = useState(null);

    useEffect(() => { loadData(); }, []);

    const loadData = async () => {
        try {
            setLoading(true);
            const data = await getLocalizacoes();
            // Ordenar por código
            setLocais(data.sort((a, b) => a.codigo.localeCompare(b.codigo)));
        } catch (error) {
            toast.error("Erro ao carregar locais");
        } finally {
            setLoading(false);
        }
    };

    const handleNew = () => { setLocalEditando(null); setModalOpen(true); };
    const handleEdit = (item) => { setLocalEditando(item); setModalOpen(true); };

    const handleDelete = (id) => {
        setConfirmAction(() => async () => {
            try {
                await excluirLocalizacao(id);
                toast.success("Local excluído!");
                loadData();
            } catch (error) {
                toast.error("Erro: Local possui estoque ou movimentação.");
            }
        });
        setConfirmOpen(true);
    };

    return (
        <Box>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
                <Typography variant="h5" fontWeight="bold">Mapeamento de Armazém</Typography>
                <Can I="LOCALIZACAO_GERENCIAR">
                    <Button variant="contained" startIcon={<Plus size={20} />} onClick={handleNew}>Novo Endereço</Button>
                </Can>
            </Box>

            <Paper sx={{ width: '100%', overflow: 'hidden', borderRadius: 2 }}>
                {loading && <LinearProgress />}
                <TableContainer sx={{ maxHeight: 600 }}>
                    <Table stickyHeader size="small">
                        <TableHead sx={{ bgcolor: 'background.subtle' }}>
                            <TableRow>
                                <TableCell><b>Código (Endereço)</b></TableCell>
                                <TableCell><b>Tipo</b></TableCell>
                                <TableCell><b>Capacidade</b></TableCell>
                                <TableCell><b>Status</b></TableCell>
                                <TableCell align="center"><b>Ações</b></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {locais.map((l) => (
                                <TableRow key={l.id} hover>
                                    <TableCell>
                                        <Box display="flex" alignItems="center" gap={1}>
                                            <MapPin size={16} color="#64748b" />
                                            <Typography fontWeight="bold">{l.codigo}</Typography>
                                        </Box>
                                    </TableCell>
                                    <TableCell><Chip label={l.tipo} size="small" variant="outlined" /></TableCell>
                                    <TableCell>{l.capacidadePesoKg ? `${l.capacidadePesoKg} kg` : '-'}</TableCell>
                                    <TableCell>
                                        {l.bloqueado ? <Chip label="Bloqueado" color="error" size="small" /> : <Chip label="Ativo" color="success" size="small" />}
                                    </TableCell>
                                    <TableCell align="center">
                                        <Can I="LOCALIZACAO_GERENCIAR">
                                            <IconButton size="small" color="primary" onClick={() => handleEdit(l)}><Edit size={18} /></IconButton>
                                            <IconButton size="small" color="error" onClick={() => handleDelete(l.id)}><Trash2 size={18} /></IconButton>
                                        </Can>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>

            <LocalizacaoForm open={modalOpen} onClose={() => setModalOpen(false)} localizacao={localEditando} onSuccess={() => { setModalOpen(false); loadData(); }} />
            <ConfirmDialog open={confirmOpen} onClose={() => setConfirmOpen(false)} onConfirm={confirmAction} title="Excluir Local" message="Tem certeza?" />
        </Box>
    );
};

export default LocalizacaoList;