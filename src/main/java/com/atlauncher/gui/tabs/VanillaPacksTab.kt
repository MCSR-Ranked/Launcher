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
package com.atlauncher.gui.tabs

import com.atlauncher.App
import com.atlauncher.constants.UIConstants
import com.atlauncher.data.MCVersionRow
import com.atlauncher.data.minecraft.loaders.LoaderType
import com.atlauncher.data.minecraft.loaders.LoaderVersion
import com.atlauncher.evnt.listener.RelocalizationListener
import com.atlauncher.evnt.manager.RelocalizationManager
import com.atlauncher.utils.ComboItem
import com.atlauncher.viewmodel.base.IVanillaPacksViewModel
import com.atlauncher.viewmodel.impl.VanillaPacksViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.mini2Dx.gettext.GetText
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableModel
import kotlin.math.max
import kotlin.math.min

class VanillaPacksTab : JPanel(BorderLayout()), Tab, RelocalizationListener {
    private val nameField = JTextField(32)
    private val descriptionField = JTextArea(2, 40)

    private fun getReleasesText() = GetText.tr("Releases")
    private val minecraftVersionReleasesFilterCheckbox = JCheckBox(getReleasesText())

    private var minecraftVersionTable: JTable? = null
    private var minecraftVersionTableModel: DefaultTableModel? = null
    private val loaderTypeButtonGroup = ButtonGroup()

    private val loaderTypeFabricRadioButton = JRadioButton("Fabric")
    private val loaderTypeLegacyFabricRadioButton = JRadioButton("Legacy Fabric")
    private val loaderVersionsDropDown = JComboBox<ComboItem<LoaderVersion?>>()

    private fun getCreateInstanceText() = GetText.tr("Create Instance")
    private val createInstanceButton = JButton(getCreateInstanceText())
    private val viewModel: IVanillaPacksViewModel = VanillaPacksViewModel()
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        name = "vanillaPacksPanel"
        setupMainPanel()
        setupBottomPanel()
        RelocalizationManager.addListener(this)
    }

    private fun setupMainPanel() {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        // Name
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.insets = UIConstants.LABEL_INSETS
        gbc.anchor = GridBagConstraints.EAST
        val nameLabel = JLabel(GetText.tr("Instance Name") + ":")
        mainPanel.add(nameLabel, gbc)
        gbc.gridx++
        gbc.insets = UIConstants.FIELD_INSETS
        gbc.anchor = GridBagConstraints.BASELINE_LEADING
        scope.launch {
            viewModel.name.collect {
                nameField.text = it
            }
        }
        nameField.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent?) {
                viewModel.setName(nameField.text)
            }
        })
        mainPanel.add(nameField, gbc)

        // Description
        gbc.gridx = 0
        gbc.gridy++
        gbc.insets = UIConstants.LABEL_INSETS
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING
        val descriptionLabel = JLabel(GetText.tr("Description") + ":")
        mainPanel.add(descriptionLabel, gbc)
        gbc.gridx++
        gbc.insets = UIConstants.FIELD_INSETS
        gbc.anchor = GridBagConstraints.BASELINE_LEADING
        val descriptionScrollPane = JScrollPane(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        )
        descriptionScrollPane.preferredSize = Dimension(450, 80)
        descriptionScrollPane.setViewportView(descriptionField)

        scope.launch {
            viewModel.description.collect {
                descriptionField.text = it
            }
        }
        descriptionField.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent?) {
                viewModel.setDescription(descriptionField.text)
            }
        })
        mainPanel.add(descriptionScrollPane, gbc)

        // Minecraft Version
        gbc.gridx = 0
        gbc.gridy += 2
        gbc.insets = UIConstants.LABEL_INSETS
        gbc.anchor = GridBagConstraints.NORTHEAST
        val minecraftVersionPanel = JPanel()
        minecraftVersionPanel.layout = BoxLayout(minecraftVersionPanel, BoxLayout.Y_AXIS)
        val minecraftVersionLabel = JLabel(GetText.tr("Minecraft Version") + ":")
        minecraftVersionPanel.add(minecraftVersionLabel)
        minecraftVersionPanel.add(Box.createVerticalStrut(20))
        val minecraftVersionFilterPanel = JPanel()
        minecraftVersionFilterPanel.layout = BoxLayout(minecraftVersionFilterPanel, BoxLayout.Y_AXIS)
        val minecraftVersionFilterLabel = JLabel(GetText.tr("Filter"))
        scope.launch {
            viewModel.font.collect {
                minecraftVersionFilterLabel.font = it
            }
        }
        minecraftVersionFilterPanel.add(minecraftVersionFilterLabel)

        // Release checkbox
        setupReleaseCheckbox(minecraftVersionFilterPanel)

        minecraftVersionPanel.add(minecraftVersionFilterPanel)
        mainPanel.add(minecraftVersionPanel, gbc)
        gbc.gridx++
        gbc.insets = UIConstants.FIELD_INSETS
        gbc.anchor = GridBagConstraints.BASELINE_LEADING
        val minecraftVersionScrollPane = JScrollPane(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        )
        minecraftVersionScrollPane.preferredSize = Dimension(450, 300)
        setupMinecraftVersionsTable()
        minecraftVersionScrollPane.setViewportView(minecraftVersionTable)
        mainPanel.add(minecraftVersionScrollPane, gbc)

        // Loader Type
        gbc.gridx = 0
        gbc.gridy++
        gbc.insets = UIConstants.LABEL_INSETS
        gbc.anchor = GridBagConstraints.EAST
        val loaderTypeLabel = JLabel(GetText.tr("Loader"))
        mainPanel.add(loaderTypeLabel, gbc)
        gbc.gridx++
        gbc.insets = UIConstants.FIELD_INSETS
        gbc.anchor = GridBagConstraints.BASELINE_LEADING
        loaderTypeButtonGroup.add(loaderTypeFabricRadioButton)
        //loaderTypeButtonGroup.add(loaderTypeLegacyFabricRadioButton)
        val loaderTypePanel = JPanel(FlowLayout())

        setupLoaderFabricButton(loaderTypePanel)
        //setupLoaderLegacyFabricButton(loaderTypePanel)

        loaderTypeButtonGroup.setSelected(loaderTypeFabricRadioButton.model, true)
        viewModel.setLoaderType(LoaderType.FABRIC)

        mainPanel.add(loaderTypePanel, gbc)

        // Loader Version
        gbc.gridx = 0
        gbc.gridy++
        gbc.insets = UIConstants.LABEL_INSETS
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING
        val loaderVersionLabel = JLabel(GetText.tr("Loader Version") + ":")
        mainPanel.add(loaderVersionLabel, gbc)
        gbc.gridx++
        gbc.insets = UIConstants.FIELD_INSETS
        gbc.anchor = GridBagConstraints.BASELINE_LEADING
        scope.launch {
            viewModel.loaderVersionsDropDownEnabled.collect {
                loaderVersionsDropDown.isEnabled = it
            }
        }
        scope.launch {
            viewModel.loaderVersions.collectLatest { loaderVersions ->
                loaderVersionsDropDown.removeAllItems()
                if (loaderVersions == null) {
                    setEmpty()
                } else {
                    var loaderVersionLength = 0

                    loaderVersions.forEach { version ->
                        // ensures that font width is taken into account
                        loaderVersionLength = max(
                            loaderVersionLength,
                            getFontMetrics(App.THEME.normalFont)
                                .stringWidth(version.toString()) + 25
                        )

                        loaderVersionsDropDown.addItem(
                            ComboItem(
                                version,
                                version.version
                            )
                        )
                    }

                    // ensures that the dropdown is at least 200 px wide
                    loaderVersionLength = max(200, loaderVersionLength)

                    // ensures that there is a maximum width of 400 px to prevent overflow
                    loaderVersionLength = min(400, loaderVersionLength)
                    loaderVersionsDropDown.preferredSize = Dimension(loaderVersionLength, 23)

                    viewModel.selectedLoaderVersion.collect {
                        if (it != null)
                            loaderVersionsDropDown.selectedIndex =
                                loaderVersions.indexOf(it)
                    }
                }
            }
        }
        scope.launch {
            viewModel.loaderLoading.collect {
                loaderVersionsDropDown.removeAllItems()
                if (it) {
                    loaderVersionsDropDown.addItem(ComboItem(null, GetText.tr("Getting Loader Versions")))
                } else {
                    setEmpty()
                }
            }
        }
        mainPanel.add(loaderVersionsDropDown, gbc)
        add(mainPanel, BorderLayout.CENTER)
    }

    private fun setEmpty() {
        loaderVersionsDropDown.addItem(ComboItem(null, GetText.tr("Select Loader First")))

    }

    private fun setupLoaderLegacyFabricButton(loaderTypePanel: JPanel) {
        scope.launch {
            viewModel.loaderTypeLegacyFabricSelected.collect {
                loaderTypeLegacyFabricRadioButton.isSelected = it
            }
        }
        scope.launch {
            viewModel.loaderTypeLegacyFabricEnabled.collect {
                loaderTypeLegacyFabricRadioButton.isEnabled = it
            }
        }
        scope.launch {
            viewModel.isLegacyFabricVisible.collect {
                loaderTypeLegacyFabricRadioButton.isVisible = it
            }
        }
        loaderTypeLegacyFabricRadioButton.addActionListener {
            viewModel.setLoaderType(
                LoaderType.LEGACY_FABRIC
            )
        }
        if (viewModel.showLegacyFabricOption) {
            loaderTypePanel.add(loaderTypeLegacyFabricRadioButton)
        }
    }

    private fun setupLoaderFabricButton(loaderTypePanel: JPanel) {
        scope.launch {
            viewModel.loaderTypeFabricSelected.collect {
                loaderTypeFabricRadioButton.isSelected = it
            }
        }
        scope.launch {
            viewModel.loaderTypeFabricEnabled.collect {
                loaderTypeFabricRadioButton.isEnabled = it
            }
        }
        scope.launch {
            viewModel.isFabricVisible.collect {
                loaderTypeFabricRadioButton.isVisible = it
            }
        }
        loaderTypeFabricRadioButton.addActionListener {
            viewModel.setLoaderType(
                LoaderType.FABRIC
            )
        }
        if (viewModel.showFabricOption) {
            loaderTypePanel.add(loaderTypeFabricRadioButton)
        }
    }


    private fun setupReleaseCheckbox(minecraftVersionFilterPanel: JPanel) {
        scope.launch {
            viewModel.releaseSelected.collect {
                minecraftVersionReleasesFilterCheckbox.isSelected = it
            }
        }
        scope.launch {
            viewModel.releaseEnabled.collect {
                minecraftVersionReleasesFilterCheckbox.isEnabled = it
            }
        }
        minecraftVersionReleasesFilterCheckbox.isSelected = true
        minecraftVersionReleasesFilterCheckbox.addActionListener {
            viewModel.setReleaseSelected(minecraftVersionReleasesFilterCheckbox.isSelected)
        }
        if (viewModel.showReleaseOption) {
            minecraftVersionFilterPanel.add(minecraftVersionReleasesFilterCheckbox)
        }
    }

    private fun setupMinecraftVersionsTable() {
        minecraftVersionTableModel = object : DefaultTableModel(
            arrayOf<Array<String>>(), arrayOf(GetText.tr("Version"), GetText.tr("Released"), GetText.tr("Type"))
        ) {
            override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
                return false
            }
        }

        minecraftVersionTable = JTable(minecraftVersionTableModel)
        minecraftVersionTable!!.tableHeader.reorderingAllowed = false
        val sm = minecraftVersionTable!!.selectionModel
        sm.addListSelectionListener(ListSelectionListener { e ->
            if (e.valueIsAdjusting) {
                return@ListSelectionListener
            }
            val lsm = e.source as ListSelectionModel

            val minIndex = e.firstIndex
            val maxIndex = e.lastIndex
            for (i in minIndex..maxIndex) {
                if (lsm.isSelectedIndex(i)) {
                    viewModel.setSelectedMinecraftVersion(
                        minecraftVersionTableModel!!.getValueAt(
                            i, 0
                        ) as String
                    )
                }
            }
        })
        scope.launch {
            viewModel.minecraftVersions.collectLatest { minecraftVersions ->

                // remove all rows
                val rowCount = minecraftVersionTableModel?.rowCount ?: 0
                if (rowCount > 0) {
                    for (i in rowCount - 1 downTo 0) {
                        minecraftVersionTableModel?.removeRow(i)
                    }
                }

                minecraftVersions.forEach { row: MCVersionRow ->
                    minecraftVersionTableModel?.addRow(
                        arrayOf(
                            row.id,
                            row.date,
                            row.type
                        )
                    )
                }

                // refresh the table
                minecraftVersionTable?.revalidate()
            }
        }
        scope.launch {
            viewModel.selectedMinecraftVersionIndex.collect {
                if (it < (minecraftVersionTable?.rowCount ?: 0)) {
                    minecraftVersionTable?.setRowSelectionInterval(it, it)
                    minecraftVersionTable?.revalidate()
                }
            }
        }

        val cm = minecraftVersionTable!!.columnModel
        cm.getColumn(0).resizable = false
        cm.getColumn(1).resizable = false
        cm.getColumn(1).maxWidth = 200
        cm.getColumn(2).resizable = false
        cm.getColumn(2).maxWidth = 200
        minecraftVersionTable!!.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        minecraftVersionTable!!.showVerticalLines = false
    }

    private fun setupBottomPanel() {
        val bottomPanel = JPanel(FlowLayout())
        bottomPanel.add(createInstanceButton)
        createInstanceButton.addActionListener { viewModel.createInstance() }
        add(bottomPanel, BorderLayout.SOUTH)
    }

    override fun getTitle(): String {
        return GetText.tr("Create Instance")
    }

    override fun getAnalyticsScreenViewName(): String {
        return "Create Instance"
    }

    override fun onRelocalization() {
        minecraftVersionReleasesFilterCheckbox.text = getReleasesText()
        createInstanceButton.text = getCreateInstanceText()
    }

}