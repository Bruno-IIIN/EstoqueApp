import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;

public class EstoqueApp extends JFrame {

    private JTable tabela;
    private DefaultTableModel modelo;
    private JTextField txtProduto;
    private JTextField txtQuantidade;

    private final String ARQUIVO = "estoque.csv";

    public EstoqueApp() {
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
            JOptionPane.showMessageDialog(this, "Preencha todos os os campos.");
            return;
        }

        int quantidade;

        try {
            quantidade = Integer.parseInt(quantidadeTexto);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida.");
            return;
        }

        modelo.addRow(new Object[]{produto, quantidade});

        salvarDados();
        limparCampos();
    }

    private void entradaProduto() {
        int linha = tabela.getSelectedRow();
}
