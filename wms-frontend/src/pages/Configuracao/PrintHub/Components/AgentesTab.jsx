import { useState, useEffect } from 'react';
import {
    Box, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    Paper, IconButton, Chip, Dialog, DialogTitle, DialogContent, TextField, DialogActions,
    Alert, Typography
} from '@mui/material';
import { Plus, Trash2, Copy, RefreshCw } from 'lucide-react';
import { toast } from 'react-toastify';
import { getAgentes, criarAgente, revogarAgente } from '../../../../services/printHubService';
import ConfirmDialog from '../../../../components/ConfirmDialog';

const AgentesTab = () => {
    const [agentes, setAgentes] = useState([]);
    const [modalOpen, setModalOpen] = useState(false);
    const [novoAgenteResponse, setNovoAgenteResponse] = useState(null); // Guarda a key
    const [form, setForm] = useState({ nome: '', hostname: '', descricao: '' });
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [idToDelete, setIdToDelete] = useState(null);

    // Polling a cada 15s
    useEffect(() => {
        load();
        const interval = setInterval(load, 15000);
        return () => clearInterval(interval);
    }, []);

    const load = async () => {
        try {
            const data = await getAgentes();
            setAgentes(data);
        } catch (e) { console.error("Erro polling agentes", e); }
    };

    const handleCreate = async () => {
        if (!form.nome || !form.hostname) return toast.warning("Preencha campos obrigatórios");
        try {
            const res = await criarAgente(form);
            setNovoAgenteResponse(res); // Mostra modal de sucesso com a chave
            load();
            setForm({ nome: '', hostname: '', descricao: '' });
        } catch (e) { toast.error("Erro ao criar agente"); }
    };

    const handleDelete = async () => {
        try {
            await revogarAgente(idToDelete);
            toast.success("Revogado!");
            load();
        } catch (e) { toast.error("Erro ao revogar"); }
    };

    const copyKey = () => {
        navigator.clipboard.writeText(novoAgenteResponse.apiKey);
        toast.success("Copiado!");
    };

    return (
        <Box>
            <Box display="flex" justifyContent="flex-end" mb={2}>
                <Button variant="contained" startIcon={<Plus size={18} />} onClick={() => setModalOpen(true)}>Novo Agente</Button>
            </Box>

            <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                    <TableHead sx={{ bgcolor: 'background.subtle' }}>
                        <TableRow>
                            <TableCell>Status</TableCell>
                            <TableCell>Nome</TableCell>
                            <TableCell>Hostname</TableCell>
                            <TableCell>Versão</TableCell>
                            <TableCell>Último Heartbeat</TableCell>
                            <TableCell align="center">Ações</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {agentes.map(a => (
                            <TableRow key={a.id}>
                                <TableCell>
                                    <Chip label={a.statusConexao} color={a.statusConexao === 'ONLINE' ? 'success' : 'error'} size="small" variant="filled" />
                                </TableCell>
                                <TableCell><b>{a.nome}</b></TableCell>
                                <TableCell>{a.hostname}</TableCell>
                                <TableCell>{a.versaoAgente || '-'}</TableCell>
                                <TableCell>{a.ultimoHeartbeat ? new Date(a.ultimoHeartbeat).toLocaleString() : '-'}</TableCell>
                                <TableCell align="center">
                                    <IconButton size="small" color="error" onClick={() => { setIdToDelete(a.id); setConfirmOpen(true); }}><Trash2 size={16} /></IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Modal de Criação / Chave */}
            <Dialog open={modalOpen} onClose={() => !novoAgenteResponse && setModalOpen(false)} maxWidth="sm" fullWidth>
                {!novoAgenteResponse ? (
                    <>
                        <DialogTitle>Novo Agente</DialogTitle>
                        <DialogContent>
                            <Box display="flex" flexDirection="column" gap={2} mt={1}>
                                <TextField label="Nome (ID)" fullWidth required value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} />
                                <TextField label="Hostname" fullWidth required value={form.hostname} onChange={e => setForm({ ...form, hostname: e.target.value })} helperText="Nome do computador onde o serviço rodará" />
                                <TextField label="Descrição" fullWidth value={form.descricao} onChange={e => setForm({ ...form, descricao: e.target.value })} />
                            </Box>
                        </DialogContent>
                        <DialogActions>
                            <Button onClick={() => setModalOpen(false)}>Cancelar</Button>
                            <Button variant="contained" onClick={handleCreate}>Criar</Button>
                        </DialogActions>
                    </>
                ) : (
                    <>
                        <DialogTitle color="success.main">Agente Criado!</DialogTitle>
                        <DialogContent>
                            <Alert severity="warning" sx={{ mb: 2 }}>Copie a chave agora. Ela não será mostrada novamente.</Alert>
                            <Paper variant="outlined" sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', bgcolor: '#f8fafc' }}>
                                <Typography fontFamily="monospace" fontWeight="bold">{novoAgenteResponse.apiKey}</Typography>
                                <IconButton onClick={copyKey}><Copy size={18} /></IconButton>
                            </Paper>
                        </DialogContent>
                        <DialogActions>
                            <Button onClick={() => { setNovoAgenteResponse(null); setModalOpen(false); }}>Concluir</Button>
                        </DialogActions>
                    </>
                )}
            </Dialog>

            <ConfirmDialog open={confirmOpen} onClose={() => setConfirmOpen(false)} onConfirm={handleDelete} title="Revogar Agente" message="O agente perderá acesso imediatamente." />
        </Box>
    );
};
export default AgentesTab;