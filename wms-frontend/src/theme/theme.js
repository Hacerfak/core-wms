import { createTheme } from '@mui/material/styles';

const theme = createTheme({
    palette: {
        primary: {
            main: '#2563eb', // Um Azul Royal moderno (Interativo)
            dark: '#1e40af',
            light: '#60a5fa',
        },
        secondary: {
            main: '#475569', // Cinza Chumbo (Para elementos secundários)
        },
        background: {
            default: '#f1f5f9', // Um cinza bem clarinho (não cansa a vista)
            paper: '#ffffff',
        },
        text: {
            primary: '#1e293b', // Quase preto (ótimo contraste)
            secondary: '#64748b',
        },
    },
    typography: {
        fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
        h1: { fontSize: '2rem', fontWeight: 600, color: '#1e293b' },
        h2: { fontSize: '1.5rem', fontWeight: 600, color: '#1e293b' },
        h6: { fontWeight: 600 },
        button: { textTransform: 'none', fontWeight: 500 }, // Botões sem CAIXA ALTA (mais elegante)
    },
    shape: {
        borderRadius: 8, // Bordas levemente arredondadas (moderno)
    },
    components: {
        MuiButton: {
            styleOverrides: {
                root: { padding: '8px 24px', boxShadow: 'none' },
                contained: { '&:hover': { boxShadow: '0px 4px 12px rgba(37, 99, 235, 0.2)' } }
            },
        },
        MuiCard: {
            styleOverrides: {
                root: { boxShadow: '0px 2px 4px rgba(0,0,0,0.05)', border: '1px solid #e2e8f0' }
            },
        },
        MuiTextField: {
            defaultProps: { variant: 'outlined', size: 'small' } // Inputs menores e mais elegantes
        }
    },
});

export default theme;