import { useState, useEffect } from 'react';
import {
    Box, Typography, Button, Paper, Table, TableBody, TableCell,
    TableContainer, TableHead, TableRow, Chip, IconButton, Tooltip, LinearProgress, TablePagination
} from '@mui/material';
import { Plus, Trash2, Edit, Package, Barcode } from 'lucide-react';
import { toast } from 'react-toastify';
import { getProdutos, excluirProduto } from '../../services/produtoService';
import ProdutoForm from './ProdutoForm';
import ConfirmDialog from '../../components/ConfirmDialog';
import Can from '../../components/Can';

const ProdutoList = () => {
    const [produtos, setProdutos] = useState([]);
    const [totalElements, setTotalElements] = useState(0);
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [loading, setLoading] = useState(true);

    const [modalOpen, setModalOpen] = useState(false);
    const [produtoEditando, setProdutoEditando] = useState(null);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmAction, setConfirmAction] = useState(null);

    useEffect(() => { loadData(); }, [page, rowsPerPage]);

    const loadData = async () => {
        try {
            setLoading(true);
            const data = await getProdutos(page, rowsPerPage);
            setProdutos(data.content);
            setTotalElements(data.totalElements);
        } catch (error) {
            toast.error("Erro ao carregar produtos");
        } finally {
            setLoading(false);
        }
    };

    const handleChangePage = (event, newPage) => setPage(newPage);
    const handleChangeRowsPerPage = (event) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    const handleNew = () => { setProdutoEditando(null); setModalOpen(true); };
    const handleEdit = (prod) => { setProdutoEditando(prod); setModalOpen(true); };

    const handleDeleteClick = (id) => {
        setConfirmAction(() => async () => {
            try {
                await excluirProduto(id);
                toast.success("Produto excluído!");
                loadData();
            } catch (error) {
                toast.error("Erro ao excluir. Verifique se há estoque.");
            }
        });
        setConfirmOpen(true);
    };

    return (
        <Box>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
                <Typography variant="h5" fontWeight="bold">Catálogo de Produtos</Typography>
                <Can I="PRODUTO_CRIAR">
                    <Button variant="contained" startIcon={<Plus size={20} />} onClick={handleNew}>Novo Produto</Button>
                </Can>
            </Box>

            <Paper sx={{ width: '100%', overflow: 'hidden', borderRadius: 2 }}>
                {loading && <LinearProgress />}
                <TableContainer>
                    <Table size="small">
                        <TableHead sx={{ bgcolor: 'background.subtle' }}>
                            <TableRow>
                                <TableCell><b>SKU / Nome</b></TableCell>
                                <TableCell><b>Un.</b></TableCell>
                                <TableCell><b>EAN</b></TableCell>
                                <TableCell><b>Controles</b></TableCell>
                                <TableCell><b>Status</b></TableCell>
                                <TableCell align="center"><b>Ações</b></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {produtos.map((p) => (
                                <TableRow key={p.id} hover>
                                    <TableCell>
                                        <Box display="flex" alignItems="center" gap={1.5}>
                                            <Box p={1} bgcolor="primary.light" borderRadius={1} color="white"><Package size={18} /></Box>
                                            <Box>
                                                <Typography fontWeight={600} variant="body2">{p.sku}</Typography>
                                                <Typography variant="caption" color="text.secondary">{p.nome}</Typography>
                                            </Box>
                                        </Box>
                                    </TableCell>
                                    <TableCell>{p.unidadeMedida}</TableCell>
                                    <TableCell>
                                        {p.ean13 && <Chip icon={<Barcode size={14} />} label={p.ean13} size="small" variant="outlined" />}
                                    </TableCell>
                                    <TableCell>
                                        <Box display="flex" gap={0.5}>
                                            {p.controlaLote && <Chip label="Lote" size="small" color="info" sx={{ fontSize: '0.7rem', height: 20 }} />}
                                            {p.controlaValidade && <Chip label="Val" size="small" color="warning" sx={{ fontSize: '0.7rem', height: 20 }} />}
                                            {p.controlaSerie && <Chip label="Série" size="small" color="secondary" sx={{ fontSize: '0.7rem', height: 20 }} />}
                                        </Box>
                                    </TableCell>
                                    <TableCell>
                                        <Chip label={p.ativo ? "Ativo" : "Inativo"} color={p.ativo ? "success" : "default"} size="small" />
                                    </TableCell>
                                    <TableCell align="center">
                                        <Box display="flex" justifyContent="center" gap={1}>
                                            <Can I="PRODUTO_EDITAR">
                                                <IconButton size="small" color="primary" onClick={() => handleEdit(p)}><Edit size={18} /></IconButton>
                                            </Can>
                                            <Can I="PRODUTO_EXCLUIR">
                                                <IconButton size="small" color="error" onClick={() => handleDeleteClick(p.id)}><Trash2 size={18} /></IconButton>
                                            </Can>
                                        </Box>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
                <TablePagination
                    component="div"
                    count={totalElements}
                    page={page}
                    onPageChange={handleChangePage}
                    rowsPerPage={rowsPerPage}
                    onRowsPerPageChange={handleChangeRowsPerPage}
                    labelRowsPerPage="Linhas:"
                />
            </Paper>

            <ProdutoForm open={modalOpen} onClose={() => setModalOpen(false)} produto={produtoEditando} onSuccess={() => { setModalOpen(false); loadData(); }} />
            <ConfirmDialog open={confirmOpen} onClose={() => setConfirmOpen(false)} onConfirm={confirmAction} title="Excluir Produto" message="Deseja realmente excluir?" />
        </Box>
    );
};

export default ProdutoList;