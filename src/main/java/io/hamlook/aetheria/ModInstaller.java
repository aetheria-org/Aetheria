package io.hamlook.aetheria;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ModInstaller {
    private static final Color BG_COLOR = new Color(25, 25, 25);
    private static final Color TITLE_BG = new Color(35, 35, 35);
    private static final Color FG_COLOR = new Color(240, 240, 240);
    private static final Color BTN_GRAY = new Color(70, 70, 70);
    private static final Color ACCENT = new Color(60, 120, 200);
    private static final Color HOVER_ACCENT = new Color(80, 140, 220);
    private static final Color INPUT_BG = new Color(40, 40, 40);
    private static final Color BORDER_COL = new Color(60, 60, 60);
    private static final Map<String, String> modLinks = new LinkedHashMap<>();
    private static final Map<String, List<ReleaseItem>> cachedReleases = new ConcurrentHashMap<>();
    private static final File CONFIG_FILE = new File(System.getProperty("user.home"), ".aetheria/config.txt");
    private static Point initialClick;
    private static boolean isMaximized = false;
    private static Rectangle normalBounds;

    private static String loadConfig() {
        try {
            if (CONFIG_FILE.exists()) {
                byte[] bytes = java.nio.file.Files.readAllBytes(CONFIG_FILE.toPath());
                return new String(bytes, StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static void saveConfig(String path) {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            java.nio.file.Files.write(CONFIG_FILE.toPath(), path.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Fakepixel Mods Installer | Aetheria's Skyblock Mod");
            frame.setUndecorated(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(650, 550);
            frame.setLocationRelativeTo(null);
            frame.setResizable(true);

            JPanel mainContainer = new JPanel(new BorderLayout());
            mainContainer.setBackground(BG_COLOR);
            mainContainer.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));

            JPanel titleBar = new JPanel(new BorderLayout());
            titleBar.setBackground(TITLE_BG);
            titleBar.setPreferredSize(new Dimension(frame.getWidth(), 35));

            JLabel titleLabel = new JLabel("Fakepixel Mod Installer | Aetheria's Skyblock Mod");
            titleLabel.setForeground(FG_COLOR);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            titleBar.add(titleLabel, BorderLayout.WEST);

            JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            controlsPanel.setOpaque(false);

            JButton minimizeBtn = createControlButton("-");
            JButton maximizeBtn = createControlButton("□");
            JButton closeBtn = createControlButton("×");

            minimizeBtn.addActionListener(e -> frame.setState(Frame.ICONIFIED));
            maximizeBtn.addActionListener(e -> {
                if (!isMaximized) {
                    normalBounds = frame.getBounds();
                    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    isMaximized = true;
                } else {
                    frame.setExtendedState(JFrame.NORMAL);
                    frame.setBounds(normalBounds);
                    isMaximized = false;
                }
            });
            closeBtn.addActionListener(e -> System.exit(0));
            closeBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    closeBtn.setBackground(new Color(232, 17, 35));
                }

                public void mouseExited(MouseEvent e) {
                    closeBtn.setBackground(TITLE_BG);
                }
            });

            controlsPanel.add(minimizeBtn);
            controlsPanel.add(maximizeBtn);
            controlsPanel.add(closeBtn);
            titleBar.add(controlsPanel, BorderLayout.EAST);

            titleBar.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    initialClick = e.getPoint();
                }
            });
            titleBar.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (isMaximized) return;
                    int xMoved = e.getX() - initialClick.x;
                    int yMoved = e.getY() - initialClick.y;
                    frame.setLocation(frame.getLocation().x + xMoved, frame.getLocation().y + yMoved);
                }
            });

            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(BG_COLOR);
            headerPanel.add(titleBar, BorderLayout.NORTH);

            JPanel globalConfigPanel = new JPanel(new GridBagLayout());
            globalConfigPanel.setBackground(BG_COLOR);
            globalConfigPanel.setBorder(new EmptyBorder(15, 30, 5, 30));
            GridBagConstraints hgbc = new GridBagConstraints();
            hgbc.fill = GridBagConstraints.HORIZONTAL;
            hgbc.insets = new Insets(5, 5, 5, 5);

            hgbc.gridx = 0;
            hgbc.gridy = 0;
            hgbc.weightx = 0;
            JLabel pathLabel = new JLabel("Mods Folder:");
            pathLabel.setForeground(FG_COLOR);
            pathLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            globalConfigPanel.add(pathLabel, hgbc);

            hgbc.gridx = 1;
            hgbc.gridy = 0;
            hgbc.weightx = 1.0;
            JPanel pathPanel = new JPanel(new BorderLayout(10, 0));
            pathPanel.setOpaque(false);
            JTextField pathField = new JTextField(loadConfig());
            pathField.setBackground(INPUT_BG);
            pathField.setForeground(FG_COLOR);
            pathField.setCaretColor(FG_COLOR);
            pathField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            pathField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COL), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            JButton browseBtn = new JButton("Browse");
            styleButton(browseBtn, BTN_GRAY, new Color(90, 90, 90));
            browseBtn.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    pathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            });
            pathPanel.add(pathField, BorderLayout.CENTER);
            pathPanel.add(browseBtn, BorderLayout.EAST);
            globalConfigPanel.add(pathPanel, hgbc);

            JPanel tabBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
            tabBtnPanel.setBackground(BG_COLOR);
            JButton installTabBtn = new JButton("Install");
            JButton updateTabBtn = new JButton("Update");
            styleButton(installTabBtn, ACCENT, HOVER_ACCENT);
            styleButton(updateTabBtn, TITLE_BG, BTN_GRAY);
            tabBtnPanel.add(installTabBtn);
            tabBtnPanel.add(updateTabBtn);

            JPanel headerBottom = new JPanel(new BorderLayout());
            headerBottom.add(globalConfigPanel, BorderLayout.NORTH);
            headerBottom.add(tabBtnPanel, BorderLayout.SOUTH);
            headerPanel.add(headerBottom, BorderLayout.CENTER);
            mainContainer.add(headerPanel, BorderLayout.NORTH);

            CardLayout cardLayout = new CardLayout();
            JPanel cards = new JPanel(cardLayout);
            cards.setBackground(BG_COLOR);

            JPanel installCard = new JPanel(new GridBagLayout());
            installCard.setBackground(BG_COLOR);
            installCard.setBorder(new EmptyBorder(10, 30, 20, 30));
            GridBagConstraints igbc = new GridBagConstraints();
            igbc.fill = GridBagConstraints.HORIZONTAL;
            igbc.insets = new Insets(10, 10, 10, 10);
            igbc.weightx = 1.0;

            igbc.gridx = 0;
            igbc.gridy = 0;
            igbc.weightx = 0.3;
            JLabel modLabel = new JLabel("Select Mod:");
            modLabel.setForeground(FG_COLOR);
            modLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            installCard.add(modLabel, igbc);

            igbc.gridx = 1;
            igbc.gridy = 0;
            igbc.weightx = 0.7;
            JComboBox<String> modCombo = createDarkComboBox(new String[]{"Loading..."});
            installCard.add(modCombo, igbc);

            igbc.gridx = 0;
            igbc.gridy = 1;
            igbc.weightx = 0.3;
            JLabel versionLabel = new JLabel("Version:");
            versionLabel.setForeground(FG_COLOR);
            versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            installCard.add(versionLabel, igbc);

            igbc.gridx = 1;
            igbc.gridy = 1;
            igbc.weightx = 0.7;
            JComboBox<ReleaseItem> versionCombo = new JComboBox<>();
            versionCombo.setBackground(INPUT_BG);
            versionCombo.setForeground(FG_COLOR);
            versionCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            versionCombo.setUI(new BasicComboBoxUI() {
                protected JButton createArrowButton() {
                    JButton btn = super.createArrowButton();
                    btn.setUI(new BasicButtonUI());
                    btn.setBackground(new Color(50, 50, 50));
                    btn.setForeground(FG_COLOR);
                    btn.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                    btn.setOpaque(true);
                    return btn;
                }
            });
            versionCombo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COL), BorderFactory.createEmptyBorder(4, 5, 4, 5)));
            installCard.add(versionCombo, igbc);

            igbc.gridx = 0;
            igbc.gridy = 2;
            igbc.gridwidth = 2;
            igbc.insets = new Insets(40, 10, 10, 10);
            ProgressButton downloadBtn = new ProgressButton("Install Mod", BTN_GRAY, ACCENT, FG_COLOR);
            downloadBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (downloadBtn.isEnabled() && downloadBtn.progress == 0) downloadBtn.setBgColor(HOVER_ACCENT);
                }

                public void mouseExited(MouseEvent e) {
                    if (downloadBtn.isEnabled() && downloadBtn.progress == 0) downloadBtn.setBgColor(BTN_GRAY);
                }
            });
            installCard.add(downloadBtn, igbc);

            igbc.gridy = 3;
            igbc.weighty = 1.0;
            installCard.add(Box.createGlue(), igbc);
            cards.add(installCard, "INSTALL");

            JPanel updateCard = new JPanel(new GridBagLayout());
            updateCard.setBackground(BG_COLOR);
            updateCard.setBorder(new EmptyBorder(10, 30, 20, 30));
            GridBagConstraints ugbc = new GridBagConstraints();
            ugbc.fill = GridBagConstraints.HORIZONTAL;
            ugbc.insets = new Insets(10, 10, 10, 10);

            ugbc.gridx = 0;
            ugbc.gridy = 0;
            ugbc.weightx = 1.0;
            JTextArea updateInfo = new JTextArea("Click below to scan your mods folder for updates across all supported mods.");
            updateInfo.setWrapStyleWord(true);
            updateInfo.setLineWrap(true);
            updateInfo.setOpaque(false);
            updateInfo.setForeground(FG_COLOR);
            updateInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            updateInfo.setEditable(false);
            updateCard.add(updateInfo, ugbc);

            ugbc.gridy = 1;
            ugbc.insets = new Insets(30, 10, 10, 10);
            ProgressButton checkUpdatesBtn = new ProgressButton("Check for Updates", BTN_GRAY, ACCENT, FG_COLOR);
            checkUpdatesBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (checkUpdatesBtn.isEnabled() && checkUpdatesBtn.progress == 0)
                        checkUpdatesBtn.setBgColor(HOVER_ACCENT);
                }

                public void mouseExited(MouseEvent e) {
                    if (checkUpdatesBtn.isEnabled() && checkUpdatesBtn.progress == 0)
                        checkUpdatesBtn.setBgColor(BTN_GRAY);
                }
            });
            updateCard.add(checkUpdatesBtn, ugbc);

            ugbc.gridy = 2;
            ugbc.weighty = 1.0;
            updateCard.add(Box.createGlue(), ugbc);
            cards.add(updateCard, "UPDATE");

            mainContainer.add(cards, BorderLayout.CENTER);
            frame.add(mainContainer);

            installTabBtn.addActionListener(e -> {
                cardLayout.show(cards, "INSTALL");
                styleButton(installTabBtn, ACCENT, HOVER_ACCENT);
                styleButton(updateTabBtn, TITLE_BG, BTN_GRAY);
            });
            updateTabBtn.addActionListener(e -> {
                cardLayout.show(cards, "UPDATE");
                styleButton(updateTabBtn, ACCENT, HOVER_ACCENT);
                styleButton(installTabBtn, TITLE_BG, BTN_GRAY);
            });

            ComponentResizer cr = new ComponentResizer();
            cr.registerComponent(frame);
            frame.setVisible(true);

            new Thread(() -> {
                try {
                    String modLinksUrl = "https://raw.githubusercontent.com/aetheria-org/Aetheria-REPO/refs/heads/main/data/modLinks.json";
                    HttpURLConnection conn = (HttpURLConnection) new URL(modLinksUrl).openConnection();
                    conn.setRequestProperty("User-Agent", "Aetheria-Installer");
                    InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    reader.close();

                    for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                        modLinks.put(entry.getKey(), entry.getValue().getAsString());
                    }

                    SwingUtilities.invokeLater(() -> {
                        modCombo.removeAllItems();
                        for (String mod : modLinks.keySet()) {
                            modCombo.addItem(mod);
                        }
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> showError(frame, "Failed to fetch mod links."));
                }
            }).start();

            modCombo.addActionListener(e -> {
                String selectedMod = (String) modCombo.getSelectedItem();
                if (selectedMod == null) return;

                versionCombo.removeAllItems();

                new Thread(() -> {
                    List<ReleaseItem> items = fetchReleasesForMod(selectedMod);
                    SwingUtilities.invokeLater(() -> {
                        for (ReleaseItem item : items) {
                            versionCombo.addItem(item);
                        }
                    });
                }).start();
            });

            downloadBtn.addActionListener(e -> {
                if (versionCombo.getSelectedItem() == null) {
                    showError(frame, "Please select a valid version.");
                    return;
                }
                File modsFolder = getValidatedModsFolder(pathField.getText(), frame);
                if (modsFolder == null) return;

                ReleaseItem selectedRelease = (ReleaseItem) versionCombo.getSelectedItem();
                downloadBtn.setEnabled(false);
                downloadBtn.setText("Downloading... 0%");

                new Thread(() -> {
                    boolean success = downloadFile(selectedRelease.downloadUrl, new File(modsFolder, selectedRelease.fileName), downloadBtn);
                    SwingUtilities.invokeLater(() -> {
                        if (success) {
                            downloadBtn.setText("Installed!");
                            downloadBtn.setBgColor(ACCENT);
                            JOptionPane.showMessageDialog(frame, selectedRelease.name + " installed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            showError(frame, "Download failed.");
                        }
                        downloadBtn.setEnabled(true);
                        downloadBtn.setProgress(0);
                        downloadBtn.setText("Install Mod");
                        downloadBtn.setBgColor(BTN_GRAY);
                    });
                }).start();
            });

            checkUpdatesBtn.addActionListener(e -> {
                File modsFolder = getValidatedModsFolder(pathField.getText(), frame);
                if (modsFolder == null) return;

                checkUpdatesBtn.setEnabled(false);
                checkUpdatesBtn.setText("Scanning...");

                new Thread(() -> {
                    try {
                        List<File> localJars = new ArrayList<>();
                        File[] files = modsFolder.listFiles();
                        if (files != null) {
                            for (File f : files) {
                                if (f.isFile() && f.getName().endsWith(".jar")) {
                                    localJars.add(f);
                                }
                            }
                        }

                        List<File> toDelete = new ArrayList<>();
                        List<ReleaseItem> toDownload = new ArrayList<>();

                        boolean hasAetheria = false;
                        for (String name : modLinks.keySet()) {
                            if (name.equalsIgnoreCase("Aetheria")) {
                                hasAetheria = true;
                                break;
                            }
                        }

                        for (String modName : modLinks.keySet()) {
                            if (modName.equalsIgnoreCase("JustEnoughFakepixel") && hasAetheria) continue;

                            List<ReleaseItem> releases = fetchReleasesForMod(modName);
                            if (releases.isEmpty()) continue;
                            ReleaseItem latest = releases.get(0);

                            String baseName = modName.replaceAll(" ", "").toLowerCase();
                            boolean hasLocal = false;

                            for (File localJar : localJars) {
                                String localName = localJar.getName().toLowerCase();
                                boolean match = false;

                                if (modName.equalsIgnoreCase("Aetheria")) {
                                    if (localName.startsWith("aetheria") || localName.startsWith("justenoughfakepixel")) {
                                        match = true;
                                    }
                                } else {
                                    if (localName.startsWith(baseName)) {
                                        match = true;
                                    }
                                }

                                if (match) {
                                    hasLocal = true;
                                    if (!localJar.getName().equalsIgnoreCase(latest.fileName)) {
                                        if (!toDelete.contains(localJar)) toDelete.add(localJar);
                                        if (!toDownload.contains(latest)) toDownload.add(latest);
                                    }
                                }
                            }

                            if (!hasLocal) {
                                if (!toDownload.contains(latest)) toDownload.add(latest);
                            }
                        }

                        SwingUtilities.invokeLater(() -> {
                            checkUpdatesBtn.setEnabled(true);
                            checkUpdatesBtn.setText("Check for Updates");

                            if (toDelete.isEmpty() && toDownload.isEmpty()) {
                                JOptionPane.showMessageDialog(frame, "All mods are up to date!", "No updates", JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }

                            showUpdateConfirmation(frame, modsFolder, toDelete, toDownload, checkUpdatesBtn);
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(() -> {
                            showError(frame, "Update scan failed.");
                            checkUpdatesBtn.setEnabled(true);
                            checkUpdatesBtn.setText("Check for Updates");
                        });
                    }
                }).start();
            });

        });
    }

    private static void showUpdateConfirmation(JFrame parent, File modsFolder, List<File> toDelete, List<ReleaseItem> toDownload, ProgressButton progressBtn) {
        JDialog dialog = new JDialog(parent, "Confirm Updates", true);
        dialog.setSize(550, 450);
        dialog.setLocationRelativeTo(parent);
        dialog.setUndecorated(true);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));

        JLabel title = new JLabel(" Updates Available", SwingConstants.CENTER);
        title.setForeground(FG_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setBorder(new EmptyBorder(10, 0, 10, 0));
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(BG_COLOR);
        listPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel warningLabel = new JLabel("CAUTION: Running 2 versions of the same mod will cause crash on startup");
        warningLabel.setForeground(new Color(255, 80, 80));
        warningLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        warningLabel.setVisible(false);
        warningLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        List<JCheckBox> deleteChecks = new ArrayList<>();
        if (!toDelete.isEmpty()) {
            JLabel delLabel = new JLabel("Files to Delete:");
            delLabel.setForeground(new Color(250, 100, 100));
            delLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            delLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(delLabel);
            for (File f : toDelete) {
                JCheckBox cb = new JCheckBox(f.getName(), true);
                cb.setOpaque(false);
                cb.setForeground(FG_COLOR);
                cb.setFocusPainted(false);
                cb.setAlignmentX(Component.LEFT_ALIGNMENT);
                cb.addItemListener(e -> {
                    boolean anyUnchecked = false;
                    for (JCheckBox c : deleteChecks) {
                        if (!c.isSelected()) {
                            anyUnchecked = true;
                            break;
                        }
                    }
                    warningLabel.setVisible(anyUnchecked);
                });
                deleteChecks.add(cb);
                listPanel.add(cb);
            }
            listPanel.add(Box.createVerticalStrut(5));
            listPanel.add(warningLabel);
            listPanel.add(Box.createVerticalStrut(15));
        }

        List<JCheckBox> dlChecks = new ArrayList<>();
        if (!toDownload.isEmpty()) {
            JLabel dlLabel = new JLabel("Mods to Install / Update:");
            dlLabel.setForeground(new Color(100, 250, 100));
            dlLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            dlLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(dlLabel);
            for (ReleaseItem ri : toDownload) {
                JCheckBox cb = new JCheckBox(ri.fileName, true);
                cb.setOpaque(false);
                cb.setForeground(FG_COLOR);
                cb.setFocusPainted(false);
                cb.setAlignmentX(Component.LEFT_ALIGNMENT);
                cb.putClientProperty("item", ri);
                dlChecks.add(cb);
                listPanel.add(cb);
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_COLOR);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(BG_COLOR);
        JButton cancelBtn = new JButton("Cancel");
        styleButton(cancelBtn, TITLE_BG, BTN_GRAY);
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton confirmBtn = new JButton("Confirm");
        styleButton(confirmBtn, ACCENT, HOVER_ACCENT);
        confirmBtn.addActionListener(e -> {
            dialog.dispose();
            List<File> finalDelete = new ArrayList<>();
            for (int i = 0; i < deleteChecks.size(); i++) {
                if (deleteChecks.get(i).isSelected()) finalDelete.add(toDelete.get(i));
            }
            List<ReleaseItem> finalDownload = new ArrayList<>();
            for (JCheckBox cb : dlChecks) {
                if (cb.isSelected()) finalDownload.add((ReleaseItem) cb.getClientProperty("item"));
            }

            executeUpdates(modsFolder, finalDelete, finalDownload, progressBtn);
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(confirmBtn);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private static void executeUpdates(File modsFolder, List<File> toDelete, List<ReleaseItem> toDownload, ProgressButton btn) {
        btn.setEnabled(false);
        new Thread(() -> {
            for (File f : toDelete) {
                if (f.exists()) f.delete();
            }

            int total = toDownload.size();
            for (int i = 0; i < total; i++) {
                ReleaseItem ri = toDownload.get(i);
                final int current = i + 1;
                SwingUtilities.invokeLater(() -> {
                    btn.setText("Downloading " + current + "/" + total + "...");
                    btn.setProgress(0);
                });
                downloadFile(ri.downloadUrl, new File(modsFolder, ri.fileName), btn);
            }

            SwingUtilities.invokeLater(() -> {
                btn.setProgress(1.0);
                btn.setText("Finished Updates!");
                btn.setBgColor(ACCENT);
                JOptionPane.showMessageDialog(btn.getParent(), "Updates installed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                btn.setEnabled(true);
                btn.setProgress(0);
                btn.setText("Check for Updates");
                btn.setBgColor(BTN_GRAY);
            });
        }).start();
    }

    private static File getValidatedModsFolder(String path, JFrame frame) {
        if (path == null || path.trim().isEmpty()) {
            showError(frame, "Please select your mods folder.");
            return null;
        }
        File modsFolder = new File(path.trim());
        if (!modsFolder.exists() || !modsFolder.isDirectory()) {
            showError(frame, "The selected path does not exist or is not a directory.");
            return null;
        }
        if (!modsFolder.getName().equalsIgnoreCase("mods")) {
            showError(frame, "The folder must be named exactly 'mods'.");
            return null;
        }
        File parentFolder = modsFolder.getParentFile();
        if (parentFolder == null || !new File(parentFolder, "resourcepacks").isDirectory()) {
            showError(frame, "Could not find 'resourcepacks' folder in the parent directory.");
            return null;
        }
        saveConfig(modsFolder.getAbsolutePath());
        return modsFolder;
    }

    private static boolean downloadFile(String urlString, File dest, ProgressButton btn) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Aetheria-Installer");

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    String newUrl = conn.getHeaderField("Location");
                    conn = (HttpURLConnection) new URL(newUrl).openConnection();
                    conn.setRequestProperty("User-Agent", "Aetheria-Installer");
                }
            }

            long fileSize = conn.getContentLengthLong();
            try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream()); FileOutputStream fos = new FileOutputStream(dest)) {
                byte[] dataBuffer = new byte[8192];
                int bytesRead;
                long totalRead = 0;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fos.write(dataBuffer, 0, bytesRead);
                    totalRead += bytesRead;
                    if (fileSize > 0 && btn != null) {
                        double progress = (double) totalRead / fileSize;
                        int percent = (int) (progress * 100);
                        SwingUtilities.invokeLater(() -> {
                            btn.setProgress(progress);
                            btn.setText("Downloading... " + percent + "%");
                        });
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static List<ReleaseItem> fetchReleasesForMod(String modName) {
        if (cachedReleases.containsKey(modName)) {
            return cachedReleases.get(modName);
        }

        List<ReleaseItem> items = new ArrayList<>();
        try {
            String repoUrl = modLinks.get(modName);
            if (repoUrl == null) return items;

            String apiUrl = repoUrl.replace("github.com", "api.github.com/repos") + "/releases";
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("User-Agent", "Aetheria-Installer");

            InputStreamReader reader = new InputStreamReader(conn.getInputStream());
            JsonElement root = JsonParser.parseReader(reader);
            reader.close();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            if (root.isJsonArray()) {
                JsonArray array = root.getAsJsonArray();
                for (JsonElement elem : array) {
                    JsonObject obj = elem.getAsJsonObject();
                    String name = obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString() : "";
                    String publishedAt = obj.has("published_at") && !obj.get("published_at").isJsonNull() ? obj.get("published_at").getAsString() : "";

                    JsonArray assets = obj.has("assets") ? obj.getAsJsonArray("assets") : new JsonArray();
                    String downloadUrl = null;
                    String fileName = null;
                    for (JsonElement assetElem : assets) {
                        JsonObject asset = assetElem.getAsJsonObject();
                        String aName = asset.get("name").getAsString();
                        if (aName.endsWith(".jar")) {
                            downloadUrl = asset.get("browser_download_url").getAsString();
                            fileName = aName;
                            break;
                        }
                    }

                    if (downloadUrl != null && !publishedAt.isEmpty()) {
                        ReleaseItem item = new ReleaseItem();
                        item.name = name;
                        item.downloadUrl = downloadUrl;
                        item.fileName = fileName;
                        item.publishedAt = sdf.parse(publishedAt);

                        if (repoUrl.contains("aetheria-org/Aetheria")) {
                            boolean isJef = name.toLowerCase().contains("justenoughfakepixel");
                            if (modName.equals("JustEnoughFakepixel") && isJef) {
                                items.add(item);
                            } else if (modName.equals("Aetheria") && !isJef) {
                                items.add(item);
                            }
                        } else {
                            items.add(item);
                        }
                    }
                    if (items.size() >= 20 && !repoUrl.contains("aetheria-org/Aetheria")) break;
                }
            }
            Collections.sort(items);
            cachedReleases.put(modName, items);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return items;
    }

    private static void showError(JFrame parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static JButton createControlButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(45, 35));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        btn.setUI(new BasicButtonUI());
        btn.setBackground(ModInstaller.TITLE_BG);
        btn.setForeground(ModInstaller.FG_COLOR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn.getBackground().equals(ModInstaller.TITLE_BG)) btn.setBackground(new Color(60, 60, 60));
            }

            public void mouseExited(MouseEvent e) {
                if (btn.getBackground().equals(new Color(60, 60, 60))) btn.setBackground(ModInstaller.TITLE_BG);
            }
        });
        return btn;
    }

    private static void styleButton(JButton btn, Color bg, Color hoverBg) {
        btn.setUI(new BasicButtonUI());
        btn.setBackground(bg);
        btn.setForeground(ModInstaller.FG_COLOR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverBg);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });
    }

    private static JComboBox<String> createDarkComboBox(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setBackground(ModInstaller.INPUT_BG);
        combo.setForeground(ModInstaller.FG_COLOR);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setUI(new BasicComboBoxUI() {
            protected JButton createArrowButton() {
                JButton btn = super.createArrowButton();
                btn.setUI(new BasicButtonUI());
                btn.setBackground(new Color(50, 50, 50));
                btn.setForeground(ModInstaller.FG_COLOR);
                btn.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                btn.setOpaque(true);
                return btn;
            }
        });
        combo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ModInstaller.BORDER_COL), BorderFactory.createEmptyBorder(4, 5, 4, 5)));
        return combo;
    }

    static class ReleaseItem implements Comparable<ReleaseItem> {
        String name;
        String downloadUrl;
        String fileName;
        Date publishedAt;

        @Override
        public int compareTo(ReleaseItem o) {
            return o.publishedAt.compareTo(this.publishedAt);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class ProgressButton extends JButton {
        private final Color progressColor;
        public double progress = 0.0;
        private Color bgColor;

        public ProgressButton(String text, Color bg, Color progressCol, Color fg) {
            super(text);
            this.bgColor = bg;
            this.progressColor = progressCol;
            setForeground(fg);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setPreferredSize(new Dimension(0, 45));
        }

        public void setProgress(double p) {
            this.progress = Math.max(0.0, Math.min(1.0, p));
            repaint();
        }

        public void setBgColor(Color bg) {
            this.bgColor = bg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(bgColor);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (progress > 0) {
                g2.setColor(progressColor);
                g2.fillRect(0, 0, (int) (getWidth() * progress), getHeight());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class ComponentResizer extends MouseAdapter {
        public void registerComponent(Component... components) {
            for (Component component : components) {
                component.addMouseListener(this);
                component.addMouseMotionListener(this);
            }
        }
    }
}
