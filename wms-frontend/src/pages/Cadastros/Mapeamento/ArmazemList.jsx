import { useState, useEffect } from 'react';
import {
    Box, Button, Table, TableBody, TableCell, TableContainer, TableHead,
    TableRow, Paper, IconButton, Dialog, DialogTitle, DialogContent,
    TextField, DialogActions, Switch, FormControlLabel, Alert
} from '@mui/material';
import { Plus, Edit, Trash2 } from 'lucide-react'; // <--- Importe Trash2
import { toast } from 'react-toastify';
import { getArmazens, salvarArmazem, excluirItem } from '../../../services/mapeamentoService';
import ConfirmDialog from '../../../components/ConfirmDialog'; // <--- Importe ConfirmDialog
import Can from '../../../components/Can'; // <--- Importe Can

const ArmazemList = () => {
    const [lista, setLista] = useState([]);
    const [modalOpen, setModalOpen] = useState(false);
    const [form, setForm] = useState({ id: '', codigo: '', nome: '', enderecoCompleto: '', ativo: true });

    // Estados para exclusão
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmAction, setConfirmAction] = useState(null);

    useEffect(() => { load(); }, []);
    const load = async () => { setLista(await getArmazens()); };

    const handleEdit = (item) => { setForm(item); setModalOpen(true); };

    const handleNew = () => {
        setForm({ id: '', codigo: '', nome: '', enderecoCompleto: '', ativo: true });
        setModalOpen(true);
    };

    const handleSubmit = async () => {
        try {
            await salvarArmazem(form);
            toast.success("Armazém salvo!");
            setModalOpen(false);
            load();
        } catch (e) {
            toast.error(e.response?.data?.message || "Erro ao salvar.");
        }
    };

    // Lógica de Exclusão
    const handleDelete = (id) => {
        setConfirmAction(() => async () => {
            try {
                await excluirItem('armazens', id);
                toast.success("Armazém excluído!");
                load();
            } catch (error) {
                toast.error(error.response?.data?.message || "Erro ao excluir. Verifique se há áreas vinculadas.");
            }
        });
        setConfirmOpen(true);
    };

    return (
        <Box>
            <Box display="flex" justifyContent="flex-end" mb={2}>
                <Can I="LOCALIZACAO_GERENCIAR">
                    <Button variant="contained" startIcon={<Plus size={18} />} onClick={handleNew}>
                        Novo Armazém
                    </Button>
                </Can>
            </Box>

            <TableContainer component={Paper}>
                <Table size="small">
                    <TableHead sx={{ bgcolor: 'background.subtle' }}>
                        <TableRow>
                            <TableCell>Código</TableCell>
                            <TableCell>Nome</TableCell>
                            <TableCell>Endereço</TableCell>
                            <TableCell align="center">Ações</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {lista.map(item => (
                            <TableRow key={item.id}>
                                <TableCell><b>{item.codigo}</b></TableCell>
                                <TableCell>{item.nome}</TableCell>
                                <TableCell>{item.enderecoCompleto}</TableCell>
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

            <Dialog open={modalOpen} onClose={() => setModalOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>{form.id ? 'Editar' : 'Novo'} Armazém</DialogTitle>
                <DialogContent>
                    <Box display="flex" flexDirection="column" gap={2} mt={1}>

                        {/* AVISO DE IMUTABILIDADE */}
                        {!form.id && (
                            <Alert severity="warning" sx={{ mb: 1 }}>
                                Atenção: O código do armazém é a base para todos os endereços e
                                <strong> não poderá ser alterado</strong> após a criação.
                            </Alert>
                        )}

                        <TextField
                            label="Código"
                            value={form.codigo}
                            onChange={e => setForm({ ...form, codigo: e.target.value.toUpperCase() })}
                            fullWidth
                            required
                            // Desabilita se for edição
                            disabled={!!form.id}
                            helperText={form.id ? "Código imutável." : "Ex: CD01, MG1 (Máx 10 chars)"}
                        />

                        <TextField label="Nome" value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} fullWidth required />
                        <TextField label="Endereço" value={form.enderecoCompleto} onChange={e => setForm({ ...form, enderecoCompleto: e.target.value })} fullWidth />
                        <FormControlLabel control={<Switch checked={form.ativo} onChange={e => setForm({ ...form, ativo: e.target.checked })} />} label="Ativo" />
                    </Box>
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
                title="Excluir Armazém"
                message="ATENÇÃO: Excluir um armazém impedirá qualquer operação futura nele. Tem certeza?"
            />
        </Box>
    );
};
export default ArmazemList;