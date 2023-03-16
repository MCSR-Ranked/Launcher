/*
 * MCSR Ranked Launcher - https://github.com/RedLime/MCSR-Ranked-Launcher
 * Copyright (C) 2023 ATLauncher
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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.constants.Constants;
import com.atlauncher.constants.UIConstants;
import com.atlauncher.data.Language;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.utils.Utils;

public class SetupDialog extends JDialog implements RelocalizationListener {
    private static final long serialVersionUID = -2931970914611329658L;

    private final JLabel setupLabel;

    private final JLabel languageLabel;
    private final JComboBox<String> language;

    private final JButton saveButton;

    public SetupDialog() {
        // #. {0} is the name of the launcher (ATLauncher)
        super(null, GetText.tr("{0} Setup", Constants.LAUNCHER_NAME), ModalityType.DOCUMENT_MODAL);
        this.requestFocus();
        this.setSize(500, 250);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setIconImage(Utils.getImage("/assets/image/icon.png"));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        // Top Panel Stuff
        JPanel top = new JPanel();

        // #. {0} is the name of the launcher (ATLauncher)
        setupLabel = new JLabel(GetText.tr("Setting up {0}", Constants.LAUNCHER_NAME));
        top.add(setupLabel);

        // Middle Panel Stuff
        JPanel middle = new JPanel();
        middle.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
        languageLabel = new JLabel(GetText.tr("Language") + ": ");
        middle.add(languageLabel, gbc);

        gbc.gridx++;
        gbc.insets = UIConstants.FIELD_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        language = new JComboBox<>(Language.locales.stream().filter(l -> l == Locale.ENGLISH || Language.languages.containsValue(l)).map(locale -> locale.getDisplayName(locale)).toArray(String[]::new));
        language.setSelectedItem(Language.selected);
        language.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Language.setLanguage((String) language.getSelectedItem());
                App.THEME.updateUIFonts();
            }
        });
        middle.add(language, gbc);

        gbc.gridx++;
        gbc.insets = UIConstants.FLOW_FIELD_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;

        // Bottom Panel Stuff
        JPanel bottom = new JPanel();
        bottom.setLayout(new FlowLayout());
        saveButton = new JButton(GetText.tr("Save"));
        saveButton.addActionListener(e -> {
            Language.setLanguage((String) language.getSelectedItem());
            App.settings.language = (String) language.getSelectedItem();
            App.settings.firstTimeRun = false;
            App.settings.save();

            setVisible(false);
            dispose();
        });
        bottom.add(saveButton);

        add(top, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg0) {
                System.exit(0);
            }
        });

        RelocalizationManager.addListener(this);

        setVisible(true);
    }

    @Override
    public void onRelocalization() {
        setupLabel.setText(GetText.tr("Setting up {0}", Constants.LAUNCHER_NAME));
        languageLabel.setText(GetText.tr("Language") + ": ");
        saveButton.setText(GetText.tr("Save"));
    }
}
