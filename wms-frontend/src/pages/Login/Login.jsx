import { useState, useContext } from 'react';
import { AuthContext } from '../../contexts/AuthContext';
import { Box, Button, TextField, Typography, Card, CardContent, CircularProgress } from '@mui/material';
import { Warehouse } from 'lucide-react'; // Ícone legal
import { toast } from 'react-toastify';
import { useNavigate } from 'react-router-dom';

const Login = () => {
    const { login } = useContext(AuthContext);
    const navigate = useNavigate();

    const [form, setForm] = useState({ login: '', password: '' });
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);

        const success = await login(form.login, form.password);

        if (success) {
            toast.success("Bem-vindo ao WMS Core!");
            navigate('/dashboard'); // Vamos criar essa rota depois
        } else {
            toast.error("Usuário ou senha inválidos.");
        }
        setIsLoading(false);
    };

    return (
        <Box
            sx={{
                height: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'linear-gradient(135deg, #1e40af 0%, #2563eb 100%)' // Gradiente Azul Bonito
            }}
        >
            <Card sx={{ maxWidth: 400, width: '100%', m: 2, p: 2, borderRadius: 3 }}>
                <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, alignItems: 'center' }}>

                    <Box sx={{ p: 2, bgcolor: 'primary.light', borderRadius: '50%', color: 'white', mb: 1 }}>
                        <Warehouse size={32} />
                    </Box>

                    <Typography variant="h5" fontWeight="bold" color="primary.main">
                        WMS Core
                    </Typography>

                    <Typography variant="body2" color="text.secondary" mb={2}>
                        Entre para gerenciar seu armazém
                    </Typography>

                    <form onSubmit={handleSubmit} style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '20px' }}>
                        <TextField
                            label="Usuário"
                            fullWidth
                            value={form.login}
                            onChange={(e) => setForm({ ...form, login: e.target.value })}
                        />
                        <TextField
                            label="Senha"
                            type="password"
                            fullWidth
                            value={form.password}
                            onChange={(e) => setForm({ ...form, password: e.target.value })}
                        />

                        <Button
                            variant="contained"
                            size="large"
                            type="submit"
                            fullWidth
                            disabled={isLoading}
                        >
                            {isLoading ? <CircularProgress size={24} color="inherit" /> : "Entrar"}
                        </Button>
                    </form>

                </CardContent>
            </Card>
        </Box>
    );
};

export default Login;