import { useState, useEffect } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button,
    TextField, MenuItem, Box, Alert, CircularProgress,
    FormControlLabel, Switch // <--- Importado
} from '@mui/material';
import { getPerfis, criarUsuario, atualizarUsuario, verificarUsuario } from '../../services/usuarioService';
import { toast } from 'react-toastify';
import { Search, ShieldAlert, CheckCircle } from 'lucide-react';

const UsuarioForm = ({ open, onClose, onSuccess, usuario }) => {
    const [perfis, setPerfis] = useState([]);

    // Adicionado campo 'ativo' no estado inicial
    const [form, setForm] = useState({ login: '', senha: '', perfilId: '', ativo: true });

    const [statusUsuario, setStatusUsuario] = useState(null);
    const [loading, setLoading] = useState(false);
    const [verificando, setVerificando] = useState(false);

    const isMaster = usuario?.perfilNome === 'MASTER' || usuario?.login === 'master';

    useEffect(() => {
        if (open) {
            if (!isMaster) carregarPerfis();

            if (usuario) {
                // MODO EDIÇÃO
                setForm({
                    login: usuario.login,
                    senha: '',
                    perfilId: usuario.perfilId || '',
                    ativo: usuario.ativo // <--- Carrega do usuário
                });
                setStatusUsuario('existente');
            } else {
                // MODO CRIAÇÃO
                setForm({ login: '', senha: '', perfilId: '', ativo: true });
                setStatusUsuario(null);
            }
        }
    }, [open, usuario, isMaster]);

    const carregarPerfis = async () => {
        try {
            const data = await getPerfis();
            setPerfis(data);
            if (usuario && !form.perfilId) {
                const perfilEncontrado = data.find(p => p.nome === usuario.perfilNome);
                if (perfilEncontrado) {
                    setForm(prev => ({ ...prev, perfilId: perfilEncontrado.id }));
                }
            }
        } catch (error) {
            console.error(error);
        }
    };

    const handleVerificarLogin = async () => {
        if (!form.login) return;
        setVerificando(true);
        try {
            const data = await verificarUsuario(form.login);
            if (data.existe) {
                setStatusUsuario('existente');
                toast.info(`Usuário "${form.login}" encontrado! Será vinculado.`);
            } else {
                setStatusUsuario('novo');
                toast.success(`Usuário disponível.`);
            }
        } catch (error) {
            toast.error("Erro ao verificar usuário.");
        } finally {
            setVerificando(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!usuario && statusUsuario === null) {
            toast.warning("Por favor, verifique o login antes de salvar.");
            return;
        }

        setLoading(true);
        try {
            if (usuario) {
                // UPDATE
                const payload = isMaster
                    ? { login: form.login, senha: form.senha, ativo: form.ativo }
                    : form;

                await atualizarUsuario(usuario.id, payload);
                toast.success("Dados atualizados com sucesso!");
            } else {
                // CREATE
                await criarUsuario(form);
                toast.success("Usuário salvo com sucesso!");
            }
            onSuccess();
        } catch (error) {
            toast.error(error.response?.data?.message || "Erro ao salvar");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>
                {usuario ? (isMaster ? "Alterar Senha do Master" : "Editar Usuário") : "Adicionar Usuário"}
            </DialogTitle>
            <form onSubmit={handleSubmit}>
                <DialogContent>
                    <Box display="flex" flexDirection="column" gap={3}>

                        {isMaster && (
                            <Alert severity="info" icon={<ShieldAlert />}>
                                Usuário <b>Master</b>. O perfil não pode ser alterado.
                            </Alert>
                        )}

                        {!usuario && statusUsuario === 'novo' && (
                            <Alert severity="success" icon={<CheckCircle />}>Usuário disponível. Clique em Salvar para criar.</Alert>
                        )}

                        {!usuario && statusUsuario === 'existente' && (
                            <Alert severity="info" icon={<CheckCircle />}>Usuário já existe. Clique em Salvar para vincular.</Alert>
                        )}

                        <Box display="flex" gap={1}>
                            <TextField
                                label="Login / Usuário"
                                fullWidth
                                value={form.login}
                                onChange={e => {
                                    setForm({ ...form, login: e.target.value });
                                    if (!usuario) setStatusUsuario(null);
                                }}
                                disabled={isMaster || !!usuario} // Login não muda na edição comum
                                required
                            />
                            {!usuario && (
                                <Button
                                    variant="contained"
                                    onClick={handleVerificarLogin}
                                    disabled={!form.login || verificando}
                                    sx={{ minWidth: 60, px: 0 }}
                                >
                                    {verificando ? <CircularProgress size={20} color="inherit" /> : <Search size={20} />}
                                </Button>
                            )}
                        </Box>

                        <TextField
                            label={usuario ? "Nova Senha (deixe em branco para manter)" : "Senha"}
                            type="password"
                            fullWidth
                            value={form.senha}
                            onChange={e => setForm({ ...form, senha: e.target.value })}
                            required={!usuario && statusUsuario === 'novo'}
                            autoFocus={isMaster}
                        />

                        {!isMaster && (
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
                        )}

                        {/* NOVO SWITCH ATIVO/INATIVO */}
                        <FormControlLabel
                            control={
                                <Switch
                                    checked={form.ativo}
                                    onChange={(e) => setForm({ ...form, ativo: e.target.checked })}
                                    color="success"
                                />
                            }
                            label={form.ativo ? "Usuário Ativo" : "Usuário Inativo"}
                        />

                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>Cancelar</Button>
                    <Button type="submit" variant="contained" disabled={loading || (!usuario && statusUsuario === null)}>
                        {loading ? "Salvando..." : "Salvar"}
                    </Button>
                </DialogActions>
            </form>
        </Dialog>
    );
};

export default UsuarioForm;