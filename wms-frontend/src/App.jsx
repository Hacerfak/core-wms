import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { ThemeProvider, CssBaseline } from '@mui/material';
import theme from './theme/theme';
import { AuthProvider, AuthContext } from './contexts/AuthContext';
import { useContext } from 'react';
import Can from './components/Can';

// Imports das Páginas
import Login from './pages/Login/Login';
import Onboarding from './pages/Login/Onboarding';
import SelecaoEmpresa from './pages/Login/SelecaoEmpresa';
import Dashboard from './pages/Dashboard/Dashboard';

// CADASTROS (Listas e Formulários)
import ParceiroList from './pages/Cadastros/ParceiroList';
import ParceiroForm from './pages/Cadastros/ParceiroForm';
import ProdutoList from './pages/Cadastros/ProdutoList';
import ProdutoForm from './pages/Cadastros/ProdutoForm';
import MapeamentoView from './pages/Cadastros/Mapeamento/MapeamentoView';

// OPERAÇÃO
import RecebimentoList from './pages/Recebimento/RecebimentoList';
import Recebimento from './pages/Recebimento/Recebimento';
import Conferencia from './pages/Recebimento/Conferencia';

// CONFIGURAÇÃO E ADMIN
import UsuariosList from './pages/Usuarios/UsuariosList';
import UsuarioForm from './pages/Usuarios/UsuarioForm';
import PerfisList from './pages/Usuarios/PerfisList';
import MinhaEmpresa from './pages/Configuracao/MinhaEmpresa';
import GestaoEmpresas from './pages/Admin/GestaoEmpresas';

// RELATÓRIOS
import AuditoriaList from './pages/Relatorios/AuditoriaList';

import MainLayout from './layout/MainLayout';

const PrivateRoute = ({ children }) => {
  const { authenticated, loading } = useContext(AuthContext);
  if (loading) return <div>Carregando...</div>;
  if (!authenticated) return <Navigate to="/login" />;
  return children;
};

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
            <Route path="/onboarding" element={<Onboarding />} />
            <Route path="/selecao-empresa" element={<SelecaoEmpresa />} />

            {/* ROTAS PRIVADAS */}
            <Route element={<PrivateRoute><MainLayout /></PrivateRoute>}>

              {/* Dashboard: Acesso geral para autenticados */}
              <Route path="/dashboard" element={<Dashboard />} />

              {/* === CADASTROS (BLINDADOS) === */}

              {/* Parceiros */}
              <Route path="/cadastros/parceiros" element={
                <Can I="PARCEIRO_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <ParceiroList />
                </Can>
              } />
              <Route path="/cadastros/parceiros/novo" element={
                <Can I="PARCEIRO_CRIAR" elseShow={<Navigate to="/dashboard" />}>
                  <ParceiroForm />
                </Can>
              } />
              <Route path="/cadastros/parceiros/:id" element={
                <Can I="PARCEIRO_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <ParceiroForm />
                </Can>
              } />

              {/* Produtos */}
              <Route path="/cadastros/produtos" element={
                <Can I="PRODUTO_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <ProdutoList />
                </Can>
              } />
              <Route path="/cadastros/produtos/novo" element={
                <Can I="PRODUTO_CRIAR" elseShow={<Navigate to="/dashboard" />}>
                  <ProdutoForm />
                </Can>
              } />
              <Route path="/cadastros/produtos/:id" element={
                <Can I="PRODUTO_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <ProdutoForm />
                </Can>
              } />

              {/* Endereços (Mapeamento) */}
              <Route path="/cadastros/locais" element={
                <Can I="LOCALIZACAO_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <MapeamentoView />
                </Can>
              } />

              {/* === OPERAÇÃO (BLINDADA) === */}

              <Route path="/recebimento" element={
                <Can I="RECEBIMENTO_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <RecebimentoList />
                </Can>
              } />
              <Route path="/recebimento/:id" element={
                <Can I="RECEBIMENTO_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <Recebimento />
                </Can>
              } />
              {/* Conferência exige permissão específica de execução */}
              <Route path="/recebimento/:id/conferencia" element={
                <Can I="RECEBIMENTO_CONFERIR" elseShow={<Navigate to="/dashboard" />}>
                  <Conferencia />
                </Can>
              } />

              <Route path="/estoque" element={
                <Can I="ESTOQUE_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <div>Módulo de Estoque (Em breve)</div>
                </Can>
              } />

              <Route path="/expedicao" element={
                <Can I="PEDIDO_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <div>Módulo de Expedição (Em breve)</div>
                </Can>
              } />

              {/* === CONFIGURAÇÕES & ADMIN (BLINDADOS) === */}

              {/* Gestão de Usuários */}
              <Route path="/usuarios" element={
                <Can I="USUARIO_LISTAR" elseShow={<Navigate to="/dashboard" />}>
                  <UsuariosList />
                </Can>
              } />
              <Route path="/usuarios/novo" element={
                <Can I="USUARIO_CRIAR" elseShow={<Navigate to="/dashboard" />}>
                  <UsuarioForm />
                </Can>
              } />
              {/* Edição de usuário: Aberta na rota para permitir auto-edição (lógica interna no form) */}
              <Route path="/usuarios/:id" element={<UsuarioForm />} />

              {/* Perfis */}
              <Route path="/perfis" element={
                <Can I="PERFIL_GERENCIAR" elseShow={<Navigate to="/dashboard" />}>
                  <PerfisList />
                </Can>
              } />

              {/* Minha Empresa */}
              <Route path="/config/empresa" element={
                <Can I="CONFIG_GERENCIAR" elseShow={<Navigate to="/dashboard" />}>
                  <MinhaEmpresa />
                </Can>
              } />

              {/* Admin Global (Master) */}
              <Route path="/admin/empresas" element={
                <Can I="ADMIN" elseShow={<Navigate to="/dashboard" />}>
                  <GestaoEmpresas />
                </Can>
              } />
              {/* Relatórios */}
              <Route path="/auditoria" element={
                <Can I="AUDITORIA_VISUALIZAR" elseShow={<Navigate to="/dashboard" />}>
                  <AuditoriaList />
                </Can>
              } />
            </Route>
            <Route path="/" element={<Navigate to="/dashboard" />} />
            <Route path="*" element={<Navigate to="/dashboard" />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
}

export default App;