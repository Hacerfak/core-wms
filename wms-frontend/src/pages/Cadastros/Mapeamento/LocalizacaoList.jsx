import { useState, useEffect } from 'react';
import {
    Box, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    Paper, IconButton, Dialog, DialogTitle, DialogContent, TextField, DialogActions,
    Switch, FormControlLabel, MenuItem, Grid, Typography, Divider, Tooltip, Chip, LinearProgress
} from '@mui/material';
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
    const [loading, setLoading] = useState(true);
    const [modalOpen, setModalOpen] = useState(false);

    // Novos estados para exclusão
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmAction, setConfirmAction] = useState(null);

    const [form, setForm] = useState({
        areaId: '', codigo: '', descricao: '', tipo: 'PULMAO',
        tipoEstrutura: 'PORTA_PALLET', // <-- Novo campo adicionado
        virtual: false, permiteMultiLpn: true, capacidadeLpn: 1, capacidadePesoKg: 1000,
        bloqueado: false, ativo: true
    });

    // Carga de Filtros
    useEffect(() => { loadArmazens(); }, []);

    const loadArmazens = async () => {
        try {
            const data = await getArmazens();
            setArmazens(data);
            if (data.length > 0) setArmazemId(data[0].id);
        } catch (e) { toast.error("Erro ao carregar armazéns"); }
    };

    useEffect(() => { if (armazemId) loadAreas(); }, [armazemId]);

    const loadAreas = async () => {
        try {
            const data = await getAreas(armazemId);
            setAreas(data);
            if (data.length > 0) setAreaId(data[0].id);
            else { setAreaId(''); setLista([]); }
        } catch (e) { toast.error("Erro ao carregar áreas"); }
    };

    useEffect(() => { if (areaId) loadLocais(); }, [areaId]);

    const loadLocais = async () => {
        setLoading(true);
        try {
            const data = await getLocais(areaId);
            setLista(data);
        } catch (e) { toast.error("Erro ao carregar locais"); }
        finally { setLoading(false); }
    };

    // Handlers
    const handleNew = () => {
        setForm({
            areaId: areaId, codigo: '', descricao: '', tipo: 'PULMAO',
            tipoEstrutura: 'PORTA_PALLET',
            virtual: false, permiteMultiLpn: true, capacidadeLpn: 1, capacidadePesoKg: 1000,
            bloqueado: false, ativo: true
        });
        setModalOpen(true);
    };

    const handleEdit = (item) => {
        setForm({
            ...item,
            areaId: item.area.id,
            tipoEstrutura: item.tipoEstrutura || 'PORTA_PALLET'
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

    const handleDelete = (id) => {
        setConfirmAction(() => async () => {
            try {
                await excluirItem('locais', id);
                toast.success("Endereço excluído com sucesso!");
                loadLocais();
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

            <Paper sx={{ width: '100%', overflow: 'hidden', borderRadius: 2 }}>
                {loading && <LinearProgress />}
                <TableContainer>
                    <Table size="small">
                        <TableHead sx={{ bgcolor: 'background.subtle' }}>
                            <TableRow>
                                <TableCell><b>Endereço Completo</b></TableCell>
                                <TableCell><b>Lógico / Físico</b></TableCell>
                                <TableCell><b>Capacidade</b></TableCell>
                                <TableCell><b>Status</b></TableCell>
                                <TableCell align="center"><b>Ações</b></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {lista.map(item => (
                                <TableRow key={item.id} hover>
                                    <TableCell>
                                        <Box display="flex" flexDirection="column">
                                            <Typography variant="body2" fontWeight="bold">{item.enderecoCompleto}</Typography>
                                            <Typography variant="caption" color="text.secondary">{item.codigo}</Typography>
                                        </Box>
                                    </TableCell>
                                    <TableCell>
                                        <Box display="flex" gap={0.5} flexWrap="wrap">
                                            <Chip label={item.tipo} size="small" variant="outlined" />
                                            <Chip label={item.tipoEstrutura} color="primary" size="small" variant="outlined" />
                                        </Box>
                                    </TableCell>
                                    <TableCell>
                                        <Typography variant="caption" display="block">{item.capacidadeLpn} Pallets</Typography>
                                        <Typography variant="caption" display="block">{item.capacidadePesoKg} kg</Typography>
                                    </TableCell>
                                    <TableCell>
                                        <Box display="flex" gap={0.5}>
                                            <Chip label={item.ativo ? "Ativo" : "Inativo"} color={item.ativo ? "success" : "default"} size="small" />
                                            {item.bloqueado && <Chip label="Bloqueado" color="error" size="small" />}
                                            {item.virtual && <Chip label="Virtual" color="info" size="small" variant="outlined" />}
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
                            {!loading && lista.length === 0 && (
                                <TableRow><TableCell colSpan={5} align="center" sx={{ py: 3 }}>Nenhum endereço cadastrado nesta área.</TableCell></TableRow>
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>

            <Dialog open={modalOpen} onClose={() => setModalOpen(false)} maxWidth="md" fullWidth>
                <DialogTitle>{form.id ? 'Editar' : 'Novo'} Endereço</DialogTitle>
                <DialogContent dividers>
                    <Grid container spacing={2} mt={0.5}>
                        <Grid item xs={12}>
                            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                                O endereço completo será gerado automaticamente no padrão: [Armazém][Área][Sufixo]
                            </Typography>
                        </Grid>

                        <Grid item xs={12} sm={4}>
                            <TextField label="Sufixo (Código)" value={form.codigo} onChange={e => setForm({ ...form, codigo: e.target.value.toUpperCase() })} fullWidth required helperText="Ex: 01-02-A" />
                        </Grid>

                        <Grid item xs={12} sm={8}>
                            <TextField label="Descrição / Observação" value={form.descricao} onChange={e => setForm({ ...form, descricao: e.target.value })} fullWidth />
                        </Grid>

                        <Grid item xs={12} sm={6}>
                            <TextField select label="Tipo de Posição (Lógico)" value={form.tipo} onChange={e => setForm({ ...form, tipo: e.target.value })} fullWidth>
                                <MenuItem value="ARMAZENAGEM">Armazenagem (Estoque)</MenuItem>
                                <MenuItem value="PICKING">Picking (Separação)</MenuItem>
                                <MenuItem value="PULMAO">Pulmão (Reservas)</MenuItem>
                                <MenuItem value="DOCA">Doca (Entrada/Saída)</MenuItem>
                                <MenuItem value="STAGE">Stage (Conferência)</MenuItem>
                                <MenuItem value="AVARIA">Avaria</MenuItem>
                                <MenuItem value="QUARENTENA">Quarentena</MenuItem>
                                <MenuItem value="PERDA">Perda</MenuItem>
                                <MenuItem value="SEGREGACAO">Segregação (Inspeção)</MenuItem>
                            </TextField>
                        </Grid>

                        <Grid item xs={12} sm={6}>
                            <TextField select label="Estrutura Física" value={form.tipoEstrutura} onChange={e => setForm({ ...form, tipoEstrutura: e.target.value })} fullWidth>
                                <MenuItem value="PORTA_PALLET">Porta Pallet</MenuItem>
                                <MenuItem value="BLOCADO">Blocado (Chão)</MenuItem>
                                <MenuItem value="DRIVE_IN">Drive-In</MenuItem>
                                <MenuItem value="PUSH_BACK">Push-Back</MenuItem>
                            </TextField>
                        </Grid>

                        <Grid item xs={12} sm={6}>
                            <TextField label="Capacidade Pallets (LPNs)" type="number" value={form.capacidadeLpn} onChange={e => setForm({ ...form, capacidadeLpn: e.target.value })} fullWidth />
                        </Grid>

                        <Grid item xs={12} sm={6}>
                            <TextField label="Capacidade Peso (kg)" type="number" value={form.capacidadePesoKg} onChange={e => setForm({ ...form, capacidadePesoKg: e.target.value })} fullWidth />
                        </Grid>

                        <Grid item xs={12}><Divider sx={{ my: 1 }} /></Grid>

                        <Grid item xs={12}>
                            <Box display="flex" gap={3} flexWrap="wrap">
                                <FormControlLabel control={<Switch checked={form.ativo} onChange={e => setForm({ ...form, ativo: e.target.checked })} color="success" />} label="Ativo" />
                                <FormControlLabel control={<Switch checked={form.bloqueado} onChange={e => setForm({ ...form, bloqueado: e.target.checked })} color="error" />} label="Bloqueado" />
                                <FormControlLabel control={<Switch checked={form.virtual} onChange={e => setForm({ ...form, virtual: e.target.checked })} />} label="Virtual (Lógico)" />
                                <FormControlLabel control={<Switch checked={form.permiteMultiLpn} onChange={e => setForm({ ...form, permiteMultiLpn: e.target.checked })} />} label="Permite Multi-LPN" />
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
                title="Excluir Endereço"
                message="Tem certeza que deseja excluir este endereço? Esta ação é irreversível."
            />
        </Box>
    );
};

export default LocalizacaoList;