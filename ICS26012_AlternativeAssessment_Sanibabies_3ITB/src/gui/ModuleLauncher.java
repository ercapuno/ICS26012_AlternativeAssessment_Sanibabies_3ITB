package gui;

import javax.swing.JFrame;

/**
 * How the main menu opens a module. Each algorithm module provides one of these
 * (usually a method reference such as PriorityNPPanel::openWindow), so adding a
 * module to the menu is a one-line change there.
 */
public interface ModuleLauncher {

    /**
     * @param menu       the main menu window; the module hides it and shows it again when closed
     * @param sampleView true to show the fixed sample (loaded, computed, read-only);
     *                   false for the normal editable module
     */
    void open(JFrame menu, boolean sampleView);
}
