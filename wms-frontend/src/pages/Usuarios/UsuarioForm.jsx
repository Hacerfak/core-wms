import { useState, useEffect } from 'react';
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, MenuItem, Box, Typography, Alert, InputAdornment, CircularProgress } from '@mui/material';
import { getPerfis, criarUsuario, atualizarUsuario, verificarUsuario } from '../../services/usuarioService'; // Import verificarUsuario
import { toast } from 'react-toastify';
import { Search, ShieldAlert, CheckCircle, UserPlus } from 'lucide-react';

const UsuarioForm = ({ open, onClose, onSuccess, usuario }) => {
    const [perfis, setPerfis] = useState([]);
    const [form, setForm] = useState({ login: '', senha: '', perfilId: '' });

    // null = não verificado, 'existente' = encontrado no banco, 'novo' = livre para cadastro
    const [statusUsuario, setStatusUsuario] = useState(null);

    const [loading, setLoading] = useState(false);
    const [verificando, setVerificando] = useState(false);

    // Verifica se é o Super Admin (Master)
    const isMaster = usuario?.perfilNome === 'MASTER' || usuario?.login === 'master';

    useEffect(() => {
        if (open) {
            // Só carrega perfis se NÃO for o Master
            if (!isMaster) {
                carregarPerfis();
            }

            if (usuario) {
                // MODO EDIÇÃO
                setForm({
                    login: usuario.login,
                    senha: '',
                    perfilId: usuario.perfilId || ''
                });
                setStatusUsuario('existente'); // Já existe
            } else {
                // MODO CRIAÇÃO (Reset)
                setForm({ login: '', senha: '', perfilId: '' });
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

    // --- LÓGICA DE PESQUISA RESTAURADA ---
    const handleVerificarLogin = async () => {
        if (!form.login) return;
        setVerificando(true);
        try {
            const data = await verificarUsuario(form.login);
            if (data.existe) {
                setStatusUsuario('existente');
                toast.info(`Usuário "${form.login}" encontrado! Será vinculado à empresa.`);
            } else {
                setStatusUsuario('novo');
                toast.success(`Usuário disponível. Será criado um novo cadastro.`);
            }
        } catch (error) {
            toast.error("Erro ao verificar usuário.");
        } finally {
            setVerificando(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Validação extra para garantir que pesquisou antes de salvar novo
        if (!usuario && statusUsuario === null) {
            toast.warning("Por favor, verifique o login antes de salvar.");
            return;
        }

        setLoading(true);
        try {
            if (usuario) {
                // UPDATE
                const payload = isMaster
                    ? { login: form.login, senha: form.senha }
                    : form;

                await atualizarUsuario(usuario.id, payload);
                toast.success("Dados atualizados com sucesso!");
            } else {
                // CREATE (ou VINCULAR)
                await criarUsuario(form);
                toast.success(statusUsuario === 'existente' ? "Usuário vinculado com sucesso!" : "Usuário criado com sucesso!");
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

                        {isMaster ? (
                            <Alert severity="info" icon={<ShieldAlert />}>
                                Usuário <b>Master</b>. O perfil não pode ser alterado, apenas a senha.
                            </Alert>
                        ) : (
                            usuario && (
                                <Alert severity="warning">
                                    Atenção: Alterar o Login ou Senha mudará o acesso deste usuário em <b>todas as empresas</b>.
                                </Alert>
                            )
                        )}

                        {/* Feedback visual da pesquisa */}
                        {!usuario && statusUsuario === 'existente' && (
                            <Alert severity="info" icon={<CheckCircle />}>
                                Usuário já existe no sistema global. Clique em Salvar para conceder acesso a esta empresa.
                            </Alert>
                        )}
                        {!usuario && statusUsuario === 'novo' && (
                            <Alert severity="success" icon={<UserPlus />}>
                                Login disponível. Preencha a senha e o perfil para criar.
                            </Alert>
                        )}

                        <Box display="flex" gap={1}>
                            <TextField
                                label="Login / Usuário"
                                fullWidth
                                value={form.login}
                                onChange={e => {
                                    setForm({ ...form, login: e.target.value });
                                    if (!usuario) setStatusUsuario(null); // Reseta status se mudar o texto
                                }}
                                disabled={isMaster}
                                required
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter' && !usuario) {
                                        e.preventDefault();
                                        handleVerificarLogin();
                                    }
                                }}
                            />
                            {/* BOTÃO DE PESQUISA REATIVADO */}
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
                            // Senha só é obrigatória se for NOVO usuário. Se for vincular (existente) ou editar, é opcional.
                            required={!usuario && statusUsuario === 'novo'}
                            disabled={!usuario && statusUsuario === 'existente'} // Se já existe, não precisa senha para vincular
                            helperText={!usuario && statusUsuario === 'existente' ? "Senha não necessária para vincular usuário existente." : ""}
                            autoFocus={isMaster}
                        />

                        {!isMaster && (
                            <TextField
                                select
                                label="Perfil de Acesso nesta Empresa"
                                fullWidth
                                value={form.perfilId}
                                onChange={e => setForm({ ...form, perfilId: e.target.value })}
                                required
                            >
                                {perfis.map(p => (
                                    <MenuItem key={p.id} value={p.id}>
                                        <Typography fontWeight="bold">{p.nome}</Typography>
                                    </MenuItem>
                                ))}
                            </TextField>
                        )}
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