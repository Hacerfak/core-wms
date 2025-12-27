import { useState, useRef, useEffect } from 'react';
import {
    Box, Typography, TextField, Button, Grid, Paper,
    InputAdornment, IconButton, Chip, Fade
} from '@mui/material';
import {
    Barcode, Layers, Calendar, Hash, Tag,
    Box as BoxIcon, X, CheckCircle, AlertTriangle
} from 'lucide-react';
import { toast } from 'react-toastify';
import { conferirProduto } from '../../../services/recebimentoService';

const BipagemPanel = ({ recebimentoId, dadosRecebimento, onSucesso }) => {
    // 1. Refs para controle manual de foco (Crucial para Coletor)
    const skuRef = useRef(null);
    const loteRef = useRef(null);
    const validadeRef = useRef(null);
    const serialRef = useRef(null);
    const qtdRef = useRef(null);
    const volumesRef = useRef(null);

    // 2. Estados
    const [form, setForm] = useState({
        sku: '', qtd: '', volumes: 1,
        lote: '', validade: '', serial: ''
    });

    const [produtoAtivo, setProdutoAtivo] = useState(null);
    const [sending, setSending] = useState(false);

    // --- LÓGICA DE BUSCA DO PRODUTO ---
    // Executa sempre que o SKU muda para dar feedback visual imediato
    useEffect(() => {
        if (!form.sku) {
            setProdutoAtivo(null);
            return;
        }

        const termo = form.sku.toUpperCase();
        const item = dadosRecebimento?.itens?.find(i =>
            i.produto.sku === termo ||
            i.produto.ean13 === termo ||
            i.produto.dun14 === termo
        );

        if (item) {
            setProdutoAtivo(item.produto);
            // Se a quantidade estiver vazia, sugere o fator de conversão (padrão 1)
            if (!form.qtd) {
                setForm(prev => ({ ...prev, qtd: item.produto.fatorConversao || 1 }));
            }
        } else {
            setProdutoAtivo(null);
        }
    }, [form.sku, dadosRecebimento]);

    // --- GERENCIAMENTO DE FOCO (A MÁGICA DA UX) ---

    // Enter no SKU: Decide para onde ir baseado nas regras do produto
    const handleSkuEnter = (e) => {
        if (e.key !== 'Enter') return;
        e.preventDefault();

        if (!produtoAtivo) {
            // Toca um som de erro ou toast se quiser
            toast.warning("Produto não encontrado na nota.");
            skuRef.current?.select(); // Seleciona para bater de novo
            return;
        }

        // Cadeia de decisão de foco
        if (produtoAtivo.controlaLote) {
            loteRef.current?.focus();
        } else if (produtoAtivo.controlaValidade) {
            validadeRef.current?.focus();
        } else if (produtoAtivo.controlaSerie) {
            serialRef.current?.focus();
        } else {
            qtdRef.current?.focus();
            qtdRef.current?.select(); // Seleciona valor para permitir digitação rápida
        }
    };

    // Enter nos campos de Rastreabilidade
    const handleTraceabilityEnter = (e, nextFieldRef) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            if (nextFieldRef?.current) {
                nextFieldRef.current.focus();
                // Se for campo de texto, seleciona o conteúdo
                if (nextFieldRef.current.select) nextFieldRef.current.select();
            } else {
                // Se não tem próximo campo específico (ex: acabou a rastreabilidade), vai pra Qtd
                qtdRef.current?.focus();
                qtdRef.current?.select();
            }
        }
    };

    // Enter na Quantidade
    const handleQtdEnter = (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            volumesRef.current?.focus();
            volumesRef.current?.select();
        }
    };

    // Enter no Volume (Finaliza o Ciclo)
    const handleVolumesEnter = async (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            await handleSubmit();
        }
    };

    // --- SUBMISSÃO ---
    const handleSubmit = async (e) => {
        if (e) e.preventDefault(); // Caso seja chamado pelo form

        if (!form.sku || !form.qtd || !produtoAtivo) return;

        // Validações rápidas
        if (produtoAtivo.controlaLote && !form.lote) return toast.warning("Lote obrigatório!");
        if (produtoAtivo.controlaValidade && !form.validade) return toast.warning("Validade obrigatória!");
        if (produtoAtivo.controlaSerie && !form.serial) return toast.warning("Serial obrigatório!");

        setSending(true);
        try {
            await conferirProduto(recebimentoId, form.sku, form.qtd, {
                volumes: form.volumes,
                lote: form.lote,
                validade: form.validade,
                serial: form.serial
            });

            toast.success(`Entrada Confirmada: ${form.volumes} vol. de ${produtoAtivo.sku}`);

            // RESET INTELIGENTE PARA LOOP RÁPIDO
            setForm({
                sku: '', // Limpa SKU para bipar o próximo
                qtd: '',
                volumes: 1,
                lote: '', validade: '', serial: ''
            });
            setProdutoAtivo(null);

            if (onSucesso) onSucesso();

            // Foco imediato no SKU para o próximo item
            setTimeout(() => skuRef.current?.focus(), 50);

        } catch (error) {
            toast.error(error.response?.data?.message || "Erro ao salvar.");
            // Em caso de erro, mantém os dados para o operador corrigir, mas foca no campo problemático ou SKU
            skuRef.current?.select();
        } finally {
            setSending(false);
        }
    };

    const handleClear = () => {
        setForm({ sku: '', qtd: '', volumes: 1, lote: '', validade: '', serial: '' });
        setProdutoAtivo(null);
        skuRef.current?.focus();
    };

    // Estilo comum para inputs grandes
    const bigInputProps = { style: { fontSize: '1.2rem', padding: '10px' } };

    return (
        <Paper elevation={0} sx={{ p: 3, borderRadius: 2, border: '1px solid', borderColor: produtoAtivo ? 'primary.main' : '#e2e8f0', bgcolor: produtoAtivo ? '#f0f9ff' : 'white' }}>

            {/* 1. CAMPO DE BUSCA (SKU) - Sempre o início */}
            <Grid container spacing={2}>
                <Grid item xs={12}>
                    <Typography variant="caption" fontWeight="bold" color="text.secondary">1. IDENTIFICAÇÃO (BIPE O PRODUTO)</Typography>
                    <TextField
                        inputRef={skuRef}
                        fullWidth
                        value={form.sku}
                        onChange={(e) => setForm({ ...form, sku: e.target.value.toUpperCase() })}
                        onKeyDown={handleSkuEnter}
                        placeholder="EAN, SKU ou DUN..."
                        autoComplete="off"
                        autoFocus
                        InputProps={{
                            startAdornment: <InputAdornment position="start"><Barcode size={24} /></InputAdornment>,
                            endAdornment: form.sku && <IconButton onClick={handleClear} size="small"><X size={16} /></IconButton>,
                            style: { fontSize: '1.4rem', fontWeight: 'bold' } // Texto Grande
                        }}
                    />
                </Grid>

                {/* Feedback Visual do Produto Encontrado */}
                {produtoAtivo && (
                    <Grid item xs={12}>
                        <Fade in={true}>
                            <Box sx={{ p: 1.5, bgcolor: 'white', borderRadius: 2, border: '1px dashed #bfdbfe', display: 'flex', alignItems: 'center', gap: 2 }}>
                                <Box sx={{ p: 1, bgcolor: 'primary.light', color: 'white', borderRadius: 1 }}>
                                    <CheckCircle size={24} />
                                </Box>
                                <Box>
                                    <Typography variant="h6" fontWeight="bold" color="primary.main" lineHeight={1.1}>
                                        {produtoAtivo.nome}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        UN: <b>{produtoAtivo.unidadeMedida}</b> | SKU: {produtoAtivo.sku}
                                    </Typography>
                                </Box>
                            </Box>
                        </Fade>
                    </Grid>
                )}

                {/* 2. CAMPOS CONDICIONAIS DE RASTREABILIDADE */}
                {produtoAtivo && (
                    <>
                        {produtoAtivo.controlaLote && (
                            <Grid item xs={12} sm={6}>
                                <TextField
                                    label="Lote Indústria"
                                    inputRef={loteRef}
                                    fullWidth
                                    value={form.lote}
                                    onChange={e => setForm({ ...form, lote: e.target.value.toUpperCase() })}
                                    onKeyDown={(e) => {
                                        // Se tiver validade, vai pra lá, senão vai pra Qtd
                                        const next = produtoAtivo.controlaValidade ? validadeRef : qtdRef;
                                        handleTraceabilityEnter(e, next);
                                    }}
                                    InputProps={{ startAdornment: <InputAdornment position="start"><Tag size={18} /></InputAdornment> }}
                                />
                            </Grid>
                        )}

                        {produtoAtivo.controlaValidade && (
                            <Grid item xs={12} sm={6}>
                                <TextField
                                    label="Validade"
                                    type="date"
                                    inputRef={validadeRef}
                                    fullWidth
                                    InputLabelProps={{ shrink: true }}
                                    value={form.validade}
                                    onChange={e => setForm({ ...form, validade: e.target.value })}
                                    onKeyDown={(e) => handleTraceabilityEnter(e, qtdRef)} // Geralmente validade é o último antes da Qtd
                                    InputProps={{ startAdornment: <InputAdornment position="start"><Calendar size={18} /></InputAdornment> }}
                                />
                            </Grid>
                        )}

                        {produtoAtivo.controlaSerie && (
                            <Grid item xs={12}>
                                <TextField
                                    label="Serial Number (S/N)"
                                    inputRef={serialRef}
                                    fullWidth
                                    value={form.serial}
                                    onChange={e => setForm({ ...form, serial: e.target.value.toUpperCase() })}
                                    onKeyDown={(e) => handleTraceabilityEnter(e, qtdRef)}
                                    InputProps={{ startAdornment: <InputAdornment position="start"><Hash size={18} /></InputAdornment> }}
                                />
                            </Grid>
                        )}

                        {/* 3. QUANTIDADE E VOLUMES (Lado a Lado) */}
                        <Grid item xs={6}>
                            <Typography variant="caption" fontWeight="bold" color="text.secondary">QTD. UNITÁRIA</Typography>
                            <TextField
                                inputRef={qtdRef}
                                fullWidth
                                type="number"
                                placeholder="0"
                                value={form.qtd}
                                onChange={(e) => setForm({ ...form, qtd: e.target.value })}
                                onKeyDown={handleQtdEnter}
                                disabled={sending}
                                inputProps={bigInputProps}
                                InputProps={{
                                    startAdornment: <InputAdornment position="start"><BoxIcon size={20} /></InputAdornment>
                                }}
                            />
                        </Grid>

                        <Grid item xs={6}>
                            <Typography variant="caption" fontWeight="bold" color="text.secondary">Nº VOLUMES</Typography>
                            <TextField
                                inputRef={volumesRef}
                                fullWidth
                                type="number"
                                placeholder="1"
                                value={form.volumes}
                                onChange={(e) => setForm({ ...form, volumes: e.target.value })}
                                onKeyDown={handleVolumesEnter} // <--- AQUI OCORRE O SUBMIT
                                disabled={sending}
                                inputProps={bigInputProps}
                                InputProps={{
                                    startAdornment: <InputAdornment position="start"><Layers size={20} /></InputAdornment>
                                }}
                            />
                        </Grid>

                        {/* Botão de Feedback Visual apenas (O Enter já submete) */}
                        <Grid item xs={12}>
                            <Button
                                variant="contained"
                                fullWidth
                                onClick={handleSubmit}
                                disabled={sending}
                                sx={{ height: 50, fontSize: '1rem', bgcolor: 'primary.main' }}
                            >
                                {sending ? "PROCESSANDO..." : "CONFIRMAR (ENTER)"}
                            </Button>
                        </Grid>
                    </>
                )}

                {/* Estado Vazio (Instrução) */}
                {!produtoAtivo && !form.sku && (
                    <Grid item xs={12}>
                        <Box display="flex" alignItems="center" justifyContent="center" gap={1} color="text.disabled" mt={1}>
                            <AlertTriangle size={16} />
                            <Typography variant="caption">Aguardando leitura do código de barras...</Typography>
                        </Box>
                    </Grid>
                )}
            </Grid>
        </Paper>
    );
};

export default BipagemPanel;