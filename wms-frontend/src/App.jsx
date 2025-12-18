import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import theme from './theme/theme';
import { AuthProvider, AuthContext } from './contexts/AuthContext';
import { useContext } from 'react';

// Páginas
import Login from './pages/Login/Login';
import SelecaoEmpresa from './pages/Login/SelecaoEmpresa';
import Onboarding from './pages/Login/Onboarding';
import Dashboard from './pages/Dashboard/Dashboard';
import RecebimentoList from './pages/Recebimento/RecebimentoList';
import Recebimento from './pages/Recebimento/Recebimento';
import Conferencia from './pages/Recebimento/Conferencia';
import Configuracoes from './pages/Configuracao/Configuracoes';
import UsuariosList from './pages/Usuarios/UsuariosList';
import MainLayout from './layout/MainLayout';

// Wrapper para rotas protegidas (Dashboard e internas)
const PrivateRoute = ({ children }) => {
  const { authenticated, loading } = useContext(AuthContext);

  if (loading) return null;
  if (!authenticated) return <Navigate to="/login" />;

  // Aqui poderíamos checar se tem Tenant selecionado, mas o Backend barra se não tiver.
  return <MainLayout>{children}</MainLayout>;
};

// Wrapper simples para telas de Seleção/Onboarding (autenticadas, mas sem Layout com Menu)
const AuthRoute = ({ children }) => {
  const { authenticated, loading } = useContext(AuthContext);
  if (loading) return null;
  if (!authenticated) return <Navigate to="/login" />;
  return children;
};

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <ToastContainer position="top-right" autoClose={3000} />

      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />

            {/* Rotas de Entrada (Sem Menu Lateral) */}
            <Route path="/selecao-empresa" element={
              <AuthRoute><SelecaoEmpresa /></AuthRoute>
            } />
            <Route path="/onboarding" element={
              <AuthRoute><Onboarding /></AuthRoute>
            } />

            {/* Rotas do Sistema (Com Menu Lateral) */}
            <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
            <Route path="/recebimento" element={<PrivateRoute><RecebimentoList /></PrivateRoute>} />
            <Route path="/recebimento/novo" element={<PrivateRoute><Recebimento /></PrivateRoute>} />
            <Route path="/recebimento/:id/conferencia" element={<PrivateRoute><Conferencia /></PrivateRoute>} />
            <Route path="/config" element={<PrivateRoute><Configuracoes /></PrivateRoute>} />
            <Route path="/usuarios" element={<PrivateRoute><UsuariosList /></PrivateRoute>} />

            <Route path="/" element={<Navigate to="/dashboard" />} />
            <Route path="*" element={<Navigate to="/login" />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;