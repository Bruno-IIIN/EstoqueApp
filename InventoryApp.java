import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InventoryApp extends JFrame {

    // Modelos das tabelas
    private DefaultTableModel itemsTableModel;
    private DefaultTableModel historyTableModel;
    private JTable itemsTable;
    private JTable historyTable;
    private JComboBox<String> itemCombo;
    private JTextField qtyField;

    // Arquivos de dados
    private static final String ITEMS_FILE = "itens.csv";
    private static final String HISTORY_FILE = "historico.csv";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Lista de itens em memória
    private List<Item> items;

    public InventoryApp() {
        setTitle("Controle de Estoque");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Carrega dados iniciais
        items = loadItems();

        // Abas
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Itens", createItemsPanel());
        tabbedPane.addTab("Movimentações", createMovementsPanel());
        tabbedPane.addTab("Histórico", createHistoryPanel());

        add(tabbedPane);
        refreshAll();
    }

    // ----------------------------------------------------------------
    //                         PAINÉIS
    // ----------------------------------------------------------------
    private JPanel createItemsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID", "Nome", "Quantidade"};
        itemsTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        itemsTable = new JTable(itemsTableModel);
        JScrollPane scroll = new JScrollPane(itemsTable);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAdd = new JButton("Adicionar Item");
        JButton btnRemove = new JButton("Remover Item");
        btnPanel.add(btnAdd);
        btnPanel.add(btnRemove);

        btnAdd.addActionListener(e -> showAddItemDialog());
        btnRemove.addActionListener(e -> removeSelectedItem());

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createMovementsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblItem = new JLabel("Item:");
        itemCombo = new JComboBox<>();
        itemCombo.setPreferredSize(new Dimension(200, 25));
        JLabel lblQty = new JLabel("Quantidade:");
        qtyField = new JTextField(10);
        JButton btnEntrada = new JButton("Entrada");
        JButton btnRetirada = new JButton("Retirada");

        gbc.gridx = 0; gbc.gridy = 0; panel.add(lblItem, gbc);
        gbc.gridx = 1; gbc.gridy = 0; panel.add(itemCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(lblQty, gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(qtyField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.add(btnEntrada);
        btnPanel.add(btnRetirada);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(btnPanel, gbc);

        btnEntrada.addActionListener(e -> realizarMovimentacao(true));
        btnRetirada.addActionListener(e -> realizarMovimentacao(false));

        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"Data/Hora", "Tipo", "Item", "Qtd. Alterada", "Detalhes"};
        historyTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        historyTable = new JTable(historyTableModel);
        JScrollPane scroll = new JScrollPane(historyTable);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ----------------------------------------------------------------
    //                    ATUALIZAÇÃO DA INTERFACE
    // ----------------------------------------------------------------
    private void refreshAll() {
        // Atualiza tabela de itens
        itemsTableModel.setRowCount(0);
        for (Item item : items) {
            itemsTableModel.addRow(new Object[]{item.id, item.name, item.quantity});
        }

        // Atualiza combo de itens (para movimentações)
        itemCombo.removeAllItems();
        for (Item item : items) {
            itemCombo.addItem(item.id + " - " + item.name);
        }

        // Atualiza histórico
        refreshHistory();
    }

    private void refreshHistory() {
        historyTableModel.setRowCount(0);
        List<HistoryEntry> history = loadHistory();
        for (HistoryEntry h : history) {
            historyTableModel.addRow(new Object[]{
                    h.timestamp,
                    formatType(h.type),
                    h.itemName,
                    (h.quantityChange >= 0 ? "+" : "") + h.quantityChange,
                    h.details
            });
        }
    }

    private String formatType(String type) {
        switch (type) {
            case "ITEM_ADICIONADO": return "Item adicionado";
            case "ITEM_REMOVIDO":   return "Item removido";
            case "ENTRADA":         return "Entrada";
            case "RETIRADA":        return "Retirada";
            default:                return type;
        }
    }

    // ----------------------------------------------------------------
    //                   OPERAÇÕES DE NEGÓCIO
    // ----------------------------------------------------------------
    private void showAddItemDialog() {
        JTextField nameField = new JTextField(15);
        JTextField initialQtyField = new JTextField("0", 5);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Nome do item:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; panel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Quantidade inicial:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(initialQtyField, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Novo Item",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "O nome não pode estar vazio.");
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(initialQtyField.getText().trim());
                if (qty < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Quantidade inválida.");
                return;
            }
            // Verifica duplicidade
            if (items.stream().anyMatch(i -> i.name.equalsIgnoreCase(name))) {
                JOptionPane.showMessageDialog(this, "Já existe um item com esse nome.");
                return;
            }
            // Gera novo ID
            int newId = items.isEmpty() ? 1 : items.get(items.size()-1).id + 1;
            Item newItem = new Item(newId, name, qty);
            items.add(newItem);
            saveItems();

            addHistory("ITEM_ADICIONADO", -1, name, qty,
                       "Item cadastrado com quantidade inicial " + qty);
            refreshAll();
        }
    }

    private void removeSelectedItem() {
        int row = itemsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um item para remover.");
            return;
        }
        int id = (int) itemsTableModel.getValueAt(row, 0);
        String name = (String) itemsTableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Remover o item \"" + name + "\"?\nEsta ação não pode ser desfeita.",
                "Confirmar remoção", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Item removed = items.stream().filter(i -> i.id == id).findFirst().orElse(null);
            if (removed != null) {
                items.remove(removed);
                saveItems();
                addHistory("ITEM_REMOVIDO", id, name, -removed.quantity,
                           "Item removido do cadastro");
                refreshAll();
            }
        }
    }

    private void realizarMovimentacao(boolean entrada) {
        String selected = (String) itemCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Nenhum item cadastrado.");
            return;
        }
        int id = Integer.parseInt(selected.split(" - ")[0]);
        String name = selected.split(" - ")[1];
        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida.");
            return;
        }

        Item item = items.stream().filter(i -> i.id == id).findFirst().orElse(null);
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Item não encontrado.");
            return;
        }

        int change = entrada ? qty : -qty;
        int newQty = item.quantity + change;
        if (newQty < 0) {
            JOptionPane.showMessageDialog(this, "Estoque insuficiente! Quantidade atual: " + item.quantity);
            return;
        }

        item.quantity = newQty;
        saveItems();

        String type = entrada ? "ENTRADA" : "RETIRADA";
        String detail = (entrada ? "Entrada" : "Retirada") + " de " + qty + " unidade(s)";
        addHistory(type, id, name, change, detail);

        qtyField.setText("");
        refreshAll();
    }

    // ----------------------------------------------------------------
    //                   PERSISTÊNCIA EM CSV
    // ----------------------------------------------------------------
    private List<Item> loadItems() {
        List<Item> list = new ArrayList<>();
        File file = new File(ITEMS_FILE);
        if (!file.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    int id = Integer.parseInt(parts[0]);
                    String name = parts[1];
                    int qty = Integer.parseInt(parts[2]);
                    list.add(new Item(id, name, qty));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private void saveItems() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ITEMS_FILE))) {
            for (Item item : items) {
                pw.println(item.id + "," + item.name + "," + item.quantity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addHistory(String type, int itemId, String itemName, int qtyChange, String details) {
        String timestamp = LocalDateTime.now().format(dtf);
        String line = timestamp + ";" + type + ";" + itemId + ";" + itemName + ";" + qtyChange + ";" + details;
        try (PrintWriter pw = new PrintWriter(new FileWriter(HISTORY_FILE, true))) {
            pw.println(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<HistoryEntry> loadHistory() {
        List<HistoryEntry> list = new ArrayList<>();
        File file = new File(HISTORY_FILE);
        if (!file.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";", 6);
                if (parts.length >= 6) {
                    list.add(new HistoryEntry(
                            parts[0], // timestamp
                            parts[1], // type
                            parts[3], // itemName (pula itemId)
                            Integer.parseInt(parts[4]), // quantityChange
                            parts[5]  // details
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // ----------------------------------------------------------------
    //                  CLASSES INTERNAS DE MODELO
    // ----------------------------------------------------------------
    static class Item {
        int id;
        String name;
        int quantity;

        Item(int id, String name, int quantity) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
        }
    }

    static class HistoryEntry {
        String timestamp;
        String type;
        String itemName;
        int quantityChange;
        String details;

        HistoryEntry(String timestamp, String type, String itemName, int quantityChange, String details) {
            this.timestamp = timestamp;
            this.type = type;
            this.itemName = itemName;
            this.quantityChange = quantityChange;
            this.details = details;
        }
    }

    // ----------------------------------------------------------------
    //                         MAIN
    // ----------------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Define um visual mais agradável (Nimbus, disponível no JDK)
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                // fallback ao padrão
            }
            new InventoryApp().setVisible(true);
        });
    }
}
