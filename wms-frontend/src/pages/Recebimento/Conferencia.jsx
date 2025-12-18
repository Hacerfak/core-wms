import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Typography, Paper, TextField, Button, Grid, LinearProgress, Card, CardContent, Divider, Alert, Chip } from '@mui/material';
import { ArrowLeft, Barcode, CheckCircle2, Box as BoxIcon, CheckCircle, XCircle, ChevronRight } from 'lucide-react';
import { toast } from 'react-toastify';
import { getRecebimentoById, conferirProduto, finalizarConferencia, cancelarConferencia } from '../../services/recebimentoService';
import { checkExibirQtdRecebimento } from '../../services/configService';
import ItemConferenciaModal from './ItemConferenciaModal';
import ConfirmDialog from '../../components/ConfirmDialog'; // <--- Import

const Conferencia = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const skuInputRef = useRef(null);
    const qtdInputRef = useRef(null);

    const [recebimento, setRecebimento] = useState(null);
    const [loading, setLoading] = useState(true);
    const [form, setForm] = useState({ sku: '', qtd: '' });
    const [ultimoLpn, setUltimoLpn] = useState(null);
    const [progresso, setProgresso] = useState(0);
    const [selectedItem, setSelectedItem] = useState(null);
    const [exibirQtdEsperada, setExibirQtdEsperada] = useState(true);

    // Confirm States
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmData, setConfirmData] = useState({ title: '', message: '', action: null, severity: 'primary' });

    useEffect(() => { carregarTudo(); }, [id]);

    const carregarTudo = async () => {
        try {
            const deveExibir = await checkExibirQtdRecebimento();
            setExibirQtdEsperada(deveExibir === true);
            await carregarDados();
        } catch (e) { console.error("Erro inicialização", e); }
    };

    const carregarDados = async () => {
        try {
            const data = await getRecebimentoById(id);
            setRecebimento({ ...data });
            calcularProgresso(data);
        } catch (error) {
            toast.error("Erro ao carregar nota.");
            navigate('/recebimento');
        } finally {
            setLoading(false);
            setTimeout(() => skuInputRef.current?.focus(), 100);
        }
    };

    const calcularProgresso = (data) => {
        if (!data?.itens) return;
        const totalEsperado = data.itens.reduce((acc, item) => acc + item.quantidadeNota, 0);
        const totalConferido = data.itens.reduce((acc, item) => acc + (item.quantidadeConferida || 0), 0);
        const porcentagem = totalEsperado > 0 ? (totalConferido / totalEsperado) * 100 : 0;
        setProgresso(porcentagem);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!form.sku || !form.qtd) return;
        try {
            const lpnGerado = await conferirProduto(id, form.sku, form.qtd);
            toast.success(`LPN Gerado: ${lpnGerado}`);
            setUltimoLpn({ codigo: lpnGerado, sku: form.sku, qtd: form.qtd });
            setForm({ sku: '', qtd: '' });
            await carregarDados();
            skuInputRef.current?.focus();
        } catch (error) {
            toast.error(error.response?.data?.message || "Erro na conferência.");
            skuInputRef.current?.select();
        }
    };

    const handleSkuKeyDown = (e) => { if (e.key === 'Enter' && form.sku) { e.preventDefault(); qtdInputRef.current?.focus(); } };

    // --- NOVOS HANDLERS COM DIALOG ---
    const handleFinalizarClick = () => {
        setConfirmData({
            title: 'Finalizar Conferência',
            message: 'Deseja realmente finalizar a conferência? O estoque será consolidado.',
            severity: 'primary',
            action: async () => {
                try {
                    setLoading(true);
                    const rec = await finalizarConferencia(id);
                    if (rec.status === 'DIVERGENTE') toast.error("Finalizado com DIVERGÊNCIAS!");
                    else toast.success("Finalizado com SUCESSO!");
                    navigate('/recebimento');
                } catch (error) {
                    toast.error("Erro ao finalizar.");
                    setLoading(false);
                }
            }
        });
        setConfirmOpen(true);
    };

    const handleCancelarClick = () => {
        setConfirmData({
            title: 'Cancelar Conferência',
            message: 'ATENÇÃO: Isso apagará TODO o progresso de bipagem desta nota. Continuar?',
            severity: 'error',
            action: async () => {
                try {
                    setLoading(true);
                    await cancelarConferencia(id);
                    toast.info("Reiniciado.");
                    navigate('/recebimento');
                } catch (error) {
                    toast.error("Erro ao cancelar.");
                    setLoading(false);
                }
            }
        });
        setConfirmOpen(true);
    };

    if (loading && !recebimento) return <LinearProgress />;

    return (
        <Box>
            <Button startIcon={<ArrowLeft />} onClick={() => navigate('/recebimento')} sx={{ mb: 2 }}>Voltar</Button>

            <Grid container spacing={3}>
                <Grid item xs={12} md={7}>
                    <Paper sx={{ p: 4, borderRadius: 2, border: '1px solid #e2e8f0' }}>
                        <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                            <Typography variant="h5" fontWeight="bold">Conferência</Typography>
                            {!exibirQtdEsperada && <Chip label="Modo Cego Ativo" color="warning" variant="outlined" sx={{ fontWeight: 'bold' }} />}
                        </Box>
                        <Typography color="text.secondary" mb={4}>Bipe o produto e informe a quantidade.</Typography>
                        <form onSubmit={handleSubmit}>
                            <Box mb={3}>
                                <Typography variant="subtitle2" fontWeight="bold" mb={1}>1. Código (SKU/EAN/DUN)</Typography>
                                <TextField inputRef={skuInputRef} fullWidth value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} onKeyDown={handleSkuKeyDown} placeholder="Bipe aqui..." autoComplete="off" InputProps={{ startAdornment: <Barcode size={20} style={{ marginRight: 8, opacity: 0.5 }} /> }} sx={{ '& input': { fontSize: '1.2rem', p: 1.5 } }} />
                            </Box>
                            <Box mb={4}>
                                <Typography variant="subtitle2" fontWeight="bold" mb={1}>2. Quantidade</Typography>
                                <TextField inputRef={qtdInputRef} fullWidth type="number" value={form.qtd} onChange={(e) => setForm({ ...form, qtd: e.target.value })} placeholder="Ex: 10" InputProps={{ startAdornment: <BoxIcon size={20} style={{ marginRight: 8, opacity: 0.5 }} /> }} sx={{ '& input': { fontSize: '1.2rem', p: 1.5 } }} />
                            </Box>
                            <Button type="submit" variant="contained" fullWidth size="large" disabled={!form.sku || !form.qtd} sx={{ py: 2 }}>Gerar Etiqueta LPN</Button>
                        </form>
                    </Paper>
                    {ultimoLpn && <Alert icon={<CheckCircle2 fontSize="inherit" />} severity="success" sx={{ mt: 3 }}>LPN Gerado: <b>{ultimoLpn.codigo}</b> ({ultimoLpn.sku} - {ultimoLpn.qtd} un.)</Alert>}
                </Grid>

                <Grid item xs={12} md={5}>
                    <Card elevation={0} sx={{ border: '1px solid #e2e8f0', bgcolor: '#f8fafc' }}>
                        <CardContent>
                            <Typography variant="overline" color="text.secondary" fontWeight="bold">Resumo</Typography>
                            <Typography variant="h6" fontWeight="bold">{recebimento?.numNotaFiscal}</Typography>
                            <Typography variant="body2" color="text.secondary" paragraph>{recebimento?.fornecedor}</Typography>
                            <Divider sx={{ my: 2 }} />
                            {exibirQtdEsperada ? (
                                <>
                                    <Box display="flex" justifyContent="space-between" mb={1}><Typography variant="body2" fontWeight="bold">Progresso</Typography><Typography variant="body2" fontWeight="bold">{Math.round(progresso)}%</Typography></Box>
                                    <LinearProgress variant="determinate" value={progresso} sx={{ height: 10, borderRadius: 5, mb: 3 }} />
                                </>
                            ) : (<Alert severity="info" sx={{ mb: 3 }}>Progresso oculto (Conferência Cega)</Alert>)}

                            <Typography variant="subtitle2" fontWeight="bold" mb={2}>Itens da Nota (Clique para Detalhes)</Typography>
                            <Box sx={{ maxHeight: 300, overflow: 'auto' }}>
                                {recebimento?.itens?.map((item) => {
                                    const qtdConf = item.quantidadeConferida || 0;
                                    const completo = exibirQtdEsperada && (qtdConf >= item.quantidadeNota);
                                    return (
                                        <Box key={item.id} onClick={() => setSelectedItem(item)} sx={{ p: 1.5, mb: 1, bgcolor: completo ? '#dcfce7' : (qtdConf > 0 ? '#fffbeb' : 'white'), borderRadius: 1, border: '1px solid', borderColor: '#e2e8f0', cursor: 'pointer', '&:hover': { borderColor: 'primary.main' } }}>
                                            <Box display="flex" justifyContent="space-between" alignItems="center">
                                                <Box>
                                                    <Typography variant="body2" fontWeight="bold">{item.produto.sku}</Typography>
                                                    <Typography variant="caption" color="text.secondary" noWrap display="block" sx={{ maxWidth: 200 }}>{item.produto.nome}</Typography>
                                                </Box>
                                                <Box textAlign="right">
                                                    <Typography variant="caption" sx={{ bgcolor: '#f1f5f9', px: 1, borderRadius: 1, fontWeight: 'bold', display: 'block', mb: 0.5 }}>{exibirQtdEsperada ? `${qtdConf} / ${item.quantidadeNota}` : `Qtd: ${qtdConf}`}</Typography>
                                                    <ChevronRight size={14} color="#94a3b8" />
                                                </Box>
                                            </Box>
                                        </Box>
                                    );
                                })}
                            </Box>
                            <Divider sx={{ my: 3 }} />
                            <Button variant="contained" fullWidth size="large" onClick={handleFinalizarClick} startIcon={<CheckCircle />} sx={{ mb: 2 }}>Finalizar Conferência</Button>
                            <Button variant="outlined" color="error" fullWidth size="large" onClick={handleCancelarClick} startIcon={<XCircle />}>Cancelar Conferência</Button>
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>

            <ItemConferenciaModal open={Boolean(selectedItem)} onClose={() => setSelectedItem(null)} recebimentoId={id} item={selectedItem} exibirQtdEsperada={exibirQtdEsperada} />

            <ConfirmDialog
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                onConfirm={confirmData.action}
                title={confirmData.title}
                message={confirmData.message}
                severity={confirmData.severity || 'error'}
            />
        </Box>
    );
};

export default Conferencia;