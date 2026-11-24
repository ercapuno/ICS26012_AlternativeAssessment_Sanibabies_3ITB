import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Entry point: starts the CPU and Disk Scheduling Simulator on the Swing event thread. */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Use the operating system's look (e.g. Windows buttons) when available.
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Not fatal: Swing keeps its default cross-platform look.
            }
            new MainMenu().setVisible(true);
        });
    }
}
