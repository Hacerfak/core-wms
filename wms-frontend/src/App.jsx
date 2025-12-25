import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { ThemeProvider, CssBaseline } from '@mui/material';
import theme from './theme/theme';
import { AuthProvider, AuthContext } from './contexts/AuthContext';
import { useContext } from 'react';
import Can from './components/Can';
import { PERMISSIONS } from './constants/permissions';

// Imports das Páginas
import Login from './pages/Login/Login';
import Onboarding from './pages/Login/Onboarding';
import SelecaoEmpresa from './pages/Login/SelecaoEmpresa';
import Dashboard from './pages/Dashboard/Dashboard';

// CADASTROS
import ParceiroList from './pages/Cadastros/ParceiroList';
import ParceiroForm from './pages/Cadastros/ParceiroForm';
import ProdutoList from './pages/Cadastros/ProdutoList';
import ProdutoForm from './pages/Cadastros/ProdutoForm';
import MapeamentoView from './pages/Cadastros/Mapeamento/MapeamentoView';

// OPERAÇÃO
import RecebimentoList from './pages/Recebimento/RecebimentoList';
import Recebimento from './pages/Recebimento/Recebimento';
import Conferencia from './pages/Recebimento/Conferencia';

// ESTOQUE
import EstoqueList from './pages/Estoque/EstoqueList';
import Armazenagem from './pages/Estoque/Armazenagem';

// EXPEDIÇÃO
import ExpedicaoMenu from './pages/Expedicao/ExpedicaoMenu';
import Checkout from './pages/Expedicao/Checkout';

// CONFIGURAÇÃO E ADMIN
import UsuariosList from './pages/Usuarios/UsuariosList';
import UsuarioForm from './pages/Usuarios/UsuarioForm';
import PerfisList from './pages/Usuarios/PerfisList';
import MinhaEmpresa from './pages/Configuracao/MinhaEmpresa';
import GestaoEmpresas from './pages/Admin/GestaoEmpresas';
import PrintHubView from './pages/Configuracao/PrintHub/PrintHubView';

// RELATÓRIOS
import AuditoriaList from './pages/Relatorios/AuditoriaList';

import MainLayout from './layout/MainLayout';

const PrivateRoute = ({ children }) => {
  const { authenticated, loading } = useContext(AuthContext);
  if (loading) return <div>Carregando...</div>;
  if (!authenticated) return <Navigate to="/login" />;
  return children ? children : <Outlet />;
};

// CORREÇÃO: Removemos MainLayout daqui para evitar duplicação
const ProtectedRoute = ({ children, permission }) => (
  <Can permissions={permission} elseShow={<Navigate to="/dashboard" />}>
    {children}
  </Can>
);

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <AuthProvider>
          <ToastContainer position="top-right" autoClose={3000} />
          <Routes>
            {/* ROTAS PÚBLICAS */}
            <Route path="/login" element={<Login />} />

            {/* --- ROTAS PRIVADAS: SEM LAYOUT (Seleção, Onboarding) --- */}
            <Route element={<PrivateRoute />}>
              <Route path="/onboarding" element={<Onboarding />} />
              <Route path="/selecao-empresa" element={<SelecaoEmpresa />} />
            </Route>

            {/* --- ROTAS PRIVADAS: COM LAYOUT (Aplicação Principal) --- */}
            <Route element={<PrivateRoute><MainLayout /></PrivateRoute>}>

              {/* Dashboard */}
              <Route path="/dashboard" element={<Dashboard />} />

              {/* CADASTROS */}
              <Route path="/cadastros/produtos" element={<ProtectedRoute permission={PERMISSIONS.PRODUTO_VISUALIZAR}><ProdutoList /></ProtectedRoute>} />
              <Route path="/cadastros/produtos/novo" element={<ProtectedRoute permission={PERMISSIONS.PRODUTO_GERENCIAR}><ProdutoForm /></ProtectedRoute>} />
              <Route path="/cadastros/produtos/:id" element={<ProtectedRoute permission={PERMISSIONS.PRODUTO_GERENCIAR}><ProdutoForm /></ProtectedRoute>} />

              <Route path="/cadastros/parceiros" element={<ProtectedRoute permission={PERMISSIONS.PARCEIRO_VISUALIZAR}><ParceiroList /></ProtectedRoute>} />
              <Route path="/cadastros/parceiros/novo" element={<ProtectedRoute permission={PERMISSIONS.PARCEIRO_GERENCIAR}><ParceiroForm /></ProtectedRoute>} />
              <Route path="/cadastros/parceiros/:id" element={<ProtectedRoute permission={PERMISSIONS.PARCEIRO_GERENCIAR}><ParceiroForm /></ProtectedRoute>} />

              <Route path="/cadastros/locais" element={
                <Can permissions="LOCALIZACAO_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <MapeamentoView />
                </Can>
              } />

              {/* OPERAÇÃO */}
              <Route path="/recebimento" element={
                <Can permissions="RECEBIMENTO_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <RecebimentoList />
                </Can>
              } />
              <Route path="/recebimento/:id" element={
                <Can permissions="RECEBIMENTO_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <Recebimento />
                </Can>
              } />
              <Route path="/recebimento/:id/conferencia" element={
                <Can permissions="RECEBIMENTO_CONFERIR" elseShow={<Navigate to="/dashboard" />}>
                  <Conferencia />
                </Can>
              } />

              <Route path="/estoque" element={
                <Can permissions="ESTOQUE_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <EstoqueList />
                </Can>
              } />
              <Route path="/estoque/armazenagem" element={
                <Can permissions="ESTOQUE_ARMAZENAR" elseShow={<Navigate to="/dashboard" />}>
                  <Armazenagem />
                </Can>
              } />

              {/* EXPEDIÇÃO */}
              <Route path="/expedicao" element={
                <Can permissions="PEDIDO_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <ExpedicaoMenu />
                </Can>
              } />
              <Route path="/expedicao/checkout" element={
                <Can permissions="EXPEDICAO_DESPACHAR" elseShow={<Navigate to="/expedicao" />}>
                  <Checkout />
                </Can>
              } />

              {/* CONFIGURAÇÃO & ADMIN */}
              <Route path="/usuarios" element={
                <Can permissions="USUARIO_LISTAR" elseShow={<Navigate to="/dashboard" />}>
                  <UsuariosList />
                </Can>
              } />
              <Route path="/usuarios/novo" element={
                <Can permissions="USUARIO_CRIAR" elseShow={<Navigate to="/dashboard" />}>
                  <UsuarioForm />
                </Can>
              } />
              <Route path="/usuarios/:id" element={<UsuarioForm />} />

              <Route path="/perfis" element={
                <Can permissions="PERFIL_GERENCIAR" elseShow={<Navigate to="/dashboard" />}>
                  <PerfisList />
                </Can>
              } />

              <Route path="/config/empresa" element={
                <Can permissions="CONFIG_GERENCIAR" elseShow={<Navigate to="/dashboard" />}>
                  <MinhaEmpresa />
                </Can>
              } />

              <Route path="/admin/empresas" element={
                <Can permissions="ADMIN" elseShow={<Navigate to="/dashboard" />}>
                  <GestaoEmpresas />
                </Can>
              } />

              {/* --- NOVA ROTA: CENTRAL DE IMPRESSÃO --- */}
              <Route path="/config/impressao" element={
                <Can permissions={PERMISSIONS.CONFIG_SISTEMA} elseShow={<Navigate to="/dashboard" />}>
                  <PrintHubView />
                </Can>
              } />

              {/* RELATÓRIOS */}
              <Route path="/auditoria" element={
                <Can permissions="AUDITORIA_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <AuditoriaList />
                </Can>
              } />
            </Route>

            {/* Redirecionamento Padrão */}
            <Route path="/" element={<Navigate to="/dashboard" />} />
            <Route path="*" element={<Navigate to="/dashboard" />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
}

export default App;