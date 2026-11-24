import clook.CLookPanel;
import gui.ModuleLauncher;
import multilevelqueue.MLQPanel;
import prioritynp.PriorityNPPanel;
import sstf.SstfPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Main menu. Each algorithm gets one row: its own button and a matching Sample button.
 *
 * To plug in a finished module, replace UNDER_DEVELOPMENT in its addAlgorithmRow(...) call
 * with the module's launcher, e.g. SstfPanel::openWindow. Nothing else in this class changes.
 */
public class MainMenu extends JFrame {

    // ---- Edit these for the About dialog ----
    private static final String[] MEMBERS = {
        "Jayvi Al Andres (C-LOOK)",
        "Erwin G. Capuno (Priority Non-Preemptive)",
        "Ranz Cyrus Cutarra (Multilevel Queue)",
        "Wynn Benedict De Villa (SSTF)",
    };

    private static final String APP_TITLE = "SaniBabies - CPU and Disk Scheduling Simulator";

    /** Stand-in for modules that are not built yet. */
    private static final ModuleLauncher UNDER_DEVELOPMENT = (menu, withSample) ->
            JOptionPane.showMessageDialog(menu, "This module is under development.",
                    APP_TITLE, JOptionPane.INFORMATION_MESSAGE);

    public MainMenu() {
        super(APP_TITLE);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // the X button asks first, like Exit
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        JPanel cpuSection = createSection("CPU Scheduling");
        addAlgorithmRow(cpuSection, "Priority (Non-Preemptive)", "Priority (Non-Preemptive) Sample",
                PriorityNPPanel::openWindow);
        addAlgorithmRow(cpuSection, "Multilevel Queue", "Multilevel Queue Sample",
                MLQPanel::openWindow);

        JPanel diskSection = createSection("Disk Scheduling");
        addAlgorithmRow(diskSection, "Shortest Seek Time First (SSTF)", "SSTF Sample", SstfPanel::openWindow);
        addAlgorithmRow(diskSection, "C-LOOK", "C-LOOK Sample", CLookPanel::openWindow);

        JLabel title = new JLabel(APP_TITLE, SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setAlignmentX(CENTER_ALIGNMENT);

        JButton aboutButton = new JButton("About");
        JButton exitButton = new JButton("Exit");
        aboutButton.addActionListener(e -> showAbout());
        exitButton.addActionListener(e -> confirmExit());
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        bottomRow.add(aboutButton);
        bottomRow.add(exitButton);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        content.add(title);
        content.add(Box.createVerticalStrut(14));
        content.add(cpuSection);
        content.add(Box.createVerticalStrut(10));
        content.add(diskSection);
        content.add(Box.createVerticalStrut(14));
        content.add(bottomRow);

        setContentPane(new JPanel(new BorderLayout()));
        getContentPane().add(content, BorderLayout.CENTER);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private static JPanel createSection(String heading) {
        JPanel section = new JPanel(new GridLayout(0, 2, 10, 8));
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(heading),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));
        return section;
    }

    /** Adds the algorithm button (opens empty) and its Sample button (opens with sample computed). */
    private void addAlgorithmRow(JPanel section, String name, String sampleName, ModuleLauncher launcher) {
        JButton openButton = new JButton(name);
        JButton sampleButton = new JButton(sampleName);
        openButton.addActionListener(e -> launcher.open(this, false));
        sampleButton.addActionListener(e -> launcher.open(this, true));
        section.add(openButton);
        section.add(sampleButton);
    }

    private void showAbout() {
        StringBuilder text = new StringBuilder();
        text.append(APP_TITLE).append("\n\n");
        text.append("Group SaniBabies\n\n");
        text.append("Members:\n");
        for (String member : MEMBERS) {
            text.append("   ").append(member).append("\n");
        }
        JOptionPane.showMessageDialog(this, text.toString(), "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private void confirmExit() {
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?",
                "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
}
