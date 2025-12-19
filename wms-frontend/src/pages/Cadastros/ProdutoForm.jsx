import { useState, useEffect } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button,
    TextField, Box, Grid, Tabs, Tab, FormControlLabel, Switch,
    MenuItem, Typography, Divider, Paper, Checkbox
} from '@mui/material';
import { Package, Truck, Scale, Barcode } from 'lucide-react';
import { toast } from 'react-toastify';
import { salvarProduto } from '../../services/produtoService';

const ProdutoForm = ({ open, onClose, produto, onSuccess }) => {
    const [tabIndex, setTabIndex] = useState(0);
    const [loading, setLoading] = useState(false);

    const [form, setForm] = useState(getInitialState());

    function getInitialState() {
        return {
            sku: '', nome: '', ean13: '', dun14: '', unidadeMedida: 'UN',
            pesoBrutoKg: '', ncm: '', cest: '', valorUnitarioPadrao: '',
            ativo: true,
            controlaLote: false, controlaValidade: false, controlaSerie: false,
            unidadeArmazenagem: '', fatorConversao: 1
        };
    }

    useEffect(() => {
        if (open) {
            setTabIndex(0);
            if (produto) {
                setForm({
                    ...produto,
                    // Garante valores para campos opcionais
                    dun14: produto.dun14 || '',
                    pesoBrutoKg: produto.pesoBrutoKg || '',
                    ncm: produto.ncm || '',
                    cest: produto.cest || '',
                    unidadeArmazenagem: produto.unidadeArmazenagem || '',
                    fatorConversao: produto.fatorConversao || 1
                });
            } else {
                setForm(getInitialState());
            }
        }
    }, [open, produto]);

    const handleChange = (field, value) => {
        setForm(prev => ({ ...prev, [field]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            await salvarProduto(form);
            toast.success("Produto salvo!");
            onSuccess();
        } catch (error) {
            console.error(error);
            toast.error(error.response?.data?.message || "Erro ao salvar.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>{produto ? "Editar Produto" : "Novo Produto"}</DialogTitle>
            <form onSubmit={handleSubmit}>
                <DialogContent dividers sx={{ p: 0 }}>
                    <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: '#f8fafc' }}>
                        <Tabs value={tabIndex} onChange={(e, v) => setTabIndex(v)} sx={{ px: 2 }}>
                            <Tab icon={<Package size={18} />} iconPosition="start" label="Dados Gerais" />
                            <Tab icon={<Scale size={18} />} iconPosition="start" label="Logística e Fiscal" />
                            <Tab icon={<Barcode size={18} />} iconPosition="start" label="Controles" />
                        </Tabs>
                    </Box>

                    <Box sx={{ p: 3 }}>
                        {/* GERAIS */}
                        {tabIndex === 0 && (
                            <Grid container spacing={2}>
                                <Grid item xs={12} display="flex" justifyContent="flex-end">
                                    <FormControlLabel control={<Switch checked={form.ativo} onChange={e => handleChange('ativo', e.target.checked)} color="success" />} label={form.ativo ? "Ativo" : "Inativo"} />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="SKU (Código)" fullWidth required value={form.sku} onChange={e => handleChange('sku', e.target.value)} disabled={!!produto} />
                                </Grid>
                                <Grid item xs={12} sm={8}>
                                    <TextField label="Descrição / Nome" fullWidth required value={form.nome} onChange={e => handleChange('nome', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="EAN-13 (Código de Barras)" fullWidth value={form.ean13} onChange={e => handleChange('ean13', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="DUN-14 (Caixa)" fullWidth value={form.dun14} onChange={e => handleChange('dun14', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField select label="Unidade Medida" fullWidth required value={form.unidadeMedida} onChange={e => handleChange('unidadeMedida', e.target.value)}>
                                        <MenuItem value="UN">Unidade (UN)</MenuItem>
                                        <MenuItem value="CX">Caixa (CX)</MenuItem>
                                        <MenuItem value="KG">Quilo (KG)</MenuItem>
                                        <MenuItem value="PC">Peça (PC)</MenuItem>
                                    </TextField>
                                </Grid>
                            </Grid>
                        )}

                        {/* LOGÍSTICA & FISCAL */}
                        {tabIndex === 1 && (
                            <Grid container spacing={2}>
                                <Grid item xs={12}><Typography variant="subtitle2" color="primary">Fiscal</Typography></Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="NCM" fullWidth value={form.ncm} onChange={e => handleChange('ncm', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="CEST" fullWidth value={form.cest} onChange={e => handleChange('cest', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="Valor Unitário (R$)" type="number" fullWidth value={form.valorUnitarioPadrao} onChange={e => handleChange('valorUnitarioPadrao', e.target.value)} />
                                </Grid>

                                <Grid item xs={12} mt={1}><Divider /><Typography variant="subtitle2" color="primary" mt={2}>Logística</Typography></Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="Peso Bruto (kg)" type="number" fullWidth value={form.pesoBrutoKg} onChange={e => handleChange('pesoBrutoKg', e.target.value)} />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="Fator Conversão (Qtd/Cx)" type="number" fullWidth value={form.fatorConversao} onChange={e => handleChange('fatorConversao', e.target.value)} helperText="Ex: 12 unidades por caixa" />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <TextField label="Unidade Armazenagem" fullWidth value={form.unidadeArmazenagem} onChange={e => handleChange('unidadeArmazenagem', e.target.value)} placeholder="Ex: CX" />
                                </Grid>
                            </Grid>
                        )}

                        {/* CONTROLES */}
                        {tabIndex === 2 && (
                            <Box>
                                <Typography variant="subtitle2" color="text.secondary" mb={2}>
                                    Defina quais rastreabilidades o sistema deve exigir na movimentação deste produto.
                                </Typography>
                                <Paper variant="outlined" sx={{ p: 2 }}>
                                    <FormControlLabel control={<Checkbox checked={form.controlaLote} onChange={e => handleChange('controlaLote', e.target.checked)} />} label="Controlar Lote (Batch)" />
                                    <Divider sx={{ my: 1 }} />
                                    <FormControlLabel control={<Checkbox checked={form.controlaValidade} onChange={e => handleChange('controlaValidade', e.target.checked)} />} label="Controlar Data de Validade" />
                                    <Divider sx={{ my: 1 }} />
                                    <FormControlLabel control={<Checkbox checked={form.controlaSerie} onChange={e => handleChange('controlaSerie', e.target.checked)} />} label="Controlar Número de Série (Serial único)" />
                                </Paper>
                            </Box>
                        )}
                    </Box>
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={onClose}>Cancelar</Button>
                    <Button type="submit" variant="contained" disabled={loading}>Salvar Produto</Button>
                </DialogActions>
            </form>
        </Dialog>
    );
};

export default ProdutoForm;