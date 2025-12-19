import { useState, useEffect } from 'react';
import { Box, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, IconButton, Dialog, DialogTitle, DialogContent, TextField, DialogActions, Switch, FormControlLabel, MenuItem, Grid, Typography, Divider } from '@mui/material';
import { Plus, Edit, MapPin, Trash2 } from 'lucide-react';
import { toast } from 'react-toastify';
import { getArmazens, getAreas, getLocais, salvarLocal, excluirItem } from '../../../services/mapeamentoService';
import ConfirmDialog from '../../../components/ConfirmDialog';
import Can from '../../../components/Can';

const LocalizacaoList = () => {
    // Filtros em Cascata
    const [armazens, setArmazens] = useState([]);
    const [armazemId, setArmazemId] = useState('');

    const [areas, setAreas] = useState([]);
    const [areaId, setAreaId] = useState('');

    const [lista, setLista] = useState([]);
    const [modalOpen, setModalOpen] = useState(false);

    // Novos estados para exclusão
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmAction, setConfirmAction] = useState(null);

    const [form, setForm] = useState({
        areaId: '', codigo: '', descricao: '', tipo: 'PULMAO',
        virtual: false, permiteMultiLpn: true, capacidadeLpn: 1, capacidadePesoKg: 1000,
        bloqueado: false, ativo: true
    });

    // Carga de Filtros
    useEffect(() => { loadArmazens(); }, []);
    const loadArmazens = async () => {
        const data = await getArmazens();
        setArmazens(data);
        if (data.length > 0) setArmazemId(data[0].id);
    };

    useEffect(() => { if (armazemId) loadAreas(); }, [armazemId]);
    const loadAreas = async () => {
        const data = await getAreas(armazemId);
        setAreas(data);
        if (data.length > 0) setAreaId(data[0].id);
        else { setAreaId(''); setLista([]); }
    };

    useEffect(() => { if (areaId) loadLocais(); }, [areaId]);
    const loadLocais = async () => { setLista(await getLocais(areaId)); };

    // Handlers
    const handleNew = () => {
        setForm({
            areaId: areaId, codigo: '', descricao: '', tipo: 'PULMAO',
            virtual: false, permiteMultiLpn: true, capacidadeLpn: 1, capacidadePesoKg: 1000,
            bloqueado: false, ativo: true
        });
        setModalOpen(true);
    };

    const handleEdit = (item) => {
        setForm({
            ...item,
            areaId: item.area.id
        });
        setModalOpen(true);
    };

    const handleSubmit = async () => {
        try {
            await salvarLocal(form);
            toast.success("Endereço salvo!");
            setModalOpen(false);
            loadLocais();
        } catch (e) { toast.error("Erro ao salvar."); }
    };

    // NOVA FUNÇÃO DE EXCLUSÃO
    const handleDelete = (id) => {
        setConfirmAction(() => async () => {
            try {
                // Chama a função genérica passando o tipo 'locais'
                await excluirItem('locais', id);
                toast.success("Endereço excluído com sucesso!");
                loadLocais(); // Recarrega a lista
            } catch (error) {
                console.error(error);
                toast.error("Erro ao excluir. Verifique se há estoque no local.");
            }
        });
        setConfirmOpen(true);
    };

    return (
        <Box>
            <Paper sx={{ p: 2, mb: 2, display: 'flex', gap: 2, alignItems: 'center' }}>
                <MapPin size={24} color="#64748b" />
                <TextField select label="Armazém" size="small" value={armazemId} onChange={e => setArmazemId(e.target.value)} sx={{ width: 200 }}>
                    {armazens.map(a => <MenuItem key={a.id} value={a.id}>{a.nome}</MenuItem>)}
                </TextField>
                <TextField select label="Área / Zona" size="small" value={areaId} onChange={e => setAreaId(e.target.value)} sx={{ width: 200 }} disabled={!armazemId}>
                    {areas.map(a => <MenuItem key={a.id} value={a.id}>{a.nome}</MenuItem>)}
                </TextField>
                <Button variant="contained" startIcon={<Plus size={18} />} onClick={handleNew} disabled={!areaId}>Novo Endereço</Button>
            </Paper>

            <TableContainer component={Paper}>
                <Table size="small">
                    <TableHead sx={{ bgcolor: 'background.subtle' }}>
                        <TableRow>
                            <TableCell>Endereço Completo</TableCell>
                            <TableCell>Sufixo</TableCell>
                            <TableCell>Tipo</TableCell>
                            <TableCell>Capacidade</TableCell>
                            <TableCell align="center">Ações</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {lista.map(item => (
                            <TableRow key={item.id}>
                                <TableCell><b>{item.enderecoCompleto}</b></TableCell>
                                <TableCell>{item.codigo}</TableCell>
                                <TableCell>{item.tipo}</TableCell>
                                <TableCell>{item.capacidadeLpn} Pallets / {item.capacidadePesoKg}kg</TableCell>
                                <TableCell align="center">
                                    <Box display="flex" justifyContent="center" gap={1}>
                                        {/* Botão Editar (Permissão GERENCIAR) */}
                                        <Can I="LOCALIZACAO_GERENCIAR">
                                            <IconButton size="small" color="primary" onClick={() => handleEdit(item)}>
                                                <Edit size={16} />
                                            </IconButton>
                                        </Can>

                                        {/* Botão Excluir (Nova Permissão EXCLUIR) */}
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
                <DialogTitle>{form.id ? 'Editar' : 'Novo'} Endereço</DialogTitle>
                <DialogContent>
                    <Grid container spacing={2} mt={0.5}>
                        <Grid item xs={12}>
                            <Typography variant="caption" color="text.secondary">
                                O endereço completo será gerado automaticamente: {armazens.find(a => a.id === armazemId)?.codigo}-{areas.find(a => a.id === areaId)?.codigo}-{form.codigo || 'XXXX'}
                            </Typography>
                        </Grid>
                        <Grid item xs={4}>
                            <TextField label="Sufixo (Código)" value={form.codigo} onChange={e => setForm({ ...form, codigo: e.target.value.toUpperCase() })} fullWidth required helperText="Ex: 01-02-A" />
                        </Grid>
                        <Grid item xs={8}>
                            <TextField label="Descrição" value={form.descricao} onChange={e => setForm({ ...form, descricao: e.target.value })} fullWidth />
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
                        <Grid item xs={3}>
                            <TextField label="Cap. Pallets" type="number" value={form.capacidadeLpn} onChange={e => setForm({ ...form, capacidadeLpn: e.target.value })} fullWidth />
                        </Grid>
                        <Grid item xs={3}>
                            <TextField label="Cap. Peso (kg)" type="number" value={form.capacidadePesoKg} onChange={e => setForm({ ...form, capacidadePesoKg: e.target.value })} fullWidth />
                        </Grid>

                        <Grid item xs={12}><Divider /></Grid>

                        <Grid item xs={12} display="flex" gap={2}>
                            <FormControlLabel control={<Switch checked={form.ativo} onChange={e => setForm({ ...form, ativo: e.target.checked })} color="success" />} label="Ativo" />
                            <FormControlLabel control={<Switch checked={form.bloqueado} onChange={e => setForm({ ...form, bloqueado: e.target.checked })} color="error" />} label="Bloqueado" />
                            <FormControlLabel control={<Switch checked={form.virtual} onChange={e => setForm({ ...form, virtual: e.target.checked })} />} label="Virtual (Lógico)" />
                            <FormControlLabel control={<Switch checked={form.permiteMultiLpn} onChange={e => setForm({ ...form, permiteMultiLpn: e.target.checked })} />} label="Multi-LPN" />
                        </Grid>
                    </Grid>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setModalOpen(false)}>Cancelar</Button>
                    <Button variant="contained" onClick={handleSubmit}>Salvar</Button>
                </DialogActions>
            </Dialog>
            {/* Componente de Confirmação */}
            <ConfirmDialog
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                onConfirm={confirmAction}
                title="Excluir Endereço"
                message="Tem certeza que deseja excluir este endereço? Esta ação é irreversível."
            />
        </Box>
    );
};
export default LocalizacaoList;