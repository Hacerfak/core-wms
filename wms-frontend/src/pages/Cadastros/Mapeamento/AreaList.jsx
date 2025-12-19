import { useState, useEffect } from 'react';
import {
    Box, Button, Table, TableBody, TableCell, TableContainer, TableHead,
    TableRow, Paper, IconButton, Dialog, DialogTitle, DialogContent,
    TextField, DialogActions, Switch, FormControlLabel, MenuItem, Grid, Chip
} from '@mui/material';
import { Plus, Edit, Trash2 } from 'lucide-react';
import { toast } from 'react-toastify';
import { getArmazens, getAreas, salvarArea, excluirItem } from '../../../services/mapeamentoService';
import ConfirmDialog from '../../../components/ConfirmDialog';
import Can from '../../../components/Can';

const AreaList = () => {
    const [armazens, setArmazens] = useState([]);
    const [armazemSelecionado, setArmazemSelecionado] = useState('');
    const [lista, setLista] = useState([]);
    const [modalOpen, setModalOpen] = useState(false);

    // Estados para exclusão
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmAction, setConfirmAction] = useState(null);

    const [form, setForm] = useState({
        id: '', armazemId: '', codigo: '', nome: '', tipo: 'ARMAZENAGEM',
        padraoRecebimento: false, padraoExpedicao: false, padraoQuarentena: false,
        ativo: true
    });

    useEffect(() => { loadArmazens(); }, []);

    const loadArmazens = async () => {
        const data = await getArmazens();
        setArmazens(data);
        if (data.length > 0) setArmazemSelecionado(data[0].id);
    };

    useEffect(() => { if (armazemSelecionado) loadAreas(); }, [armazemSelecionado]);

    const loadAreas = async () => { setLista(await getAreas(armazemSelecionado)); };

    const handleNew = () => {
        setForm({
            id: '', armazemId: armazemSelecionado, codigo: '', nome: '', tipo: 'ARMAZENAGEM',
            padraoRecebimento: false, padraoExpedicao: false, padraoQuarentena: false,
            ativo: true
        });
        setModalOpen(true);
    };

    const handleEdit = (item) => {
        setForm({
            ...item,
            armazemId: item.armazem.id
        });
        setModalOpen(true);
    };

    const handleSubmit = async () => {
        try {
            await salvarArea(form);
            toast.success("Área salva!");
            setModalOpen(false);
            loadAreas();
        } catch (e) {
            toast.error(e.response?.data?.message || "Erro ao salvar.");
        }
    };

    // Lógica de Exclusão
    const handleDelete = (id) => {
        setConfirmAction(() => async () => {
            try {
                await excluirItem('areas', id);
                toast.success("Área excluída!");
                loadAreas();
            } catch (error) {
                toast.error(error.response?.data?.message || "Erro ao excluir. Verifique se há endereços vinculados.");
            }
        });
        setConfirmOpen(true);
    };

    return (
        <Box>
            <Box display="flex" gap={2} mb={2} alignItems="center">
                <TextField select label="Filtrar por Armazém" size="small" value={armazemSelecionado} onChange={e => setArmazemSelecionado(e.target.value)} sx={{ width: 250 }}>
                    {armazens.map(a => <MenuItem key={a.id} value={a.id}>{a.nome}</MenuItem>)}
                </TextField>

                <Can I="LOCALIZACAO_GERENCIAR">
                    <Button variant="contained" startIcon={<Plus size={18} />} onClick={handleNew} disabled={!armazemSelecionado}>
                        Nova Área
                    </Button>
                </Can>
            </Box>

            <TableContainer component={Paper}>
                <Table size="small">
                    <TableHead sx={{ bgcolor: 'background.subtle' }}>
                        <TableRow>
                            <TableCell>Código</TableCell>
                            <TableCell>Nome</TableCell>
                            <TableCell>Tipo</TableCell>
                            <TableCell>Padrões</TableCell>
                            <TableCell align="center">Ações</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {lista.map(item => (
                            <TableRow key={item.id}>
                                <TableCell><b>{item.codigo}</b></TableCell>
                                <TableCell>{item.nome}</TableCell>
                                <TableCell><Chip label={item.tipo} size="small" /></TableCell>
                                <TableCell>
                                    <Box display="flex" gap={0.5}>
                                        {item.padraoRecebimento && <Chip label="REC" color="info" size="small" />}
                                        {item.padraoExpedicao && <Chip label="EXP" color="warning" size="small" />}
                                        {item.padraoQuarentena && <Chip label="QUA" color="error" size="small" />}
                                    </Box>
                                </TableCell>
                                <TableCell align="center">
                                    <Box display="flex" justifyContent="center" gap={1}>
                                        <Can I="LOCALIZACAO_GERENCIAR">
                                            <IconButton size="small" color="primary" onClick={() => handleEdit(item)}>
                                                <Edit size={16} />
                                            </IconButton>
                                        </Can>

                                        <Can I="LOCALIZACAO_EXCLUIR">
                                            <IconButton size="small" color="error" onClick={() => handleDelete(item.id)}>
                                                <Trash2 size={16} />
                                            </IconButton>
                                        </Can>
                                    </Box>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            <Dialog open={modalOpen} onClose={() => setModalOpen(false)} maxWidth="md" fullWidth>
                <DialogTitle>{form.id ? 'Editar' : 'Nova'} Área</DialogTitle>
                <DialogContent>
                    <Grid container spacing={2} mt={0.5}>
                        <Grid item xs={4}>
                            <TextField label="Código" value={form.codigo} onChange={e => setForm({ ...form, codigo: e.target.value.toUpperCase() })} fullWidth required helperText="Ex: DOCA1" />
                        </Grid>
                        <Grid item xs={8}>
                            <TextField label="Nome da Área" value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} fullWidth required />
                        </Grid>
                        <Grid item xs={6}>
                            <TextField select label="Tipo" value={form.tipo} onChange={e => setForm({ ...form, tipo: e.target.value })} fullWidth>
                                <MenuItem value="DOCA">Doca (Entrada/Saída)</MenuItem>
                                <MenuItem value="STAGE">Stage (Conferência)</MenuItem>
                                <MenuItem value="SEGREGACAO">Segregação (Inspeção)</MenuItem>
                                <MenuItem value="ARMAZENAGEM">Armazenagem (Estoque)</MenuItem>
                                <MenuItem value="PICKING">Picking (Separação)</MenuItem>
                                <MenuItem value="AVARIA">Avaria</MenuItem>
                                <MenuItem value="QUARENTENA">Quarentena</MenuItem>
                                <MenuItem value="PERDA">Perda</MenuItem>
                                <MenuItem value="PULMAO">Pulmão (Reservas)</MenuItem>
                            </TextField>
                        </Grid>
                        <Grid item xs={12}>
                            <Box display="flex" gap={2}>
                                <FormControlLabel control={<Switch checked={form.padraoRecebimento} onChange={e => setForm({ ...form, padraoRecebimento: e.target.checked })} />} label="Padrão Recebimento" />
                                <FormControlLabel control={<Switch checked={form.padraoExpedicao} onChange={e => setForm({ ...form, padraoExpedicao: e.target.checked })} />} label="Padrão Expedição" />
                                <FormControlLabel control={<Switch checked={form.padraoQuarentena} onChange={e => setForm({ ...form, padraoQuarentena: e.target.checked })} color="error" />} label="Padrão Quarentena" />
                            </Box>
                        </Grid>
                    </Grid>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setModalOpen(false)}>Cancelar</Button>
                    <Button variant="contained" onClick={handleSubmit}>Salvar</Button>
                </DialogActions>
            </Dialog>

            <ConfirmDialog
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                onConfirm={confirmAction}
                title="Excluir Área"
                message="Tem certeza? A exclusão só é permitida se não houver endereços nesta área."
            />
        </Box>
    );
};
export default AreaList;