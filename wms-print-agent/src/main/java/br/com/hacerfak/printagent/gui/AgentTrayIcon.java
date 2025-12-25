package br.com.hacerfak.printagent.gui;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.SwingUtilities;
import java.awt.*;
import java.net.URI;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentTrayIcon {

    @Value("${server.port:8099}")
    private String serverPort;

    @PostConstruct
    public void init() {
        // Verifica se o ambiente suporta interface gráfica (não é headless)
        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
            log.info("SystemTray não suportado ou ambiente Headless. TrayIcon ignorado.");
            return;
        }

        // Garante que a GUI rode na Thread de Eventos do AWT (evita travamentos no
        // Linux)
        SwingUtilities.invokeLater(this::createTray);
    }

    private void createTray() {
        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Carrega a imagem (certifique-se de usar a versão em alta resolução)
            Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/icon.png"));

            PopupMenu popup = new PopupMenu();

            MenuItem statusItem = new MenuItem("WMS Agent: Rodando");
            statusItem.setEnabled(false);

            MenuItem openWebItem = new MenuItem("Abrir Painel Web");
            openWebItem.addActionListener(e -> openBrowser());

            MenuItem exitItem = new MenuItem("Encerrar Agente");
            exitItem.addActionListener(e -> System.exit(0));

            popup.add(statusItem);
            popup.addSeparator();
            popup.add(openWebItem);
            popup.addSeparator();
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "WMS Print Agent", popup);

            // Corrige o tamanho do ícone no Linux/Gnome
            trayIcon.setImageAutoSize(true);

            // Duplo clique abre o navegador
            trayIcon.addActionListener(e -> openBrowser());

            tray.add(trayIcon);

        } catch (Exception e) {
            log.error("Erro ao criar TrayIcon", e);
        }
    }

    private void openBrowser() {
        try {
            String url = "http://localhost:" + serverPort;
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback para Linux usando ProcessBuilder (Substitui o Runtime.exec
                // depreciado)
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception e) {
            log.error("Não foi possível abrir o navegador", e);
        }
    }
}