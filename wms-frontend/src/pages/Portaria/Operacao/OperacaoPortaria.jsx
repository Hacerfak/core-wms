import { useState, useEffect, useRef } from 'react';
import {
    Box, Button, Typography, Paper, Grid, TextField, InputAdornment, Card, CardContent,
    Divider, IconButton, Dialog, DialogTitle, DialogContent, DialogActions, Chip, Alert
} from '@mui/material';
import { Search, ArrowLeft, LogIn, LogOut, Truck, User, Clock, MapPin, RectangleHorizontal } from 'lucide-react';
import { toast } from 'react-toastify';
import { useNavigate } from 'react-router-dom';
import { getPatio, realizarCheckin, realizarCheckout } from '../../../services/portariaService';

// Mapeamento de Status mais conciso para caber no card
const STATUS_CONFIG = {
    'NA_PORTARIA': { label: 'NO PÁTIO', color: 'warning' }, // Texto reduzido
    'NA_DOCA': { label: 'NA DOCA', color: 'primary' },
    'FINALIZADO': { label: 'SAIU', color: 'success' }
};

const OperacaoPortaria = () => {
    const navigate = useNavigate();
    const searchInputRef = useRef(null);
    const [veiculosPatio, setVeiculosPatio] = useState([]);
    const [codigoBusca, setCodigoBusca] = useState('');

    const [checkinOpen, setCheckinOpen] = useState(false);
    const [checkinForm, setCheckinForm] = useState({ codigo: '', placa: '', motorista: '', cpf: '' });

    const [checkoutOpen, setCheckoutOpen] = useState(false);
    const [selectedReserva, setSelectedReserva] = useState(null);

    useEffect(() => {
        loadPatio();
        setTimeout(() => searchInputRef.current?.focus(), 500);
    }, []);

    useEffect(() => {
        const interval = setInterval(() => {
            if (!checkinOpen && !checkoutOpen && document.activeElement !== searchInputRef.current) {
                searchInputRef.current?.focus();
            }
        }, 5000);
        return () => clearInterval(interval);
    }, [checkinOpen, checkoutOpen]);

    const loadPatio = async () => {
        try {
            const data = await getPatio();
            setVeiculosPatio(data);
        } catch (e) { toast.error("Erro ao atualizar pátio."); }
    };

    const handleSearchCheckin = () => {
        if (!codigoBusca) return;
        setCheckinForm({ codigo: codigoBusca, placa: '', motorista: '', cpf: '' });
        setCheckinOpen(true);
    };

    const confirmCheckin = async () => {
        try {
            await realizarCheckin(checkinForm.codigo, {
                placa: checkinForm.placa,
                motorista: checkinForm.motorista,
                cpf: checkinForm.cpf
            });
            toast.success("Entrada registrada com sucesso!");
            setCheckinOpen(false);
            setCodigoBusca('');
            loadPatio();
            setTimeout(() => searchInputRef.current?.focus(), 100);
        } catch (e) {
            toast.error(e.response?.data?.message || "Erro no check-in.");
        }
    };

    const confirmCheckout = async () => {
        try {
            await realizarCheckout(selectedReserva.codigoReserva, null);
            toast.success("Saída registrada com sucesso!");
            setCheckoutOpen(false);
            loadPatio();
        } catch (e) {
            toast.error("Erro no check-out.");
        }
    };

    return (
        <Box>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                <Box display="flex" alignItems="center" gap={2}>
                    <Button startIcon={<ArrowLeft />} onClick={() => navigate('/portaria')} color="inherit">Voltar</Button>
                    <Typography variant="h5" fontWeight="bold">Operação de Portaria</Typography>
                </Box>
            </Box>

            {/* ÁREA DE CHECK-IN RÁPIDO */}
            <Paper sx={{ p: 4, mb: 4, bgcolor: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: 2 }}>
                <Typography variant="subtitle1" fontWeight="bold" mb={2}>Realizar Entrada (Check-in)</Typography>
                <Box display="flex" gap={2}>
                    <TextField
                        inputRef={searchInputRef}
                        fullWidth
                        autoFocus
                        placeholder="Bipe o QR Code ou digite o código da reserva..."
                        value={codigoBusca}
                        onChange={e => setCodigoBusca(e.target.value.toUpperCase())}
                        InputProps={{
                            startAdornment: <InputAdornment position="start"><Search size={24} color="#94a3b8" /></InputAdornment>,
                            style: { fontSize: '1.2rem', padding: '8px' }
                        }}
                        onKeyDown={e => e.key === 'Enter' && handleSearchCheckin()}
                    />
                    <Button
                        variant="contained"
                        size="large"
                        onClick={handleSearchCheckin}
                        startIcon={<LogIn size={24} />}
                        sx={{ px: 4 }}
                    >
                        Registrar Entrada
                    </Button>
                </Box>
            </Paper>

            {/* LISTAGEM DO PÁTIO */}
            <Typography variant="h6" fontWeight="bold" mb={2} display="flex" alignItems="center" gap={1}>
                <Truck size={24} /> Veículos no Pátio ({veiculosPatio.length})
            </Typography>

            <Grid container spacing={2}>
                {veiculosPatio.map(v => {
                    const status = STATUS_CONFIG[v.status] || { label: v.status, color: 'default' };
                    return (
                        <Grid item xs={12} md={6} lg={4} key={v.id}>
                            <Card sx={{ borderLeft: '4px solid', borderColor: v.tipo === 'ENTRADA' ? 'success.main' : 'primary.main', height: '100%' }}>
                                <CardContent>
                                    <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={1} gap={1}>
                                        <Box>
                                            <Typography variant="h5" fontWeight="bold" display="flex" alignItems="center" gap={1}>
                                                {v.placaVeiculo || 'SEM PLACA'}
                                            </Typography>
                                            <Typography variant="caption" color="text.secondary" fontFamily="monospace">{v.codigoReserva}</Typography>
                                        </Box>

                                        {/* Status Chip Ajustado */}
                                        <Chip
                                            label={status.label}
                                            size="small"
                                            color={status.color}
                                            variant="filled" // Ou 'outlined' se preferir mais leve
                                            sx={{ fontWeight: 'bold', fontSize: '0.7rem', height: 24 }}
                                        />
                                    </Box>

                                    <Divider sx={{ my: 2 }} />

                                    <Box display="flex" flexDirection="column" gap={1.5}>
                                        <Box display="flex" alignItems="center" gap={1}>
                                            <User size={18} color="#64748b" />
                                            <Typography variant="body1">{v.nomeMotoristaAvulso || v.motorista?.nome || 'Motorista não ident.'}</Typography>
                                        </Box>
                                        <Box display="flex" alignItems="center" gap={1}>
                                            <Clock size={18} color="#64748b" />
                                            <Typography variant="body2">Entrou às {new Date(v.dataChegada).toLocaleTimeString()}</Typography>
                                        </Box>
                                        {v.doca && (
                                            <Box display="flex" alignItems="center" gap={1} color="primary.main" bgcolor="#f0f9ff" p={0.5} borderRadius={1}>
                                                <MapPin size={18} />
                                                <Typography variant="body2" fontWeight="bold">Dirigir-se à: {v.doca.enderecoCompleto}</Typography>
                                            </Box>
                                        )}
                                    </Box>

                                    <Button
                                        fullWidth
                                        variant="outlined"
                                        color="error"
                                        sx={{ mt: 3 }}
                                        startIcon={<LogOut size={18} />}
                                        onClick={() => { setSelectedReserva(v); setCheckoutOpen(true); }}
                                    >
                                        Registrar Saída (Check-out)
                                    </Button>
                                </CardContent>
                            </Card>
                        </Grid>
                    );
                })}
                {veiculosPatio.length === 0 && (
                    <Grid item xs={12}>
                        <Alert severity="info">Nenhum veículo no pátio no momento.</Alert>
                    </Grid>
                )}
            </Grid>

            {/* Dialog Check-in */}
            <Dialog open={checkinOpen} onClose={() => setCheckinOpen(false)} maxWidth="xs" fullWidth>
                <DialogTitle>Confirmar Entrada</DialogTitle>
                <DialogContent>
                    <Box display="flex" flexDirection="column" gap={2} mt={1}>
                        <TextField label="Código Reserva" disabled value={checkinForm.codigo} fullWidth />
                        <TextField
                            label="Placa do Veículo"
                            value={checkinForm.placa}
                            onChange={e => setCheckinForm({ ...checkinForm, placa: e.target.value.toUpperCase() })}
                            fullWidth autoFocus
                            InputProps={{
                                startAdornment: <InputAdornment position="start"><RectangleHorizontal size={20} color="#94a3b8" /></InputAdornment>
                            }}
                        />
                        <TextField label="Nome do Motorista" value={checkinForm.motorista} onChange={e => setCheckinForm({ ...checkinForm, motorista: e.target.value })} fullWidth />
                        <TextField label="CPF" value={checkinForm.cpf} onChange={e => setCheckinForm({ ...checkinForm, cpf: e.target.value })} fullWidth />
                    </Box>
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={() => setCheckinOpen(false)}>Cancelar</Button>
                    <Button variant="contained" onClick={confirmCheckin} size="large">Confirmar Entrada</Button>
                </DialogActions>
            </Dialog>

            {/* Dialog Check-out */}
            <Dialog open={checkoutOpen} onClose={() => setCheckoutOpen(false)} maxWidth="xs" fullWidth>
                <DialogTitle>Saída de Veículo</DialogTitle>
                <DialogContent>
                    <Typography>Confirma a saída do veículo <b>{selectedReserva?.placaVeiculo}</b>?</Typography>
                    <Typography variant="body2" color="text.secondary" mt={1}>
                        Permanência: {selectedReserva ? ((new Date() - new Date(selectedReserva.dataChegada)) / 60000).toFixed(0) : 0} minutos.
                    </Typography>
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={() => setCheckoutOpen(false)}>Cancelar</Button>
                    <Button variant="contained" color="error" onClick={confirmCheckout} autoFocus>Confirmar Saída</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};
export default OperacaoPortaria;