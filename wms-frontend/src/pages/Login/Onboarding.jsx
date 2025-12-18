import { useState } from 'react';
import api from '../../services/api'; // Ajuste o caminho conforme necessário
import { useNavigate } from 'react-router-dom';
import { Box, Button, Card, Typography, TextField, CircularProgress, Alert } from '@mui/material';
import { Upload, Lock } from 'lucide-react';

const Onboarding = () => {
    const [file, setFile] = useState(null);
    const [senha, setSenha] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!file || !senha) return;

        setLoading(true);
        setError('');

        const formData = new FormData();
        formData.append('file', file);
        formData.append('senha', senha);

        try {
            await api.post('/onboarding/upload-certificado', formData);
            alert("Ambiente criado com sucesso! Faça login novamente.");
            navigate('/login');
        } catch (err) {
            setError(err.response?.data || "Erro ao processar certificado.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'background.default', p: 2 }}>
            <Card sx={{ maxWidth: 450, width: '100%', p: 4, borderRadius: 2 }}>
                <Box sx={{ textAlign: 'center', mb: 4 }}>
                    <Typography variant="h4" fontWeight="bold" color="primary.main" gutterBottom>
                        Bem-vindo!
                    </Typography>
                    <Typography variant="body1" color="text.secondary">
                        Para começar, configure sua primeira empresa enviando o Certificado Digital A1.
                    </Typography>
                </Box>

                {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

                <form onSubmit={handleSubmit}>
                    <Box sx={{
                        border: '2px dashed #cbd5e1', borderRadius: 2, p: 4, textAlign: 'center', mb: 3, cursor: 'pointer',
                        bgcolor: '#f8fafc', '&:hover': { bgcolor: '#f1f5f9', borderColor: 'primary.main' }
                    }}>
                        <input
                            type="file"
                            accept=".pfx"
                            id="cert-upload"
                            style={{ display: 'none' }}
                            onChange={(e) => setFile(e.target.files[0])}
                        />
                        <label htmlFor="cert-upload" style={{ cursor: 'pointer', width: '100%', height: '100%', display: 'block' }}>
                            <Upload size={40} color="#64748b" style={{ marginBottom: 10 }} />
                            <Typography variant="body2" color="text.primary" fontWeight="bold">
                                {file ? file.name : "Clique para selecionar o arquivo .pfx"}
                            </Typography>
                        </label>
                    </Box>

                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                        <Lock size={20} color="#64748b" style={{ marginRight: 10 }} />
                        <TextField
                            fullWidth
                            label="Senha do Certificado"
                            type="password"
                            value={senha}
                            onChange={(e) => setSenha(e.target.value)}
                        />
                    </Box>

                    <Button
                        fullWidth
                        variant="contained"
                        size="large"
                        type="submit"
                        disabled={loading || !file || !senha}
                        sx={{ py: 1.5 }}
                    >
                        {loading ? <CircularProgress size={24} color="inherit" /> : 'Criar Ambiente'}
                    </Button>
                </form>
            </Card>
        </Box>
    );
};

export default Onboarding;