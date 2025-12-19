import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { ThemeProvider, CssBaseline } from '@mui/material';
import theme from './theme/theme';
import { AuthProvider, AuthContext } from './contexts/AuthContext';
import { useContext } from 'react';

// Imports das Páginas
import Login from './pages/Login/Login';
import Onboarding from './pages/Login/Onboarding';
import SelecaoEmpresa from './pages/Login/SelecaoEmpresa';
import Dashboard from './pages/Dashboard/Dashboard';
import ParceiroList from './pages/Cadastros/ParceiroList';
import ProdutoList from './pages/Cadastros/ProdutoList';
import MapeamentoView from './pages/Cadastros/Mapeamento/MapeamentoView';
import RecebimentoList from './pages/Recebimento/RecebimentoList';
import Recebimento from './pages/Recebimento/Recebimento';
import Conferencia from './pages/Recebimento/Conferencia';
import UsuariosList from './pages/Usuarios/UsuariosList';
import PerfisList from './pages/Usuarios/PerfisList';
import Configuracoes from './pages/Configuracao/Configuracoes';
import MainLayout from './layout/MainLayout';

// Componente de Rota Privada
const PrivateRoute = ({ children }) => {
  const { authenticated, loading } = useContext(AuthContext);

  if (loading) return <div>Carregando...</div>;

  if (!authenticated) {
    return <Navigate to="/login" />;
  }

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
            {/* --- ROTAS PÚBLICAS --- */}
            <Route path="/login" element={<Login />} />
            <Route path="/onboarding" element={<Onboarding />} />
            <Route path="/selecao-empresa" element={<SelecaoEmpresa />} />

            {/* --- ROTAS PROTEGIDAS (LAYOUT + SIDEBAR) --- */}
            {/* Envolvemos o MainLayout no PrivateRoute. Assim, se não logado, nem carrega o layout. */}
            <Route element={
              <PrivateRoute>
                <MainLayout />
              </PrivateRoute>
            }>
              <Route path="/dashboard" element={<Dashboard />} />

              {/* Cadastros Básicos */}
              <Route path="/cadastros/parceiros" element={<ParceiroList />} />
              <Route path="/cadastros/produtos" element={<ProdutoList />} />
              <Route path="/cadastros/locais" element={<MapeamentoView />} />

              {/* Módulo Recebimento */}
              <Route path="/recebimento" element={<RecebimentoList />} />
              <Route path="/recebimento/:id" element={<Recebimento />} />
              <Route path="/recebimento/:id/conferencia" element={<Conferencia />} />

              {/* Placeholders para módulos futuros */}
              <Route path="/estoque" element={<div>Módulo de Estoque (Em breve)</div>} />
              <Route path="/expedicao" element={<div>Módulo de Expedição (Em breve)</div>} />

              {/* Gestão e Configuração */}
              <Route path="/usuarios" element={<UsuariosList />} />
              <Route path="/perfis" element={<PerfisList />} />
              <Route path="/config" element={<Configuracoes />} />
            </Route>

            {/* Fallback: Se tentar acessar a raiz, joga pro dashboard (que vai jogar pro login se precisar) */}
            <Route path="/" element={<Navigate to="/dashboard" />} />
            <Route path="*" element={<Navigate to="/dashboard" />} />
          </Routes>

        </AuthProvider>
      </BrowserRouter>

    </ThemeProvider>
  );
}

export default App;