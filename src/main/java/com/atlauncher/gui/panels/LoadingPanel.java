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
package com.atlauncher.gui.panels;

import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.utils.Utils;

@SuppressWarnings("serial")
public class LoadingPanel extends JPanel {
    private final JLabel label;

    public LoadingPanel() {
        this(GetText.tr("Loading..."));
    }

    public LoadingPanel(String text) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        ImageIcon imageIcon = Utils.getIconImage("/assets/image/loading-bars.gif");

        JLabel iconLabel = new JLabel();
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        iconLabel.setIcon(imageIcon);
        imageIcon.setImageObserver(iconLabel);

        label = new JLabel(text);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setAlignmentY(Component.CENTER_ALIGNMENT);

        add(iconLabel);
        add(label);
    }

    public void updateText(String text) {
        label.setText(text);
    }
}
