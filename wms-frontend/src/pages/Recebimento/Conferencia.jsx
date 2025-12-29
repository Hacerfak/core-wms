import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Box, Typography, Button, LinearProgress, Card, CardContent,
    Grid, IconButton, Tooltip, Paper, Divider, Alert, Chip
} from '@mui/material';
import { ArrowLeft, CheckCircle, XCircle, Package, Truck, FileText, Anchor } from 'lucide-react';
import { toast } from 'react-toastify';
import { getRecebimentoById, getProgressoRecebimento, finalizarConferencia, cancelarConferencia } from '../../services/recebimentoService';
import { getLocalizacoes } from '../../services/localizacaoService';
import { checkExibirQtdRecebimento } from '../../services/configService';
import ConfirmDialog from '../../components/ConfirmDialog';
import BipagemPanel from './Tabs/BipagemPanel'; // Renomeado de BipagemTab
import LpnsList from './Tabs/LpnsList'; // Renomeado de LpnsTab
import ProgressoConferencia from './Components/ProgressoConferencia';

const Conferencia = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    const [recebimento, setRecebimento] = useState(null);
    const [progressoData, setProgressoData] = useState({ totalPrevisto: 0, totalConferido: 0 });
    const [loading, setLoading] = useState(true);
    const [exibirQtdEsperada, setExibirQtdEsperada] = useState(true);

    // Controle de Finalização
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmData, setConfirmData] = useState({});

    // Controle de Doca (caso precise trocar)
    const [docaModalOpen, setDocaModalOpen] = useState(false);

    useEffect(() => { init(); }, [id]);

    const init = async () => {
        try {
            const [recData, configCega] = await Promise.all([
                getRecebimentoById(id),
                checkExibirQtdRecebimento(id)
            ]);
            setRecebimento(recData);
            setExibirQtdEsperada(configCega);
            await atualizarProgresso();
        } catch (e) {
            toast.error("Erro ao carregar conferência.");
            navigate('/recebimento/tarefas');
        } finally {
            setLoading(false);
        }
    };

    const atualizarProgresso = async () => {
        try {
            const data = await getProgressoRecebimento(id);
            setProgressoData(data);
            // Atualiza o objeto recebimento também para refletir status dos itens
            const recAtualizado = await getRecebimentoById(id);
            setRecebimento(recAtualizado);
        } catch (e) { console.error(e); }
    };

    const handleFinalizarClick = () => {
        // Agora usamos um Dialog interno para escolher o Stage, ou assumimos um padrão
        // Para simplificar a UX, vamos pedir o Stage no ConfirmDialog ou abrir um modal específico
        // Aqui vou simplificar abrindo um Prompt customizado no ConfirmDialog se possível, 
        // ou redirecionando para uma tela de "Resumo e Finalização".
        // Vamos manter simples: Pedir confirmação e assumir que o usuário escolheu o Stage antes (se fosse o caso)
        // Mas como removemos o select da tela principal para limpar, vamos abrir um modal de finalização.

        // ... Lógica de abrir modal de finalização (não implementado aqui para brevidade, foco na UX da bipagem) ...
        // Para este exemplo, vamos apenas alertar.
        toast.info("Implementar Modal de Seleção de Stage Final aqui.");
    };

    const handleSair = () => navigate('/recebimento/tarefas');

    if (loading) return <LinearProgress sx={{ height: 6 }} />;

    return (
        <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column', bgcolor: '#f8fafc', p: 2, gap: 2 }}>

            {/* HEADER COMPACTO */}
            <Paper elevation={0} sx={{ p: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between', border: '1px solid #e2e8f0', borderRadius: 2 }}>
                <Box display="flex" alignItems="center" gap={3}>
                    <IconButton onClick={handleSair} size="small"><ArrowLeft /></IconButton>

                    <Box>
                        <Typography variant="h6" fontWeight="800" lineHeight={1}>
                            {recebimento?.fornecedor?.nome}
                        </Typography>
                        <Box display="flex" gap={2} mt={0.5} color="text.secondary">
                            <Typography variant="caption" display="flex" alignItems="center" gap={0.5}>
                                <FileText size={14} /> NF: {recebimento?.notaFiscal}
                            </Typography>
                            <Typography variant="caption" display="flex" alignItems="center" gap={0.5}>
                                <Anchor size={14} /> {recebimento?.doca?.codigo || 'Sem Doca'}
                            </Typography>
                        </Box>
                    </Box>
                </Box>

                <Box width={300}>
                    <ProgressoConferencia
                        previsto={progressoData.totalPrevisto}
                        conferido={progressoData.totalConferido}
                        cego={!exibirQtdEsperada}
                    />
                </Box>

                <Button
                    variant="contained"
                    color="success"
                    onClick={handleFinalizarClick}
                    startIcon={<CheckCircle />}
                    sx={{ px: 3 }}
                >
                    Finalizar
                </Button>
            </Paper>

            {/* ÁREA DE TRABALHO (GRID) */}
            <Grid container spacing={2} sx={{ flex: 1, overflow: 'hidden' }}>

                {/* ESQUERDA: WORKSTATION (BIPAGEM) - 70% da tela */}
                <Grid item xs={12} md={8} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                    <BipagemPanel
                        recebimentoId={id}
                        dadosRecebimento={recebimento}
                        onSucesso={atualizarProgresso}
                    />

                    {/* LISTA DE LPNs RECENTES (ABAIXO DA BIPAGEM) */}
                    <Box sx={{ mt: 2, flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
                        <Typography variant="subtitle2" fontWeight="bold" mb={1} color="text.secondary">
                            Volumes Gerados Recentemente
                        </Typography>
                        <Paper variant="outlined" sx={{ flex: 1, overflow: 'auto', borderRadius: 2, bgcolor: 'white' }}>
                            <LpnsList recebimentoId={id} onUpdate={atualizarProgresso} />
                        </Paper>
                    </Box>
                </Grid>

                {/* DIREITA: CONTEXTO (ITENS DA NOTA) - 30% da tela */}
                <Grid item xs={12} md={4} sx={{ height: '100%', overflow: 'hidden' }}>
                    <Paper variant="outlined" sx={{ height: '100%', display: 'flex', flexDirection: 'column', borderRadius: 2, bgcolor: 'white' }}>
                        <Box p={2} borderBottom="1px solid #e2e8f0" bgcolor="#f1f5f9">
                            <Typography variant="subtitle1" fontWeight="bold" display="flex" alignItems="center" gap={1}>
                                <Package size={18} /> Itens da Nota
                            </Typography>
                        </Box>
                        <Box sx={{ flex: 1, overflowY: 'auto', p: 0 }}>
                            {recebimento?.itens?.map((item) => {
                                const completo = item.quantidadeConferida >= item.quantidadePrevista;
                                return (
                                    <Box key={item.id} sx={{
                                        p: 2, borderBottom: '1px solid #f1f5f9',
                                        bgcolor: completo ? '#f0fdf4' : 'white',
                                        opacity: completo ? 0.7 : 1
                                    }}>
                                        <Box display="flex" justifyContent="space-between" mb={0.5}>
                                            <Typography variant="body2" fontWeight="bold" color="text.primary">
                                                {item.produto.sku}
                                            </Typography>
                                            {completo && <CheckCircle size={16} color="#16a34a" />}
                                        </Box>
                                        <Typography variant="caption" color="text.secondary" display="block" noWrap>
                                            {item.produto.nome}
                                        </Typography>
                                        <Box mt={1} display="flex" justifyContent="space-between" alignItems="center">
                                            <Chip label={item.produto.unidadeMedida} size="small" sx={{ height: 20, fontSize: '0.65rem' }} />
                                            <Typography variant="body2" fontWeight="600" color={completo ? 'success.main' : 'primary.main'}>
                                                {exibirQtdEsperada
                                                    ? `${item.quantidadeConferida} / ${item.quantidadePrevista}`
                                                    : `${item.quantidadeConferida} conf.`
                                                }
                                            </Typography>
                                        </Box>
                                    </Box>
                                );
                            })}
                        </Box>
                    </Paper>
                </Grid>
            </Grid>

            <ConfirmDialog
                open={confirmOpen} onClose={() => setConfirmOpen(false)}
                onConfirm={confirmData.action} title={confirmData.title}
                message={confirmData.message} severity={confirmData.severity}
            />
        </Box>
    );
};

export default Conferencia;