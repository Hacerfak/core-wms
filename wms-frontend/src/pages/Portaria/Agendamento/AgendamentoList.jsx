import { useState, useEffect } from 'react';
import {
    Box, Button, Paper, Typography, Grid, Card, CardContent,
    TextField, Chip, Dialog, DialogTitle, DialogContent,
    DialogActions, MenuItem, IconButton, Tooltip, Divider
} from '@mui/material';
import {
    Calendar, ArrowLeft, Plus, Upload, Truck,
    MapPin, Clock, Filter, Eye, FileText, Share2,
    Printer, Copy, XCircle, UserX, Trash2, RectangleHorizontal
} from 'lucide-react';
import { toast } from 'react-toastify';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import QRCode from 'react-qr-code';

import { getAgenda, vincularXmlAgendamento, cancelarAgendamento, marcarNoShow, excluirAgendamento } from '../../../services/portariaService';
import { getParceiros } from '../../../services/parceiroService';
import AgendamentoForm from './AgendamentoForm';
import SearchableSelect from '../../../components/SearchableSelect';
import ConfirmDialog from '../../../components/ConfirmDialog';

const STATUS_MAP = {
    'AGENDADO': { label: 'AGENDADO', color: 'default' },
    'NA_PORTARIA': { label: 'CHECK-IN OK', color: 'primary' },
    'NA_DOCA': { label: 'NA DOCA', color: 'info' },
    'FINALIZADO': { label: 'FINALIZADO', color: 'success' },
    'CANCELADO': { label: 'CANCELADO', color: 'error' },
    'NO_SHOW': { label: 'NÃO COMPARECEU', color: 'warning' }
};

const AgendamentoList = () => {
    const navigate = useNavigate();
    const [agenda, setAgenda] = useState([]);

    // Filtros
    const [filtros, setFiltros] = useState({
        data: dayjs().format('YYYY-MM-DD'),
        tipo: 'TODOS',
        parceiroId: ''
    });
    const [listaParceiros, setListaParceiros] = useState([]);

    // Modais
    const [modalOpen, setModalOpen] = useState(false);
    const [xmlModalOpen, setXmlModalOpen] = useState(false);
    const [detalhesOpen, setDetalhesOpen] = useState(false);
    const [qrModalOpen, setQrModalOpen] = useState(false);

    // Dialog Confirmação
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmData, setConfirmData] = useState({ title: '', message: '', action: null });

    const [selectedItem, setSelectedItem] = useState(null);
    const [file, setFile] = useState(null);

    useEffect(() => {
        loadParceiros();
        loadAgenda();
    }, [filtros.data]);

    const loadParceiros = async () => {
        try {
            const data = await getParceiros();
            setListaParceiros(data.map(p => ({ value: p.id, label: p.nome })));
        } catch (e) { }
    };

    const loadAgenda = async () => {
        try {
            const data = await getAgenda(filtros.data);
            setAgenda(data);
        } catch (e) { toast.error("Erro ao carregar agenda."); }
    };

    const agendaFiltrada = agenda.filter(item => {
        if (filtros.tipo !== 'TODOS' && item.tipo !== filtros.tipo) return false;
        if (filtros.parceiroId) {
            const isTransp = item.transportadora?.id === filtros.parceiroId;
            const isCliente = item.solicitacaoSaida?.cliente?.id === filtros.parceiroId;
            const isForn = item.solicitacaoEntrada?.fornecedor?.id === filtros.parceiroId;
            if (!isTransp && !isCliente && !isForn) return false;
        }
        return true;
    });

    const handleCopyCode = (code) => {
        navigator.clipboard.writeText(code);
        toast.info("Código copiado!");
    };

    const handleUploadXml = async () => {
        if (!file || !selectedItem) return;
        try {
            await vincularXmlAgendamento(selectedItem.id, file);
            toast.success("XML Vinculado com Sucesso!");
            setXmlModalOpen(false);
            setFile(null);
            loadAgenda();
        } catch (e) { toast.error("Erro no upload."); }
    };

    // Ações de Cancelamento / No Show
    const handleAction = (type, item) => {
        let title, message, apiFunc;

        if (type === 'CANCELAR') {
            title = 'Cancelar Agendamento';
            message = `Deseja cancelar o agendamento ${item.codigoReserva}?`;
            apiFunc = () => cancelarAgendamento(item.id);
        } else if (type === 'NO_SHOW') {
            title = 'Registrar Não Comparecimento';
            message = `Confirmar que o veículo NÃO compareceu?`;
            apiFunc = () => marcarNoShow(item.id);
        } else if (type === 'EXCLUIR') {
            title = 'Excluir Agendamento';
            message = `Isso removerá o registro permanentemente. Continuar?`;
            apiFunc = () => excluirAgendamento(item.id);
        }

        setConfirmData({
            title, message,
            action: async () => {
                try {
                    await apiFunc();
                    toast.success("Operação realizada!");
                    loadAgenda();
                } catch (e) { toast.error("Erro na operação."); }
            }
        });
        setConfirmOpen(true);
    };

    const handlePrintQr = () => {
        const printWindow = window.open('', '', 'width=600,height=600');
        printWindow.document.write('<html><body>');
        printWindow.document.write(document.getElementById('qr-code-container').innerHTML);
        printWindow.document.write('</body></html>');
        printWindow.document.close();
        printWindow.print();
    };

    return (
        <Box>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                <Box display="flex" alignItems="center" gap={2}>
                    <Button startIcon={<ArrowLeft />} onClick={() => navigate('/portaria')} color="inherit">Voltar</Button>
                    <Typography variant="h5" fontWeight="bold">Agenda de Pátio</Typography>
                </Box>
                <Button variant="contained" startIcon={<Plus />} onClick={() => setModalOpen(true)} size="large">
                    Novo Agendamento
                </Button>
            </Box>

            <Paper sx={{ p: 2, mb: 3, borderRadius: 2 }}>
                <Grid container spacing={2} alignItems="center">
                    <Grid item xs={12} sm={3}>
                        <TextField
                            type="date" label="Data" fullWidth size="small"
                            InputLabelProps={{ shrink: true }}
                            value={filtros.data}
                            onChange={e => setFiltros({ ...filtros, data: e.target.value })}
                        />
                    </Grid>
                    <Grid item xs={6} sm={3}>
                        <TextField
                            select label="Tipo" fullWidth size="small"
                            value={filtros.tipo}
                            onChange={e => setFiltros({ ...filtros, tipo: e.target.value })}
                        >
                            <MenuItem value="TODOS">Todos</MenuItem>
                            <MenuItem value="ENTRADA">Entrada</MenuItem>
                            <MenuItem value="SAIDA">Saída</MenuItem>
                        </TextField>
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <SearchableSelect
                            label="Filtrar por Parceiro"
                            options={listaParceiros}
                            value={filtros.parceiroId}
                            onChange={e => setFiltros({ ...filtros, parceiroId: e.target.value })}
                        />
                    </Grid>
                    <Grid item xs={6} sm={2}>
                        <Button startIcon={<Filter size={18} />} onClick={loadAgenda} fullWidth variant="outlined">Atualizar</Button>
                    </Grid>
                </Grid>
            </Paper>

            <Grid container spacing={2}>
                {agendaFiltrada.length === 0 && (
                    <Grid item xs={12}>
                        <Paper sx={{ p: 6, textAlign: 'center', bgcolor: '#f8fafc', border: '1px dashed #e2e8f0', borderRadius: 3 }}>
                            <Calendar size={48} color="#cbd5e1" style={{ marginBottom: 16 }} />
                            <Typography color="text.secondary" variant="h6">Nenhum agendamento encontrado para esta data.</Typography>
                            <Button sx={{ mt: 2 }} onClick={() => setModalOpen(true)}>Criar Primeiro Agendamento</Button>
                        </Paper>
                    </Grid>
                )}

                {agendaFiltrada.map(item => {
                    const parceiroPrincipal = item.tipo === 'ENTRADA'
                        ? item.solicitacaoEntrada?.fornecedor?.nome
                        : item.solicitacaoSaida?.cliente?.nome;

                    const docRef = item.tipo === 'ENTRADA'
                        ? (item.solicitacaoEntrada?.notaFiscal ? item.solicitacaoEntrada.notaFiscal : null)
                        : (item.solicitacaoSaida?.codigoExterno ? item.solicitacaoSaida.codigoExterno : null);

                    const corTipo = item.tipo === 'ENTRADA' ? 'success' : 'primary';
                    const statusInfo = STATUS_MAP[item.status] || { label: item.status, color: 'default' };

                    return (
                        <Grid item xs={12} key={item.id}>
                            <Card
                                elevation={0}
                                sx={{
                                    border: '1px solid #e2e8f0', borderRadius: 3,
                                    borderLeft: '6px solid', borderLeftColor: `${corTipo}.main`,
                                    transition: '0.2s',
                                    opacity: item.status === 'CANCELADO' ? 0.6 : 1,
                                    '&:hover': { boxShadow: '0 8px 24px rgba(0,0,0,0.08)', transform: 'translateY(-2px)' }
                                }}
                            >
                                <CardContent sx={{ p: 3, '&:last-child': { pb: 3 } }}>
                                    <Grid container alignItems="center" spacing={3}>

                                        {/* HORA E CÓDIGO */}
                                        <Grid item xs={12} sm={2} md={1.5} textAlign="center" sx={{ borderRight: { sm: '1px solid #f1f5f9' } }}>
                                            <Typography variant="h4" fontWeight="bold" color="text.primary" sx={{ letterSpacing: -1 }}>
                                                {dayjs(item.dataPrevistaInicio).format('HH:mm')}
                                            </Typography>

                                            <Tooltip title="Clique para copiar código">
                                                <Chip
                                                    label={item.codigoReserva}
                                                    size="small"
                                                    icon={<Copy size={12} />}
                                                    onClick={() => handleCopyCode(item.codigoReserva)}
                                                    sx={{ mt: 1, fontFamily: 'monospace', fontWeight: 'bold', bgcolor: '#f1f5f9', cursor: 'pointer' }}
                                                />
                                            </Tooltip>
                                        </Grid>

                                        {/* DADOS PRINCIPAIS */}
                                        <Grid item xs={12} sm={6} md={6.5}>
                                            <Box display="flex" alignItems="center" gap={1.5} mb={1} flexWrap="wrap">
                                                <Chip label={item.tipo} color={corTipo} size="small" sx={{ fontWeight: '800', px: 1 }} />
                                                <Typography variant="h6" fontWeight="bold" noWrap sx={{ maxWidth: '100%' }}>
                                                    {parceiroPrincipal || 'Parceiro não identificado'}
                                                </Typography>
                                                {docRef && <Chip label={docRef} variant="outlined" size="small" icon={<FileText size={16} />} />}

                                                {item.tipo === 'ENTRADA' && !item.xmlVinculado && item.status !== 'CANCELADO' && (
                                                    <Chip label="Aguardando XML" size="small" color="warning" variant="outlined" />
                                                )}
                                            </Box>

                                            <Box display="flex" gap={4} color="text.secondary" flexWrap="wrap">
                                                <Box display="flex" alignItems="center" gap={1}>
                                                    <Truck size={18} />
                                                    <Typography variant="body2">{item.transportadora?.nome || 'Transp. Própria'}</Typography>
                                                </Box>
                                                {item.placaVeiculo && (
                                                    <Box display="flex" alignItems="center" gap={1}>
                                                        <RectangleHorizontal size={18} />
                                                        <Box px={0.8} py={0} border="1px solid #cbd5e1" borderRadius={1} bgcolor="#fff">
                                                            <Typography variant="caption" fontWeight="bold" color="text.primary">{item.placaVeiculo}</Typography>
                                                        </Box>
                                                    </Box>
                                                )}
                                                <Box display="flex" alignItems="center" gap={1}>
                                                    <MapPin size={18} />
                                                    <Typography variant="body2">{item.doca ? item.doca.enderecoCompleto : 'Doca não definida'}</Typography>
                                                </Box>
                                            </Box>
                                        </Grid>

                                        {/* AÇÕES (Direita) */}
                                        <Grid item xs={12} sm={4} md={4} display="flex" justifyContent={{ xs: 'flex-start', sm: 'flex-end' }} alignItems="center" gap={1} flexWrap="wrap">

                                            {/* Ações só habilitadas se NÃO estiver cancelado ou finalizado */}
                                            {['AGENDADO', 'NA_PORTARIA'].includes(item.status) && (
                                                <>
                                                    {item.tipo === 'ENTRADA' && !item.xmlVinculado && (
                                                        <Tooltip title="Vincular XML">
                                                            <IconButton size="small" color="secondary" onClick={() => { setSelectedItem(item); setXmlModalOpen(true); }}>
                                                                <Upload size={18} />
                                                            </IconButton>
                                                        </Tooltip>
                                                    )}

                                                    <Tooltip title="Compartilhar QR">
                                                        <IconButton color="primary" onClick={() => { setSelectedItem(item); setQrModalOpen(true); }}>
                                                            <Share2 size={20} />
                                                        </IconButton>
                                                    </Tooltip>

                                                    <Tooltip title="Cancelar">
                                                        <IconButton size="small" color="error" onClick={() => handleAction('CANCELAR', item)}>
                                                            <XCircle size={20} />
                                                        </IconButton>
                                                    </Tooltip>

                                                    {/* Opção No Show apenas se já passou do horário e ainda não chegou */}
                                                    {dayjs().isAfter(dayjs(item.dataPrevistaInicio)) && item.status === 'AGENDADO' && (
                                                        <Tooltip title="Não Compareceu">
                                                            <IconButton size="small" color="warning" onClick={() => handleAction('NO_SHOW', item)}>
                                                                <UserX size={20} />
                                                            </IconButton>
                                                        </Tooltip>
                                                    )}
                                                </>
                                            )}

                                            {/* Botão de Excluir apenas para Cancelados */}
                                            {item.status === 'CANCELADO' && (
                                                <Tooltip title="Excluir Definitivamente">
                                                    <IconButton size="small" onClick={() => handleAction('EXCLUIR', item)}>
                                                        <Trash2 size={20} />
                                                    </IconButton>
                                                </Tooltip>
                                            )}

                                            <Tooltip title="Detalhes">
                                                <IconButton onClick={() => { setSelectedItem(item); setDetalhesOpen(true); }}>
                                                    <Eye size={20} />
                                                </IconButton>
                                            </Tooltip>

                                            <Chip
                                                label={statusInfo.label}
                                                color={statusInfo.color}
                                                size="small"
                                                sx={{ fontWeight: 'bold', ml: 1 }}
                                            />
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>
                    );
                })}
            </Grid>

            {/* MODAIS (Mantidos iguais ao anterior, apenas chamam os novos handlers) */}
            <AgendamentoForm open={modalOpen} onClose={() => setModalOpen(false)} onSuccess={() => { setModalOpen(false); loadAgenda(); }} dataInicial={{ data: filtros.data }} />

            <Dialog open={xmlModalOpen} onClose={() => setXmlModalOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Vincular Nota Fiscal</DialogTitle>
                <DialogContent>
                    <Box component="label" sx={{ mt: 1, p: 5, border: '2px dashed #cbd5e1', borderRadius: 3, bgcolor: '#f8fafc', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', transition: '0.2s', '&:hover': { borderColor: 'primary.main', bgcolor: '#f1f5f9' } }}>
                        <input type="file" hidden accept=".xml" onChange={e => setFile(e.target.files[0])} />
                        <Box p={2} bgcolor="white" borderRadius="50%" boxShadow={2} mb={2}><Upload size={32} color="#2563eb" /></Box>
                        <Typography variant="h6" color="text.primary">Clique para selecionar o XML</Typography>
                        <Typography variant="body2" color="text.secondary">O sistema preencherá os dados automaticamente.</Typography>
                        {file && <Chip label={file.name} color="primary" onDelete={(e) => { e.preventDefault(); setFile(null); }} sx={{ mt: 2 }} />}
                    </Box>
                </DialogContent>
                <DialogActions sx={{ p: 3 }}>
                    <Button onClick={() => setXmlModalOpen(false)} color="inherit">Cancelar</Button>
                    <Button variant="contained" onClick={handleUploadXml} disabled={!file} size="large">Confirmar Vínculo</Button>
                </DialogActions>
            </Dialog>

            <Dialog open={qrModalOpen} onClose={() => setQrModalOpen(false)} maxWidth="xs" fullWidth>
                <DialogTitle>Compartilhar Agendamento</DialogTitle>
                <DialogContent>
                    <Box id="qr-code-container" textAlign="center" py={2}>
                        <Typography variant="h6" fontWeight="bold" gutterBottom>{selectedItem?.transportadora?.nome}</Typography>
                        <Typography variant="body2" color="text.secondary" mb={3}>Apresente este código na portaria.</Typography>
                        <div style={{ background: 'white', padding: '16px', display: 'inline-block', border: '1px solid #eee', borderRadius: 8 }}>
                            {selectedItem && <QRCode value={selectedItem.codigoReserva} size={200} />}
                        </div>
                        <Typography variant="h5" fontWeight="bold" mt={2} letterSpacing={2}>{selectedItem?.codigoReserva}</Typography>
                        <Typography variant="caption" display="block">{dayjs(selectedItem?.dataPrevistaInicio).format('DD/MM/YYYY HH:mm')}</Typography>
                    </Box>
                </DialogContent>
                <DialogActions sx={{ justifyContent: 'center', pb: 3 }}>
                    <Button startIcon={<Printer />} variant="outlined" onClick={handlePrintQr}>Imprimir</Button>
                    <Button onClick={() => setQrModalOpen(false)}>Fechar</Button>
                </DialogActions>
            </Dialog>

            <Dialog open={detalhesOpen} onClose={() => setDetalhesOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Detalhes do Agendamento</DialogTitle>
                <DialogContent dividers>
                    {selectedItem && (
                        <Grid container spacing={2}>
                            <Grid item xs={6}><Typography variant="caption">Código</Typography><Typography variant="body1" fontWeight="bold">{selectedItem.codigoReserva}</Typography></Grid>
                            <Grid item xs={6}><Typography variant="caption">Status</Typography><br /><Chip label={STATUS_MAP[selectedItem.status]?.label} color={STATUS_MAP[selectedItem.status]?.color} size="small" /></Grid>
                            <Grid item xs={12}><Divider /></Grid>
                            <Grid item xs={6}><Typography variant="caption">Início Previsto</Typography><Typography>{dayjs(selectedItem.dataPrevistaInicio).format('DD/MM/YYYY HH:mm')}</Typography></Grid>
                            <Grid item xs={6}><Typography variant="caption">Fim Previsto</Typography><Typography>{dayjs(selectedItem.dataPrevistaFim).format('DD/MM/YYYY HH:mm')}</Typography></Grid>
                            <Grid item xs={12}><Typography variant="caption">Transportadora</Typography><Typography fontWeight="bold">{selectedItem.transportadora?.nome || '-'}</Typography></Grid>
                            <Grid item xs={6}><Typography variant="caption">Placa</Typography><Typography>{selectedItem.placaVeiculo || '-'}</Typography></Grid>
                            <Grid item xs={6}><Typography variant="caption">Motorista</Typography><Typography>{selectedItem.nomeMotoristaAvulso || selectedItem.motorista?.nome || '-'}</Typography></Grid>

                            {selectedItem.tipo === 'ENTRADA' && (
                                <Grid item xs={12}>
                                    <Box bgcolor="#f0fdf4" p={2} borderRadius={2} mt={1} border="1px solid #bbf7d0">
                                        <Typography variant="caption" color="success.main" fontWeight="bold" display="block" mb={1}>DADOS DA ENTRADA (NFe)</Typography>
                                        <Typography variant="body2"><b>Fornecedor:</b> {selectedItem.solicitacaoEntrada?.fornecedor?.nome || 'Aguardando XML'}</Typography>
                                        <Typography variant="body2"><b>Nota Fiscal:</b> {selectedItem.solicitacaoEntrada?.notaFiscal || '-'}</Typography>
                                        <Typography variant="body2"><b>Chave:</b> <span style={{ fontSize: '0.8rem', fontFamily: 'monospace' }}>{selectedItem.solicitacaoEntrada?.chaveAcesso || '-'}</span></Typography>
                                    </Box>
                                </Grid>
                            )}

                            {selectedItem.tipo === 'SAIDA' && (
                                <Grid item xs={12}>
                                    <Box bgcolor="#eff6ff" p={2} borderRadius={2} mt={1} border="1px solid #bfdbfe">
                                        <Typography variant="caption" color="primary.main" fontWeight="bold" display="block" mb={1}>DADOS DA SAÍDA</Typography>
                                        <Typography variant="body2"><b>Cliente:</b> {selectedItem.solicitacaoSaida?.cliente?.nome}</Typography>
                                        <Typography variant="body2"><b>Pedido:</b> {selectedItem.solicitacaoSaida?.codigoExterno}</Typography>
                                        <Typography variant="body2"><b>Rota:</b> {selectedItem.solicitacaoSaida?.rota}</Typography>
                                    </Box>
                                </Grid>
                            )}
                        </Grid>
                    )}
                </DialogContent>
                <DialogActions><Button onClick={() => setDetalhesOpen(false)}>Fechar</Button></DialogActions>
            </Dialog>

            <ConfirmDialog
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                onConfirm={confirmData.action}
                title={confirmData.title}
                message={confirmData.message}
                severity="warning"
            />
        </Box>
    );
};

export default AgendamentoList;