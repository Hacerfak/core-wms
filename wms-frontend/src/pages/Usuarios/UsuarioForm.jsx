import { useState, useEffect } from 'react';
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, MenuItem, Box, Alert, CircularProgress, Typography } from '@mui/material';
import { getPerfis, criarUsuario } from '../../services/usuarioService';
import api from '../../services/api'; // Import direto para a verificação
import { toast } from 'react-toastify';
import { CheckCircle, AlertCircle, Search } from 'lucide-react';

const UsuarioForm = ({ open, onClose, onSuccess }) => {
    const [perfis, setPerfis] = useState([]);
    const [form, setForm] = useState({ login: '', senha: '', perfilId: '' });
    const [statusUsuario, setStatusUsuario] = useState(null); // 'novo', 'existente', null
    const [verificando, setVerificando] = useState(false);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (open) {
            carregarPerfis();
            setForm({ login: '', senha: '', perfilId: '' });
            setStatusUsuario(null);
        }
    }, [open]);

    const carregarPerfis = async () => {
        try {
            const data = await getPerfis();
            setPerfis(data);
        } catch (error) {
            toast.error("Erro ao carregar perfis");
        }
    };

    // Função que checa no Backend se o usuário já existe
    const verificarLogin = async () => {
        if (!form.login) return;
        setVerificando(true);
        try {
            const res = await api.get(`/api/gestao-usuarios/verificar/${form.login}`);
            if (res.data.existe) {
                setStatusUsuario('existente');
                toast.info("Usuário já existe no sistema global. Vamos apenas vinculá-lo.");
            } else {
                setStatusUsuario('novo');
            }
        } catch (error) {
            console.error(error);
        } finally {
            setVerificando(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Validação: Se for novo, exige senha. Se existente, senha é opcional.
        if (statusUsuario === 'novo' && !form.senha) {
            toast.warning("Defina uma senha para o novo usuário.");
            return;
        }
        if (!form.perfilId) {
            toast.warning("Selecione um perfil de acesso.");
            return;
        }

        setLoading(true);
        try {
            await criarUsuario(form);
            toast.success(statusUsuario === 'existente' ? "Usuário vinculado com sucesso!" : "Usuário criado e vinculado!");
            onSuccess();
        } catch (error) {
            toast.error(error.response?.data?.message || "Erro ao salvar usuário");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>Gerenciar Acesso</DialogTitle>
            <form onSubmit={handleSubmit}>
                <DialogContent>
                    <Box display="flex" flexDirection="column" gap={3}>

                        {/* Passo 1: Login e Verificação */}
                        <Box display="flex" gap={1}>
                            <TextField
                                label="Login / Usuário"
                                fullWidth
                                value={form.login}
                                onChange={e => {
                                    setForm({ ...form, login: e.target.value });
                                    setStatusUsuario(null); // Reseta status se mudar o texto
                                }}
                                onBlur={verificarLogin} // Checa ao sair do campo
                                required
                                helperText="Digite o login para verificar se já existe."
                            />
                            <Button
                                variant="outlined"
                                onClick={verificarLogin}
                                disabled={verificando || !form.login}
                                sx={{ minWidth: 50, px: 0 }}
                            >
                                {verificando ? <CircularProgress size={20} /> : <Search size={20} />}
                            </Button>
                        </Box>

                        {/* Feedback Visual */}
                        {statusUsuario === 'existente' && (
                            <Alert icon={<CheckCircle size={20} />} severity="success">
                                <b>Usuário Encontrado!</b> Ele será vinculado a esta empresa. Nenhuma senha necessária.
                            </Alert>
                        )}
                        {statusUsuario === 'novo' && (
                            <Alert icon={<AlertCircle size={20} />} severity="info">
                                <b>Usuário Novo.</b> Preencha a senha para criá-lo no sistema global.
                            </Alert>
                        )}

                        {/* Passo 2: Campos Condicionais */}
                        {statusUsuario && (
                            <>
                                {statusUsuario === 'novo' && (
                                    <TextField
                                        label="Definir Senha Inicial"
                                        type="password"
                                        fullWidth
                                        value={form.senha}
                                        onChange={e => setForm({ ...form, senha: e.target.value })}
                                        required
                                    />
                                )}

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
                                            <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                                                - {p.descricao}
                                            </Typography>
                                        </MenuItem>
                                    ))}
                                </TextField>
                            </>
                        )}
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>Cancelar</Button>
                    <Button
                        type="submit"
                        variant="contained"
                        disabled={loading || !statusUsuario}
                    >
                        {loading ? "Salvando..." : (statusUsuario === 'existente' ? "Vincular Usuário" : "Criar Usuário")}
                    </Button>
                </DialogActions>
            </form>
        </Dialog>
    );
};

export default UsuarioForm;