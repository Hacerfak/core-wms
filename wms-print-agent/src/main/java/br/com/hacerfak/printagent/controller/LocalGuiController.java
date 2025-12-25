package br.com.hacerfak.printagent.controller;

import br.com.hacerfak.printagent.installer.ServiceInstaller;
import br.com.hacerfak.printagent.service.AgentConfigService;
import br.com.hacerfak.printagent.service.AgentLogStore;
import br.com.hacerfak.printagent.service.AgentStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class LocalGuiController {

    private final AgentLogStore logStore;
    private final AgentStatusService statusService;
    private final AgentConfigService configService;
    private final ServiceInstaller serviceInstaller;

    @Value("${server.port}")
    private String serverPort;

    @GetMapping(produces = "text/html")
    public String dashboard() {
        return renderPage(false);
    }

    @GetMapping(value = "/config", produces = "text/html")
    public String configPage() {
        return renderPage(true);
    }

    @PostMapping("/api/save-config")
    public ResponseEntity<Void> saveConfig(@RequestBody Map<String, String> body) {
        configService.salvarConfiguracoes(
                body.get("agentId"),
                body.get("cnpj"),
                body.get("dominio"),
                body.get("apiKey"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/install-service")
    public ResponseEntity<String> installService() {
        try {
            serviceInstaller.install();
            return ResponseEntity.ok("Comando de instalação enviado. Verifique os serviços do sistema.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    @PostMapping("/api/uninstall-service")
    public ResponseEntity<String> uninstallService() {
        try {
            serviceInstaller.uninstall();
            return ResponseEntity.ok("Comando de desinstalação enviado. O serviço será removido.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/status-data")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "online", statusService.isOnline(),
                "lastCheck", statusService.getLastCheckFormatted(),
                "message", statusService.getLastMessage(),
                "tenant", configService.getTenantId()));
    }

    @GetMapping("/logs-data")
    public ResponseEntity<String> getLogs() {
        return ResponseEntity.ok(logStore.getLogsAsString());
    }

    private String renderPage(boolean isConfig) {
        try {
            // Tenta ler o arquivo externo primeiro
            ApplicationHome home = new ApplicationHome(getClass());
            File customTemplate = new File(home.getDir(), "dashboard.html");

            String template;
            if (customTemplate.exists()) {
                template = Files.readString(customTemplate.toPath());
            } else {
                // Fallback para o template padrão se não houver arquivo
                template = getDefaultTemplate();
            }

            // Injeta variáveis
            return template
                    .replace("{{AGENT_ID}}", configService.getAgentId())
                    .replace("{{PORT}}", serverPort)
                    .replace("{{BACKEND_URL}}", configService.getBackendUrl())
                    .replace("{{TENANT}}", configService.getTenantId())
                    .replace("{{VAL_CNPJ}}", configService.getCnpjEmpresa())
                    .replace("{{VAL_DOMINIO}}", configService.getServidorDominio())
                    .replace("{{VAL_KEY}}", configService.getApiKey())
                    // Lógica simples para mostrar/esconder seções baseada na URL atual
                    .replace("{{SHOW_CONFIG}}", isConfig ? "block" : "none")
                    .replace("{{SHOW_DASHBOARD}}", isConfig ? "none" : "block");

        } catch (Exception e) {
            return "<h1>Erro ao carregar interface</h1><p>" + e.getMessage() + "</p>";
        }
    }

    private String getDefaultTemplate() {
        // Retorna o mesmo conteúdo que vou te passar no arquivo dashboard.html abaixo
        // para garantir que o comportamento padrão seja idêntico ao customizado.
        return """
                    <!DOCTYPE html>
                    <html lang="pt-BR">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>WMS Print Agent</title>
                        <style>
                            body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; color: #334155; padding: 20px; }
                            .card { background: white; padding: 30px; border-radius: 16px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); max-width: 800px; margin: 0 auto; }
                            .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; border-bottom: 2px solid #f1f5f9; padding-bottom: 20px; }
                            .badge { padding: 6px 12px; border-radius: 20px; font-weight: bold; font-size: 0.8rem; background: #e2e8f0; color: #64748b; }
                            .badge.on { background: #dcfce7; color: #166534; }
                            .badge.off { background: #fee2e2; color: #991b1b; }
                            .form-group { margin-bottom: 15px; }
                            .form-group label { display: block; font-weight: 600; margin-bottom: 5px; font-size: 0.9rem; }
                            .form-group input { width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; box-sizing: border-box; }
                            .form-group small { color: #94a3b8; font-size: 0.8rem; }
                            button, .btn { padding: 10px 20px; border-radius: 6px; border: none; cursor: pointer; font-weight: 600; transition: 0.2s; text-decoration: none; display: inline-block; }
                            .btn-pri { background: #2563eb; color: white; } .btn-pri:hover { background: #1d4ed8; }
                            .btn-sec { background: white; border: 1px solid #cbd5e1; color: #475569; } .btn-sec:hover { background: #f8fafc; }
                            .btn-install { background: #0f172a; color: white; width: 100%; margin-top: 10px; }
                            .btn-uninstall { background: #fee2e2; color: #991b1b; width: 100%; margin-top: 10px; } .btn-uninstall:hover { background: #fecaca; }
                            .log-box { background: #0f172a; color: #e2e8f0; padding: 15px; border-radius: 8px; height: 300px; overflow-y: auto; font-family: monospace; font-size: 0.85rem; white-space: pre-wrap; }
                            .info-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 15px; margin-bottom: 20px; background: #f8fafc; padding: 15px; border-radius: 8px; }
                            .actions { display: flex; gap: 10px; justify-content: flex-end; }
                        </style>
                        <script>
                            async function salvarConfig(e) {
                                e.preventDefault();
                                const formData = new FormData(e.target);
                                const data = Object.fromEntries(formData.entries());
                                try {
                                    const res = await fetch('/api/save-config', {
                                        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)
                                    });
                                    if(res.ok) { alert('Salvo com sucesso!'); window.location = '/'; }
                                    else alert('Erro ao salvar.');
                                } catch(err) { alert('Erro de conexão.'); }
                            }
                            async function manageService(action) {
                                if(!confirm('Esta ação requer privilégios de Administrador. Continuar?')) return;
                                try {
                                    const res = await fetch('/api/' + action + '-service', { method: 'POST' });
                                    alert(await res.text());
                                } catch(err) { alert('Erro na requisição.'); }
                            }
                            if(window.location.pathname === '/') {
                                setInterval(async () => {
                                    try {
                                        const s = await (await fetch('/status-data')).json();
                                        document.getElementById('badge-status').className = 'badge ' + (s.online ? 'on' : 'off');
                                        document.getElementById('badge-status').innerText = s.online ? 'CONECTADO' : 'OFFLINE';
                                        document.getElementById('status-text-dyn').innerText = s.online ? 'ONLINE' : 'OFFLINE';
                                        document.getElementById('log-content').innerText = await (await fetch('/logs-data')).text();
                                    } catch(e){}
                                }, 2000);
                            }
                        </script>
                    </head>
                    <body>
                        <div class="card">
                            <div class="header">
                                <h3>🖨️ WMS Print Agent</h3>
                                <span id="badge-status" class="badge">...</span>
                            </div>

                            <div style="display: {{SHOW_DASHBOARD}}">
                                <div class="info-grid">
                                    <div><small>AGENTE</small><br><strong>{{AGENT_ID}}</strong></div>
                                    <div><small>TENANT</small><br><strong>{{TENANT}}</strong></div>
                                    <div><small>STATUS</small><br><strong id="status-text-dyn">...</strong></div>
                                </div>
                                <div id="log-content" class="log-box">Carregando logs...</div>
                                <div style="text-align: right; margin-top: 15px;">
                                    <a href="/config" class="btn btn-sec">⚙️ Configurar</a>
                                </div>
                            </div>

                            <div style="display: {{SHOW_CONFIG}}">
                                <h2>⚙️ Configuração</h2>
                                <form onsubmit="salvarConfig(event)">
                                    <div class="form-group">
                                        <label>Nome do Agente (ID)</label>
                                        <input type="text" name="agentId" value="{{AGENT_ID}}" required>
                                    </div>
                                    <div class="form-group">
                                        <label>CNPJ da Empresa</label>
                                        <input type="text" name="cnpj" value="{{VAL_CNPJ}}" required placeholder="Apenas números">
                                    </div>
                                    <div class="form-group">
                                        <label>Domínio do Servidor</label>
                                        <input type="text" name="dominio" value="{{VAL_DOMINIO}}" required>
                                    </div>
                                    <div class="form-group">
                                        <label>Chave de API</label>
                                        <input type="password" name="apiKey" value="{{VAL_KEY}}" required>
                                    </div>
                                    <div class="actions">
                                        <a href="/" class="btn btn-sec">Voltar</a>
                                        <button type="submit" class="btn btn-pri">Salvar</button>
                                    </div>
                                </form>
                                <hr style="margin: 30px 0; border: 0; border-top: 1px solid #e2e8f0;">
                                <h3>🛠️ Serviço do Sistema</h3>
                                <button onclick="manageService('install')" class="btn btn-install">Instalar Serviço</button>
                                <button onclick="manageService('uninstall')" class="btn btn-uninstall">Desinstalar Serviço</button>
                            </div>
                        </div>
                    </body>
                    </html>
                """;
    }
}