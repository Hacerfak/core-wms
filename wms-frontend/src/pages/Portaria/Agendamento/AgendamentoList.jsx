import { useState, useEffect } from 'react';
import {
    Box, Button, Paper, Typography, Grid, Card, CardContent,
    TextField, Chip, Dialog, DialogTitle, DialogContent,
    DialogActions, MenuItem, IconButton, Tooltip, Divider, Avatar
} from '@mui/material';
import {
    Calendar, ArrowLeft, Plus, Upload, Truck,
    MapPin, Clock, Filter, Eye, FileText, Share2,
    Printer, Copy, XCircle, UserX, Trash2, RectangleHorizontal,
    CheckCircle, LogOut, LogIn, User, ClipboardList
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

// Mapa de Status com Cores e Ícones
const STATUS_MAP = {
    'AGENDADO': { label: 'AGENDADO', color: 'default', icon: <Calendar size={16} /> },
    'NA_PORTARIA': { label: 'NO PÁTIO', color: 'warning', icon: <Truck size={16} /> },
    'NA_DOCA': { label: 'NA DOCA', color: 'primary', icon: <MapPin size={16} /> },
    'AGUARDANDO_SAIDA': { label: 'SAÍDA PENDENTE', color: 'info', icon: <LogOut size={16} /> },
    'FINALIZADO': { label: 'FINALIZADO', color: 'success', icon: <CheckCircle size={16} /> },
    'CANCELADO': { label: 'CANCELADO', color: 'error', icon: <XCircle size={16} /> },
    'NO_SHOW': { label: 'NÃO COMPARECEU', color: 'error', icon: <UserX size={16} /> }
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

            <Paper sx={{ p: 3, mb: 3, borderRadius: 2 }}>
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
                        <Button startIcon={<Filter size={18} />} onClick={loadAgenda} fullWidth variant="outlined" sx={{ height: 40 }}>
                            Atualizar
                        </Button>
                    </Grid>
                </Grid>
            </Paper>

            <Grid container spacing={2}>
                {agendaFiltrada.length === 0 && (
                    <Grid item xs={12}>
                        <Paper sx={{ p: 6, textAlign: 'center', bgcolor: '#f8fafc', border: '1px dashed #e2e8f0', borderRadius: 3 }}>
                            <Calendar size={48} color="#cbd5e1" style={{ marginBottom: 16 }} />
                            <Typography color="text.secondary" variant="h6">Nenhum agendamento encontrado.</Typography>
                            <Button sx={{ mt: 2 }} onClick={() => setModalOpen(true)}>Criar Primeiro Agendamento</Button>
                        </Paper>
                    </Grid>
                )}

                {agendaFiltrada.map(item => {
                    const isEntry = item.tipo === 'ENTRADA';
                    const parceiroPrincipal = isEntry
                        ? item.solicitacaoEntrada?.fornecedor?.nome
                        : item.solicitacaoSaida?.cliente?.nome;

                    const corTipo = isEntry ? 'success' : 'primary';
                    const statusInfo = STATUS_MAP[item.status] || { label: item.status, color: 'default', icon: null };

                    // Dados para Chip de Documento (NF ou Pedido)
                    const docNum = isEntry
                        ? item.solicitacaoEntrada?.notaFiscal
                        : item.solicitacaoSaida?.codigoExterno;

                    const DocIcon = isEntry ? FileText : ClipboardList;
                    const docLabel = isEntry ? "NF" : "Ped";

                    return (
                        <Grid item xs={12} key={item.id}>
                            <Card
                                elevation={0}
                                sx={{
                                    border: '1px solid #e2e8f0', borderRadius: 3,
                                    borderLeft: `6px solid`,
                                    // A cor da borda segue o STATUS, não o tipo
                                    borderLeftColor: `${statusInfo.color}.main`,
                                    transition: '0.2s',
                                    opacity: item.status === 'CANCELADO' ? 0.6 : 1,
                                    '&:hover': { boxShadow: '0 8px 24px rgba(0,0,0,0.08)', transform: 'translateY(-2px)' }
                                }}
                            >
                                <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                                    <Grid container alignItems="center" spacing={2}>

                                        {/* COLUNA 1: HORA e CÓDIGO 
                                            UX Update: Espaçamento na borda direita para não ficar colado
                                        */}
                                        <Grid item xs={12} sm={2} md={1.5}
                                            textAlign="center"
                                            sx={{
                                                borderRight: { sm: '1px solid #e2e8f0' },
                                                pr: { sm: 3 }, // Espaçamento interno à direita
                                                display: 'flex',
                                                flexDirection: 'column',
                                                alignItems: 'center',
                                                justifyContent: 'center'
                                            }}
                                        >
                                            <Typography variant="h4" fontWeight="bold" color="text.primary" sx={{ letterSpacing: -1 }}>
                                                {dayjs(item.dataPrevistaInicio).format('HH:mm')}
                                            </Typography>
                                            <Tooltip title="Clique para copiar código">
                                                <Chip
                                                    label={item.codigoReserva}
                                                    size="small"
                                                    icon={<Copy size={12} />}
                                                    onClick={() => handleCopyCode(item.codigoReserva)}
                                                    sx={{ mt: 1, fontFamily: 'monospace', fontWeight: 'bold', bgcolor: '#f1f5f9', cursor: 'pointer', maxWidth: '100%' }}
                                                />
                                            </Tooltip>
                                        </Grid>

                                        {/* COLUNA 2: DADOS PRINCIPAIS (Parceiro e Tipo) */}
                                        <Grid item xs={12} sm={4} md={4}>
                                            <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                                                <Chip
                                                    label={item.tipo}
                                                    color={corTipo}
                                                    size="small"
                                                    icon={isEntry ? <LogIn size={12} /> : <LogOut size={12} />}
                                                    sx={{ fontWeight: '800', px: 0.5, height: 20, fontSize: '0.65rem' }}
                                                />

                                                {/* UX Update: Ícone para NF/Pedido */}
                                                {docNum && (
                                                    <Chip
                                                        icon={<DocIcon size={12} />}
                                                        label={`${docLabel}: ${docNum}`}
                                                        variant="outlined"
                                                        size="small"
                                                        sx={{ height: 20, fontSize: '0.65rem', fontWeight: '500' }}
                                                    />
                                                )}

                                                {/* Aviso de falta de XML */}
                                                {isEntry && !item.xmlVinculado && item.status !== 'CANCELADO' && (
                                                    <Tooltip title="Necessário vincular XML">
                                                        <Chip label="XML Pendente" size="small" color="warning" variant="outlined" sx={{ height: 20, fontSize: '0.65rem' }} />
                                                    </Tooltip>
                                                )}
                                            </Box>

                                            <Typography variant="h6" fontWeight="bold" noWrap title={parceiroPrincipal}>
                                                {parceiroPrincipal || 'Parceiro não identificado'}
                                            </Typography>

                                            <Box display="flex" alignItems="center" gap={1} color="text.secondary" mt={0.5}>
                                                <User size={14} />
                                                <Typography variant="caption">
                                                    {item.nomeMotoristaAvulso || item.motorista?.nome || 'Motorista não inf.'}
                                                </Typography>
                                            </Box>
                                        </Grid>

                                        {/* COLUNA 3: LOGÍSTICA (Transp, Placa, Doca) */}
                                        <Grid item xs={12} sm={3} md={3.5}>
                                            <Box display="flex" flexDirection="column" gap={0.5}>
                                                <Box display="flex" alignItems="center" gap={1}>
                                                    <Truck size={16} color="#64748b" />
                                                    <Typography variant="body2" fontWeight="500">
                                                        {item.transportadora?.nome || 'Transp. Própria'}
                                                    </Typography>
                                                </Box>

                                                <Box display="flex" gap={2}>
                                                    {item.placaVeiculo && (
                                                        <Box display="flex" alignItems="center" gap={0.5}>
                                                            <RectangleHorizontal size={16} color="#64748b" />
                                                            <Typography variant="body2" fontWeight="bold">{item.placaVeiculo}</Typography>
                                                        </Box>
                                                    )}

                                                    <Box display="flex" alignItems="center" gap={0.5}>
                                                        <MapPin size={16} color={item.doca ? "#2563eb" : "#94a3b8"} />
                                                        <Typography variant="body2" color={item.doca ? "primary" : "text.secondary"} fontWeight={item.doca ? "bold" : "normal"}>
                                                            {/* UX Update: Endereço completo da doca */}
                                                            {item.doca ? item.doca.enderecoCompleto : 'Sem Doca'}
                                                        </Typography>
                                                    </Box>
                                                </Box>
                                            </Box>
                                        </Grid>

                                        {/* COLUNA 4: AÇÕES E STATUS */}
                                        <Grid item xs={12} sm={3} md={3} display="flex" flexDirection="column" alignItems={{ xs: 'flex-start', sm: 'flex-end' }} gap={1}>
                                            <Chip
                                                icon={statusInfo.icon}
                                                label={statusInfo.label}
                                                color={statusInfo.color}
                                                size="small"
                                                sx={{ fontWeight: 'bold' }}
                                            />

                                            <Box display="flex" gap={0.5} mt={0.5}>
                                                {/* Ações para Agendado/No Pátio */}
                                                {['AGENDADO', 'NA_PORTARIA'].includes(item.status) && (
                                                    <>
                                                        {isEntry && !item.xmlVinculado && (
                                                            <Tooltip title="Vincular XML">
                                                                <IconButton size="small" sx={{ color: '#ec4899', border: '1px solid #fce7f3' }} onClick={() => { setSelectedItem(item); setXmlModalOpen(true); }}>
                                                                    <Upload size={16} />
                                                                </IconButton>
                                                            </Tooltip>
                                                        )}
                                                        <Tooltip title="QR Code">
                                                            <IconButton size="small" color="primary" sx={{ border: '1px solid #e0f2fe' }} onClick={() => { setSelectedItem(item); setQrModalOpen(true); }}>
                                                                <Share2 size={16} />
                                                            </IconButton>
                                                        </Tooltip>

                                                        {/* Apenas se ainda não chegou */}
                                                        {item.status === 'AGENDADO' && (
                                                            <Tooltip title="Cancelar/No Show">
                                                                <IconButton size="small" color="error" sx={{ border: '1px solid #fee2e2' }} onClick={() => handleAction('CANCELAR', item)}>
                                                                    <XCircle size={16} />
                                                                </IconButton>
                                                            </Tooltip>
                                                        )}
                                                    </>
                                                )}

                                                {/* Detalhes sempre visível */}
                                                <Tooltip title="Ver Detalhes">
                                                    <IconButton size="small" onClick={() => { setSelectedItem(item); setDetalhesOpen(true); }} sx={{ border: '1px solid #e2e8f0' }}>
                                                        <Eye size={16} />
                                                    </IconButton>
                                                </Tooltip>

                                                {/* Excluir se Cancelado */}
                                                {item.status === 'CANCELADO' && (
                                                    <Tooltip title="Excluir">
                                                        <IconButton size="small" color="error" onClick={() => handleAction('EXCLUIR', item)}>
                                                            <Trash2 size={16} />
                                                        </IconButton>
                                                    </Tooltip>
                                                )}
                                            </Box>
                                        </Grid>

                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>
                    );
                })}
            </Grid>

            {/* MODAIS (Lógica Original Preservada) */}

            <AgendamentoForm
                open={modalOpen}
                onClose={() => setModalOpen(false)}
                onSuccess={() => { setModalOpen(false); loadAgenda(); }}
                dataInicial={{ data: filtros.data }}
            />

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
                            <Grid item xs={6}><Typography variant="caption">Status</Typography><br />
                                <Chip icon={STATUS_MAP[selectedItem.status]?.icon} label={STATUS_MAP[selectedItem.status]?.label} color={STATUS_MAP[selectedItem.status]?.color} size="small" />
                            </Grid>
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