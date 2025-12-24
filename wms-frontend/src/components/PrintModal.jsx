import { useState, useEffect } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, MenuItem, TextField, CircularProgress, Alert
} from '@mui/material';
import { Printer } from 'lucide-react';
import { toast } from 'react-toastify';
import { getImpressoras, imprimirLpn } from '../services/impressaoService';

const PrintModal = ({ open, onClose, lpnId, lpnCodigo }) => {
    const [impressoras, setImpressoras] = useState([]);
    const [selectedPrinter, setSelectedPrinter] = useState('');
    const [loading, setLoading] = useState(false);
    const [sending, setSending] = useState(false);

    // Carrega impressoras ao abrir o modal
    useEffect(() => {
        if (open) {
            setLoading(true);
            getImpressoras()
                .then(data => {
                    setImpressoras(data);
                    // Seleciona a primeira automaticamente se houver
                    if (data.length > 0) setSelectedPrinter(data[0].id);
                })
                .catch(() => toast.error("Erro ao listar impressoras"))
                .finally(() => setLoading(false));
        }
    }, [open]);

    const handlePrint = async () => {
        if (!selectedPrinter) return;
        setSending(true);
        try {
            await imprimirLpn(lpnId, selectedPrinter);
            toast.success(`Enviado para impressão: ${lpnCodigo}`);
            onClose();
        } catch (error) {
            toast.error("Erro ao solicitar impressão.");
            console.error(error);
        } finally {
            setSending(false);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
            <DialogTitle display="flex" alignItems="center" gap={1}>
                <Printer size={20} /> Imprimir Etiqueta LPN
            </DialogTitle>

            <DialogContent>
                {loading ? (
                    <CircularProgress size={24} sx={{ mt: 2, mx: 'auto', display: 'block' }} />
                ) : impressoras.length === 0 ? (
                    <Alert severity="warning" sx={{ mt: 2 }}>
                        Nenhuma impressora ativa cadastrada.
                    </Alert>
                ) : (
                    <>
                        <p style={{ marginBottom: 15 }}>
                            LPN Selecionada: <strong>{lpnCodigo}</strong>
                        </p>
                        <TextField
                            select
                            label="Selecione a Impressora"
                            fullWidth
                            value={selectedPrinter}
                            onChange={(e) => setSelectedPrinter(e.target.value)}
                            disabled={sending}
                        >
                            {impressoras.map((imp) => (
                                <MenuItem key={imp.id} value={imp.id}>
                                    {imp.nome} ({imp.tipoConexao})
                                </MenuItem>
                            ))}
                        </TextField>
                    </>
                )}
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose} disabled={sending}>Cancelar</Button>
                <Button
                    onClick={handlePrint}
                    variant="contained"
                    color="primary"
                    disabled={sending || impressoras.length === 0 || !selectedPrinter}
                    startIcon={sending ? <CircularProgress size={16} color="inherit" /> : <Printer size={18} />}
                >
                    {sending ? "Enviando..." : "Imprimir"}
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default PrintModal;