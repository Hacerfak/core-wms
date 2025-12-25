import { useState, useEffect, useMemo } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button,
    TextField, Grid, MenuItem, Typography, Alert, Box, Fade,
    CircularProgress, Stepper, Step, StepLabel, Paper, Divider
} from '@mui/material';
import { toast } from 'react-toastify';
import dayjs from 'dayjs';
import 'dayjs/locale/pt-br';
import { criarAgendamento, getTurnos, getSaidasPendentes } from '../../../services/portariaService';
import { getParceiros } from '../../../services/parceiroService';
import { getLocalizacoes } from '../../../services/localizacaoService';
import SearchableSelect from '../../../components/SearchableSelect';
import { CheckCircle2, AlertCircle, Truck, Package, CalendarClock, MapPin } from 'lucide-react';

const DIAS_SEMANA_MAP = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SAB'];
const STEPS = ['Horário & Turno', 'Dados da Carga', 'Localização (Doca)'];

const AgendamentoForm = ({ open, onClose, onSuccess, dataInicial }) => {
    const [activeStep, setActiveStep] = useState(0);
    const [loading, setLoading] = useState(false);
    const [validatingTurno, setValidatingTurno] = useState(false);

    // Combos
    const [turnos, setTurnos] = useState([]);
    const [parceiros, setParceiros] = useState([]); // Transportadoras
    const [depositantes, setDepositantes] = useState([]); // Depositantes
    const [docas, setDocas] = useState([]);
    const [saidasPendentes, setSaidasPendentes] = useState([]); // Lista de pedidos para saída

    // Estado do Turno
    const [turnoDetectado, setTurnoDetectado] = useState(null);
    const [erroTurno, setErroTurno] = useState(null);

    const [form, setForm] = useState({
        tipo: 'ENTRADA',
        data: dayjs().format('YYYY-MM-DD'),
        horaInicio: '',

        transportadoraId: '',
        depositanteId: '', // Para entrada manual
        solicitacaoSaidaId: '', // Para saída

        placa: '',
        motorista: '',
        cpf: '',

        docaId: '',
        turnoId: ''
    });

    useEffect(() => {
        if (open) {
            carregarDependencias();
            setActiveStep(0);
            if (dataInicial) setForm(prev => ({ ...prev, ...dataInicial }));
            setTurnoDetectado(null);
        }
    }, [open, dataInicial]);

    // Validação de Turno (Passo 1)
    useEffect(() => {
        if (form.data && form.horaInicio && turnos.length > 0) {
            validarDisponibilidade();
        } else {
            setTurnoDetectado(null);
        }
    }, [form.data, form.horaInicio, turnos]);

    const carregarDependencias = async () => {
        try {
            const [t, p, d, s] = await Promise.all([
                getTurnos(),
                getParceiros(),
                getLocalizacoes('DOCA'),
                getSaidasPendentes()
            ]);
            setTurnos(t);

            // Separa parceiros
            const transportadoras = p.filter(x => x.tipo === 'TRANSPORTADORA' || x.tipo === 'AMBOS').map(x => ({ value: x.id, label: x.nome }));
            const deps = p.filter(x => x.tipo === 'CLIENTE' || x.tipo === 'AMBOS').map(x => ({ value: x.id, label: x.nome }));

            setParceiros(transportadoras);
            setDepositantes(deps);
            setDocas(d.map(x => ({ value: x.id, label: x.enderecoCompleto })));
            setSaidasPendentes(s);

        } catch (e) {
            toast.error("Erro ao carregar dados.");
        }
    };

    const validarDisponibilidade = () => {
        setValidatingTurno(true);
        setErroTurno(null);
        setTimeout(() => {
            const dataObj = dayjs(form.data);
            const diaSemanaStr = DIAS_SEMANA_MAP[dataObj.day()];
            const horaAgendada = form.horaInicio;

            const turnoValido = turnos.find(t => {
                if (!t.diasSemana || !t.diasSemana.includes(diaSemanaStr)) return false;
                const inicioStr = t.inicio.substring(0, 5);
                const fimStr = t.fim.substring(0, 5);
                return horaAgendada >= inicioStr && horaAgendada <= fimStr;
            });

            if (turnoValido) {
                setTurnoDetectado(turnoValido);
                setForm(prev => ({ ...prev, turnoId: turnoValido.id }));
            } else {
                setErroTurno(`Sem turno operacional em ${diaSemanaStr} às ${horaAgendada}.`);
                setTurnoDetectado(null);
            }
            setValidatingTurno(false);
        }, 300);
    };

    const handleNext = () => {
        // Validações antes de avançar
        if (activeStep === 0) {
            if (!turnoDetectado) return toast.warning("Selecione um horário válido dentro de um turno.");
        }
        if (activeStep === 1) {
            if (form.tipo === 'SAIDA' && !form.solicitacaoSaidaId) return toast.warning("Selecione o pedido de saída.");
            if (form.transportadoraId && !form.placa) return toast.warning("Se informou transportadora, a placa é obrigatória.");
        }

        setActiveStep((prev) => prev + 1);
    };

    const handleBack = () => setActiveStep((prev) => prev - 1);

    const handleSave = async () => {
        setLoading(true);
        try {
            const inicio = `${form.data}T${form.horaInicio}:00`;
            const fim = dayjs(inicio).add(1, 'hour').format('YYYY-MM-DDTHH:mm:ss');

            const payload = {
                ...form,
                dataInicio: inicio,
                dataFim: fim,
                placa: form.placa ? form.placa.toUpperCase() : '',
                docaId: form.docaId || null
            };

            await criarAgendamento(payload);
            toast.success("Agendamento realizado!");
            onSuccess();
            onClose();
        } catch (e) {
            toast.error(e.response?.data?.message || "Erro ao agendar.");
        } finally {
            setLoading(false);
        }
    };

    // --- RENDERIZAÇÃO DOS PASSOS ---

    const renderStep0 = () => (
        <Grid container spacing={2}>
            <Grid item xs={12}><Typography variant="subtitle2" color="primary">Definição de Horário</Typography></Grid>
            <Grid item xs={12} sm={4}>
                <TextField select label="Tipo de Operação" fullWidth value={form.tipo} onChange={e => setForm({ ...form, tipo: e.target.value })}>
                    <MenuItem value="ENTRADA">Recebimento (Entrada)</MenuItem>
                    <MenuItem value="SAIDA">Expedição (Saída)</MenuItem>
                </TextField>
            </Grid>
            <Grid item xs={6} sm={4}>
                <TextField type="date" label="Data" fullWidth InputLabelProps={{ shrink: true }} value={form.data} onChange={e => setForm({ ...form, data: e.target.value })} />
            </Grid>
            <Grid item xs={6} sm={4}>
                <TextField type="time" label="Hora" fullWidth InputLabelProps={{ shrink: true }} value={form.horaInicio} onChange={e => setForm({ ...form, horaInicio: e.target.value })} />
            </Grid>
            <Grid item xs={12}>
                {validatingTurno && <Box display="flex" gap={1}><CircularProgress size={16} /><Typography variant="caption">Validando turno...</Typography></Box>}
                {turnoDetectado && <Alert severity="success" icon={<CheckCircle2 fontSize="inherit" />}>Turno: <strong>{turnoDetectado.nome}</strong> disponível.</Alert>}
                {erroTurno && <Alert severity="error" icon={<AlertCircle fontSize="inherit" />}>{erroTurno}</Alert>}
            </Grid>
        </Grid>
    );

    const renderStep1 = () => (
        <Grid container spacing={2}>
            <Grid item xs={12}><Typography variant="subtitle2" color="primary">Dados da Carga</Typography></Grid>

            {/* CONDICIONAL: SAÍDA */}
            {form.tipo === 'SAIDA' && (
                <Grid item xs={12}>
                    <TextField select label="Solicitação de Saída (Pedido)" fullWidth required value={form.solicitacaoSaidaId} onChange={e => setForm({ ...form, solicitacaoSaidaId: e.target.value })} helperText="Selecione o pedido pronto para despacho">
                        {saidasPendentes.length === 0 && <MenuItem disabled>Nenhum pedido pendente</MenuItem>}
                        {saidasPendentes.map(s => (
                            <MenuItem key={s.id} value={s.id}>
                                #{s.codigoExterno} - {s.cliente?.nome} ({s.rota || 'Sem rota'})
                            </MenuItem>
                        ))}
                    </TextField>
                </Grid>
            )}

            {/* CONDICIONAL: ENTRADA */}
            {form.tipo === 'ENTRADA' && (
                <Grid item xs={12}>
                    <SearchableSelect
                        label="Depositante / Fornecedor (Opcional)"
                        options={depositantes}
                        value={form.depositanteId}
                        onChange={e => setForm({ ...form, depositanteId: e.target.value })}
                        helperText="Será atualizado automaticamente ao subir o XML"
                    />
                </Grid>
            )}

            <Grid item xs={12}><Divider sx={{ my: 1 }}><Typography variant="caption">TRANSPORTE</Typography></Divider></Grid>

            <Grid item xs={12} sm={8}>
                <SearchableSelect label="Transportadora (Opcional)" options={parceiros} value={form.transportadoraId} onChange={e => setForm({ ...form, transportadoraId: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={4}>
                <TextField label="Placa Veículo" fullWidth value={form.placa} onChange={e => setForm({ ...form, placa: e.target.value.toUpperCase() })} placeholder="ABC-1234" required={!!form.transportadoraId} />
            </Grid>
            <Grid item xs={12} sm={8}>
                <TextField label="Nome Motorista (Opcional)" fullWidth value={form.motorista} onChange={e => setForm({ ...form, motorista: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={4}>
                <TextField label="CPF Motorista" fullWidth value={form.cpf} onChange={e => setForm({ ...form, cpf: e.target.value })} />
            </Grid>
        </Grid>
    );

    const renderStep2 = () => (
        <Grid container spacing={2}>
            <Grid item xs={12}><Typography variant="subtitle2" color="primary">Alocação de Doca</Typography></Grid>
            <Grid item xs={12}>
                <Alert severity="info" sx={{ mb: 2 }}>
                    A definição da doca é opcional neste momento. Ela pode ser atribuída ou alterada durante o Check-in na portaria.
                </Alert>
                <TextField select label="Doca Sugerida" fullWidth value={form.docaId} onChange={e => setForm({ ...form, docaId: e.target.value })}>
                    <MenuItem value=""><em>Definir na Chegada</em></MenuItem>
                    {docas.map(d => <MenuItem key={d.value} value={d.value}>{d.label}</MenuItem>)}
                </TextField>
            </Grid>

            <Grid item xs={12}>
                <Paper variant="outlined" sx={{ p: 2, bgcolor: '#f8fafc', mt: 2 }}>
                    <Typography variant="subtitle2" fontWeight="bold">Resumo:</Typography>
                    <Box display="flex" gap={2} mt={1} flexWrap="wrap">
                        <Typography variant="body2">📅 {dayjs(form.data).format('DD/MM/YYYY')} às {form.horaInicio}</Typography>
                        <Typography variant="body2">🚛 {form.tipo}</Typography>
                        {form.placa && <Typography variant="body2">🔢 Placa: {form.placa}</Typography>}
                    </Box>
                </Paper>
            </Grid>
        </Grid>
    );

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle sx={{ borderBottom: '1px solid #e2e8f0' }}>
                {dataInicial?.id ? 'Editar Agendamento' : 'Novo Agendamento'}
            </DialogTitle>

            <DialogContent sx={{ pt: 3, minHeight: 400 }}>
                <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 4 }}>
                    {STEPS.map((label) => <Step key={label}><StepLabel>{label}</StepLabel></Step>)}
                </Stepper>

                <Box sx={{ mt: 2 }}>
                    {activeStep === 0 && renderStep0()}
                    {activeStep === 1 && renderStep1()}
                    {activeStep === 2 && renderStep2()}
                </Box>
            </DialogContent>

            <DialogActions sx={{ p: 3, borderTop: '1px solid #e2e8f0', bgcolor: '#fafafa' }}>
                <Button onClick={onClose} color="inherit" sx={{ mr: 'auto' }}>Cancelar</Button>

                {activeStep > 0 && <Button onClick={handleBack}>Voltar</Button>}

                {activeStep < STEPS.length - 1 ? (
                    <Button variant="contained" onClick={handleNext} disabled={activeStep === 0 && !turnoDetectado}>
                        Próximo
                    </Button>
                ) : (
                    <Button variant="contained" color="success" onClick={handleSave} disabled={loading}>
                        {loading ? 'Salvando...' : 'Confirmar Agendamento'}
                    </Button>
                )}
            </DialogActions>
        </Dialog>
    );
};

export default AgendamentoForm;