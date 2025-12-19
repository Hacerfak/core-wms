import { useState, useEffect } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button,
    TextField, Box, MenuItem, FormControlLabel, Switch, Alert
} from '@mui/material';
import { Plus, Trash2, Edit, MapPin, Package } from 'lucide-react';
import { toast } from 'react-toastify';
import { salvarLocalizacao } from '../../services/localizacaoService';

const LocalizacaoForm = ({ open, onClose, localizacao, onSuccess }) => {
    const [loading, setLoading] = useState(false);
    const [form, setForm] = useState({
        codigo: '', tipo: 'PULMAO', capacidadePesoKg: '', ativo: true, bloqueado: false
    });

    useEffect(() => {
        if (open) {
            if (localizacao) {
                setForm({
                    id: localizacao.id,
                    codigo: localizacao.codigo,
                    tipo: localizacao.tipo,
                    capacidadePesoKg: localizacao.capacidadePesoKg || '',
                    ativo: localizacao.ativo,
                    bloqueado: localizacao.bloqueado
                });
            } else {
                setForm({ codigo: '', tipo: 'PULMAO', capacidadePesoKg: '', ativo: true, bloqueado: false });
            }
        }
    }, [open, localizacao]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            await salvarLocalizacao(form);
            toast.success("Local salvo!");
            onSuccess();
        } catch (error) {
            toast.error("Erro ao salvar.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>{localizacao ? "Editar Local" : "Novo Endereço"}</DialogTitle>
            <form onSubmit={handleSubmit}>
                <DialogContent>
                    <Box display="flex" flexDirection="column" gap={2}>
                        <TextField
                            label="Código do Endereço"
                            fullWidth required
                            value={form.codigo}
                            onChange={e => setForm({ ...form, codigo: e.target.value.toUpperCase() })}
                            helperText="Ex: A-01-02-03 (Rua-Predio-Nivel-Apto)"
                        />

                        <TextField select label="Tipo de Posição" fullWidth value={form.tipo} onChange={e => setForm({ ...form, tipo: e.target.value })}>
                            <MenuItem value="PULMAO">Pulmão (Reserva/Porta-Pallet)</MenuItem>
                            <MenuItem value="PICKING">Picking (Separação)</MenuItem>
                            <MenuItem value="STAGE">Stage (Chão/Blocado)</MenuItem>
                            <MenuItem value="DOCA">Doca (Virtual)</MenuItem>
                            <MenuItem value="AVARIA">Avaria (Segregação)</MenuItem>
                            <MenuItem value="QUARENTENA">Quarentena</MenuItem>
                        </TextField>

                        <TextField
                            label="Capacidade de Peso (kg)"
                            type="number"
                            fullWidth
                            value={form.capacidadePesoKg}
                            onChange={e => setForm({ ...form, capacidadePesoKg: e.target.value })}
                        />

                        <Box display="flex" gap={2} mt={1}>
                            <FormControlLabel
                                control={<Switch checked={form.ativo} onChange={e => setForm({ ...form, ativo: e.target.checked })} color="success" />}
                                label="Ativo"
                            />
                            <FormControlLabel
                                control={<Switch checked={form.bloqueado} onChange={e => setForm({ ...form, bloqueado: e.target.checked })} color="error" />}
                                label="Bloqueado (Impede Movimentação)"
                            />
                        </Box>
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>Cancelar</Button>
                    <Button type="submit" variant="contained" disabled={loading}>Salvar</Button>
                </DialogActions>
            </form>
        </Dialog>
    );
};

export default LocalizacaoForm;