import { Grid, Card, CardContent, Typography, Box } from '@mui/material';
import { PackagePlus, Truck, AlertTriangle, CheckCircle } from 'lucide-react';

const KPICard = ({ title, value, icon, color, subtext }) => (
    <Card sx={{ height: '100%' }}>
        <CardContent>
            <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
                <Box>
                    <Typography color="text.secondary" variant="body2" fontWeight={600} gutterBottom>
                        {title}
                    </Typography>
                    <Typography variant="h4" fontWeight="bold" color="text.primary">
                        {value}
                    </Typography>
                </Box>
                <Box sx={{
                    p: 1.5,
                    borderRadius: 2,
                    bgcolor: `${color}15`, // Cor com transparência
                    color: color
                }}>
                    {icon}
                </Box>
            </Box>
            <Typography variant="caption" color="text.secondary">
                {subtext}
            </Typography>
        </CardContent>
    </Card>
);

const Dashboard = () => {
    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" mb={3} color="text.primary">
                Dashboard
            </Typography>

            <Grid container spacing={3}>
                {/* KPI 1 - Recebimento */}
                <Grid item xs={12} sm={6} md={3}>
                    <KPICard
                        title="Aguardando Recebimento"
                        value="12"
                        subtext="3 notas chegaram hoje"
                        icon={<PackagePlus />}
                        color="#2563eb" // Azul
                    />
                </Grid>

                {/* KPI 2 - Expedição */}
                <Grid item xs={12} sm={6} md={3}>
                    <KPICard
                        title="Pedidos a Expedir"
                        value="45"
                        subtext="8 urgentes"
                        icon={<Truck />}
                        color="#16a34a" // Verde
                    />
                </Grid>

                {/* KPI 3 - Ocupação */}
                <Grid item xs={12} sm={6} md={3}>
                    <KPICard
                        title="Ocupação Armazém"
                        value="87%"
                        subtext="Área B está crítica"
                        icon={<AlertTriangle />}
                        color="#ea580c" // Laranja
                    />
                </Grid>

                {/* KPI 4 - Finalizados */}
                <Grid item xs={12} sm={6} md={3}>
                    <KPICard
                        title="Movimentações Hoje"
                        value="1,203"
                        subtext="+12% que ontem"
                        icon={<CheckCircle />}
                        color="#475569" // Cinza
                    />
                </Grid>
            </Grid>

            {/* Aqui embaixo colocaremos gráficos no futuro */}
        </Box>
    );
};

export default Dashboard;