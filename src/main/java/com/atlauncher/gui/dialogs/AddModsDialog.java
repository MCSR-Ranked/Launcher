/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.Instance;
import com.atlauncher.data.ModPlatform;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.data.modcheck.ModCheckManager;
import com.atlauncher.data.modcheck.ModCheckProject;
import com.atlauncher.data.modcheck.ModCheckSearchHitCard;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.panels.LoadingPanel;
import com.atlauncher.gui.panels.NoCurseModsPanel;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.utils.ComboItem;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.google.common.collect.Lists;

@SuppressWarnings("serial")
public final class AddModsDialog extends JDialog {
    private final Instance instance;

    private boolean updating = false;

    private final JPanel contentPanel = new JPanel(new WrapLayout());
    private final JPanel topPanel = new JPanel(new BorderLayout());
    private final JTextField searchField = new JTextField(16);
    private final JComboBox<ComboItem<ModPlatform>> hostComboBox = new JComboBox<ComboItem<ModPlatform>>();

    private JScrollPane jscrollPane;
    private JButton nextButton;
    private JButton prevButton;
    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private int page = 0;

    public AddModsDialog(Instance instance) {
        this(App.launcher.getParent(), instance);
    }

    public AddModsDialog(Window parent, Instance instance) {
        // #. {0} is the name of the mod we're installing
        super(parent, GetText.tr("Adding Mods For {0}", instance.launcher.name), ModalityType.DOCUMENT_MODAL);
        this.instance = instance;

        this.setPreferredSize(new Dimension(800, 500));
        this.setMinimumSize(new Dimension(800, 500));
        this.setResizable(true);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        hostComboBox.addItem(new ComboItem<>(ModPlatform.MODCHECK, "ModCheck"));

        searchField.putClientProperty("JTextField.placeholderText", GetText.tr("Search"));
        searchField.putClientProperty("JTextField.leadingIcon", new FlatSearchIcon());
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.putClientProperty("JTextField.clearCallback", (Runnable) () -> {
            searchField.setText("");
            searchForMods();
        });

        setupComponents();

        this.loadDefaultMods();

        this.pack();
        this.setLocationRelativeTo(parent);
        this.setVisible(true);
    }

    private void setupComponents() {
        this.topPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JPanel searchButtonsPanel = new JPanel();

        searchButtonsPanel.setLayout(new BoxLayout(searchButtonsPanel, BoxLayout.X_AXIS));
        searchButtonsPanel.add(this.hostComboBox);
        searchButtonsPanel.add(Box.createHorizontalStrut(20));
        searchButtonsPanel.add(this.searchField);

        LoaderVersion loaderVersion = this.instance.launcher.loaderVersion;

        this.topPanel.add(searchButtonsPanel, BorderLayout.NORTH);

        this.jscrollPane = new JScrollPane(this.contentPanel) {
            {
                this.getVerticalScrollBar().setUnitIncrement(16);
            }
        };

        this.jscrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        mainPanel.add(this.topPanel, BorderLayout.NORTH);
        mainPanel.add(this.jscrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel bottomButtonsPanel = new JPanel(new FlowLayout());

        prevButton = new JButton("<<");
        prevButton.setEnabled(false);
        prevButton.addActionListener(e -> goToPreviousPage());

        nextButton = new JButton(">>");
        nextButton.setEnabled(false);
        nextButton.addActionListener(e -> goToNextPage());

        bottomButtonsPanel.add(prevButton);
        bottomButtonsPanel.add(nextButton);

        bottomPanel.add(bottomButtonsPanel, BorderLayout.CENTER);

        this.add(mainPanel, BorderLayout.CENTER);
        this.add(bottomPanel, BorderLayout.SOUTH);

        this.hostComboBox.addActionListener(e -> {
            updating = true;
            page = 0;

            if (searchField.getText().isEmpty()) {
                loadDefaultMods();
            } else {
                searchForMods();
            }
            updating = false;
        });

        this.searchField.addActionListener(e -> searchForMods());
    }

    private void setLoading(boolean loading) {
        if (loading) {
            contentPanel.removeAll();
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(new LoadingPanel(), BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }

    private void goToPreviousPage() {
        if (page > 0) {
            page -= 1;
        }

        getMods();
    }

    private void goToNextPage() {
        if (contentPanel.getComponentCount() != 0) {
            page += 1;
        }

        getMods();
    }

    @SuppressWarnings("unchecked")
    private void getMods() {
        setLoading(true);
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);

        String query = searchField.getText();

        new Thread(() -> {
            setModCheckSearch(this.instance.getMinecraftVersion(), query);

            setLoading(false);
        }).start();
    }

    private void loadDefaultMods() {
        getMods();
    }

    private void searchForMods() {
        String query = searchField.getText();

        page = 0;

        getMods();
    }

    private void setModCheckSearch(String mcVersion, String str) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.set(2, 2, 2, 2);

        contentPanel.removeAll();

        List<ModCheckProject> searchResult = Lists.newArrayList();
        for (ModCheckProject availableMod : ModCheckManager.getAvailableMods(mcVersion)) {
            if (availableMod.getName().toLowerCase(Locale.ROOT).contains(str.toLowerCase(Locale.ROOT)))
                searchResult.add(availableMod);
        }

        if (searchResult.size() == 0) {
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(new NoCurseModsPanel(!this.searchField.getText().isEmpty()), BorderLayout.CENTER);
        } else {
            contentPanel.setLayout(new WrapLayout());

            searchResult.forEach(mod -> {
                contentPanel.add(new ModCheckSearchHitCard(mod, e -> {
                    final ProgressDialog<ModCheckProject> modCheckProjectLookupDialog = new ProgressDialog<>(
                            GetText.tr("Getting Mod Information"), 0, GetText.tr("Getting Mod Information"),
                            "Aborting Getting Mod Information");

                    modCheckProjectLookupDialog.addThread(new Thread(() -> {
                        modCheckProjectLookupDialog.setReturnValue(mod);
                        modCheckProjectLookupDialog.close();
                    }));

                    modCheckProjectLookupDialog.start();

                    ModCheckProject modCheckProject = modCheckProjectLookupDialog.getReturnValue();

                    if (modCheckProject == null) {
                        DialogManager.okDialog().setTitle(GetText.tr("Error Getting Mod Information"))
                                .setContent(new HTMLBuilder().center().text(GetText.tr(
                                        "There was an error getting mod information from Modrinth. Please try again later."))
                                        .build())
                                .setType(DialogManager.ERROR).show();
                        return;
                    }

                    ProgressDialog<?> dialog = new ProgressDialog<>(GetText.tr("Installing " + mod.getName()));
                    dialog.addThread(new Thread(() -> {
                        instance.addFileFromModCheck(mod, dialog);
                        dialog.close();
                    }));
                    dialog.start();
                }), gbc);

                gbc.gridy++;
            });
        }

        SwingUtilities.invokeLater(() -> jscrollPane.getVerticalScrollBar().setValue(0));

        revalidate();
        repaint();
    }
}
