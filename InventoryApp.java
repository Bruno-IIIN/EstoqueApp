import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;

public class InventoryApp extends JFrame {

    private JTable tabela;
    private DefaultTableModel modelo;
    private JTextField txtProduto;
    private JTextField txtQuantidade;

    private final String ARQUIVO =
            System.getProperty("user.home") +
            "\\estoque.csv";

    public InventoryApp() {

        setTitle("Controle de Estoque");
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel painelTopo = new JPanel(new GridLayout(2, 2, 10, 10));

        txtProduto = new JTextField();
        txtQuantidade = new JTextField();

        painelTopo.add(new JLabel("Produto:"));
        painelTopo.add(txtProduto);

        painelTopo.add(new JLabel("Quantidade:"));
        painelTopo.add(txtQuantidade);

        add(painelTopo, BorderLayout.NORTH);

        modelo = new DefaultTableModel();

        modelo.addColumn("Produto");
        modelo.addColumn("Quantidade");

        tabela = new JTable(modelo);

        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel();

        JButton btnAdicionar = new JButton("Adicionar Produto");
        JButton btnEntrada = new JButton("Entrada");
        JButton btnSaida = new JButton("Saída");
        JButton btnExcluir = new JButton("Excluir Produto");

        painelBotoes.add(btnAdicionar);
        painelBotoes.add(btnEntrada);
        painelBotoes.add(btnSaida);
        painelBotoes.add(btnExcluir);

        add(painelBotoes, BorderLayout.SOUTH);

        btnAdicionar.addActionListener(e -> adicionarProduto());
        btnEntrada.addActionListener(e -> entradaProduto());
        btnSaida.addActionListener(e -> saidaProduto());
        btnExcluir.addActionListener(e -> excluirProduto());

        carregarDados();
    }

    private void adicionarProduto() {

        String produto = txtProduto.getText().trim();
        String quantidadeTexto = txtQuantidade.getText().trim();

        if (produto.isEmpty() || quantidadeTexto.isEmpty()) {

            JOptionPane.showMessageDialog(this,
                    "Preencha todos os campos.");

            return;
        }

        int quantidade;

        try {

            quantidade = Integer.parseInt(quantidadeTexto);

        } catch (Exception e) {

            JOptionPane.showMessageDialog(this,
                    "Quantidade inválida.");

            return;
        }

        modelo.addRow(new Object[]{produto, quantidade});

        salvarDados();

        limparCampos();
    }

    private void entradaProduto() {

        int linha = tabela.getSelectedRow();

        if (linha == -1) {

            JOptionPane.showMessageDialog(this,
                    "Selecione um produto.");

            return;
        }

        int quantidadeAtual =
                Integer.parseInt(
                        modelo.getValueAt(linha, 1).toString()
                );

        int entrada;

        try {

            entrada = Integer.parseInt(
                    txtQuantidade.getText()
            );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(this,
                    "Quantidade inválida.");

            return;
        }

        quantidadeAtual += entrada;

        modelo.setValueAt(quantidadeAtual, linha, 1);

        salvarDados();

        limparCampos();
    }

    private void saidaProduto() {

        int linha = tabela.getSelectedRow();

        if (linha == -1) {

            JOptionPane.showMessageDialog(this,
                    "Selecione um produto.");

            return;
        }

        int quantidadeAtual =
                Integer.parseInt(
                        modelo.getValueAt(linha, 1).toString()
                );

        int saida;

        try {

            saida = Integer.parseInt(
                    txtQuantidade.getText()
            );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(this,
                    "Quantidade inválida.");

            return;
        }

        if (saida > quantidadeAtual) {

            JOptionPane.showMessageDialog(this,
                    "Estoque insuficiente.");

            return;
        }

        quantidadeAtual -= saida;

        modelo.setValueAt(quantidadeAtual, linha, 1);

        salvarDados();

        limparCampos();
    }

    private void excluirProduto() {

        int linha = tabela.getSelectedRow();

        if (linha == -1) {

            JOptionPane.showMessageDialog(this,
                    "Selecione um produto.");

            return;
        }

        modelo.removeRow(linha);

        salvarDados();
    }

    private void limparCampos() {

        txtProduto.setText("");
        txtQuantidade.setText("");
    }

    private void salvarDados() {

        try {

            PrintWriter writer =
                    new PrintWriter(
                            new FileWriter(ARQUIVO)
                    );

            for (int i = 0;
                 i < modelo.getRowCount();
                 i++) {

                String produto =
                        modelo.getValueAt(i, 0).toString();

                String quantidade =
                        modelo.getValueAt(i, 1).toString();

                writer.println(
                        produto + ";" + quantidade
                );
            }

            writer.close();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar.");
        }
    }

    private void carregarDados() {

        try {

            File arquivo = new File(ARQUIVO);

            if (!arquivo.exists()) {
                return;
            }

            BufferedReader reader =
                    new BufferedReader(
                            new FileReader(arquivo)
                    );

            String linha;

            while ((linha = reader.readLine()) != null) {

                String[] dados = linha.split(";");

                modelo.addRow(new Object[]{
                        dados[0],
                        dados[1]
                });
            }

            reader.close();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar dados.");
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {

                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );

            } catch (Exception e) {
                e.printStackTrace();
            }

            new InventoryApp().setVisible(true);
        });
    }
}
