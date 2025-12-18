import { useState, useEffect } from 'react';
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, MenuItem, Box, Alert } from '@mui/material';
import { getPerfis, criarUsuario } from '../../services/usuarioService';
import { toast } from 'react-toastify';

const UsuarioForm = ({ open, onClose, onSuccess }) => {
    const [perfis, setPerfis] = useState([]);
    const [form, setForm] = useState({ login: '', senha: '', perfilId: '' });
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (open) carregarPerfis();
    }, [open]);

    const carregarPerfis = async () => {
        try {
            const data = await getPerfis();
            setPerfis(data);
        } catch (error) {
            toast.error("Erro ao carregar perfis");
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!form.login || !form.senha || !form.perfilId) return;

        setLoading(true);
        try {
            await criarUsuario(form);
            toast.success("Usuário criado com sucesso!");
            onSuccess();
        } catch (error) {
            toast.error(error.response?.data?.message || "Erro ao criar usuário");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>Novo Usuário</DialogTitle>
            <form onSubmit={handleSubmit}>
                <DialogContent>
                    <Alert severity="info" sx={{ mb: 2 }}>
                        Se o usuário já existir em outra empresa, ele será vinculado a esta com o perfil selecionado.
                    </Alert>
                    <Box display="flex" flexDirection="column" gap={2}>
                        <TextField
                            label="Login / E-mail"
                            fullWidth
                            value={form.login}
                            onChange={e => setForm({ ...form, login: e.target.value })}
                            required
                        />
                        <TextField
                            label="Senha Inicial"
                            type="password"
                            fullWidth
                            value={form.senha}
                            onChange={e => setForm({ ...form, senha: e.target.value })}
                            required
                        />
                        <TextField
                            select
                            label="Perfil de Acesso"
                            fullWidth
                            value={form.perfilId}
                            onChange={e => setForm({ ...form, perfilId: e.target.value })}
                            required
                        >
                            {perfis.map(p => (
                                <MenuItem key={p.id} value={p.id}>{p.nome}</MenuItem>
                            ))}
                        </TextField>
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>Cancelar</Button>
                    <Button type="submit" variant="contained" disabled={loading}>
                        {loading ? "Salvando..." : "Criar Usuário"}
                    </Button>
                </DialogActions>
            </form>
        </Dialog>
    );
};

export default UsuarioForm;