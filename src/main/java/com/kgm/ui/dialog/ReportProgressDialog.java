package com.kgm.ui.dialog;

import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;

public class ReportProgressDialog extends JDialog {
    public ReportProgressDialog(Window owner) {
        super(owner, "Generating Report", ModalityType.MODELESS);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(content());
        setSize(430, 176);
        setLocationRelativeTo(owner);
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HomeViewHelper.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JLabel title = new JLabel("Generating Report");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JLabel state = new JLabel("Preparing A4 report and writing the document...");
        state.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        state.setForeground(HomeViewHelper.TEXT_SECONDARY);
        state.setAlignmentX(Component.LEFT_ALIGNMENT);
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setPreferredSize(new Dimension(370, 16));
        body.add(state);
        body.add(Box.createVerticalStrut(14));
        body.add(progress);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        return root;
    }
}
