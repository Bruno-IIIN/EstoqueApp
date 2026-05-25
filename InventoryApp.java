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

    // Componentes da aba Estoque
    private JTable tabelaEstoque;
    private DefaultTableModel modeloEstoque;
    private JTextField txtProduto;
    private JTextField txtQuantidade;

    // Componentes da aba Histórico
    private JTable tabelaHistorico;
    private DefaultTableModel modeloHistorico;

    // Arquivos
    private final String ARQUIVO_ESTOQUE = System.getProperty("user.home") + "/estoque.csv";
    private final String ARQUIVO_HISTORICO = System.getProperty("user.home") + "/historico.csv";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public InventoryApp() {
        setTitle("Controle de Estoque");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Painel principal com abas
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Estoque", criarPainelEstoque());
        tabbedPane.addTab("Histórico", criarPainelHistorico());
        add(tabbedPane, BorderLayout.CENTER);

        // Painel de botões de relatório (inferior)
        JPanel painelRelatorios = new JPanel();
        JButton btnPDFEstoque = new JButton("Gerar PDF - Estoque");
        JButton btnPDFHistorico = new JButton("Gerar PDF - Histórico");
        painelRelatorios.add(btnPDFEstoque);
        painelRelatorios.add(btnPDFHistorico);
        add(painelRelatorios, BorderLayout.SOUTH);

        // Eventos dos botões de PDF
        btnPDFEstoque.addActionListener(e -> gerarPDFEstoque());
        btnPDFHistorico.addActionListener(e -> gerarPDFHistorico());

        // Carregar dados ao iniciar
        carregarEstoque();
        carregarHistorico();
    }

    // -------------------------------------------------------
    // PAINEL ESTOQUE (com campos e botões originais)
    // -------------------------------------------------------
    private JPanel criarPainelEstoque() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));

        // Topo: campos
        JPanel painelTopo = new JPanel(new GridLayout(2, 2, 10, 10));
        txtProduto = new JTextField();
        txtQuantidade = new JTextField();
        painelTopo.add(new JLabel("Produto:"));
        painelTopo.add(txtProduto);
        painelTopo.add(new JLabel("Quantidade:"));
        painelTopo.add(txtQuantidade);
        painel.add(painelTopo, BorderLayout.NORTH);

        // Centro: tabela
        modeloEstoque = new DefaultTableModel();
        modeloEstoque.addColumn("Produto");
        modeloEstoque.addColumn("Quantidade");
        tabelaEstoque = new JTable(modeloEstoque);
        painel.add(new JScrollPane(tabelaEstoque), BorderLayout.CENTER);

        // Inferior: botões de ação
        JPanel painelBotoes = new JPanel();
        JButton btnAdicionar = new JButton("Adicionar Produto");
        JButton btnEntrada = new JButton("Entrada");
        JButton btnSaida = new JButton("Saída");
        JButton btnExcluir = new JButton("Excluir Produto");
        painelBotoes.add(btnAdicionar);
        painelBotoes.add(btnEntrada);
        painelBotoes.add(btnSaida);
        painelBotoes.add(btnExcluir);
        painel.add(painelBotoes, BorderLayout.SOUTH);

        // Ações
        btnAdicionar.addActionListener(e -> adicionarProduto());
        btnEntrada.addActionListener(e -> entradaProduto());
        btnSaida.addActionListener(e -> saidaProduto());
        btnExcluir.addActionListener(e -> excluirProduto());

        return painel;
    }

    // -------------------------------------------------------
    // PAINEL HISTÓRICO (tabela)
    // -------------------------------------------------------
    private JPanel criarPainelHistorico() {
        JPanel painel = new JPanel(new BorderLayout());
        modeloHistorico = new DefaultTableModel();
        modeloHistorico.addColumn("Data/Hora");
        modeloHistorico.addColumn("Tipo");
        modeloHistorico.addColumn("Produto");
        modeloHistorico.addColumn("Qtd. Alterada");
        modeloHistorico.addColumn("Detalhes");
        tabelaHistorico = new JTable(modeloHistorico);
        painel.add(new JScrollPane(tabelaHistorico), BorderLayout.CENTER);
        return painel;
    }

    // -------------------------------------------------------
    // MÉTODOS DE NEGÓCIO (com registro de histórico)
    // -------------------------------------------------------
    private void adicionarProduto() {
        String produto = txtProduto.getText().trim();
        String qtdTexto = txtQuantidade.getText().trim();
        if (produto.isEmpty() || qtdTexto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha todos os campos.");
            return;
        }
        int quantidade;
        try {
            quantidade = Integer.parseInt(qtdTexto);
            if (quantidade < 0) throw new NumberFormatException();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida.");
            return;
        }
        // Verificar duplicata (opcional)
        for (int i = 0; i < modeloEstoque.getRowCount(); i++) {
            if (modeloEstoque.getValueAt(i, 0).toString().equalsIgnoreCase(produto)) {
                JOptionPane.showMessageDialog(this, "Produto já cadastrado.");
                return;
            }
        }
        modeloEstoque.addRow(new Object[]{produto, quantidade});
        salvarEstoque();
        registrarHistorico("Adição", produto, quantidade, "Produto adicionado ao estoque.");
        limparCampos();
    }

    private void entradaProduto() {
        int linha = tabelaEstoque.getSelectedRow();
        if (linha == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um produto na tabela.");
            return;
        }
        String produto = modeloEstoque.getValueAt(linha, 0).toString();
        int atual = Integer.parseInt(modeloEstoque.getValueAt(linha, 1).toString());
        int entrada;
        try {
            entrada = Integer.parseInt(txtQuantidade.getText().trim());
            if (entrada <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida.");
            return;
        }
        int novo = atual + entrada;
        modeloEstoque.setValueAt(novo, linha, 1);
        salvarEstoque();
        registrarHistorico("Entrada", produto, entrada, "Entrada de " + entrada + " unidade(s). Estoque atual: " + novo);
        limparCampos();
    }

    private void saidaProduto() {
        int linha = tabelaEstoque.getSelectedRow();
        if (linha == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um produto na tabela.");
            return;
        }
        String produto = modeloEstoque.getValueAt(linha, 0).toString();
        int atual = Integer.parseInt(modeloEstoque.getValueAt(linha, 1).toString());
        int saida;
        try {
            saida = Integer.parseInt(txtQuantidade.getText().trim());
            if (saida <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida.");
            return;
        }
        if (saida > atual) {
            JOptionPane.showMessageDialog(this, "Estoque insuficiente. Quantidade atual: " + atual);
            return;
        }
        int novo = atual - saida;
        modeloEstoque.setValueAt(novo, linha, 1);
        salvarEstoque();
        registrarHistorico("Saída", produto, saida, "Saída de " + saida + " unidade(s). Estoque atual: " + novo);
        limparCampos();
    }

    private void excluirProduto() {
        int linha = tabelaEstoque.getSelectedRow();
        if (linha == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um produto para excluir.");
            return;
        }
        String produto = modeloEstoque.getValueAt(linha, 0).toString();
        int quantidade = Integer.parseInt(modeloEstoque.getValueAt(linha, 1).toString());
        int confirm = JOptionPane.showConfirmDialog(this,
                "Excluir \"" + produto + "\"? Esta ação não pode ser desfeita.",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            modeloEstoque.removeRow(linha);
            salvarEstoque();
            registrarHistorico("Exclusão", produto, quantidade,
                    "Produto removido do cadastro. Quantidade descartada: " + quantidade);
        }
    }

    private void limparCampos() {
        txtProduto.setText("");
        txtQuantidade.setText("");
    }

    // -------------------------------------------------------
    // REGISTRO E CARREGAMENTO DO HISTÓRICO
    // -------------------------------------------------------
    private void registrarHistorico(String tipo, String produto, int quantidade, String detalhes) {
        String dataHora = LocalDateTime.now().format(formatter);
        // Formato CSV: data;tipo;produto;quantidade;detalhes
        String linha = dataHora + ";" + tipo + ";" + produto + ";" + quantidade + ";" + detalhes;
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARQUIVO_HISTORICO, true))) {
            pw.println(linha);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar histórico: " + e.getMessage());
        }
        // Atualiza tabela de histórico (se estiver visível)
        modeloHistorico.addRow(new Object[]{dataHora, tipo, produto, quantidade, detalhes});
    }

    private void carregarHistorico() {
        modeloHistorico.setRowCount(0);
        File arquivo = new File(ARQUIVO_HISTORICO);
        if (!arquivo.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split(";", 5);
                if (partes.length >= 5) {
                    modeloHistorico.addRow(new Object[]{
                            partes[0], partes[1], partes[2],
                            Integer.parseInt(partes[3]), partes[4]
                    });
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar histórico: " + e.getMessage());
        }
    }

    // -------------------------------------------------------
    // PERSISTÊNCIA DO ESTOQUE (CSV)
    // -------------------------------------------------------
    private void salvarEstoque() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARQUIVO_ESTOQUE))) {
            for (int i = 0; i < modeloEstoque.getRowCount(); i++) {
                String produto = modeloEstoque.getValueAt(i, 0).toString();
                String quantidade = modeloEstoque.getValueAt(i, 1).toString();
                pw.println(produto + ";" + quantidade);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar estoque.");
        }
    }

    private void carregarEstoque() {
        modeloEstoque.setRowCount(0);
        File arquivo = new File(ARQUIVO_ESTOQUE);
        if (!arquivo.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split(";");
                if (partes.length >= 2) {
                    modeloEstoque.addRow(new Object[]{partes[0], Integer.parseInt(partes[1])});
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar estoque.");
        }
    }

    // -------------------------------------------------------
    // GERAÇÃO DE PDF (PURO JAVA)
    // -------------------------------------------------------
    private void gerarPDFEstoque() {
        List<String[]> dados = new ArrayList<>();
        dados.add(new String[]{"Produto", "Quantidade"});
        for (int i = 0; i < modeloEstoque.getRowCount(); i++) {
            String prod = modeloEstoque.getValueAt(i, 0).toString();
            String qtd = modeloEstoque.getValueAt(i, 1).toString();
            dados.add(new String[]{prod, qtd});
        }
        String titulo = "Relatório de Estoque - " + LocalDateTime.now().format(formatter);
        gerarPDF(titulo, dados, "relatorio_estoque.pdf");
    }

    private void gerarPDFHistorico() {
        List<String[]> dados = new ArrayList<>();
        dados.add(new String[]{"Data/Hora", "Tipo", "Produto", "Qtd. Alterada", "Detalhes"});
        for (int i = 0; i < modeloHistorico.getRowCount(); i++) {
            String data = modeloHistorico.getValueAt(i, 0).toString();
            String tipo = modeloHistorico.getValueAt(i, 1).toString();
            String prod = modeloHistorico.getValueAt(i, 2).toString();
            String qtd = modeloHistorico.getValueAt(i, 3).toString();
            String det = modeloHistorico.getValueAt(i, 4).toString();
            dados.add(new String[]{data, tipo, prod, qtd, det});
        }
        String titulo = "Relatório de Movimentações - " + LocalDateTime.now().format(formatter);
        gerarPDF(titulo, dados, "relatorio_historico.pdf");
    }

    private void gerarPDF(String titulo, List<String[]> dados, String nomeArquivo) {
        // Salva na área de trabalho ou diretório do usuário
        String caminho = System.getProperty("user.home") + "/" + nomeArquivo;
        try (FileOutputStream fos = new FileOutputStream(caminho)) {
            PDFWriter writer = new PDFWriter(fos);
            writer.addTitle(titulo);
            writer.addTable(dados);
            writer.close();
            JOptionPane.showMessageDialog(this, "PDF gerado em:\n" + caminho);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =================================================================
    // CLASSE INTERNA PARA ESCREVER PDF SIMPLES (puro Java)
    // =================================================================
    static class PDFWriter {
        private OutputStream out;
        private int objCount = 0;
        private ByteArrayOutputStream pageContent;
        private int pageWidth = 595; // A4
        private int pageHeight = 842;
        private int margin = 50;
        private int currentY;

        public PDFWriter(OutputStream out) {
            this.out = out;
            pageContent = new ByteArrayOutputStream();
            currentY = pageHeight - margin;
        }

        public void addTitle(String title) throws IOException {
            writeText(title, margin, currentY, 18);
            currentY -= 30;
        }

        public void addTable(List<String[]> rows) throws IOException {
            int fontSize = 12;
            int rowHeight = 20;
            int[] colWidths = calculateColWidths(rows);
            int x = margin;
            int y = currentY;

            // Desenhar cabeçalho (primeira linha)
            if (!rows.isEmpty()) {
                String[] header = rows.get(0);
                for (int i = 0; i < header.length; i++) {
                    writeText(header[i], x, y, fontSize, true);
                    x += colWidths[i];
                }
                y -= rowHeight;
                // Linha horizontal
                drawLine(margin, y + 5, pageWidth - margin, y + 5);
                y -= 5;
            }

            // Demais linhas
            for (int r = 1; r < rows.size(); r++) {
                x = margin;
                String[] row = rows.get(r);
                for (int c = 0; c < row.length; c++) {
                    writeText(row[c], x, y, fontSize, false);
                    x += colWidths[c];
                }
                y -= rowHeight;
                if (y < margin + 50) {
                    // Nova página simples (não implementada totalmente, apenas quebra)
                    break;
                }
            }
            currentY = y;
        }

        private int[] calculateColWidths(List<String[]> rows) {
            int cols = 0;
            for (String[] r : rows) if (r.length > cols) cols = r.length;
            int[] widths = new int[cols];
            // Distribui uniformemente
            int available = pageWidth - 2 * margin;
            int each = available / cols;
            for (int i = 0; i < cols; i++) widths[i] = each;
            return widths;
        }

        private void writeText(String text, int x, int y, int fontSize) throws IOException {
            writeText(text, x, y, fontSize, false);
        }

        private void writeText(String text, int x, int y, int fontSize, boolean bold) throws IOException {
            // PDF simples: usa fonte Helvetica (padrão)
            String font = bold ? "/F1" : "/F2"; // Vamos definir F1=Helv-Bold, F2=Helv
            String escaped = escapePDF(text);
            String line = "BT " + font + " " + fontSize + " Tf " + x + " " + y + " Td (" + escaped + ") Tj ET\n";
            pageContent.write(line.getBytes("ISO-8859-1"));
        }

        private void drawLine(int x1, int y1, int x2, int y2) throws IOException {
            String line = x1 + " " + y1 + " m " + x2 + " " + y2 + " l S\n";
            pageContent.write(line.getBytes());
        }

        private String escapePDF(String text) {
            // Substitui caracteres especiais por sequências octais ou escapadas
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (c == '(' || c == ')' || c == '\\') {
                    sb.append("\\").append(c);
                } else if (c < 32 || c > 126) {
                    // acentos e outros: usar octal
                    sb.append("\\").append(Integer.toOctalString(c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        public void close() throws IOException {
            // Escrever estrutura do PDF
            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            int pageContentObj = 1;
            int font1Obj = 2; // Helvetica Bold
            int font2Obj = 3; // Helvetica
            int pageObj = 4;
            int catalogObj = 5;
            int pagesObj = 6;

            // Fonte 1 (Bold)
            String font1 = font1Obj + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n";
            // Fonte 2 (Normal)
            String font2 = font2Obj + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n";

            // Conteúdo da página
            String content = pageContentObj + " 0 obj\n<< /Length " + pageContent.size() + " >>\nstream\n" +
                    pageContent.toString("ISO-8859-1") + "\nendstream\nendobj\n";

            // Página
            String page = pageObj + " 0 obj\n<< /Type /Page /Parent " + pagesObj + " 0 R /MediaBox [0 0 " +
                    pageWidth + " " + pageHeight + "] /Contents " + pageContentObj + " 0 R " +
                    "/Resources << /Font << /F1 " + font1Obj + " 0 R /F2 " + font2Obj + " 0 R >> >> >>\nendobj\n";

            // Pages
            String pages = pagesObj + " 0 obj\n<< /Type /Pages /Kids [" + pageObj + " 0 R] /Count 1 >>\nendobj\n";

            // Catalog
            String catalog = catalogObj + " 0 obj\n<< /Type /Catalog /Pages " + pagesObj + " 0 R >>\nendobj\n";

            // Montar PDF
            pdf.write("%PDF-1.4\n".getBytes());
            pdf.write(content.getBytes("ISO-8859-1"));
            pdf.write(font1.getBytes());
            pdf.write(font2.getBytes());
            pdf.write(page.getBytes());
            pdf.write(pages.getBytes());
            pdf.write(catalog.getBytes());

            // Cross-reference table e trailer
            int[] objNumbers = {pageContentObj, font1Obj, font2Obj, pageObj, pagesObj, catalogObj};
            long[] offsets = new long[objNumbers.length];
            ByteArrayOutputStream temp = new ByteArrayOutputStream();
            // ordem de objetos no PDF: content, font1, font2, page, pages, catalog
            // calcular offsets
            // Header
            long pos = 0;
            String header = "%PDF-1.4\n";
            temp.write(header.getBytes());
            pos += header.length();

            offsets[0] = pos;
            temp.write(content.getBytes("ISO-8859-1"));
            pos += content.getBytes("ISO-8859-1").length;

            offsets[1] = pos;
            temp.write(font1.getBytes());
            pos += font1.getBytes().length;

            offsets[2] = pos;
            temp.write(font2.getBytes());
            pos += font2.getBytes().length;

            offsets[3] = pos;
            temp.write(page.getBytes());
            pos += page.getBytes().length;

            offsets[4] = pos;
            temp.write(pages.getBytes());
            pos += pages.getBytes().length;

            offsets[5] = pos;
            temp.write(catalog.getBytes());
            pos += catalog.getBytes().length;

            // Cross-reference table
            String xref = "xref\n0 " + (objNumbers.length + 1) + "\n0000000000 65535 f \n";
            for (int i = 0; i < objNumbers.length; i++) {
                xref += String.format("%010d", offsets[i]) + " 00000 n \n";
            }
            temp.write(xref.getBytes());

            String trailer = "trailer\n<< /Size " + (objNumbers.length + 1) + " /Root " + catalogObj + " 0 R >>\nstartxref\n" + pos + "\n%%EOF";
            temp.write(trailer.getBytes());

            out.write(temp.toByteArray());
            out.close();
        }
    }

    // =================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new InventoryApp().setVisible(true);
        });
    }
}
