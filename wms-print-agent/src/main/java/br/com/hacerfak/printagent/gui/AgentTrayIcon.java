package br.com.hacerfak.printagent.gui;

import br.com.hacerfak.printagent.service.AgentLogStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;

@Component
@RequiredArgsConstructor
public class AgentTrayIcon {

    private final AgentLogStore logStore;

    @PostConstruct
    public void init() {
        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
            return;
        }

        SwingUtilities.invokeLater(this::createTray);
    }

    private void createTray() {
        try {
            SystemTray tray = SystemTray.getSystemTray();
            // Certifique-se de ter um icon.png em src/main/resources
            Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/icon.png"));

            PopupMenu popup = new PopupMenu();

            MenuItem statusItem = new MenuItem("Status: Rodando 🟢");
            statusItem.setEnabled(true);

            MenuItem logItem = new MenuItem("Ver Logs em Tempo Real");
            logItem.addActionListener(e -> showLogWindow());

            MenuItem exitItem = new MenuItem("Sair");
            exitItem.addActionListener(e -> System.exit(0));

            popup.add(logItem);
            popup.addSeparator();
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "WMS Print Agent", popup);
            trayIcon.setImageAutoSize(false);
            tray.add(trayIcon);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showLogWindow() {
        JDialog dialog = new JDialog((Frame) null, "Logs do Agente", false); // false = não modal
        dialog.setSize(500, 400);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setText(logStore.getLogsAsString());

        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton btnRefresh = new JButton("Atualizar / Limpar");
        btnRefresh.addActionListener(e -> textArea.setText(logStore.getLogsAsString()));

        dialog.add(btnRefresh, BorderLayout.SOUTH);

        // Centraliza na tela
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}