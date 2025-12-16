import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import theme from './theme/theme';
import { AuthProvider, AuthContext } from './contexts/AuthContext';
import Login from './pages/Login/Login';
import Dashboard from './pages/Dashboard/Dashboard';
import Recebimento from './pages/Recebimento/Recebimento';
import RecebimentoList from './pages/Recebimento/RecebimentoList';
import Conferencia from './pages/Recebimento/Conferencia';
import Configuracoes from './pages/Configuracao/Configuracoes';
import MainLayout from './layout/MainLayout'; // <--- Importe
import { useContext } from 'react';

// Wrapper para rotas protegidas que adiciona o Layout
const PrivateRoute = ({ children }) => {
  const { authenticated, loading } = useContext(AuthContext);

  if (loading) return null;
  if (!authenticated) return <Navigate to="/login" />;

  // Agora o Layout "abraça" a página filha
  return <MainLayout>{children}</MainLayout>;
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

            <Route path="/dashboard" element={
              <PrivateRoute>
                <Dashboard />
              </PrivateRoute>
            } />
            {/* LISTAGEM (Tela Principal) */}
            <Route path="/recebimento" element={
              <PrivateRoute>
                <RecebimentoList />
              </PrivateRoute>
            } />
            {/* UPLOAD (Nova Importação) */}
            <Route path="/recebimento/novo" element={
              <PrivateRoute>
                <Recebimento />
              </PrivateRoute>
            } />
            {/* CONFERÊNCIA (Operação) */}
            <Route path="/recebimento/:id/conferencia" element={
              <PrivateRoute>
                <Conferencia />
              </PrivateRoute>
            } />
            {/* CONFIGURAÇÕES DO SISTEMA */}
            <Route path="/config" element={
              <PrivateRoute>
                <Configuracoes />
              </PrivateRoute>
            } />
            {/* Redireciona raiz para dashboard */}
            <Route path="/" element={<Navigate to="/dashboard" />} />
            <Route path="*" element={<Navigate to="/login" />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;